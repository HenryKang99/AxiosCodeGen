package site.henrykang.plugin.util;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.apache.velocity.util.DuckType;
import site.henrykang.plugin.entity.Constant;

import java.io.StringWriter;

public class TemplateUtil implements Disposable {

    private static final VelocityEngine VE;
    private static final StringResourceRepository REPO;

    static {
        VE = new VelocityEngine();
        VE.setProperty(RuntimeConstants.RESOURCE_LOADER, "string");
        VE.setProperty("resource.loader.string.class", StringResourceLoader.class.getName());
        VE.setProperty("resource.loader.string.repository.name", Constant.PLUGIN_ID);
        VE.init();
        REPO = StringResourceLoader.getRepository(Constant.PLUGIN_ID);
        // for auto release
        Disposer.register(Constant.PLUGIN_DISPOSABLE, new TemplateUtil());
    }

    public static String merge(VelocityContext ctx, String templateName) {
        reloadTemplate();
        Template template = VE.getTemplate(templateName);
        StringWriter sw = new StringWriter();
        template.merge(ctx, sw);
        return sw.toString();
    }

    private static void reloadTemplate() {
        FileTemplateManager ftManager = FileTemplateManager.getDefaultInstance();

        FileTemplate t0 = ftManager.getJ2eeTemplate(Constant.FILE_TEMPLATE_NAME);
        FileTemplate t1 = ftManager.getJ2eeTemplate(Constant.METHOD_TEMPLATE_NAME);
        FileTemplate t2 = ftManager.getJ2eeTemplate(Constant.DOC_TEMPLATE_NAME);

        // idea 只支持解析 Includes 中的代码片段，所以此处手动替换
        String fullTemplateText = t0.getText()
            .replace("#parse(\"" + Constant.METHOD_TEMPLATE_NAME + "\")", t1.getText())
            .replace("#parse(\"" + Constant.DOC_TEMPLATE_NAME + "\")", t2.getText());
        REPO.putStringResource(Constant.FILE_TEMPLATE_NAME, fullTemplateText);
        REPO.putStringResource(Constant.METHOD_TEMPLATE_NAME, t1.getText() + "\n" + t2.getText());
        REPO.putStringResource(Constant.DOC_TEMPLATE_NAME, t2.getText());
    }

    /**
     * 由于使用的是 IDE 自带的 velocity 依赖，产生了跨类加载器引用，
     * 所以这里需要手动释放 velocity 内部的缓存，断开引用链，否则插件无法动态卸载
     */
    @Override
    public void dispose() {
        if (REPO != null) {
            StringResourceLoader.removeRepository(Constant.PLUGIN_ID);
        }
        DuckType.clearCache();
    }

}
