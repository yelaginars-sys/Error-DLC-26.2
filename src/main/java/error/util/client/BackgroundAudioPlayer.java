package error.util.client;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class BackgroundAudioPlayer {

    private static BackgroundAudioPlayer instance;
    public static BackgroundAudioPlayer getInstance() {
        if (instance == null) instance = new BackgroundAudioPlayer();
        return instance;
    }

    private Clip clip;
    private FloatControl gainControl;
    private float volume = 0.65F;
    private boolean playing = false;

    private BackgroundAudioPlayer() {}

    public void start() {
        if (playing && clip != null && clip.isRunning()) return;

        try {
            if (clip == null) {
                InputStream is = BackgroundAudioPlayer.class.getResourceAsStream("/assets/error/sounds/menu_bg.wav");
                if (is == null) return;

                BufferedInputStream bis = new BufferedInputStream(is);
                AudioInputStream ais = AudioSystem.getAudioInputStream(bis);

                clip = AudioSystem.getClip();
                clip.open(ais);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                }

                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }

            applyVolume();
            clip.start();
            playing = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (clip != null) {
            try {
                clip.stop();
                clip.close();
            } catch (Exception ignored) {}
            clip = null;
            gainControl = null;
        }
        playing = false;
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
            float dB = (v <= 0.001F) ? -80.0F : (float) (Math.log10(v) * 20.0F);
            dB = Math.clamp(dB, gainControl.getMinimum(), gainControl.getMaximum());
            gainControl.setValue(dB);
        }
    }
}
