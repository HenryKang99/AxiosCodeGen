package site.henrykang.plugin.action;

import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.apache.velocity.VelocityContext;
import org.jetbrains.annotations.NotNull;
import site.henrykang.plugin.entity.ClassInfo;
import site.henrykang.plugin.entity.Constant;
import site.henrykang.plugin.entity.MethodInfo;
import site.henrykang.plugin.entity.PojoInfo;
import site.henrykang.plugin.service.JsDocTypeResolver;
import site.henrykang.plugin.service.PropertiesManager;
import site.henrykang.plugin.ui.PreviewDialog;
import site.henrykang.plugin.util.TemplateUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 从 Controller 方法生成 Axios 请求代码片段
 */
public class FunctionGenIntention extends BaseElementAtCaretIntentionAction {

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class);
        assert psiMethod != null;
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiMethod, PsiClass.class);
        assert psiClass != null;
        // 是否清空缓存
        String isClearCache = PropertiesManager.getInstance(project).get(Constant.CACHE_KEY_IS_CLEAR_CACHE);
        if (Boolean.parseBoolean(isClearCache)) {
            JsDocTypeResolver.getInstance(project).invalidateCache();
        }
        // 解析
        MethodInfo methodInfo = MethodInfo.handlePsiMethod(psiMethod);
        List<PojoInfo> pojoList = PojoInfo.handlePojoInfo(psiClass.getProject(), methodInfo.getAllParams().stream().flatMap(param -> param.getPojoSet().stream()).collect(Collectors.toSet()));
        // 模板填充
        VelocityContext ctx = new VelocityContext();
        ctx.put("clazz",  new ClassInfo().setMethodList(List.of(methodInfo)));
        ctx.put("pojoList", pojoList);
        String resultText = TemplateUtil.merge(ctx, Constant.METHOD_TEMPLATE_NAME);
        // 代码格式化
        PsiFile formattedPsiFile = PsiFileFactory.getInstance(project).createFileFromText("temp.js", JavaScriptFileType.INSTANCE, resultText);
        CodeStyleManager.getInstance(project).reformat(formattedPsiFile);

        new PreviewDialog(project, formattedPsiFile).show();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement psiElement) {
        return ReadAction.compute(() -> {
            if (DumbService.isDumb(project) || !(psiElement instanceof PsiIdentifier) || psiElement.getLanguage() != JavaLanguage.INSTANCE) {
                return false;
            }
            // 光标必须在方法名上
            PsiMethod method = PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class);
            if (method == null || !Objects.equals(method.getNameIdentifier(), psiElement)) {
                return false;
            }
            // 方法必须被 @RequestMapping 等修饰
            return Constant.MAPPING_ANNO_SET.stream().anyMatch(method::hasAnnotation);
        });
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return Constant.PLUGIN_NAME;
    }

    @Override
    public @NotNull @IntentionName String getText() {
        return "Generate axios code";
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    /**
     * 预览模式返回内容
     */
    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return new IntentionPreviewInfo.Html("<p>Generate axios code snippets for this method</p>");
    }

}
