package site.henrykang.plugin.action;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.apache.velocity.VelocityContext;
import org.jetbrains.annotations.NotNull;
import site.henrykang.plugin.entity.ClassInfo;
import site.henrykang.plugin.entity.Constant;
import site.henrykang.plugin.entity.PojoInfo;
import site.henrykang.plugin.service.JsDocTypeResolver;
import site.henrykang.plugin.service.PropertiesManager;
import site.henrykang.plugin.ui.SettingsDialog;
import site.henrykang.plugin.util.MyPsiUtil;
import site.henrykang.plugin.util.StringUtil;
import site.henrykang.plugin.util.TemplateUtil;
import site.henrykang.plugin.util.UiUtil;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 从 Controller.java 文件生成 Axios 请求代码
 */
public class FileGenAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(FileGenAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        assert project != null;
        PsiManager psiManager = PsiManager.getInstance(project);
        VirtualFile[] virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        assert virtualFiles != null;

        // 过滤出 Controller.java 文件，转换为 PsiClass 对象
        List<PsiClass> psiClassList = getControllerClass(virtualFiles, psiManager);
        if (psiClassList.isEmpty()) {
            UiUtil.showNotification(project, "No compliant files recognized", NotificationType.WARNING, TimeUnit.SECONDS.toMillis(3));
            return;
        }

        // 配置弹窗
        SettingsDialog dialog = new SettingsDialog(e.getProject());
        String savePath = "";
        String pojoPackages = "";
        Boolean isClearCache = false;
        if (dialog.showAndGet()) {
            savePath = dialog.getSavePath();
            pojoPackages = dialog.getPojoPackages();
            isClearCache = dialog.isClearCacheSelected();

            if (StringUtil.isBlank(savePath)) {
                UiUtil.showNotification(project, "No save path selected", NotificationType.WARNING, TimeUnit.SECONDS.toMillis(3));
                return;
            }
            if (StringUtil.isBlank(pojoPackages)) {
                pojoPackages = StringUtil.extractDomainPart(MyPsiUtil.getPackageName(psiClassList.get(0)));
                UiUtil.showNotification(project, "Default POJO packages is set to " + pojoPackages, NotificationType.WARNING, TimeUnit.SECONDS.toMillis(3));
            }

            PropertiesManager pm = PropertiesManager.getInstance(project);
            pm.put(Constant.CACHE_KEY_SAVE_PATH, savePath);
            pm.put(Constant.CACHE_KEY_POJO_PACKAGES, pojoPackages);
            pm.put(Constant.CACHE_KEY_IS_CLEAR_CACHE, isClearCache.toString());
            if (isClearCache) {
                JsDocTypeResolver.getInstance(project).invalidateCache();
            }
        } else {
            UiUtil.showNotification(project, "Action cancelled", NotificationType.WARNING, TimeUnit.SECONDS.toMillis(3));
            return;
        }

        // 遍历每个 PsiClass
        int errCnt = 0;
        int allCnt = psiClassList.size();
        for (int i = 0; i < allCnt; i++) {
            try {
                PsiClass psiClass = psiClassList.get(i);
                LOG.info("[" + (i + 1) + "/" + allCnt + "] handling: " + psiClass.getName());
                // 处理每个类，封装方法、参数信息
                ClassInfo classInfo = ClassInfo.handlePsiClass(psiClass);
                // 提取 JSDoc Pojo 类定义信息
                List<PojoInfo> pojoList = PojoInfo.handlePojoInfo(psiClass.getProject(), classInfo.getPojoSet());
                // 模板填充
                VelocityContext ctx = new VelocityContext();
                ctx.put("clazz", classInfo);
                ctx.put("pojoList", pojoList);
                String resultText = TemplateUtil.merge(ctx, Constant.FILE_TEMPLATE_NAME);
                // 写出文件
                String resultFileName = classInfo.getName().replace("Controller", "") + "Api.js";
                MyPsiUtil.writeTextToFile(project, Path.of(savePath, resultFileName), resultText);
                LOG.info("[" + (i + 1) + "/" + allCnt + "] completed: " + psiClass.getName());
            } catch (Exception ex) {
                errCnt++;
                LOG.error("Error occurred: " + ex.getMessage(), ex);
            }
        }

        UiUtil.showNotification(project, "Action completed, success[" + (allCnt - errCnt) + "/" + allCnt + "], please check: \n" + savePath, NotificationType.INFORMATION, TimeUnit.SECONDS.toMillis(3));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        if (e.getProject() == null || DumbService.isDumb(e.getProject())) presentation.setEnabled(false);

        AtomicBoolean isEnabled = new AtomicBoolean(false);

        // for EditorPopupMenu
        Optional.ofNullable(e.getData(PlatformDataKeys.EDITOR))
            .map(Editor::getVirtualFile)
            .filter(file -> file.getName().endsWith(".java"))
            .ifPresentOrElse(file -> isEnabled.set(true),
                // for ProjectViewPopupMenu
                () -> {
                    Object[] selected = e.getData(PlatformDataKeys.SELECTED_ITEMS);
                    if (selected != null && selected.length > 0) isEnabled.set(true);
                }
            );

        presentation.setEnabled(isEnabled.get());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 获取 Controller.java 文件
     */
    private static List<PsiClass> getControllerClass(VirtualFile[] virtualFiles, PsiManager psiManager) {
        return Arrays.stream(virtualFiles)
            // 提取文件
            .flatMap(file -> {
                if (!file.isDirectory()) return Stream.of(file);
                List<VirtualFile> flatten = new ArrayList<>();
                flattenFiles(file, flatten);
                return flatten.stream();
            })
            .filter(file -> file.getName().endsWith(".java"))
            .distinct()
            // 转换为 PsiClass 对象
            .flatMap(file -> {
                PsiFile psiFile = psiManager.findFile(file);
                if (!(psiFile instanceof PsiJavaFile psiJavaFile)) return null;
                return Stream.of(psiJavaFile.getClasses());
            })
            // 只保留被 RequestMapping 注解修饰的类
            .map(clazz -> {
                if (clazz == null) return null;
                return clazz.hasAnnotation(Constant.ANNO_REQUEST_MAPPING) ? clazz : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 递归获取目录下的所有文件
     */
    private static void flattenFiles(VirtualFile file, List<VirtualFile> result) {
        if (file == null) return;
        if (!file.isDirectory()) result.add(file);
        VirtualFile[] children = file.getChildren();
        if (children != null) {
            for (VirtualFile child : children) {
                flattenFiles(child, result);
            }
        }
    }

}
