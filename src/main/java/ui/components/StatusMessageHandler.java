package ui.components;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StatusMessageHandler {

    public enum MessageType {
        SUCCESS(new Color(144, 238, 144), "/sounds/success.wav"),
        ERROR(new Color(255, 182, 193), "/sounds/error.wav"),
        WARNING(new Color(255, 255, 153), "/sounds/warning.wav"),
        INFO(new Color(173, 216, 230), "/sounds/info.wav");

        private final Color color;
        private final String sound;

        MessageType(Color color, String sound) {
            this.color = color;
            this.sound = sound;
        }

        public Color getColor() {
            return color;
        }

        public String getSound() {
            return sound;
        }
    }

    private static Timer currentTimer;

    public static void showStatus(JLabel label, String message, MessageType type) {

        if (currentTimer != null && currentTimer.isRunning()) {
            currentTimer.stop();
        }

        label.setText(message);
        label.setBackground(type.getColor());
        label.setOpaque(true);

        SoundPlayer.play(type.getSound());

        currentTimer = new Timer(100, new ActionListener() {
            private float alpha = 1.0f;
            private final Color originalColor = type.getColor();
            private final Color defaultColor = UIManager.getColor("Label.background");

            @Override
            public void actionPerformed(ActionEvent e) {
                alpha -= 0.05f;

                if (alpha <= 0f) {
                    label.setText("");
                    label.setBackground(defaultColor);
                    ((Timer) e.getSource()).stop();
                } else {
                    label.setBackground(blendColors(originalColor, defaultColor, alpha));
                }
            }
        });

        currentTimer.setInitialDelay(5000);
        currentTimer.start();
    }

    public static void showSuccess(JLabel label, String message) {
        showStatus(label, message, MessageType.SUCCESS);
    }

    public static void showError(JLabel label, String message) {
        showStatus(label, message, MessageType.ERROR);
    }

    public static void showWarning(JLabel label, String message) {
        showStatus(label, message, MessageType.WARNING);
    }

    public static void showInfo(JLabel label, String message) {
        showStatus(label, message, MessageType.INFO);
    }

    public static void clearStatus(JLabel label) {
        if (currentTimer != null) {
            currentTimer.stop();
        }
        label.setText("");
        label.setBackground(UIManager.getColor("Label.background"));
    }

    private static Color blendColors(Color c1, Color c2, float alpha) {
        float beta = 1f - alpha;
        return new Color(
                (int) (c1.getRed() * alpha + c2.getRed() * beta),
                (int) (c1.getGreen() * alpha + c2.getGreen() * beta),
                (int) (c1.getBlue() * alpha + c2.getBlue() * beta)
        );
    }
}
