package ui.components;

import javax.sound.sampled.*;
import java.net.URL;

public final class SoundPlayer {

    private static boolean soundEnabled = true;

    private SoundPlayer() {}

    public static void play(String resourcePath) {
        if (!soundEnabled || resourcePath == null) return;

        new Thread(() -> {
            try {
                URL url = SoundPlayer.class.getResource(resourcePath);
                if (url == null) return;

                AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            } catch (Exception ignored) {
                
            }
        }, "sound-player-thread").start();
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }
}
