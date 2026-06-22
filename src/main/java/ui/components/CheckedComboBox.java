package ui.components;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class CheckedComboBox<T> extends JComboBox<T> {
    private final Set<T> selectedItems = new HashSet<>();
    private final List<T> allItems = new ArrayList<>();
    protected boolean keepOpen;
    private JTextField filterField;
    private String placeholder = "Select Items...";
    private final JPanel rendererPanel = new JPanel(new BorderLayout());
    private String selectAllLabel = null;
    private JList<?> popupList;

    public CheckedComboBox() {
        super();
        init();
    }

    public CheckedComboBox(T[] items) {
        super();
        init();
        for (T item : items) {
            addItem(item);
        }
    }

    private void init() {
        setRenderer(new CheckBoxRenderer());
        setEditable(true);
        filterField = (JTextField) getEditor().getEditorComponent();
        
        filterField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    return;
                }
                String text = filterField.getText();
                filterItems(text);
            }
        });

        filterField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (selectedItems.isEmpty()) {
                    filterField.setText("");
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                updateEditorText();
            }
        });

        addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                updateEditorText();
            }
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                updateEditorText();
            }
        });
    }

    public void setSelectAllLabel(String label) {
        this.selectAllLabel = label;
    }

    private boolean isSelectAllItem(T item) {
        return selectAllLabel != null && item != null && item.toString().equals(selectAllLabel);
    }

    private boolean isRegularItem(T item) {
        return !isSelectAllItem(item);
    }

    private int countSelectedRegular() {
        int count = 0;
        for (T item : selectedItems) {
            if (isRegularItem(item)) count++;
        }
        return count;
    }

    private int countAllRegular() {
        int count = 0;
        for (T item : allItems) {
            if (isRegularItem(item)) count++;
        }
        return count;
    }

    private void syncSelectAllState() {
        if (selectAllLabel == null) return;

        T saItem = null;
        for (T item : allItems) {
            if (isSelectAllItem(item)) {
                saItem = item;
                break;
            }
        }
        if (saItem == null) return;

        boolean allRegularSelected = true;
        int regularTotal = 0;
        for (T item : allItems) {
            if (isRegularItem(item)) {
                regularTotal++;
                if (!selectedItems.contains(item)) {
                    allRegularSelected = false;
                }
            }
        }

        if (regularTotal == 0) return;

        if (allRegularSelected) {
            selectedItems.add(saItem);
        } else {
            selectedItems.remove(saItem);
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        javax.accessibility.Accessible a = getAccessibleContext().getAccessibleChild(0);
        if (a instanceof ComboPopup) {
            popupList = ((ComboPopup) a).getList();
            popupList.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        keepOpen = true;
                        int index = popupList.locationToIndex(e.getPoint());
                        if (index >= 0) {
                            T item = (T) getModel().getElementAt(index);
                            toggleSelection(item);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void setPopupVisible(boolean v) {
        if (keepOpen) {
            keepOpen = false;
        } else {
            super.setPopupVisible(v);
        }
    }

    private void filterItems(String text) {
        DefaultComboBoxModel<T> model = (DefaultComboBoxModel<T>) getModel();
        model.removeAllElements();
        for (T item : allItems) {
            if (item.toString().toLowerCase().contains(text.toLowerCase())) {
                model.addElement(item);
            }
        }
        setPopupVisible(true);
        filterField.setText(text);
    }

    @Override
    public void addItem(T item) {
        allItems.add(item);
        super.addItem(item);
    }

    @Override
    public void removeAllItems() {
        allItems.clear();
        selectedItems.clear();
        super.removeAllItems();
    }

    public void addSelectAllItem(String label) {
        if (label == null) return;
        this.selectAllLabel = label;
        for (T item : allItems) {
            if (isSelectAllItem(item)) return;
        }
        addItem((T) label);
    }

    public void toggleSelection(T item) {
        if (isSelectAllItem(item)) {
            if (selectedItems.contains(item)) {
                selectedItems.clear();
            } else {
                selectedItems.clear();
                selectedItems.addAll(allItems);
            }
        } else {
            if (selectedItems.contains(item)) {
                selectedItems.remove(item);
            } else {
                selectedItems.add(item);
            }
            syncSelectAllState();
        }
        updateEditorText();
        repaint();
        if (popupList != null) {
            popupList.repaint();
        }
        fireAction();
    }

    private void updateEditorText() {
        int selectedRegular = countSelectedRegular();
        int totalRegular = countAllRegular();

        if (selectedRegular == 0) {
            filterField.setText("");
        } else if (selectedRegular == totalRegular) {
            filterField.setText("All Selected");
        } else {
            filterField.setText(selectedRegular + " items selected");
        }
    }

    private void fireAction() {
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "selectionChanged");
        for (ActionListener l : getActionListeners()) {
            l.actionPerformed(e);
        }
    }

    @Override
    public void setSelectedIndex(int anIndex) {
        if (anIndex == -1) {
            super.setSelectedIndex(-1);
        } else {
            super.setSelectedIndex(anIndex);
        }
    }

    public Set<T> getSelectedItems() {
        Set<T> result = new HashSet<>();
        for (T item : selectedItems) {
            if (isRegularItem(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public List<T> getAllItems() {
        return new ArrayList<>(allItems);
    }

    public void setSelectedItems(Collection<T> items) {
        selectedItems.clear();
        for (T item : items) {
            if (isRegularItem(item)) {
                selectedItems.add(item);
            }
        }
        syncSelectAllState();
        updateEditorText();
        repaint();
        fireAction();
    }

    public void clearSelection() {
        selectedItems.clear();
        updateEditorText();
        repaint();
        fireAction();
    }

    private class CheckBoxRenderer implements ListCellRenderer<T> {
        private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
        private final JCheckBox checkBox = new JCheckBox();

        @Override
        public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
            if (index < 0) {
                JLabel label = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                int selectedRegular = countSelectedRegular();
                int totalRegular = countAllRegular();
                if (selectedRegular == 0) {
                    label.setText(placeholder);
                    label.setForeground(Color.GRAY);
                } else if (selectedRegular == totalRegular) {
                    label.setText("All Selected");
                    label.setForeground(list.getForeground());
                } else {
                    label.setText(selectedRegular + " Items Selected");
                    label.setForeground(list.getForeground());
                }
                return label;
            }

            rendererPanel.removeAll();
            checkBox.setSelected(selectedItems.contains(value));
            checkBox.setOpaque(false);
            
            Component c = defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            rendererPanel.setBackground(c.getBackground());
            rendererPanel.add(checkBox, BorderLayout.WEST);
            rendererPanel.add(c, BorderLayout.CENTER);
            
            return rendererPanel;
        }
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
}
