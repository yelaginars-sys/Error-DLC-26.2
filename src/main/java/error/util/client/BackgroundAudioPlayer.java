package error.util.client;

public class BackgroundAudioPlayer {

    private static BackgroundAudioPlayer instance;
    public static synchronized BackgroundAudioPlayer getInstance() {
        if (instance == null) instance = new BackgroundAudioPlayer();
        return instance;
    }

    private float volume = 0.0F;

    private BackgroundAudioPlayer() {}

    public synchronized void start() {
        // Disabled per user request to prevent OpenAL conflicts
    }

    public synchronized void stop() {
        // Disabled
    }

    public void setVolume(float vol) {
        this.volume = vol;
    }

    public float getVolume() {
        return this.volume;
    }
}
