package error.util.client;

import error.module.impl.misc.ClientSounds;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineEvent.Type;

public final class ClientSoundPlayer {
   private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r, "error-ClientSounds");
      thread.setDaemon(true);
      return thread;
   });

   public static void playSound(String fileName, double volume, float pitch) {
      executor.execute(() -> handleFileName(fileName, volume, pitch));
   }

   public static void playModuleToggle(boolean enabled) {
      playGuiSound(enabled);
   }

   public static void playGuiOpen() {
      playGuiSound(true);
   }

   public static void playGuiClose() {
      playGuiSound(false);
   }

   public static void playGuiClick() {
      playGuiSound(true);
   }

   public static void playModePreview(String mode) {
      ClientSounds sounds = ClientSounds.INSTANCE;
      if (sounds != null && sounds.isEnabled() && mode != null && !"Нет".equalsIgnoreCase(mode)) {
         String soundFile = mapModeToSound(mode, true);
         playSound(soundFile, sounds.volume.get() / sounds.volume.getMax(), 1.0F);
      }
   }

   private static void playGuiSound(boolean open) {
      ClientSounds sounds = ClientSounds.INSTANCE;
      if (sounds != null && sounds.isEnabled()) {
         String mode = sounds.stateSounds.getValue();
         if (mode != null && !"Нет".equalsIgnoreCase(mode)) {
            String soundFile = mapModeToSound(mode, open);
            playSound(soundFile, sounds.volume.get() / sounds.volume.getMax(), 1.0F);
         }
      }
   }

   private static String mapModeToSound(String mode, boolean open) {
      switch (mode) {
         case "Первый":
            return open ? "1.wav" : "MODULE_OFF.wav";
         case "Второй":
            return open ? "2.wav" : "MODULE_OFF2.wav";
         case "Третий":
            return open ? "MODULE_ON.wav" : "MODULE_OFF.wav";
         case "Четвертый":
            return open ? "MODULE_ON2.wav" : "MODULE_OFF2.wav";
         case "Пятый":
            return open ? "MODULE_ON3.wav" : "MODULE_OFF3.wav";
         case "Шестой":
            return open ? "Function_ON.wav" : "Function_OFF.wav";
         default:
            return open ? "guiopen.wav" : "guiclose.wav";
      }
   }

   private static void handleFileName(String fileName, double volume, float pitch) {
      String resourcePath = "/assets/error/sounds/" + fileName;

      try (InputStream is = ClientSoundPlayer.class.getResourceAsStream(resourcePath)) {
         if (is != null) {
            try (
               BufferedInputStream bis = new BufferedInputStream(is);
               AudioInputStream originalStream = AudioSystem.getAudioInputStream(bis);
               AudioInputStream pitchStream = getOriginalStream(originalStream, pitch)
            ) {
               Clip clip = AudioSystem.getClip();
               clip.addLineListener(event -> {
                  if (event.getType() == Type.STOP) {
                     clip.close();
                  }
               });
               clip.open(pitchStream);
               handleClip(clip, volume);
               clip.start();
            }
         }
      } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ignored) {
      }
   }

   private static AudioInputStream getOriginalStream(AudioInputStream originalStream, float pitch) throws IOException {
      AudioFormat format = originalStream.getFormat();
      byte[] bytes = originalStream.readAllBytes();
      float newSampleRate = format.getSampleRate() * Math.max(0.5F, Math.min(2.0F, pitch));
      AudioFormat newFormat = new AudioFormat(newSampleRate, format.getSampleSizeInBits(), format.getChannels(), true, format.isBigEndian());
      return new AudioInputStream(new ByteArrayInputStream(bytes), newFormat, bytes.length / newFormat.getFrameSize());
   }

   private static void handleClip(Clip clip, double volume) {
      if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
         double vol = Math.max(0.0, Math.min(1.0, volume));
         FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
         float dB = (float) (Math.log10(vol <= 0.0 ? 1.0E-4 : vol) * 20.0);
         gainControl.setValue(dB);
      }
   }

   private ClientSoundPlayer() {
      throw new UnsupportedOperationException("Utility class");
   }
}
