package site.henrykang.plugin.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import site.henrykang.plugin.entity.Constant;
import site.henrykang.plugin.util.StringUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 将 PsiType 映射为 JS 类型：
 * <pre>
 * 基本类型：{@code string、number、boolean}
 * 对象：{@code Object}
 * 表示集合：{@code Array<T>}
 * 表示映射：{@code Object<K,T>}
 * 其他：{@code File}
 * </pre>
 */
@Data
@Service(Service.Level.PROJECT) // 当项目关闭、插件卸载时，会自动调用 dispose
public final class JsDocTypeResolver implements Disposable {

    /**
     * 缓存映射结果
     * key：PsiType.getCanonicalText()
     * value：A-映射结果，B-涉及的pojo
     */
    private final Cache<String, Pair<String, Set<String>>> mappingCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();

    /** pojo 类包集合 */
    private List<String> pojoPackageList = Collections.emptyList();
    /** 保存一些祖先 psiClass 引用，例如 Collection、Map，用于 isInheritor() 方法判断使用 */
    private Map<String, SmartPsiElementPointer<PsiClass>> ancestralPsiClassMap;

    public JsDocTypeResolver(@NotNull Project project) {
        String pojoPackages = PropertiesManager.getInstance(project).get(Constant.CACHE_KEY_POJO_PACKAGES);
        this.initPojoPackageList(pojoPackages);
        // 监听 pojo 包路径配置更改，清空缓存
        PropertiesManager.getInstance(project).addObserver(Constant.CACHE_KEY_POJO_PACKAGES, (oldVal, newVal) -> {
            if (!Objects.equals(oldVal, newVal)) {
                this.initPojoPackageList(newVal);
                this.invalidateCache();
            }
        });
        this.initSomeAncestralPsiCLass(project);
    }

    public static JsDocTypeResolver getInstance(@NotNull Project project) {
        return project.getService(JsDocTypeResolver.class);
    }

    /**
     * 初始化 pojo 包列表
     */
    private void initPojoPackageList(String pojoPackages) {
        this.pojoPackageList = Collections.emptyList();
        if (StringUtil.isNotBlank(pojoPackages)) {
            this.pojoPackageList = Arrays.stream(pojoPackages.split(","))
                .filter(StringUtil::isNotBlank)
                .map(String::trim)
                .toList();
        }
    }

    /**
     * 加载一些 psiClass 信息
     */
    private void initSomeAncestralPsiCLass(@NotNull Project project) {
        List<String> list = List.of("java.util.Collection", "java.util.Map", "java.util.UUID");
        this.ancestralPsiClassMap = new HashMap<>(list.size());
        // 智能指针，自动追踪 psi 元素
        SmartPointerManager spm = SmartPointerManager.getInstance(project);
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        ProjectAndLibrariesScope searchScope = new ProjectAndLibrariesScope(project);

        list.forEach(qualifiedName -> {
            PsiClass p = facade.findClass(qualifiedName, searchScope);
            if (p != null) this.ancestralPsiClassMap.put(qualifiedName, spm.createSmartPsiElementPointer(p));
        });
    }

    /**
     * 根据 psiType 返回 jsType 与涉及的 pojo 全类名集合
     */
    @NotNull
    public Pair<String, Set<String>> resolve(@NotNull PsiType type) {
        // 尝试从缓存中获取
        Pair<String, Set<String>> cache = this.mappingCache.getIfPresent(type.getCanonicalText());
        if (cache != null) return cache;
        // 处理
        Set<String> pojoNameSet = new HashSet<>();
        String jsType = this.resolveInternal(type, pojoNameSet);
        if (pojoNameSet.isEmpty()) pojoNameSet = Collections.emptySet();
        return Pair.pair(jsType, Collections.unmodifiableSet(pojoNameSet));
    }

    /**
     * 根据 psiType 返回 jsType，并收集 pojo 全类名。
     * 对于泛型，可能存在几种情况：
     * <pre>
     * List 不带泛型参数 -> {@code Array<Object>}
     * {@code List<?>} 使用通配符 -> {@code Array<Object>}
     * {@code List<Pojo>} 使用泛型参数  -> {@code Array<Pojo>}
     * {@code Map<String, Pojo>} 普通 map -> {@code Object<string, Pojo>}
     * {@code Map<String, List<Map<String, Pojo>>>} 泛型嵌套 -> {@code Object<string, Array<Object<string, Pojo>>>}
     * {@code Pojo<A,B>} Pojo泛型(解析后原样输出) -> {@code Pojo<A,B>}
     * </pre>
     */
    private String resolveInternal(@Nullable PsiType type, Set<String> pojoNameSet) {
        String resultStr = "Object";
        // 处理未指定泛型类型的泛型类时，可能为 null
        if (type == null) {
            return resultStr;
        }
        // 泛型通配符类型 ?、extend、super
        else if (type instanceof PsiWildcardType) {
            return resultStr;
        }
        // 基本类型
        else if (type instanceof PsiPrimitiveType) {
            resultStr = this.mapPrimitive(type.getCanonicalText());
        }
        // 数组类型
        else if (type instanceof PsiArrayType typed) {
            resultStr = "Array<" + this.resolveInternal(typed.getComponentType(), pojoNameSet) + ">";
        }
        // 其他引用类型
        else if (type instanceof PsiClassType typed) {
            // 获取对应的 psiClass
            PsiClass classed = typed.resolve();
            if (classed == null) {
                resultStr = "Object";
            }
            // 如果是泛型的类型参数，则直接返回类型参数名
            else if (classed instanceof PsiTypeParameter) {
                resultStr = Optional.ofNullable(classed.getName()).orElse("Object");
            }
            // 如果是枚举，则查找被 @JsonValue 修饰的属性的类型，没找到则默认返回 string
            else if (classed.isEnum()) {
                resultStr = this.mapEnum(classed, pojoNameSet);
            } else {
                // 获取类名
                String shortName = Optional.ofNullable(classed.getName()).orElse("");
                String qualifiedName = Optional.ofNullable(classed.getQualifiedName()).orElse("");
                boolean isNeedCollect = this.isNeedCollectPojoInfo(qualifiedName);
                if (isNeedCollect) {
                    pojoNameSet.add(qualifiedName);
                }
                // 处理泛型
                List<String> typeArgs = Collections.emptyList();
                if (classed.hasTypeParameters()) {
                    // 泛型匹配器
                    PsiSubstitutor substitutor = typed.resolveGenerics().getSubstitutor();
                    typeArgs = Arrays.stream(classed.getTypeParameters())
                        // 没有写泛型参数的话，substitute 会返回 null
                        .map(substitutor::substitute)
                        .map(psiType -> this.resolveInternal(psiType, pojoNameSet))
                        .toList();
                }
                String ancestralQualifiedName = this.findAncestralQualifiedName(classed);
                qualifiedName = ancestralQualifiedName != null ? ancestralQualifiedName : qualifiedName;
                resultStr = switch (qualifiedName) {
                    // 基本类型的包装类
                    case "java.lang.String" -> "string";
                    case "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double" -> "number";
                    case "java.lang.Boolean" -> "boolean";
                    // UUID 当作 string
                    case "java.util.UUID" -> "string";
                    // 日期时间相关，均当作 string
                    case "java.time.LocalDate", "java.time.LocalTime", "java.time.LocalDateTime", "java.util.Date", "java.util.Time" -> "string";
                    // MultipartFile
                    case "org.springframework.web.multipart.MultipartFile" -> "File";
                    // 集合类型
                    case "java.util.Collection" -> "Array<" + String.join(", ", typeArgs) + ">";
                    case "java.util.Map" -> "Object<" + String.join(", ", typeArgs) + ">";
                    // 其他类型，直接取 shortName + 泛型(若有)
                    default -> (isNeedCollect ? shortName : "Object") + (typeArgs.isEmpty() ? "" : "<" + String.join(", ", typeArgs) + ">");
                };
            }
        }
        // 缓存
        this.mappingCache.put(type.getCanonicalText(), Pair.pair(resultStr, Collections.unmodifiableSet(pojoNameSet)));
        return resultStr;
    }

    /**
     * 使用 PsiClass.isInheritor() 判断继承关系，将一些包名替换为父类包名，例如 List、Set 替换为 Collection，
     * 若有匹配则返回父类全类名，无匹配则返回 null
     */
    private String findAncestralQualifiedName(PsiClass psiClass) {
        for (Map.Entry<String, SmartPsiElementPointer<PsiClass>> entry : ancestralPsiClassMap.entrySet()) {
            SmartPsiElementPointer<PsiClass> pointer = entry.getValue();
            if (pointer.getElement() == null) continue;
            if (psiClass.isInheritor(pointer.getElement(), true)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 处理基本类型
     */
    private String mapPrimitive(String canonicalText) {
        return switch (canonicalText) {
            case "byte", "short", "int", "long", "float", "double" -> "number";
            case "boolean" -> "boolean";
            case "char" -> "string";
            default -> canonicalText;
        };
    }

    /**
     * 对于枚举，查找被 @JsonValue 修饰的属性的类型，没找到则默认返回 string
     */
    private String mapEnum(PsiClass psiClass, Set<String> pojoNameSet) {
        return Arrays.stream(psiClass.getAllFields())
            .filter(field -> field.hasAnnotation(Constant.ANNO_JSON_VALUE))
            .findFirst()
            .map(PsiField::getType)
            .map(psiType -> this.resolveInternal(psiType, pojoNameSet))
            .orElse("string");
    }

    /**
     * 是否收集该 pojo 信息
     */
    private boolean isNeedCollectPojoInfo(String qualifiedName) {
        if (StringUtil.isBlank(qualifiedName)) return false;
        if (qualifiedName.startsWith("java.")) return false;
        if (pojoPackageList.isEmpty()) return false;
        return pojoPackageList.stream().anyMatch(qualifiedName::startsWith);
    }

    public void invalidateCache() {
        this.mappingCache.invalidateAll();
    }

    @Override
    public void dispose() {
        this.invalidateCache();
        ancestralPsiClassMap.clear();
    }

}
