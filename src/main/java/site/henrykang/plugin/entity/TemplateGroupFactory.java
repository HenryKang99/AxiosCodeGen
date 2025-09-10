package site.henrykang.plugin.entity;

import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;

public class TemplateGroupFactory implements FileTemplateGroupDescriptorFactory {

    @Override
    public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
        FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor(Constant.PLUGIN_NAME, null);

        group.addTemplate(new FileTemplateDescriptor(Constant.FILE_TEMPLATE_NAME + ".js", null));
        group.addTemplate(new FileTemplateDescriptor(Constant.METHOD_TEMPLATE_NAME + ".js", null));
        group.addTemplate(new FileTemplateDescriptor(Constant.DOC_TEMPLATE_NAME + ".js", null));

        return group;
    }

}
