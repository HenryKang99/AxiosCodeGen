# AxiosCodeGen

![Build](https://github.com/HenryKang99/AxiosCodeGen/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->

Adds support for generating Axios code and JSDoc from SpringMVC Controller classes.

**Features：**

- Support generating **"xxxApi.js"** file from **"xxxController.java"** through the right-click menu item named **"Generate Axios Code"** in the current editor or any package(s).
- Support generating axios code snippet for single method through **Intention** on method name.
    - Press **alt+enter** on controller method name and select **"Generate Axios Code"**, then a preview dialog will pop up for editing and copying.
- Support converting POJO to type definition information in JSDoc(@typedef) through `Intention` on POJO class name.
    - Press **alt+enter** on POJO class name and select **"Generate JSDoc"**, then a preview dialog will pop up for editing and copying.
- Support modifying templates in the group named **"AxiosCodeGen"** under the **Other** section of the **Settings | Editor | File and Code Templates** settings page.

本插件支持从 SpringMVC Controller 类生成前端 Axios 请求代码和相应的 JSDoc。

**功能点：**

- 通过在当前编辑器或项目目录树的右键菜单上点击 **"Generate Axios Code"**，可以从 **"xxxController.java"** 生成包含 Axios 请求代码和 JSDoc 的 **"xxxApi.js"** 文件。
- 通过在 Controller 中的方法名上按下 **alt+enter**, 选择 **"Generate Axios Code"** 可以只生成该方法的 Axios 请求代码片段，会弹出预览窗口供编辑和复制。
- 通过在 POJO 类名上按下 **alt+enter**, 选择 **"Generate JSDoc"** 可以生成对应类的 JSDoc 类型定义信息，会弹出预览窗口供编辑和复制。
- 如果想要微调输出模板，可以在 **Settings | Editor | File and Code Templates** 的 **Other** 标签页下找到 **"AxiosCodeGen"** 分组。

---

**Rules:**

- First, ensure that IDEA has completed indexing the SDK and current project (it's best to ensure the project can be compiled correctly), otherwise inaccurate results may be generated, such as inability to recognize some types or inherited properties.
- Only processes classes annotated with **@RequestMapping**.
- Only processes methods annotated with **@RequestMapping**, **@GetMapping** and other **@XxxMapping** annotations.
- Only processes parameters annotated with **@RequestParam**, **@PathVariable**, **@RequestBody**, or **@RequestPart**.
    - Parameters annotated with **@RequestParam** will be treated as **params** parameters in Axios;
    - Parameters annotated with **@PathVariable** will be concatenated to the Axios request **url** in the form of **${xxx}**;
    - Parameters annotated with **@RequestBody** will be treated as the **data** parameter in Axios;
    - Parameters annotated with **@RequestPart** will be treated as **FormData** elements and passed to the Axios **data** parameter.
- About parameter type conversion:
    - Basic types will be converted to **string**, **number**, **boolean**;
    - **Collection** types will be converted to **Array<T>**;
    - **Map** types will be converted to **Object<K,T>**;
    - **MultipartFile** will be converted to **File**;
    - **Date**、**LocalDate**... will be converted to **string**;
    - Unknown objects will be converted to **Object**;
    - Enum types will be converted to the type of the property annotated with **@JsonValue** (com.fasterxml.jackson.annotation) in the enum class, defaulting to **string** if not found;
    - Generic wildcard types or unknown generic types will be converted to **Object**.
- About POJO class conversion:
    - Only classes under the **POJO Package(s)** selected in the configuration dialog and their sub-packages will be parsed, otherwise they will be treated as **Object**;
    - If a POJO class is a generic class, the generic parameters (T, E...) will be output as-is using JSDoc's **@Template**;
    - Properties in POJO classes decorated with **static**, **final**, or **transient** will be ignored, and other properties will be processed using the same rules as parameter type conversion.
- Supports recognition of **@Tag**, **@Operation**, **@Parameter**, and **@Schema** annotations from the **io.swagger.v3.oas.annotations** package.
    - Prioritizes extracting class, method, and parameter description information from these annotations, and attempts to extract corresponding information from JavaDoc if not found.

**规则：**

- 首先，请确保 IDEA 已完成 SDK 和当前项目的索引工作(最好保证项目已可以正确编译)，否则可能生成不准确的结果，例如无法识别一些类型或继承的属性。
- 只会处理被 **@RequestMapping** 修饰的类。
- 只会处理被 **@RequestMapping** 和 **@GetMapping** 等 **@XxxMapping** 修饰的方法。
- 只会处理被 **@RequestParam**、**@PathVariable**、**@RequestBody**、**@RequestPart** 修饰的参数。
    - **@RequestParam** 修饰的参数将被视作 Axios 中的 **params** 参数;
    - **@PathVariable** 修饰的参数会以 **${xxx}** 的方式被拼接到 Axios 请求 **url** 中;
    - **@RequestBody** 修饰的参数将被视作 Axios 的 **data** 参数;
    - **@RequestPart** 修饰的参数将被视作 **FormData** 元素，传递给 Axios 的 **data** 参数。
- 关于参数类型转换：
    - 基本类型将被转换为 **string**、**number**、**boolean**;
    - **Collection** 类型将被转换为 **Array<T>**;
    - **Map** 类型将被转换为 **Object<K,T>**;
    - **MultipartFile** 将被转换为 **File**;
    - **Date**、**LocalDate** 等将被转换为 **string**;
    - 未知的对象将被转换为 **Object**;
    - 枚举类型将被转换为枚举类中被 **@JsonValue**(com.fasterxml.jackson.annotation) 注解修饰的属性的类型，若未找到则默认为 **string**;
    - 泛型通配符类型或未知的泛型类型将被转换为 **Object**.
- 关于 POJO 类的转换：
    - 只有在配置弹窗中选择的 **POJO Package(s)** 及其子包下的类才会被解析，否则将被视为 **Object**;
    - POJO 类若是泛型类，则泛型参数(T、E...) 将使用 JSDoc 的 **@Template** 原样输出;
    - POJO 类中被 **static**、**final**、**transient** 修饰的属性会被忽略，其他属性将应用和参数类型转换一样的规则进行处理。
- 支持识别 **@Tag**、**@Operation**、**@Parameter**、**@Schema** 这几个 **io.swagger.v3.oas.annotations** 包下的注解。
    - 优先从这些注解中提取类、方法、参数的描述信息，若未找到则尝试从 JavaDoc 中提取对应信息。

---

**Example：**

```java
/**
 * class comment from JavaDoc
 */
@RestController
@RequestMapping(value = "user")
@Tag(name = "comment from @Tag name", description = "comment from @Tag description")
public class UserInfoController {

    /**
     * function comment from JavaDoc
     *
     * @param userInfo param comment from JavaDoc
     */
    // @Operation(summary = "comment from @Operation summary")
    @PostMapping("create")
    public ResultBody create(@RequestBody @Parameter(description = "comment from @Parameter description") UserInfo userInfo) {return ResultBody.success();}

    /**
     * remove user (JavaDoc)
     *
     * @param id user id (JavaDoc)
     */
    @Operation(summary = "remove user (@Operation)")
    @PostMapping("remove")
    public ResultBody remove(@RequestParam UUID id) {return ResultBody.success();}

    @Operation(summary = "update userInfo")
    @PostMapping("update")
    public ResultBody update(@Validated({UpdateGroup.class}) @RequestBody UserInfo userInfo) {return ResultBody.success();}

    /**
     * 分页查询
     *
     * @param query 查询参数
     */
    @PostMapping("page")
    public ResultBody<List<UserInfo>> page(@RequestBody PageQuery<UserInfo, ?> query) {return ResultBody.success();}

    @Operation(summary = "获取单个用户信息")
    @GetMapping("info/{userId}")
    public ResultBody<UserInfo> info(@PathVariable("userId") UUID id) {return ResultBody.success();}

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public ResultBody upload(@RequestParam String q,
                             @RequestPart UserInfo userInfo,
                             @RequestPart MultipartFile file,
                             @RequestPart List<MultipartFile> fileList
    ) {return ResultBody.success();}

}
```

**Generate file: UserInfoApi.js:**

```js
/**
 * comment from @Tag name, comment from @Tag description
 */
import api from "@/utils/request.js";

const prefix = '/user'

/**
 * function comment from JavaDoc
 * @param {UserInfo} userInfo  comment from @Parameter description
 */
export function create(userInfo) {
  return api.post(`${prefix}/create`, userInfo);
}

/**
 * remove user (@Operation)
 * @param {string} id  user id (JavaDoc)
 */
export function remove(id) {
  return api.post(`${prefix}/remove`, null, {params: {id}});
}

/**
 * update userInfo
 * @param {UserInfo} userInfo
 */
export function update(userInfo) {
  return api.post(`${prefix}/update`, userInfo);
}

/**
 * 分页查询
 * @param {PageQuery<UserInfo, Object>} query  查询参数
 */
export function page(query) {
  return api.post(`${prefix}/page`, query);
}

/**
 * 获取单个用户信息
 * @param {string} userId
 */
export function info(userId) {
  return api.get(`${prefix}/info/${userId}`);
}

/**
 * 上传文件
 * @param {string} q
 * @param {UserInfo} userInfo
 * @param {File} file
 * @param {Array<File>} fileList
 */
export function upload({q, userInfo, file, fileList}) {
  let formData = new FormData();
  formData.append('userInfo', new Blob([JSON.stringify(userInfo)], {type: 'application/json'}))
  formData.append('file', file);
  for (const f of fileList) {
    formData.append('fileList', f);
  }
  return api.post(`${prefix}/upload`, formData, {params: {q}});
}

// region pojo def

/**
 * 测试分页查询参数
 * @template T
 * @template E
 * @typedef PageQuery
 * @property {number} total 总数
 * @property {number} size 每页显示条数, 默认 10
 * @property {number} current 当前页
 * @property {T} params 查询参数
 * @property {Array<E>} paramsList 查询参数列表
 */

/**
 * 用户信息
 * @typedef UserInfo
 * @property {string} name 用户名
 * @property {string} phone 手机号
 * @property {string} email 邮箱
 * @property {Address} address 地址
 * @property {number} gender 性别
 * @property {string} id
 * @property {string} createTime
 * @property {string} createUserId
 * @property {string} createUserName
 * @property {string} updateTime
 * @property {string} updateUserId
 * @property {string} updateUserName
 */

/**
 *
 * @typedef Address
 * @property {string} province 省
 * @property {string} city 市
 * @property {string} district 区
 * @property {string} street 街道
 */


// endregion
```

---

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "AxiosCodeGen"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/HenryKang99/AxiosCodeGen/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

<!-- Plugin description end -->