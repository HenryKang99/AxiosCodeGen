package site.henrykang.plugin.listener;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import site.henrykang.plugin.entity.Constant;

import java.util.Objects;

/**
 * 插件加载监听器
 */
public class MyDynamicPluginListener implements DynamicPluginListener {

    private static final Logger LOG = Logger.getInstance(MyDynamicPluginListener.class);

    @Override
        public void pluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        if (isMyPlugin(pluginDescriptor)) {
            LOG.info("pluginLoaded: " + Constant.PLUGIN_NAME);
        }
    }

    @Override
    public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        if (isMyPlugin(pluginDescriptor)) {
            LOG.info("beforePluginUnload: " + Constant.PLUGIN_NAME);
            // 释放资源
            Disposer.dispose(Constant.PLUGIN_DISPOSABLE);
        }
    }

    private static boolean isMyPlugin(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        return Objects.equals(pluginDescriptor.getPluginId().getIdString(), Constant.PLUGIN_ID);
    }

}
