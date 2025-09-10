package site.henrykang.plugin.entity;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import site.henrykang.plugin.util.MyPsiUtil;
import site.henrykang.plugin.util.StringUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
public class ClassInfo {

    /** 类名 */
    private String name = "";
    /** 注释，去除了注释标记，仅提取单行描述，优先从 @Tag 注解获取，然后才从句 JavaDoc 获取 */
    private String comment = "";
    /** 请求路径，从 @RequestMapping 提取，不包含前缀和后缀 / */
    private String reqPrefix = "";

    /** 方法列表 */
    private List<MethodInfo> methodList = Collections.emptyList();
    /** 参数涉及的 Pojo 类 */
    private Set<String> pojoSet;

    public static ClassInfo handlePsiClass(@NotNull PsiClass psiClass) {
        ClassInfo classInfo = new ClassInfo();
        classInfo.name = psiClass.getName();
        classInfo.comment = MyPsiUtil.getSimpleComment(psiClass.getAnnotation(Constant.ANNO_SWAGGER_TAG), List.of("name", "description"), psiClass.getDocComment());
        // 提取请求路径
        String prefix = MyPsiUtil.getAnnotationValueLiteralStr(psiClass.getAnnotation(Constant.ANNO_REQUEST_MAPPING), "value");
        classInfo.reqPrefix = StringUtil.trimSlashes(prefix);
        // 提取方法
        classInfo.methodList = PsiTreeUtil.getChildrenOfTypeAsList(psiClass, PsiMethod.class)
            .stream()
            // 只保留被 RequestMapping 等注解修饰的方法
            .filter(psiMethod -> Arrays.stream(psiMethod.getAnnotations()).anyMatch(anno -> Constant.MAPPING_ANNO_SET.contains(anno.getQualifiedName())))
            // 处理每个方法
            .map(MethodInfo::handlePsiMethod)
            .toList();
        // 提取所有参数涉及的 pojo 全类名
        classInfo.pojoSet = classInfo.methodList.stream()
            .flatMap(methodInfo -> methodInfo.getAllParams().stream())
            .flatMap(paramInfo -> paramInfo.getPojoSet().stream())
            .collect(Collectors.toSet());

        return classInfo;
    }

}
