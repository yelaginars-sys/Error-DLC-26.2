package dev.syntrix.clienttest.client.gui;

import dev.syntrix.clienttest.client.combat.CrystalAuraSettings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClickGuiState {
    public final List<Module> modules = new ArrayList<>();

    public ClickGuiState() {
        modules.add(new Module(CrystalAuraSettings.ID));
    }

    public static final class Module {
        public final String id;
        public boolean enabled;
        public final Map<String, Double> values = new LinkedHashMap<>();

        public Module(String id) {
            this.id = id;
            CrystalAuraSettings.DEFINITION.settings().forEach(setting -> values.put(setting.id(), setting.initial()));
        }
    }
}
