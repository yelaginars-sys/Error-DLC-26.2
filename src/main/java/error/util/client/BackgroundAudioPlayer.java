package error.util.client;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundAudioPlayer {

    private static BackgroundAudioPlayer instance;
    public static synchronized BackgroundAudioPlayer getInstance() {
        if (instance == null) instance = new BackgroundAudioPlayer();
        return instance;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "error-BackgroundAudio");
        t.setDaemon(true);
        return t;
    });

    private Clip clip;
    private FloatControl gainControl;
    private float volume = 0.65F;
    private volatile boolean playing = false;

    private BackgroundAudioPlayer() {}

    public synchronized void start() {
        if (playing && clip != null && clip.isRunning()) return;
        playing = true;

        executor.execute(() -> {
            try {
                if (clip == null) {
                    InputStream is = BackgroundAudioPlayer.class.getResourceAsStream("/assets/error/sounds/menu_bg.wav");
                    if (is == null) {
                        is = BackgroundAudioPlayer.class.getClassLoader().getResourceAsStream("assets/error/sounds/menu_bg.wav");
                    }
                    if (is == null) {
                        System.err.println("[ErrorDLC] Background audio: /assets/error/sounds/menu_bg.wav not found!");
                        return;
                    }

                    BufferedInputStream bis = new BufferedInputStream(is);
                    AudioInputStream ais = AudioSystem.getAudioInputStream(bis);

                    clip = AudioSystem.getClip();
                    clip.open(ais);

                    if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    }
                }

                if (clip != null && playing) {
                    applyVolume();
                    clip.setFramePosition(0);
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    clip.start();
                    System.out.println("[ErrorDLC] Background audio playing successfully!");
                }
            } catch (Throwable t) {
                System.err.println("[ErrorDLC] Failed to play background audio:");
                t.printStackTrace();
            }
        });
    }

    public synchronized void stop() {
        playing = false;
        executor.execute(() -> {
            if (clip != null) {
                try {
                    clip.stop();
                    clip.close();
                } catch (Throwable ignored) {}
                clip = null;
                gainControl = null;
            }
        });
    }

    public void setVolume(float vol) {
        this.volume = Math.clamp(vol, 0.0F, 1.0F);
        applyVolume();
    }

    public float getVolume() {
        return this.volume;
    }

    private void applyVolume() {
        if (gainControl != null) {
            float v = Math.clamp(this.volume, 0.0F, 1.0F);
            float dB = (v <= 0.0001F) ? -80.0F : (float) (Math.log10(v) * 20.0F);
            dB = Math.clamp(dB, gainControl.getMinimum(), gainControl.getMaximum());
            gainControl.setValue(dB);
        }
    }
}
