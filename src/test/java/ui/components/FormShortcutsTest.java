package ui.components;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FormShortcutsTest {

    @Test
    @DisplayName("Alt and function keys both trigger the same form action")
    void altAndFunctionKeysTriggerAction() {
        JRootPane rootPane = new JRootPane();
        JButton add = countingButton();
        JButton save = countingButton();
        JButton clear = countingButton();
        JButton update = countingButton();
        JButton delete = countingButton();

        FormShortcuts.install(rootPane,
                FormShortcuts.add(add),
                FormShortcuts.save(save),
                FormShortcuts.clear(clear),
                FormShortcuts.update(update),
                FormShortcuts.delete(delete));

        assertClick(rootPane, key(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK), add, 1);
        assertClick(rootPane, key(KeyEvent.VK_F6, 0), add, 2);
        assertClick(rootPane, key(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK), save, 1);
        assertClick(rootPane, key(KeyEvent.VK_F7, 0), save, 2);
        assertClick(rootPane, key(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK), clear, 1);
        assertClick(rootPane, key(KeyEvent.VK_F8, 0), clear, 2);
        assertClick(rootPane, key(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK), update, 1);
        assertClick(rootPane, key(KeyEvent.VK_F9, 0), update, 2);
        assertClick(rootPane, key(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK), delete, 1);
        assertClick(rootPane, key(KeyEvent.VK_F10, 0), delete, 2);
    }

    @Test
    @DisplayName("Alt+R, Alt+Q, Ctrl+P and Ctrl+E are registered once each")
    void singleKeyBindingsAreRegistered() {
        JRootPane rootPane = new JRootPane();
        FormShortcuts.install(rootPane,
                FormShortcuts.refresh(new JButton()),
                FormShortcuts.search(new JButton()),
                FormShortcuts.print(new JButton()),
                FormShortcuts.export(new JButton()));

        assertBound(rootPane, key(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
        assertBound(rootPane, key(KeyEvent.VK_Q, InputEvent.ALT_DOWN_MASK));
        assertBound(rootPane, key(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
        assertBound(rootPane, key(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
    }

    @Test
    @DisplayName("Disabled buttons are not clicked")
    void disabledButtonIsNotClicked() {
        JRootPane rootPane = new JRootPane();
        JButton button = countingButton();
        button.setEnabled(false);

        FormShortcuts.install(rootPane, FormShortcuts.add(button));

        fire(rootPane, key(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK));
        fire(rootPane, key(KeyEvent.VK_F6, 0));
        assertEquals(0, clicks(button));
    }

    @Test
    @DisplayName("Runnable bindings support actions without a button")
    void runnableBindingTriggersAction() {
        JRootPane rootPane = new JRootPane();
        AtomicInteger count = new AtomicInteger();
        FormShortcuts.install(rootPane, FormShortcuts.delete(count::incrementAndGet));

        fire(rootPane, key(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK));
        fire(rootPane, key(KeyEvent.VK_F10, 0));
        assertEquals(2, count.get());
    }

    @Test
    @DisplayName("Main window confirm and sign out chords are registered")
    void confirmAndSignOutAreRegistered() {
        JRootPane rootPane = new JRootPane();
        FormShortcuts.install(rootPane,
                FormShortcuts.confirm(new JButton()),
                FormShortcuts.signOut());

        assertBound(rootPane, key(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
        assertBound(rootPane, key(KeyEvent.VK_F10, 0));
        assertBound(rootPane, key(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK));
    }

    private static void assertClick(JRootPane rootPane, KeyStroke key, JButton button, int expectedClicks) {
        fire(rootPane, key);
        assertEquals(expectedClicks, clicks(button), "Unexpected click count for " + key);
    }

    private static void assertBound(JRootPane rootPane, KeyStroke key) {
        assertNotNull(rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key),
                "Missing binding for " + key);
        assertNotNull(rootPane.getActionMap().get(
                rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key)));
    }

    private static void fire(JRootPane rootPane, KeyStroke key) {
        Object actionName = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key);
        assertNotNull(actionName, "Missing binding for " + key);
        Action action = rootPane.getActionMap().get(actionName);
        assertNotNull(action, "Missing action for " + key);
        action.actionPerformed(null);
    }

    private static KeyStroke key(int keyCode, int modifiers) {
        return KeyStroke.getKeyStroke(keyCode, modifiers);
    }

    private static JButton countingButton() {
        JButton button = new JButton();
        AtomicInteger count = new AtomicInteger();
        button.addActionListener(e -> count.incrementAndGet());
        button.putClientProperty("clickCount", count);
        return button;
    }

    private static int clicks(JButton button) {
        return ((AtomicInteger) button.getClientProperty("clickCount")).get();
    }
}
