/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.workers;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import ui.dialogs.SystemTrayPanel;

/**
 * Worker managing the application system tray icon and its actions.
 *
 * @author Sanod D. Mendis
 */
public class SystemTray {

    private static java.awt.SystemTray systemTray;
    private static TrayIcon trayIcon;
    private static boolean initialized = false;

    private static JDialog hiddenDialog;
    private static JPopupMenu activePopup;
    private static long lastMenuOpenTime = 0;

    /**
     * Initializes the system tray icon, popup menu, and event listeners.
     */
    public static void init() {
        if (initialized) {
            return;
        }

        if (SwingUtilities.isEventDispatchThread()) {
            setupTray();
        } else {
            SwingUtilities.invokeLater(SystemTray::setupTray);
        }
    }

    private static void setupTray() {
        try {
            if (!java.awt.SystemTray.isSupported()) {
                System.out.println("SystemTray: Not supported on this platform. Continuing application startup.");
                return;
            }
            System.out.println("SystemTray: System tray is supported.");

            systemTray = java.awt.SystemTray.getSystemTray();

            URL iconUrl = SystemTray.class.getResource("/icons/app-icon-mini.png");
            if (iconUrl == null) {
                System.out.println("SystemTray: Icon not found at /icons/app-icon-mini.png. Continuing without tray icon.");
                return;
            }

            Image image = new ImageIcon(iconUrl).getImage();

            // Resize image to fit system tray dimension if available
            Dimension trayIconSize = systemTray.getTrayIconSize();
            if (trayIconSize != null && trayIconSize.width > 0 && trayIconSize.height > 0) {
                image = image.getScaledInstance(trayIconSize.width, trayIconSize.height, Image.SCALE_SMOOTH);
                System.out.println("SystemTray: Icon resized to " + trayIconSize.width + "x" + trayIconSize.height);
            }

            // Create tray icon without native AWT PopupMenu to allow Swing JPopupMenu with custom font styling
            trayIcon = new TrayIcon(image, "Hivon");
            trayIcon.setImageAutoSize(true);

            // Double click handler (Action listener fires on default tray icon activation)
            trayIcon.addActionListener(e -> {
                System.out.println("SystemTray: TrayIcon action performed (double click).");
                showSystemTrayPanel();
            });

            // Mouse listener for right-click context menu and explicit double-click
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                        showContextMenu();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                        showContextMenu();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        System.out.println("SystemTray: TrayIcon mouse double-clicked.");
                        showSystemTrayPanel();
                    }
                }
            });

            systemTray.add(trayIcon);
            initialized = true;
            System.out.println("SystemTray: System tray icon added successfully.");

        } catch (AWTException e) {
            System.out.println("SystemTray: AWTException while adding icon to system tray: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("SystemTray: Error initializing system tray: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static JDialog getHiddenDialog() {
        if (hiddenDialog == null) {
            hiddenDialog = new JDialog((java.awt.Frame) null);
            hiddenDialog.setUndecorated(true);
            hiddenDialog.setType(java.awt.Window.Type.POPUP);
            hiddenDialog.setSize(0, 0);
            hiddenDialog.addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowLostFocus(WindowEvent e) {
                    if (activePopup != null && activePopup.isVisible()) {
                        activePopup.setVisible(false);
                    }
                    hiddenDialog.setVisible(false);
                }
            });
        }
        return hiddenDialog;
    }

    /**
     * Shows a Swing JPopupMenu with Segoe UI Semibold 12 font on right click.
     */
    private static void showContextMenu() {
        long now = System.currentTimeMillis();
        if (now - lastMenuOpenTime < 250) {
            return;
        }
        lastMenuOpenTime = now;

        SwingUtilities.invokeLater(() -> {
            JDialog dialog = getHiddenDialog();

            JPopupMenu popup = new JPopupMenu();
            activePopup = popup;

            Font menuFont = new Font("Segoe UI Semibold", Font.PLAIN, 12);
            popup.setFont(menuFont);

            JMenuItem syncItem = new JMenuItem("Sync Progress");
            syncItem.setFont(menuFont);
            syncItem.setActionCommand("sync progress");
            syncItem.addActionListener(e -> {
                System.out.println("SystemTray: Menu item 'Sync Progress' clicked.");
                dialog.setVisible(false);
                SystemTrayPanel.showInDialog();
            });

            JMenuItem exitItem = new JMenuItem("Exit");
            exitItem.setFont(menuFont);
            exitItem.setActionCommand("exit");
            exitItem.addActionListener(e -> {
                System.out.println("SystemTray: Menu item 'Exit' clicked.");
                dialog.setVisible(false);
                exit();
            });

            popup.add(syncItem);
            popup.addSeparator();
            popup.add(exitItem);

            popup.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    SwingUtilities.invokeLater(() -> dialog.setVisible(false));
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                    SwingUtilities.invokeLater(() -> dialog.setVisible(false));
                }
            });

            Point mousePos = MouseInfo.getPointerInfo() != null
                    ? MouseInfo.getPointerInfo().getLocation()
                    : new Point(0, 0);
            Dimension prefSize = popup.getPreferredSize();

            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            Rectangle screenBounds = gc.getBounds();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

            int maxX = screenBounds.x + screenBounds.width - screenInsets.right - prefSize.width - 4;
            int maxY = screenBounds.y + screenBounds.height - screenInsets.bottom - prefSize.height - 4;

            int x = mousePos.x - (prefSize.width / 2);
            int y = mousePos.y - prefSize.height - 4;

            x = Math.max(screenBounds.x, Math.min(x, maxX));
            y = Math.max(screenBounds.y, Math.min(y, maxY));

            dialog.setLocation(x, y);
            dialog.setVisible(true);
            dialog.toFront();
            popup.show(dialog, 0, 0);
            dialog.requestFocus();
        });
    }

    private static long lastPanelOpenTime = 0;

    /**
     * Shows the SystemTrayPanel with debounce protection.
     */
    public static void showSystemTrayPanel() {
        long now = System.currentTimeMillis();
        if (now - lastPanelOpenTime < 350) {
            return;
        }
        lastPanelOpenTime = now;
        SystemTrayPanel.showInDialog();
    }

    /**
     * Cleanly removes the tray icon and exits the application.
     */
    public static void exit() {
        try {
            if (systemTray != null && trayIcon != null) {
                systemTray.remove(trayIcon);
            }
        } catch (Exception e) {
            System.out.println("SystemTray: Error removing tray icon: " + e.getMessage());
        }
        System.exit(0);
    }
}
