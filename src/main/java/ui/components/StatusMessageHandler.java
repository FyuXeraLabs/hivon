package ui.components;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StatusMessageHandler {

    public enum MessageType {
        SUCCESS(new Color(144, 238, 144)),
        ERROR(new Color(255, 182, 193)),
        WARNING(new Color(255, 255, 153)),
        INFO(new Color(173, 216, 230));

        private final Color color;

        MessageType(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }

    private static Timer currentTimer = null;

    public static void showStatus(JLabel label, String message, MessageType type) {

        if (currentTimer != null && currentTimer.isRunning()) {
            currentTimer.stop();
        }

        label.setText(message);
        label.setBackground(type.getColor());
        label.setOpaque(true);

        currentTimer = new Timer(5000, new ActionListener() {
            private float alpha = 1.0f;
            private final Color originalColor = type.getColor();
            private final Color defaultColor = UIManager.getColor("Label.background");

            @Override
            public void actionPerformed(ActionEvent e) {
                alpha -= 0.05f;
                if (alpha <= 0) {
                    alpha = 0;
                    label.setBackground(defaultColor);
                    label.setText("");
                    ((Timer) e.getSource()).stop();
                } else {

                    Color blendedColor = blendColors(originalColor, defaultColor, alpha);
                    label.setBackground(blendedColor);
                }
            }
        });
        currentTimer.setDelay(100);
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

    private static Color blendColors(Color c1, Color c2, float alpha) {
        float beta = 1 - alpha;
        int red = (int) (c1.getRed() * alpha + c2.getRed() * beta);
        int green = (int) (c1.getGreen() * alpha + c2.getGreen() * beta);
        int blue = (int) (c1.getBlue() * alpha + c2.getBlue() * beta);
        return new Color(red, green, blue);
    }

    public static void clearStatus(JLabel label) {
        if (currentTimer != null && currentTimer.isRunning()) {
            currentTimer.stop();
        }
        label.setText("");
        label.setBackground(UIManager.getColor("Label.background"));
    }
}