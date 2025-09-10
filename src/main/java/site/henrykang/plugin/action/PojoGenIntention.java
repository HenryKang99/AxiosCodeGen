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
import site.henrykang.plugin.entity.Constant;
import site.henrykang.plugin.entity.PojoInfo;
import site.henrykang.plugin.service.JsDocTypeResolver;
import site.henrykang.plugin.service.PropertiesManager;
import site.henrykang.plugin.ui.PreviewDialog;
import site.henrykang.plugin.util.TemplateUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 从 Controller.java 文件生成 Axios 请求代码片段
 */
public class PojoGenIntention extends BaseElementAtCaretIntentionAction {

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
        assert psiClass != null;
        // 是否清空缓存
        String isClearCache = PropertiesManager.getInstance(project).get(Constant.CACHE_KEY_IS_CLEAR_CACHE);
        if (Boolean.parseBoolean(isClearCache)) {
            JsDocTypeResolver.getInstance(project).invalidateCache();
        }
        String qualifiedName = Optional.ofNullable(psiClass.getQualifiedName()).orElse("");
        // 解析 pojo、模板填充
        List<PojoInfo> pojoList = PojoInfo.handlePojoInfo(psiClass.getProject(), Set.of(qualifiedName));
        VelocityContext ctx = new VelocityContext();
        ctx.put("pojoList", pojoList);
        String resultText = TemplateUtil.merge(ctx, Constant.DOC_TEMPLATE_NAME);
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
            // 光标必须在类名上
            PsiClass clazz = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
            return clazz != null && Objects.equals(clazz.getNameIdentifier(), psiElement);
        });
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return Constant.PLUGIN_NAME;
    }

    @Override
    public @NotNull @IntentionName String getText() {
        return "Generate JSDoc";
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
        return new IntentionPreviewInfo.Html("<p>Generate JSDoc for this POJO</p>");
    }

}
