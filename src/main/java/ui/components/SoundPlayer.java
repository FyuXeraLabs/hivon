package ui.components;

import javax.sound.sampled.*;
import java.net.URL;
import core.config.SettingsManager;

public final class SoundPlayer {

    private SoundPlayer() {}

    public static void play(String resourcePath) {
        if (!SettingsManager.isSoundEnabled() || resourcePath == null) return;

        new Thread(() -> {
            try {
                URL url = SoundPlayer.class.getResource(resourcePath);
                if (url == null) return;

                AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);

                // adjust volume using MASTER_GAIN control if supported
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    // map volume from 0-100 scale to log scale decibels (0.0 to 1.0)
                    float volume = SettingsManager.getSoundVolume() / 100.0f;
                    // logarithmic calculation for volume level
                    float dB = (float) (Math.log(volume == 0 ? 0.0001f : volume) / Math.log(10.0) * 20.0);

                    // clamp dB to gainControl limits
                    float min = gainControl.getMinimum();
                    float max = gainControl.getMaximum();
                    if (dB < min) {
                        dB = min;
                    } else if (dB > max) {
                        dB = max;
                    }
                    gainControl.setValue(dB);
                }

                clip.start();
            } catch (Exception ignored) {
                
            }
        }, "sound-player-thread").start();
    }

    public static void setSoundEnabled(boolean enabled) {
        SettingsManager.setSoundEnabled(enabled);
    }

    public static boolean isSoundEnabled() {
        return SettingsManager.isSoundEnabled();
    }
}
