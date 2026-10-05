package error.module.impl.misc;

import error.module.Category;
import error.module.Module;
import error.rpc.DiscordIPCClient;
import error.rpc.DiscordRichPresence;

public class DiscordRPC extends Module {
    public static DiscordRPC INSTANCE;

    public static final String CLIENT_ID = "1556662831880081558";
    private DiscordIPCClient ipcClient;
    private DiscordRichPresence presence;
    private Thread thread;
    private boolean started = false;
    private long startTime = 0;

    public DiscordRPC() {
        super("DiscordRPC", "Отображает статус Error DLC в Discord Rich Presence", Category.MISC);
        INSTANCE = this;
        setEnabled(true);
        startRpc();
    }

    public static DiscordRPC getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DiscordRPC();
        }
        return INSTANCE;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        startRpc();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        stopRpc();
    }

    public void startRpc() {
        if (started) return;
        started = true;
        startTime = System.currentTimeMillis() / 1000L;
        presence = new DiscordRichPresence();
        ipcClient = new DiscordIPCClient(CLIENT_ID);

        thread = new Thread(() -> {
            System.out.println("[DiscordRPC] Starting RPC handler thread (App ID: " + CLIENT_ID + ")");

            while (started && !Thread.currentThread().isInterrupted()) {
                try {
                    updatePresenceData();
                    ipcClient.sendActivity(presence);
                } catch (Throwable t) {
                    System.err.println("[DiscordRPC] Presence error: " + t.getMessage());
                }

                try {
                    Thread.sleep(2500L);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "Error-DiscordRPC-Thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void stopRpc() {
        started = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (ipcClient != null) {
            try {
                ipcClient.close();
            } catch (Throwable ignored) {
            }
            ipcClient = null;
        }
    }

    private void updatePresenceData() {
        if (presence == null) return;

        presence.startTimestamp = startTime;
        presence.details = "Error DLC | 26.2";

        if (mc != null && mc.level != null) {
            if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
                presence.state = "Играет на " + mc.getCurrentServer().ip;
            } else {
                presence.state = "В одиночной игре";
            }
        } else {
            presence.state = "В главном меню";
        }

        presence.largeImageKey = "https://i.imgur.com/FKAJvwg.jpeg";
        presence.largeImageText = "Error DLC";

        presence.button_label_1 = "Telegram";
        presence.button_url_1 = "https://t.me/errordlc";

        presence.button_label_2 = "Website";
        presence.button_url_2 = "https://t.me/errordlc";
    }
}
