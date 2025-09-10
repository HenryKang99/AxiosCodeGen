package site.henrykang.plugin.entity;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;

import java.util.Arrays;
import java.util.HashSet;

public class Constant {

    public static final String PLUGIN_ID = "site.henrykang.plugin.axios-code-gen";
    public static final String PLUGIN_NAME = "AxiosCodeGen";
    public static final String PLUGIN_PACKAGE_NAME = "site.henrykang.plugin";

    /**
     * 若有需要释放的资源，可以通过 Disposer.register(parentDisposable, childDisposable) 挂到该节点下，
     * 在 beforePluginUnload 事件中调用 Disposer.dispose(parentDisposable)，会自动调用所有子节点的 dispose 方法
     */
    public static final Disposable PLUGIN_DISPOSABLE = Disposer.newDisposable(PLUGIN_ID);

    // notification groupId
    public static final String NOTIFICATION_GROUP = PLUGIN_PACKAGE_NAME + ".AxiosCodeGenNotificationGroup";

    // cache key
    public static final String CACHE_KEY_SAVE_PATH = String.join(".", PLUGIN_PACKAGE_NAME, PLUGIN_NAME, "cache", "savePath");
    public static final String CACHE_KEY_POJO_PACKAGES = String.join(".", PLUGIN_PACKAGE_NAME, PLUGIN_NAME, "cache", "pojoPackages");
    public static final String CACHE_KEY_IS_CLEAR_CACHE = String.join(".", PLUGIN_PACKAGE_NAME, PLUGIN_NAME, "cache", "isClearCache");

    // template name
    public static final String FILE_TEMPLATE_NAME = PLUGIN_NAME + "_all";
    public static final String METHOD_TEMPLATE_NAME = PLUGIN_NAME + "_method";
    public static final String DOC_TEMPLATE_NAME = PLUGIN_NAME + "_doc";

    // region support annotation qualifiedName

    // -- RequestMapping --
    public static final String ANNO_REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping";
    public static final String ANNO_GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping";
    public static final String ANNO_POST_MAPPING = "org.springframework.web.bind.annotation.PostMapping";
    public static final String ANNO_PUT_MAPPING = "org.springframework.web.bind.annotation.PutMapping";
    public static final String ANNO_DELETE_MAPPING = "org.springframework.web.bind.annotation.DeleteMapping";
    public static final String ANNO_PATCH_MAPPING = "org.springframework.web.bind.annotation.PatchMapping";
    public static final HashSet<String> MAPPING_ANNO_SET = new HashSet<>(Arrays.asList(ANNO_REQUEST_MAPPING, ANNO_GET_MAPPING, ANNO_POST_MAPPING, ANNO_PUT_MAPPING, ANNO_DELETE_MAPPING, ANNO_PATCH_MAPPING));

    // -- RequestParam --
    public static final String ANNO_REQUEST_PARAM = "org.springframework.web.bind.annotation.RequestParam";
    public static final String ANNO_REQUEST_BODY = "org.springframework.web.bind.annotation.RequestBody";
    public static final String ANNO_REQUEST_PART = "org.springframework.web.bind.annotation.RequestPart";
    public static final String ANNO_PATH_VARIABLE = "org.springframework.web.bind.annotation.PathVariable";
    public static final HashSet<String> PARAM_BIND_ANNO_SET = new HashSet<>(Arrays.asList(ANNO_REQUEST_PARAM, ANNO_REQUEST_BODY, ANNO_REQUEST_PART, ANNO_PATH_VARIABLE));

    // -- Swagger --
    public static final String ANNO_SWAGGER_TAG = "io.swagger.v3.oas.annotations.tags.Tag";
    public static final String ANNO_SWAGGER_OPERATION = "io.swagger.v3.oas.annotations.Operation";
    public static final String ANNO_SWAGGER_PARAMETER = "io.swagger.v3.oas.annotations.Parameter";
    public static final String ANNO_SWAGGER_SCHEMA = "io.swagger.v3.oas.annotations.media.Schema";

    // -- JsonValue --
    public static final String ANNO_JSON_VALUE = "com.fasterxml.jackson.annotation.JsonValue";

    // endregion support annotation qualifiedName
}
