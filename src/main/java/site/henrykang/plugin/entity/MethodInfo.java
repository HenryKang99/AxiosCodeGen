package site.henrykang.plugin.entity;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiMethod;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import site.henrykang.plugin.util.MyPsiUtil;

import java.util.*;
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
    /** multipart参数 */
    private List<ParamInfo> partParams;
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
        methodInfo.pathParams = map.getOrDefault("PathVariable", Collections.emptyList());
        methodInfo.queryParams = map.getOrDefault("RequestParam", Collections.emptyList());
        methodInfo.partParams = map.getOrDefault("RequestPart", Collections.emptyList());
        methodInfo.bodyParam = Optional.ofNullable(map.get("RequestBody")).map(list -> list.get(0)).orElse(null);

        methodInfo.allParamsNameStr = methodInfo.allParams.stream().map(ParamInfo::getName).collect(Collectors.joining(", "));
        methodInfo.queryParamsNameStr = methodInfo.queryParams.stream().map(ParamInfo::getName).collect(Collectors.joining(", "));
        methodInfo.paramsCnt = methodInfo.allParams.size();
        return methodInfo;
    }

}
