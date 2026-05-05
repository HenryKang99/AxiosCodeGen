package site.henrykang.plugin.entity;

import com.intellij.icons.AllIcons;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;

public class TemplateGroupFactory implements FileTemplateGroupDescriptorFactory {

    @Override
    public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
        // 创建自定义模板组
        FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor(Constant.PLUGIN_NAME, AllIcons.FileTypes.JavaScript);
        // 从资源目录加载模板文件
        group.addTemplate(new FileTemplateDescriptor(Constant.FILE_TEMPLATE_NAME + ".js", null));
        group.addTemplate(new FileTemplateDescriptor(Constant.METHOD_TEMPLATE_NAME + ".js", null));
        group.addTemplate(new FileTemplateDescriptor(Constant.DOC_TEMPLATE_NAME + ".js", null));

        return group;
    }

}
