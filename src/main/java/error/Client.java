package error;

import net.fabricmc.api.ModInitializer;
import error.event.EventManager;
import error.builder.RotationBuilderManager;
import error.ui.mainmenu.CustomTitleScreen;
import error.account.AccountManager;
import error.command.CommandManager;
import error.config.ConfigManager;
import error.ui.hud.HudManager;
import error.ui.mainmenu.PanelKeyBoardHandler;
import error.ui.mainmenu.PanelLapRenderHandler;
import error.friend.FriendManager;
import error.module.Modules;
import error.util.client.persiki.Tps;

public class Client implements ModInitializer {
    public static Client INSTANCE;
    public final EventManager eventManager = new EventManager();
    public CommandManager commandManager;
    public ConfigManager configManager;
    public FriendManager friendManager;
    public final Modules moduleManager = new Modules();
    public static float Timer = 1.0f;


    @Override
    public void onInitialize() {
        INSTANCE = this;
        this.moduleManager.init();
        FriendManager.getInstance().load();
        AccountManager.getInstance().load();
        this.commandManager = new CommandManager();
        this.configManager = new ConfigManager();
        EventManager.register(RotationBuilderManager.INSTANCE);
        CustomTitleScreen.loadWallpaper();
        this.configManager.loadConfig("default", false);
        this.eventManager.register(HudManager.getInstance());
        this.eventManager.register(Tps.INSTANCE);
        this.eventManager.register(new PanelKeyBoardHandler());
        this.eventManager.register(new PanelLapRenderHandler());

    }

    public static Client getInstance() {
        return INSTANCE;
    }

    public EventManager getEventManager() {
        return eventManager;
    }
}