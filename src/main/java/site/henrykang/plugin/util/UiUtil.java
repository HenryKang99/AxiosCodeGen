package site.henrykang.plugin.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import site.henrykang.plugin.entity.Constant;

import java.util.concurrent.TimeUnit;

public class UiUtil {

    /**
     * 右下角通知信息，并自动关闭
     */
    public static void showNotification(Project project, String message, NotificationType type, long expireMillis) {
        Notification notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(Constant.NOTIFICATION_GROUP)
            .createNotification(Constant.PLUGIN_NAME, message, type);
        // notification.setImportant(false);
        notification.notify(project);
        AppExecutorUtil.getAppScheduledExecutorService().schedule(notification::expire, expireMillis, TimeUnit.MILLISECONDS);
    }

}
