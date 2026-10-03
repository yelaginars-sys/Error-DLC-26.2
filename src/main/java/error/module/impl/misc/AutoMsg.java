package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.persiki.ChatUtil;

import java.util.Random;

public class AutoMsg extends Module {
    public static AutoMsg INSTANCE;

    public final ModeSetting messageText = mode("Сообщение", "Error DLC 26.2 | Лучший софт!",
            "Error DLC 26.2 | Лучший софт!",
            "Продам ресурсы на аукционе /ah!",
            "Ищу тимейтов для игры на анархии!",
            "Покупаю руды и талисманы!");
    public final ModeSetting chatChannel = mode("Канал", "Глобальный", "Глобальный", "/ad", "/clan", "/me");
    public final SliderSetting delaySeconds = slider("Интервал (сек)", 15.0f, 3.0f, 120.0f, 1.0f);
    public final CheckBox antiSpamBypass = checkbox("Обход анти-спама", true);
    public final CheckBox notifyChat = checkbox("Лог в чат", true);

    private long lastMessageTime = 0;
    private final Random random = new Random();

    public AutoMsg() {
        super("AutoMsg", "Автоматическая отправка сообщений и рекламы в чат", Category.MISC);
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        lastMessageTime = 0;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || mc.getConnection() == null) return;

        long now = System.currentTimeMillis();
        long intervalMs = (long) (delaySeconds.getValue() * 1000.0f);

        if (now - lastMessageTime >= intervalMs) {
            sendMessage();
            lastMessageTime = now;
        }
    }

    public void sendMessage() {
        if (mc.getConnection() == null) return;

        String baseText = messageText.getValue();
        if (baseText == null || baseText.isBlank()) return;

        String resultMsg = baseText;
        if (antiSpamBypass.getValue()) {
            String randomCode = String.format(" [%04x]", random.nextInt(0xFFFF));
            resultMsg += randomCode;
        }

        String finalCommand;
        switch (chatChannel.getValue()) {
            case "/ad" -> finalCommand = "ad " + resultMsg;
            case "/clan" -> finalCommand = "c " + resultMsg;
            case "/me" -> finalCommand = "me " + resultMsg;
            default -> finalCommand = null;
        }

        if (finalCommand != null) {
            mc.getConnection().sendCommand(finalCommand);
        } else {
            mc.getConnection().sendChat(resultMsg);
        }

        if (notifyChat.getValue()) {
            ChatUtil.info("AutoMsg отправлено: §7" + resultMsg);
        }
    }
}
