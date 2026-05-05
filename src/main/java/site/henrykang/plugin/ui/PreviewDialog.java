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
 * 预览弹窗 - 用于显示生成的代码并提供复制功能
 * <p>
 * 主要用途：
 * 1. 在生成单个方法的 Axios 代码时，提供预览窗口供用户查看和编辑
 * 2. 在生成 POJO 的 JSDoc 定义时，提供预览窗口供用户查看和编辑
 * 3. 支持一键复制生成的代码到剪贴板
 * </p>
 * <p>
 * 继承自 DialogWrapper，获得标准的对话框行为（OK/Cancel 按钮、模态显示等）
 * </p>
 * 
 * @see DialogWrapper
 */
public class PreviewDialog extends DialogWrapper {

    /** 当前项目实例，用于通知显示等操作 */
    private final Project project;
    
    /** 要预览的 PSI 文件对象，包含生成的代码内容 */
    private final PsiFile psiFile;
    
    /** 代码编辑器实例，用于显示和编辑代码 */
    private final EditorEx editor;

    /**
     * 构造预览对话框
     *
     * @param project 当前项目实例，用于获取服务和支持对话框操作
     * @param psiFile 要预览的 PSI 文件对象，通常是临时创建的 JavaScript 文件
     *                包含已格式化的生成代码
     */
    public PreviewDialog(Project project, PsiFile psiFile) {
        super(true);
        this.project = project;
        this.psiFile = psiFile;
        
        // 创建只读编辑器，显示 PSI 文件的内容
        // createEditor 参数说明：
        // - psiFile.getFileDocument(): 获取文件对应的文档对象
        // - project: 项目上下文
        // - psiFile.getFileType(): 文件类型（用于语法高亮）
        // - false: 不启用文档模式
        this.editor = (EditorEx) EditorFactory.getInstance().createEditor(
            psiFile.getFileDocument(), 
            project, 
            psiFile.getFileType(), 
            false
        );
        
        // 设置编辑器组件的首选尺寸（宽 800px, 高 500px）
        this.editor.getComponent().setPreferredSize(new Dimension(800, 500));
        
        // 设置编辑器组件的最小尺寸（宽 400px, 高 300px），防止窗口过小
        this.editor.getComponent().setMinimumSize(new Dimension(400, 300));
        
        // 设置编辑器边框为 5px 的空心边框，增加视觉留白
        this.editor.getComponent().setBorder(JBUI.Borders.empty(5));

        // 设置对话框标题
        setTitle("Preview");
        
        // 允许用户调整对话框大小
        setResizable(true);
        
        // 初始化对话框（调用 init() 后会触发 createCenterPanel 和 createActions）
        init();
    }

    /**
     * 创建对话框的中心面板内容
     * <p>
     * 返回编辑器组件，使其成为对话框的主体部分
     * 编辑器会自动应用 IDE 的主题、字体、语法高亮等设置
     * </p>
     *
     * @return 编辑器 Swing 组件，显示生成的代码
     */
    @Override
    protected @Nullable JComponent createCenterPanel() {
        return editor.getComponent();
    }

    /**
     * 创建对话框底部的操作按钮
     * <p>
     * 自定义按钮数组：
     * 1. "Copy" 按钮 - 复制代码到剪贴板并关闭对话框
     * 2. "Cancel" 按钮 - 取消操作，关闭对话框
     * </p>
     *
     * @return 操作按钮数组
     */
    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{
            // 自定义复制按钮
            new DialogWrapperAction("Copy") {
                /**
                 * 点击 Copy 按钮时的处理逻辑
                 *
                 * @param e 动作事件对象
                 */
                @Override
                protected void doAction(ActionEvent e) {
                    // 将 PSI 文件的完整文本内容复制到系统剪贴板
                    // getText() 返回格式化后的完整代码字符串
                    CopyPasteManager.getInstance().setContents(
                        new StringSelection(psiFile.getText())
                    );
                    
                    // 在右下角显示成功提示通知，持续 3 秒
                    UiUtil.showNotification(
                        project, 
                        "Copied to clipboard", 
                        NotificationType.INFORMATION, 
                        TimeUnit.SECONDS.toMillis(3)
                    );
                    
                    // 关闭对话框，返回 OK_EXIT_CODE 表示用户确认操作
                    close(OK_EXIT_CODE);
                }
            },
            // 默认的取消按钮（自动绑定 Cancel 文本和关闭行为）
            getCancelAction()
        };
    }

    /**
     * 释放资源
     * <p>
     * 当对话框关闭时调用，释放编辑器占用的资源
     * 必须调用 releaseEditor 以避免内存泄漏
     * </p>
     * <p>
     * 注意：此方法会被 DialogWrapper.dispose() 自动调用
     * </p>
     */
    @Override
    public void dispose() {
        // 释放编辑器资源，包括文档监听器、高亮器等
        EditorFactory.getInstance().releaseEditor(editor);
        
        // 调用父类的 dispose 方法，完成标准清理工作
        super.dispose();
    }

}
