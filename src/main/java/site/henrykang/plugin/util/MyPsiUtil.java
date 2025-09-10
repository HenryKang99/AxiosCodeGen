package site.henrykang.plugin.util;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.javadoc.PsiDocParamRef;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import site.henrykang.plugin.entity.Constant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MyPsiUtil {

    private static final Logger LOG = Logger.getInstance(MyPsiUtil.class);

    /**
     * 从注释中提取第一行非空描述
     */
    @NotNull
    public static String getFirstLineComment(PsiDocComment psiDocComment) {
        if (psiDocComment == null) return "";
        return Arrays.stream(psiDocComment.getDescriptionElements())
            .filter(p -> p instanceof PsiDocToken && StringUtil.isNotBlank(p.getText()))
            .findFirst()
            .map(PsiElement::getText)
            .map(String::trim)
            .orElse("");
    }

    /**
     * 从注解中提取指定属性值，如果值为空(null 或 "")，返回默认值
     */
    @NotNull
    public static String getAnnotationValueLiteralStrOrDefault(PsiAnnotation psiAnnotation, String attrName, String defaultValue) {
        return Optional.ofNullable(psiAnnotation)
            .map(anno -> anno.findAttributeValue(attrName))
            .map(value -> {
                // 适用于 @Xxx(attr1="xx", attr2=true) -> xx
                if (value instanceof PsiLiteralExpression) {
                    Object obj = ((PsiLiteralExpression) value).getValue();
                    // 空字符串也返回 null
                    if (obj != null && StringUtil.isNotBlank(obj.toString())) return obj.toString().trim();
                    return null;
                }
                // 适用于 @Xxx(attr1=xx.class) -> xx
                else if (value instanceof PsiReferenceExpression) {
                    return ((PsiReferenceExpression) value).getReferenceName();
                }
                return null;
            }).orElse(defaultValue == null ? "" : defaultValue);
    }

    /**
     * 从注解中提取指定属性值，没有则返回 ""
     */
    @NotNull
    public static String getAnnotationValueLiteralStr(PsiAnnotation psiAnnotation, String attrName) {
        return getAnnotationValueLiteralStrOrDefault(psiAnnotation, attrName, null);
    }

    /**
     * 从注解中提取指定多个属性值
     */
    @NotNull
    public static List<String> getAnnotationValueLiteralStr(PsiAnnotation psiAnnotation, List<String> attrNames) {
        return attrNames.stream()
            .map(attrName -> getAnnotationValueLiteralStr(psiAnnotation, attrName))
            .toList();
    }

    /**
     * 优先从注解中提取描述，若指定多个属性则将非空结果使用逗号拼接，如果没有则从注释中提取
     */
    public static String getSimpleComment(PsiAnnotation psiAnnotation, List<String> attrNames, PsiDocComment psiDocComment) {
        String str = getAnnotationValueLiteralStr(psiAnnotation, attrNames).stream()
            .filter(StringUtil::isNotBlank)
            .collect(Collectors.joining(", "));
        return StringUtil.isBlank(str) ? getFirstLineComment(psiDocComment) : str;
    }

    /**
     * 从方法注释中获取 @param 注释，返回属性名到注释的映射
     */
    public static Map<String, String> getParamCommentsMap(PsiDocComment psiDocComment) {
        if (psiDocComment == null) return Collections.emptyMap();
        Map<String, String> resultMap = new HashMap<>();

        Arrays.stream(psiDocComment.getTags())
            .filter(tag -> Objects.equals(tag.getName(), "param"))
            .forEach(tag -> {
                PsiElement[] elements = tag.getDataElements();
                if (elements.length == 0) return;
                if (!(elements[0] instanceof PsiDocParamRef)) return;

                String name = elements[0].getText();
                if (StringUtil.isBlank(name)) return;
                String comment = Arrays.stream(elements)
                    .skip(1)
                    .map(PsiElement::getText)
                    .filter(StringUtil::isNotBlank)
                    .collect(Collectors.joining(" "));

                resultMap.put(name, comment);
            });

        return resultMap;
    }

    /**
     * 从 @XxxMapping 中提取请求方式和路径，返回 [method, path]
     */
    public static Pair<String, String> getRequestMappingInfo(@NotNull PsiMethod psiMethod) {
        String method = "";
        String path = "";
        // 先查找 RequestMapping
        PsiAnnotation anno = psiMethod.getAnnotation(Constant.ANNO_REQUEST_MAPPING);
        if (anno != null) {
            method = getAnnotationValueLiteralStr(anno, "method").toLowerCase();
            path = StringUtil.trimSlashes(getAnnotationValueLiteralStr(anno, "value"));
        }
        // 再查找其他 Mapping
        else {
            List<String> otherMappings = new ArrayList<>(Constant.MAPPING_ANNO_SET);
            otherMappings.remove(Constant.ANNO_REQUEST_MAPPING);
            for (String mapping : otherMappings) {
                anno = psiMethod.getAnnotation(mapping);
                if (anno != null) {
                    method = switch (mapping) {
                        case Constant.ANNO_GET_MAPPING -> "get";
                        case Constant.ANNO_POST_MAPPING -> "post";
                        case Constant.ANNO_PUT_MAPPING -> "put";
                        case Constant.ANNO_DELETE_MAPPING -> "delete";
                        case Constant.ANNO_PATCH_MAPPING -> "patch";
                        default -> "";
                    };
                    path = StringUtil.trimSlashes(getAnnotationValueLiteralStr(anno, "value"));
                    break;
                }
            }
        }
        return Pair.pair(method, path);
    }

    /**
     * 从 Psi 元素尝试提取包名，未找到时返回 ""
     */
    public static String getPackageName(@NotNull PsiElement psiElement) {
        return Optional.ofNullable(PsiTreeUtil.getParentOfType(psiElement, PsiJavaFile.class))
            .map(PsiJavaFile::getPackageName)
            .orElse("");
    }

    /**
     * 写出内容到指定文件
     */
    public static void writeTextToFile(Project project, Path resultFilePath, String content) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                // 确保目录存在
                Files.createDirectories(resultFilePath.getParent());
                if (!Files.exists(resultFilePath)) Files.createFile(resultFilePath);
                VirtualFile resultVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(resultFilePath);
                assert resultVirtualFile != null;
                // 创建具有指定内容的 PsiFile, 并格式化
                PsiFile tempPsiFile = PsiFileFactory.getInstance(project).createFileFromText(resultFilePath.getFileName().toString(), JavaScriptFileType.INSTANCE, content);
                CodeStyleManager.getInstance(project).reformat(tempPsiFile);
                // 保存
                // resultVirtualFile.setBinaryContent(tempPsiFile.getText().getBytes(StandardCharsets.UTF_8));
                // resultVirtualFile.refresh(false, false);
                // 使用 Document 可以支持撤销、同步到已打开的文件等特性
                Document resultDoc = FileDocumentManager.getInstance().getDocument(resultVirtualFile);
                resultDoc.setText(tempPsiFile.getText());
                FileDocumentManager.getInstance().saveDocument(resultDoc);
            } catch (IOException e) {
                LOG.error(e);
                throw new RuntimeException(e);
            }
        });
    }

}
