package site.henrykang.plugin.service;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;

@Service(Service.Level.PROJECT)
public final class PropertiesManager {

    private final PropertiesComponent props;

    /** 在 key 上的监听列表，BiConsumer 传参 oldVal, newVal */
    private final Map<String, List<BiConsumer<String, String>>> listenersMap = new HashMap<>();

    public PropertiesManager(@NotNull Project project) {
        this.props = PropertiesComponent.getInstance(project);
    }

    public static PropertiesManager getInstance(@NotNull Project project) {
        return project.getService(PropertiesManager.class);
    }

    public void put(String key, String value) {
        String oldVal = this.get(key);
        if (!Objects.equals(oldVal, value)) {
            this.props.setValue(key, value);
        }
        Optional.ofNullable(this.listenersMap.get(key))
            .ifPresent(listeners -> listeners.forEach(listener -> listener.accept(oldVal, value)));
    }

    public String get(String key) {
        return this.props.getValue(key);
    }

    public void addObserver(String key, BiConsumer<String, String> listener) {
        if (!this.listenersMap.containsKey(key)) {
            List<BiConsumer<String, String>> list = new ArrayList<>();
            list.add(listener);
            this.listenersMap.put(key, list);
        } else {
            this.listenersMap.get(key).add(listener);
        }
    }

}
