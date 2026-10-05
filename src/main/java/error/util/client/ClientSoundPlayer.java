package error.util.client;

import error.module.impl.misc.ClientSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ClientSoundPlayer {

    private static final CopyOnWriteArrayList<Clip> ACTIVE_CLIPS = new CopyOnWriteArrayList<>();

    public static void playSound(String fileName, double volume, float pitch) {
        if (fileName == null || fileName.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                cleanUpClips();

                InputStream inputStream = openSoundStream(fileName);
                if (inputStream == null) return;

                playStream(inputStream, (float) volume, pitch);
            } catch (Throwable ignored) {}
        });
    }

    public static void playFile(File file, float volume, float pitch) {
        if (file == null || !file.exists()) return;

        CompletableFuture.runAsync(() -> {
            try {
                cleanUpClips();
                try (InputStream inputStream = new FileInputStream(file)) {
                    playStream(inputStream, volume, pitch);
                }
            } catch (Throwable ignored) {}
        });
    }

    private static void playStream(InputStream inputStream, float volume, float pitch) {
        try (AudioInputStream rawStream = AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream))) {
            Clip clip = AudioSystem.getClip();

            if (Math.abs(pitch - 1.0F) < 0.01F) {
                clip.open(rawStream);
            } else {
                openPitched(clip, rawStream, pitch);
            }

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float volumeVal = Math.max(0.0001F, Math.min(1.0F, volume));
                float dB = (float) (Math.log10(volumeVal) * 20.0);
                dB = Math.max(volumeControl.getMinimum(), Math.min(volumeControl.getMaximum(), dB));
                volumeControl.setValue(dB);
            }

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    ACTIVE_CLIPS.remove(clip);
                }
            });

            ACTIVE_CLIPS.add(clip);
            clip.start();
        } catch (Throwable ignored) {}
    }

    private static void openPitched(Clip clip, AudioInputStream stream, float pitch) throws Exception {
        float p = Math.max(0.5F, Math.min(2.0F, pitch));

        AudioInputStream pcm = stream;
        AudioFormat src = stream.getFormat();

        if (src.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            pcm = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, stream);
            src = pcm.getFormat();
        }

        byte[] data = pcm.readAllBytes();
        if (pcm != stream) pcm.close();

        int frameSize = src.getFrameSize();
        if (frameSize <= 0) {
            clip.open(src, data, 0, data.length);
            return;
        }

        int inFrames = data.length / frameSize;
        int outFrames = (int) (inFrames / p);
        if (outFrames <= 0) {
            clip.open(src, data, 0, data.length);
            return;
        }

        byte[] out = new byte[outFrames * frameSize];
        for (int i = 0; i < outFrames; i++) {
            int srcFrame = Math.min(inFrames - 1, (int) (i * p));
            System.arraycopy(data, srcFrame * frameSize, out, i * frameSize, frameSize);
        }

        clip.open(src, out, 0, out.length);
    }

    private static void cleanUpClips() {
        ACTIVE_CLIPS.removeIf(clip -> !clip.isOpen());
    }

    public static void playModuleToggle(boolean enabled) {
        ClientSounds sounds = ClientSounds.INSTANCE;
        if (sounds != null && sounds.isEnabled()) {
            String mode = sounds.stateSounds.getValue();
            if (mode != null && !"Нет".equalsIgnoreCase(mode)) {
                String soundFile = mapModeToSound(mode, enabled);
                playSound(soundFile, getVolume(), 1.0F);
            }
        }
    }

    public static void playGuiOpen() {
        playGuiSound(true);
    }

    public static void playGuiClose() {
        playGuiSound(false);
    }

    public static void playGuiClick() {
        ClientSounds sounds = ClientSounds.INSTANCE;
        if (sounds != null && sounds.isEnabled()) {
            String mode = sounds.stateSounds.getValue();
            if (mode != null && !"Нет".equalsIgnoreCase(mode)) {
                String soundFile = mapModeToSound(mode, true);
                playSound(soundFile, getVolume(), 1.0F);
                return;
            }
        }
        playSound("guiopen.wav", 0.4, 1.0F);
    }

    public static void playModePreview(String mode) {
        if (mode != null && !"Нет".equalsIgnoreCase(mode)) {
            String soundFile = mapModeToSound(mode, true);
            playSound(soundFile, getVolume(), 1.0F);
        }
    }

    private static void playGuiSound(boolean open) {
        ClientSounds sounds = ClientSounds.INSTANCE;
        if (sounds != null && sounds.isEnabled()) {
            String mode = sounds.stateSounds.getValue();
            if (mode != null && !"Нет".equalsIgnoreCase(mode)) {
                String soundFile = mapModeToSound(mode, open);
                playSound(soundFile, getVolume(), 1.0F);
                return;
            }
        }
        playSound(open ? "guiopen.wav" : "guiclose.wav", 0.5, 1.0F);
    }

    private static double getVolume() {
        ClientSounds sounds = ClientSounds.INSTANCE;
        if (sounds != null && sounds.volume != null) {
            float v = sounds.volume.get();
            float max = sounds.volume.getMax();
            if (max > 0.0F) {
                return Math.max(0.01, Math.min(1.0, (double) v / (double) max));
            }
        }
        return 0.5;
    }

    private static String mapModeToSound(String mode, boolean open) {
        switch (mode) {
            case "Первый":
                return open ? "1.wav" : "2.wav";
            case "Второй":
                return open ? "enable2.wav" : "disable2.wav";
            case "Третий":
                return open ? "MODULE_ON.wav" : "MODULE_OFF.wav";
            case "Четвертый":
                return open ? "MODULE_ON2.wav" : "MODULE_OFF2.wav";
            case "Пятый":
                return open ? "MODULE_ON3.wav" : "MODULE_OFF3.wav";
            case "Шестой":
                return open ? "Function_ON.wav" : "Function_OFF.wav";
            case "Celestial":
                return open ? "celestial_on.wav" : "celestial_off.wav";
            case "Bubble":
                return open ? "enableBubbles.wav" : "disableBubbles.wav";
            case "Heavy":
                return open ? "heavyenable.wav" : "heavydisable.wav";
            case "Droplet":
                return open ? "dropletenable.wav" : "dropletdisable.wav";
            case "Pop":
                return open ? "popenable.wav" : "popdisable.wav";
            case "Slide":
                return open ? "slideenable.wav" : "slidedisable.wav";
            case "Win":
                return open ? "winenable.wav" : "windisable.wav";
            default:
                return open ? "guiopen.wav" : "guiclose.wav";
        }
    }

    private static long lastTypeTime;

    public static void playType() {
        long now = System.currentTimeMillis();
        if (now - lastTypeTime < 20L) return;
        lastTypeTime = now;
        playSound("gui_key_click.wav", 0.38, 1.0F + (float) (Math.random() * 0.12F - 0.06F));
    }

    public static void playErase() {
        long now = System.currentTimeMillis();
        if (now - lastTypeTime < 20L) return;
        lastTypeTime = now;
        playSound("gui_key_click.wav", 0.33, 0.88F);
    }

    public static void playSearchClear() {
        playSound("gui_clear.wav", 0.4, 1.0F);
    }

    private static InputStream openSoundStream(String fileName) {
        String[] prefixes = {
            "/assets/error/sounds/",
            "/assets/error/sounds/gui/",
            "/assets/client/sound/",
            "/assets/client/sound/gui/",
            "/assets/client/sounds/",
            "assets/error/sounds/",
            "assets/error/sounds/gui/",
            "assets/client/sound/",
            "assets/client/sound/gui/",
            "assets/client/sounds/"
        };

        String[] names;
        if (!fileName.endsWith(".wav") && !fileName.endsWith(".ogg") && !fileName.endsWith(".WAV")) {
            names = new String[]{fileName + ".wav", fileName, fileName.toLowerCase() + ".wav"};
        } else {
            names = new String[]{fileName, fileName.toLowerCase()};
        }

        for (String name : names) {
            for (String prefix : prefixes) {
                String path = prefix + name;
                InputStream is = ClientSoundPlayer.class.getResourceAsStream(path);
                if (is != null) return is;

                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl != null) {
                    is = cl.getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
                    if (is != null) return is;
                }
            }
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                for (String name : names) {
                    try {
                        return mc.getResourceManager().open(Identifier.fromNamespaceAndPath("error", "sounds/" + name));
                    } catch (Throwable ignored) {}
                    try {
                        return mc.getResourceManager().open(Identifier.fromNamespaceAndPath("error", "sounds/gui/" + name));
                    } catch (Throwable ignored) {}
                    try {
                        return mc.getResourceManager().open(Identifier.fromNamespaceAndPath("client", "sound/" + name));
                    } catch (Throwable ignored) {}
                    try {
                        return mc.getResourceManager().open(Identifier.fromNamespaceAndPath("client", "sound/gui/" + name));
                    } catch (Throwable ignored) {}
                    try {
                        return mc.getResourceManager().open(Identifier.fromNamespaceAndPath("client", "sounds/" + name));
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private ClientSoundPlayer() {
        throw new UnsupportedOperationException("Utility class");
    }
}
