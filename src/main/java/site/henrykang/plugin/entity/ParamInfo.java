package site.henrykang.plugin.entity;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import site.henrykang.plugin.service.JsDocTypeResolver;
import site.henrykang.plugin.service.PropertiesManager;
import site.henrykang.plugin.util.MyPsiUtil;
import site.henrykang.plugin.util.StringUtil;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
public class ParamInfo {

    /** 参数名称，优先取参数绑定注解的值，其次取参数名 */
    private String name;

    /** 是否必须 */
    private Boolean isRequired = true;

    /** 参数注释，优先取 Swagger 注解值，其次取方法注释值 */
    private String comment;

    /**
     * 参数绑定注解类型：
     * RequestParam、RequestBody、RequestPart、 PathVariable
     * 否则置为 NoBindType
     */
    private String annoBindType;

    /**
     * js 参数类型，用于 JSDoc 生成
     * @see JsDocTypeResolver
     */
    private String jsType;
    /** 参数涉及的 pojo 类型全类名 */
    private Set<String> pojoSet;

    private Boolean isArray = false;
    private Boolean isFile = false;
    private Boolean isPojo = false;

    public static ParamInfo handlePsiParameter(@NotNull PsiMethod psiMethod, @NotNull PsiParameter psiParameter) {
        ParamInfo paramInfo = new ParamInfo();
        // 参数注释
        Map<String, String> paramCommentsMap = MyPsiUtil.getParamCommentsMap(psiMethod.getDocComment());
        paramInfo.comment = MyPsiUtil.getAnnotationValueLiteralStrOrDefault(psiParameter.getAnnotation(Constant.ANNO_SWAGGER_PARAMETER), "description", paramCommentsMap.getOrDefault(psiParameter.getName(), ""));
        // 参数名称、绑定类型
        paramInfo.name = psiParameter.getName();
        for (String anno : Constant.PARAM_BIND_ANNO_SET) {
            PsiAnnotation psiAnno = psiParameter.getAnnotation(anno);
            if (psiAnno != null) {
                paramInfo.annoBindType = anno.substring(anno.lastIndexOf('.') + 1);
                paramInfo.isRequired = Boolean.valueOf(MyPsiUtil.getAnnotationValueLiteralStrOrDefault(psiAnno, "required", "true"));
                // 请求参数名可能和方法参数名不一致，即提取注解的 value 属性值
                if (!Objects.equals(anno, Constant.ANNO_REQUEST_BODY)) {
                    paramInfo.name = MyPsiUtil.getAnnotationValueLiteralStrOrDefault(psiAnno, "value", paramInfo.name);
                }
                break;
            }
        }
        // 如果没有被参数绑定注解修饰，则设置默认值
        if (StringUtil.isEmpty(paramInfo.annoBindType)) {
            // 检查是否在忽略列表中
            if (isIgnoredParameterType(psiParameter.getType(), psiParameter.getProject())) {
                return null;
            }
            // 设置默认值
            paramInfo.annoBindType = "NoBindType";
            paramInfo.isRequired = true;
        }

        // 映射为 JS 类型，并记录 Java 类型全类名
        Pair<String, Set<String>> resolve = JsDocTypeResolver.getInstance(psiMethod.getProject()).resolve(psiParameter.getType());
        paramInfo.jsType = resolve.getFirst();
        paramInfo.pojoSet = resolve.getSecond();

        paramInfo.isArray = paramInfo.jsType.contains("Array<");
        paramInfo.isFile = paramInfo.jsType.equals("File") || paramInfo.jsType.equals("Array<File>");
        paramInfo.isPojo = !paramInfo.jsType.equals("string")
            && !paramInfo.jsType.equals("number")
            && !paramInfo.jsType.equals("boolean")
            && !paramInfo.isFile;

        return paramInfo;
    }

    /**
     * 检查参数类型是否在忽略列表中
     * @param psiType 参数类型
     * @param project 项目
     * @return true 表示应该忽略该参数
     */
    private static boolean isIgnoredParameterType(PsiType psiType, Project project) {
        String qualifiedName = psiType.getCanonicalText();
        String ignoreTypes = PropertiesManager.getInstance(project)
            .get(Constant.CACHE_KEY_IGNORE_PARAM_TYPES);
        
        if (StringUtil.isBlank(ignoreTypes)) {
            return false;
        }

        ignoreTypes = ignoreTypes.replaceAll("\n", "").replaceAll("\r", "");

        // 解析配置的类型列表
        String[] patterns = ignoreTypes.split(",");
        
        for (String pattern : patterns) {
            if (StringUtil.isBlank(pattern)) {
                continue;
            }
            pattern = pattern.trim();
            if (pattern.endsWith("*")) {
                // 包名通配符匹配
                String prefix = pattern.substring(0, pattern.length() - 1);
                if (qualifiedName.startsWith(prefix)) {
                    return true;
                }
            } else {
                // 精确匹配
                if (qualifiedName.equals(pattern)) {
                    return true;
                }
            }
        }
        
        return false;
    }

}
