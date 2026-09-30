package error.command.impl;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import error.command.Command;
import error.util.client.persiki.ChatUtil;
import error.voice.VoiceBridge;

import java.util.List;

public class VoiceCommand extends Command {

    public VoiceCommand() {
        super("voice", "Управление встроенным голосовым чатом");
    }

    @Override
    public List<String> aliases() {
        return List.of("vc", "voicechat");
    }

    @Override
    public void build(LiteralArgumentBuilder<Object> builder) {
        builder.executes(context -> {
            showHelp();
            return 1;
        });

        builder.then(LiteralArgumentBuilder.literal("status")
                .executes(context -> {
                    if (!VoiceBridge.isPresent()) {
                        ChatUtil.error("Simple Voice Chat не обнаружен в сборке.");
                    } else {
                        ChatUtil.info("Состояние Voice Chat:");
                        ChatUtil.entry("Микрофон", VoiceBridge.getMicrophone().isEmpty() ? "По умолчанию" : VoiceBridge.getMicrophone());
                        ChatUtil.entry("Динамик", VoiceBridge.getSpeaker().isEmpty() ? "По умолчанию" : VoiceBridge.getSpeaker());
                        ChatUtil.entry("Громкость", (int)(VoiceBridge.getVoiceChatVolume() * 100) + "%");
                        ChatUtil.entry("Усиление мика", (int)(VoiceBridge.getMicrophoneGain() * 100) + "%");
                        ChatUtil.entry("Mute / Disable", VoiceBridge.isMuted() ? "Muted" : (VoiceBridge.isDisabled() ? "Disabled" : "Active"));
                        ChatUtil.entry("Режим активации", VoiceBridge.isPushToTalk() ? "Push-To-Talk (PTT)" : "Voice Activation");
                    }
                    return 1;
                })
        );

        builder.then(LiteralArgumentBuilder.literal("mute")
                .executes(context -> {
                    boolean muted = !VoiceBridge.isMuted();
                    VoiceBridge.setMuted(muted);
                    ChatUtil.success(muted ? "Микрофон отключен." : "Микрофон включен.");
                    return 1;
                })
        );

        builder.then(LiteralArgumentBuilder.literal("toggle")
                .executes(context -> {
                    boolean disabled = !VoiceBridge.isDisabled();
                    VoiceBridge.setDisabled(disabled);
                    ChatUtil.success(disabled ? "Голосовой чат выключен." : "Голосовой чат включен.");
                    return 1;
                })
        );

        builder.then(LiteralArgumentBuilder.literal("ptt")
                .executes(context -> {
                    boolean ptt = !VoiceBridge.isPushToTalk();
                    VoiceBridge.setPushToTalk(ptt);
                    ChatUtil.success(ptt ? "Режим активации: Push-To-Talk" : "Режим активации: По голосу");
                    return 1;
                })
        );

        builder.then(LiteralArgumentBuilder.literal("volume")
                .then(RequiredArgumentBuilder.<Object, Double>argument("percent", DoubleArgumentType.doubleArg(0, 200))
                        .executes(context -> {
                            double val = DoubleArgumentType.getDouble(context, "percent") / 100.0;
                            VoiceBridge.setVoiceChatVolume(val);
                            ChatUtil.success("Громкость Voice Chat установлена на " + (int)(val * 100) + "%");
                            return 1;
                        }))
        );

        builder.then(LiteralArgumentBuilder.literal("gain")
                .then(RequiredArgumentBuilder.<Object, Double>argument("percent", DoubleArgumentType.doubleArg(0, 300))
                        .executes(context -> {
                            double val = DoubleArgumentType.getDouble(context, "percent") / 100.0;
                            VoiceBridge.setMicrophoneGain(val);
                            ChatUtil.success("Усиление микрофона установлено на " + (int)(val * 100) + "%");
                            return 1;
                        }))
        );

        builder.then(LiteralArgumentBuilder.literal("devices")
                .executes(context -> {
                    List<String> mics = VoiceBridge.getMicrophones();
                    List<String> speakers = VoiceBridge.getSpeakers();
                    ChatUtil.info("Микрофоны:");
                    if (mics.isEmpty()) ChatUtil.entry("  нет доступных микрофонов", "");
                    for (int i = 0; i < mics.size(); i++) {
                        ChatUtil.entry("  [" + i + "]", mics.get(i));
                    }
                    ChatUtil.info("Динамики:");
                    if (speakers.isEmpty()) ChatUtil.entry("  нет доступных динамиков", "");
                    for (int i = 0; i < speakers.size(); i++) {
                        ChatUtil.entry("  [" + i + "]", speakers.get(i));
                    }
                    return 1;
                })
        );

        builder.then(LiteralArgumentBuilder.literal("mic")
                .then(RequiredArgumentBuilder.<Object, String>argument("name", StringArgumentType.greedyString())
                        .executes(context -> {
                            String micName = StringArgumentType.getString(context, "name");
                            VoiceBridge.setMicrophone(micName);
                            ChatUtil.success("Выбран микрофон: " + micName);
                            return 1;
                        }))
        );

        builder.then(LiteralArgumentBuilder.literal("speaker")
                .then(RequiredArgumentBuilder.<Object, String>argument("name", StringArgumentType.greedyString())
                        .executes(context -> {
                            String speakerName = StringArgumentType.getString(context, "name");
                            VoiceBridge.setSpeaker(speakerName);
                            ChatUtil.success("Выбран динамик: " + speakerName);
                            return 1;
                        }))
        );
    }

    private void showHelp() {
        ChatUtil.info("Команды настройки Voice Chat:");
        ChatUtil.entry(".voice status", "Проверить статус и настройки");
        ChatUtil.entry(".voice mute", "Включить/выключить микрофон");
        ChatUtil.entry(".voice toggle", "Включить/выключить голосовой чат");
        ChatUtil.entry(".voice ptt", "Переключить Push-To-Talk / Активацию голосом");
        ChatUtil.entry(".voice volume <0-200>", "Изменить громкость вывода");
        ChatUtil.entry(".voice gain <0-300>", "Изменить усиление микрофона");
        ChatUtil.entry(".voice devices", "Список доступных звуковых устройств");
        ChatUtil.entry(".voice mic <название>", "Указать устройства ввода");
        ChatUtil.entry(".voice speaker <название>", "Указать устройство вывода");
    }
}
