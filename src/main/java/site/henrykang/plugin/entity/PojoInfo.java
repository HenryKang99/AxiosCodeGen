package site.henrykang.plugin.entity;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections.set.UnmodifiableSet;
import site.henrykang.plugin.service.JsDocTypeResolver;
import site.henrykang.plugin.util.MyPsiUtil;

import java.util.*;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
public class PojoInfo {

    /** pojo 短类名 */
    private String name;
    /** 注释 */
    private String comment;
    /** 属性集合 */
    private List<FieldVo> fieldList;

    private List<String> typeParamList;

    @Data
    @AllArgsConstructor
    public static class FieldVo {

        /** 属性名 */
        private String name;
        /** 注释 */
        private String comment;
        /** js 参数类型 */
        private String jsType;

    }

    /**
     * 解析 Java Pojo 用于生成 JSDoc @typedef 定义，形如：
     * <code>
     * <p>/**
     * <p> * comment
     * <p> * @template K
     * <p> * @template T
     * <p> * @typedef {Object} Pojo
     * <p> * @property {string} id
     * <p> * @property {string} name
     * <p> * @property {Other<Xxx>} other
     * <p> * @property {K} one
     * <p> * @property {T[]} list
     * <p> *\/
     * </code>
     */
    public static List<PojoInfo> handlePojoInfo(Project project, Set<String> pojoSet) {
        if (pojoSet == null || pojoSet.isEmpty()) return Collections.emptyList();

        List<PojoInfo> resultList = new ArrayList<>();
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        ProjectAndLibrariesScope searchScope = new ProjectAndLibrariesScope(project);

        // 遍历所有类，收集属性的类型，每遍历一轮，通过差集判断是否有新增元素，若有则进行新一轮遍历
        Set<String> visitedSet = new HashSet<>(pojoSet);
        Set<String> currentSet = Set.copyOf(visitedSet);
        Set<String> newSet = new HashSet<>();
        while (true) {
            List<PojoInfo> list = currentSet.stream()
                .map(qualifiedName -> javaPsiFacade.findClass(qualifiedName, searchScope))
                .map(findClass -> {
                    if (findClass == null || findClass.isEnum() || findClass.isInterface()) {
                        return null;
                    }
                    String name = findClass.getName();
                    String comment = MyPsiUtil.getSimpleComment(findClass.getAnnotation(Constant.ANNO_SWAGGER_SCHEMA), List.of("title", "description"), findClass.getDocComment());
                    // 泛型信息：E、T...
                    List<String> typeParamList = Arrays.stream(findClass.getTypeParameters()).map(PsiTypeParameter::getName).toList();
                    // 属性信息
                    PsiField[] allFields = findClass.getAllFields();
                    List<FieldVo> attributeList = Stream.of(allFields)
                        .filter(psiField -> Stream.of("static", "final", "transient").noneMatch(psiField::hasModifierProperty))
                        .map(psiField -> {
                            String fieldComment = MyPsiUtil.getSimpleComment(psiField.getAnnotation(Constant.ANNO_SWAGGER_SCHEMA), List.of("title", "description"), psiField.getDocComment());
                            Pair<String, Set<String>> resolve = JsDocTypeResolver.getInstance(psiField.getProject()).resolve(psiField.getType());
                            newSet.addAll(resolve.getSecond());
                            return new FieldVo(psiField.getName(), fieldComment, resolve.getFirst());
                        })
                        .toList();
                    return new PojoInfo(name, comment, attributeList, typeParamList);
                })
                .filter(Objects::nonNull)
                .toList();
            resultList.addAll(list);
            newSet.removeAll(visitedSet);
            if (!newSet.isEmpty()) {
                visitedSet.addAll(newSet); // 标记为已访问
                currentSet = Set.copyOf(newSet);
                newSet.clear();
                continue;
            }
            break;
        }

        return resultList;
    }

}
