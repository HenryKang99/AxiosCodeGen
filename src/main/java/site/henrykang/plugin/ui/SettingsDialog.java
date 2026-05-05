package site.henrykang.plugin.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.psi.PsiPackage;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import site.henrykang.plugin.entity.Constant;
import site.henrykang.plugin.service.PropertiesManager;
import site.henrykang.plugin.util.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 配置弹窗，用户输入保存到 PropertiesComponent 中
 */
public class SettingsDialog extends DialogWrapper {

    private final Project project;

    private final JBTextField savePathField = new JBTextField();
    private final JBTextField pojoPackagesField = new JBTextField();
    private final JBCheckBox clearCacheCheckBox = new JBCheckBox();
    private final JBCheckBox ignoreDeprecatedCheckBox = new JBCheckBox();
    private final JBTextArea ignoreParamTypesArea = new JBTextArea();

    public SettingsDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle(Constant.PLUGIN_NAME);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        int labelWidth = JBUI.scale(150);
        JPanel savePathPanel = initSavePathPanel(labelWidth);
        JPanel pojoPanel = initPojoPackagePanel(labelWidth);
        JPanel clearCachePanel = initClearCachePanel(labelWidth);
        JPanel ignoreDeprecatedPanel = initIgnoreDeprecatedPanel(labelWidth);
        JPanel ignoreParamTypesPanel = initIgnoreParamTypesPanel(labelWidth);

        JPanel panel = FormBuilder.createFormBuilder()
            .addComponent(savePathPanel)
            .addVerticalGap(JBUI.scale(8))
            .addComponent(pojoPanel)
            .addVerticalGap(JBUI.scale(8))
            .addComponent(clearCachePanel)
            .addVerticalGap(JBUI.scale(8))
            .addComponent(ignoreDeprecatedPanel)
            .addVerticalGap(JBUI.scale(8))
            .addComponent(ignoreParamTypesPanel)
            .getPanel();
        panel.setPreferredSize(new Dimension(650, panel.getPreferredSize().height));
        return panel;
    }

    private @NotNull JPanel initSavePathPanel(int labelWidth) {
        TextFieldWithBrowseButton savePathBrowse = new TextFieldWithBrowseButton(savePathField, e -> {
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            descriptor.setTitle("Select Save Path");
            descriptor.setDescription("Choose a directory to save the generated files");
            com.intellij.openapi.fileChooser.FileChooser.chooseFile(descriptor, project, null, virtualFile -> {
                if (virtualFile != null) {
                    savePathField.setText(virtualFile.getPath());
                }
            });
        });
        savePathBrowse.setButtonIcon(AllIcons.Nodes.Folder);
        String value = PropertiesManager.getInstance(project).get(Constant.CACHE_KEY_SAVE_PATH);
        if (StringUtil.isNotBlank(value)) {
            savePathField.setText(value);
        }

        JBLabel label = new JBLabel("Save path");
        label.setPreferredSize(new Dimension(labelWidth, label.getPreferredSize().height));

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(label, BorderLayout.WEST);
        panel.add(savePathBrowse, BorderLayout.CENTER);

        return panel;
    }

    private @NotNull JPanel initPojoPackagePanel(int labelWidth) {
        TextFieldWithBrowseButton pojoPackageBrowse = new TextFieldWithBrowseButton(pojoPackagesField, e -> {
            PackageChooserDialog chooser = new PackageChooserDialog("Select POJO Package(S)", project);
            chooser.show();
            if (chooser.isOK()) {
                List<String> packages = chooser.getSelectedPackages().stream()
                    .map(PsiPackage::getQualifiedName)
                    .collect(Collectors.toList());
                pojoPackagesField.setText(String.join(",", packages));
            }
        });
        pojoPackageBrowse.setButtonIcon(AllIcons.Nodes.Folder);
        String value = PropertiesManager.getInstance(project).get(Constant.CACHE_KEY_POJO_PACKAGES);
        if (StringUtil.isNotBlank(value)) {
            pojoPackagesField.setText(value);
        }

        JBLabel label = new JBLabel("POJO package(s)");
        JLabel helpIcon = new JLabel(AllIcons.General.ContextHelp);
        helpIcon.setToolTipText("When resolving parameter's type, only classes under these packages and their subpackages will be parsed to generate JSDoc; otherwise, the type is treated as Object.");

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
        labelPanel.setOpaque(false);
        labelPanel.add(label);
        labelPanel.add(Box.createHorizontalStrut(4));
        labelPanel.add(helpIcon);
        labelPanel.setPreferredSize(new Dimension(labelWidth, label.getPreferredSize().height));

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(labelPanel, BorderLayout.WEST);
        panel.add(pojoPackageBrowse, BorderLayout.CENTER);

        return panel;
    }

    private @NotNull JPanel initClearCachePanel(int labelWidth) {
        String isSelected = PropertiesManager.getInstance(project).get(Constant.CACHE_KEY_IS_CLEAR_CACHE);
        this.clearCacheCheckBox.setSelected(Boolean.parseBoolean(isSelected));
        this.clearCacheCheckBox.setToolTipText("If checked, the plugin will clear the POJO resolution cache from previous operations");

        JBLabel label = new JBLabel("Clear cache");
        label.setPreferredSize(new Dimension(labelWidth, label.getPreferredSize().height));

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(label, BorderLayout.WEST);
        panel.add(clearCacheCheckBox, BorderLayout.CENTER);

        return panel;
    }

    private @NotNull JPanel initIgnoreDeprecatedPanel(int labelWidth) {
        String isSelected = PropertiesManager.getInstance(project).get(Constant.CACHE_KEY_IGNORE_DEPRECATED);
        // 默认选中
        this.ignoreDeprecatedCheckBox.setSelected(isSelected == null || Boolean.parseBoolean(isSelected));

        JBLabel label = new JBLabel("Ignore @Deprecated");
        JLabel helpIcon = new JLabel(AllIcons.General.ContextHelp);
        helpIcon.setToolTipText("If checked, the plugin will ignore @Deprecated annotated controller methods");

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
        labelPanel.setOpaque(false);
        labelPanel.add(label);
        labelPanel.add(Box.createHorizontalStrut(4));
        labelPanel.add(helpIcon);
        labelPanel.setPreferredSize(new Dimension(labelWidth, label.getPreferredSize().height));

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(labelPanel, BorderLayout.WEST);
        panel.add(ignoreDeprecatedCheckBox, BorderLayout.CENTER);

        return panel;
    }

    private @NotNull JPanel initIgnoreParamTypesPanel(int labelWidth) {
        String value = PropertiesManager.getInstance(project).get(Constant.CACHE_KEY_IGNORE_PARAM_TYPES);
        // 设置默认值
        if (StringUtil.isEmpty(value)) {
            value = """
                javax.servlet.http.*,
                org.springframework.validation.BindingResult,
                """;
        }
        ignoreParamTypesArea.setLineWrap(true);
        ignoreParamTypesArea.setWrapStyleWord(true);
        ignoreParamTypesArea.setRows(4);
        ignoreParamTypesArea.setText(value);

        JBLabel label = new JBLabel("Ignore param types");
        JLabel helpIcon = new JLabel(AllIcons.General.ContextHelp);
        helpIcon.setToolTipText("Parameter types to ignore, separated by commas. Supports wildcard * for package names.");

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
        labelPanel.setOpaque(false);
        labelPanel.add(label);
        labelPanel.add(Box.createHorizontalStrut(4));
        labelPanel.add(helpIcon);
        labelPanel.setPreferredSize(new Dimension(labelWidth, label.getPreferredSize().height));

        JBScrollPane scrollPane = new JBScrollPane(ignoreParamTypesArea);
        scrollPane.setPreferredSize(new Dimension(300, 80));

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(labelPanel, BorderLayout.WEST);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    public String getSavePath() {
        return savePathField.getText().trim();
    }

    public String getPojoPackages() {
        return pojoPackagesField.getText().trim();
    }

    public Boolean isClearCacheSelected() {
        return this.clearCacheCheckBox.isSelected();
    }

    public Boolean isIgnoreDeprecatedSelected() {
        return this.ignoreDeprecatedCheckBox.isSelected();
    }

    public String getIgnoreParamTypes() {
        return ignoreParamTypesArea.getText().trim();
    }

}