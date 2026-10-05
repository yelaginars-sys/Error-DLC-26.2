package error;

import net.fabricmc.api.ModInitializer;
import error.event.EventManager;
import error.builder.RotationBuilderManager;
import error.account.AccountManager;
import error.command.CommandManager;
import error.config.ConfigManager;
import error.ui.hud.HudManager;
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
        error.config.ConfigManager.isLoadingConfig = true;
        try {
        error.module.impl.misc.UnHook.unhooked = false;
        this.moduleManager.init();
        if (this.moduleManager.unHook != null) {
            this.moduleManager.unHook.setState(false);
        }
        FriendManager.getInstance().load();
        AccountManager.getInstance().load();
        this.commandManager = new CommandManager();
        this.configManager = new ConfigManager();
        EventManager.register(RotationBuilderManager.INSTANCE);
        this.configManager.loadConfig("default", false);
        error.module.impl.misc.UnHook.unhooked = false;
        if (this.moduleManager.unHook != null) {
            this.moduleManager.unHook.setState(false);
        }
        error.irc.IrcManager.getInstance().updateSelfPresence();
        error.cosmetic.CosmeticsManager.getInstance();
        this.eventManager.register(HudManager.getInstance());
        this.eventManager.register(Tps.INSTANCE);
        error.event.ServerEventManager.getInstance();
        } finally {
            error.config.ConfigManager.isLoadingConfig = false;
        }
    }

    public static Client getInstance() {
        return INSTANCE;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public Modules getModuleManager() {
        return moduleManager;
    }
}