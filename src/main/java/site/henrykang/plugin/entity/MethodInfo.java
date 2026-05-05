package site.henrykang.plugin.entity;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import site.henrykang.plugin.service.JsDocTypeResolver;
import site.henrykang.plugin.util.MyPsiUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class MethodInfo {

    /** 注释 */
    private String comment;
    /** 方法名 */
    private String name;
    /** 请求方式 get delete post put patch */
    private String method;
    /** 请求路径，从 @XxxMapping 提取，不包含前缀和后缀 / */
    private String uri;

    /** 路径参数 */
    private List<ParamInfo> pathParams;
    /** 查询参数 */
    private List<ParamInfo> queryParams;
    /** multipart 参数 */
    private List<ParamInfo> partParams;
    /** urlencoded 参数 */
    private List<ParamInfo> urlencodedParams;
    /** 请求体参数 */
    private ParamInfo bodyParam;

    /** 所有参数 */
    private List<ParamInfo> allParams;
    /** 参数个数 */
    private int paramsCnt;
    /** 所有参数名称 join */
    private String allParamsNameStr;
    /** 查询参数名称 join */
    private String queryParamsNameStr;

    /** 返回值的 JS 类型，用于 JSDoc @returns */
    private String returnJsType;
    /** 返回值涉及的 pojo 全类名集合 */
    private Set<String> returnPojoSet;

    public static MethodInfo handlePsiMethod(@NotNull PsiMethod psiMethod) {
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.comment = MyPsiUtil.getSimpleComment(psiMethod.getAnnotation(Constant.ANNO_SWAGGER_OPERATION), List.of("summary"), psiMethod.getDocComment());
        methodInfo.name = psiMethod.getName();
        // 提取请求方式和路径
        Pair<String, String> pair = MyPsiUtil.getRequestMappingInfo(psiMethod);
        methodInfo.method = pair.getFirst();
        methodInfo.uri = pair.getSecond().replaceAll("\\{", "\\${");

        methodInfo.allParams = Arrays.stream(psiMethod.getParameterList().getParameters())
            // 处理每个参数
            .map(psiParameter -> ParamInfo.handlePsiParameter(psiMethod, psiParameter))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        Map<String, List<ParamInfo>> map = methodInfo.allParams.stream().collect(Collectors.groupingBy(ParamInfo::getAnnoBindType));
        methodInfo.pathParams = map.getOrDefault("PathVariable", new ArrayList<>());
        methodInfo.queryParams = map.getOrDefault("RequestParam", new ArrayList<>());
        methodInfo.partParams = map.getOrDefault("RequestPart", new ArrayList<>());
        methodInfo.urlencodedParams = new ArrayList<>();
        methodInfo.bodyParam = Optional.ofNullable(map.get("RequestBody")).map(list -> list.get(0)).orElse(null);

        /*
         * 处理 NoBindType 参数，即无参数绑定注解的参数。
         * 无注解的自动绑定方式支持：url查询参数、multipart/form-data、application/x-www-form-urlencoded
         * 对于 pojo 不支持 json 自动解析绑定，支持参数层级嵌套，例如：address.city=shanghai、friends[0].name=henry
         * 因此，对于此类参数的处理：
         * 1. get/delete 类型请求，直接将参数添加到 queryParams
         * 2. 否则统一使用 application/x-www-form-urlencoded
         */
        List<ParamInfo> noBindTypeParams = map.getOrDefault("NoBindType", Collections.emptyList());
        for (ParamInfo noBindParam : noBindTypeParams) {
            if ("get".equals(methodInfo.method) || "delete".equals(methodInfo.method)) {
                methodInfo.queryParams.add(noBindParam);
            } else {
                methodInfo.urlencodedParams.add(noBindParam);
            }
        }

        methodInfo.allParamsNameStr = methodInfo.allParams.stream().map(ParamInfo::getName).collect(Collectors.joining(", "));
        methodInfo.queryParamsNameStr = methodInfo.queryParams.stream()
            .map(paramInfo -> {
                String prefix = "...";
                return paramInfo.getIsPojo() ? prefix + paramInfo.getName() : paramInfo.getName();
            })
            .collect(Collectors.joining(", "));
        methodInfo.paramsCnt = methodInfo.allParams.size();

        // 处理返回值类型
        PsiType returnType = psiMethod.getReturnType();
        if (returnType != null) {
            Pair<String, Set<String>> returnTypePair = JsDocTypeResolver.getInstance(psiMethod.getProject()).resolve(returnType);
            methodInfo.returnJsType = returnTypePair.getFirst();
            methodInfo.returnPojoSet = returnTypePair.getSecond();
        }

        return methodInfo;
    }

}
