package ui.components;

import core.security.LogoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 * Installs the application's standard keyboard shortcuts on a form's root pane.
 */
public final class FormShortcuts {

    private static final int NO_FUNCTION_KEY = -1;

    private FormShortcuts() {
    }

    public static void install(JRootPane rootPane, Binding... bindings) {
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        for (int i = 0; i < bindings.length; i++) {
            Binding binding = bindings[i];
            Action action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    binding.run();
                }
            };

            String actionName = "formShortcut" + i;
            for (KeyStroke key : binding.keys) {
                inputMap.put(key, actionName);
            }
            actionMap.put(actionName, action);
        }
    }

    public static Binding add(AbstractButton button) {
        return buttonBinding("add", button, KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_F6);
    }

    public static Binding save(AbstractButton button) {
        return buttonBinding("save", button, KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_F7);
    }

    public static Binding clear(AbstractButton button) {
        return buttonBinding("clear", button, KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_F8);
    }

    public static Binding update(AbstractButton button) {
        return buttonBinding("update", button, KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_F9);
    }

    public static Binding delete(AbstractButton button) {
        return buttonBinding("delete", button, KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_F10);
    }

    public static Binding delete(Runnable action) {
        return actionBinding("delete", action, KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_F10);
    }

    public static Binding refresh(AbstractButton button) {
        return buttonBinding("refresh", button, KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK, NO_FUNCTION_KEY);
    }

    public static Binding search(AbstractButton button) {
        return buttonBinding("search", button, KeyEvent.VK_Q, InputEvent.ALT_DOWN_MASK, NO_FUNCTION_KEY);
    }

    public static Binding print(AbstractButton button) {
        return buttonBinding("print", button, KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK, NO_FUNCTION_KEY);
    }

    public static Binding export(AbstractButton button) {
        return buttonBinding("export", button, KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK, NO_FUNCTION_KEY);
    }

    public static Binding confirm(AbstractButton button) {
        return buttonBinding("confirm", button, KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_F10);
    }

    public static Binding activateDeactivate(AbstractButton button) {
        return buttonBinding("activateDeactivate", button, KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_F11);
    }

    public static Binding passwordReset(AbstractButton button) {
        return buttonBinding("passwordReset", button, KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_F12);
    }

    public static Binding signOut() {
        return actionBinding("signOut", LogoutManager::logout,
                KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, NO_FUNCTION_KEY);
    }

    private static Binding buttonBinding(String name, AbstractButton button,
            int keyCode, int modifiers, int functionKey) {
        return actionBinding(name, () -> {
            if (button.isEnabled()) {
                button.doClick();
            }
        }, keyCode, modifiers, functionKey);
    }

    private static Binding actionBinding(String name, Runnable action,
            int keyCode, int modifiers, int functionKey) {
        KeyStroke primary = KeyStroke.getKeyStroke(keyCode, modifiers);
        if (functionKey == NO_FUNCTION_KEY) {
            return new Binding(name, action, primary);
        }
        return new Binding(name, action, primary, KeyStroke.getKeyStroke(functionKey, 0));
    }

    public static final class Binding {

        private final String name;
        private final Runnable action;
        private final KeyStroke[] keys;

        private Binding(String name, Runnable action, KeyStroke... keys) {
            this.name = name;
            this.action = action;
            this.keys = keys;
        }

        private void run() {
            action.run();
        }

        public String name() {
            return name;
        }

        public KeyStroke[] keys() {
            return keys.clone();
        }
    }
}
