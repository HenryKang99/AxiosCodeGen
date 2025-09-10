package site.henrykang.plugin.ui;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import site.henrykang.plugin.util.UiUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.concurrent.TimeUnit;

/**
 * 预览弹窗
 */
public class PreviewDialog extends DialogWrapper {

    private final Project project;
    private final PsiFile psiFile;
    private final EditorEx editor;

    /**
     * @param project 当前项目
     * @param psiFile 要预览的 psiFile
     */
    public PreviewDialog(Project project, PsiFile psiFile) {
        super(true);
        this.project = project;
        this.psiFile = psiFile;
        this.editor = (EditorEx) EditorFactory.getInstance().createEditor(psiFile.getFileDocument(), project, psiFile.getFileType(), false);
        this.editor.getComponent().setPreferredSize(new Dimension(800, 500));
        this.editor.getComponent().setMinimumSize(new Dimension(400, 300));
        this.editor.getComponent().setBorder(JBUI.Borders.empty(5));

        setTitle("Preview");
        setResizable(true);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return editor.getComponent();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{
            new DialogWrapperAction("Copy") {
                @Override
                protected void doAction(ActionEvent e) {
                    CopyPasteManager.getInstance().setContents(new StringSelection(psiFile.getText()));
                    UiUtil.showNotification(project, "Copied to clipboard", NotificationType.INFORMATION, TimeUnit.SECONDS.toMillis(3));
                    close(OK_EXIT_CODE);
                }
            },
            getCancelAction()
        };
    }

    @Override
    public void dispose() {
        EditorFactory.getInstance().releaseEditor(editor);
        super.dispose();
    }

}
