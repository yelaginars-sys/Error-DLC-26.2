package dev.syntrix.clienttest.client.combat.meow.module;
import java.util.*;
import dev.syntrix.clienttest.client.combat.meow.setting.Setting;
import dev.syntrix.clienttest.client.gui.ClickGuiState;
/** Bridge from recovered settings to this client's persisted module state. */
public abstract class Module implements MinecraftAccess {
    private ClickGuiState.Module state;
    private final List<Setting> settings=new ArrayList<>();
    protected Module(ModuleMetadata metadata) {}
    public void bind(ClickGuiState.Module state) { this.state=state; }
    public boolean isEnabled() { return state!=null&&state.enabled; }
    public boolean isInGame() { return mc.player!=null&&mc.level!=null; }
    protected void addSettings(Setting... values) { settings.addAll(List.of(values)); }
    public List<Setting> getSettings() { return List.copyOf(settings); }
    public void onDisable() {}
}
