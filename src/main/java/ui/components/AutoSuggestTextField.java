package ui.components;

import core.utils.RetryHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * High-performance, non-blocking auto-suggestion component for JTextFields.
 * Provides smooth debounced suggestions, LRU caching, silent background data loading,
 * and keyboard navigation without popping up progress dialogs or blocking the EDT.
 *
 * @author Sanod
 */
public class AutoSuggestTextField {

    private static final int DEBOUNCE_DELAY_MS = 200;
    private static final int MAX_CACHE_SIZE = 250;
    private static final int MAX_DISPLAY_SUGGESTIONS = 80;
    private static final int ITEM_ROW_HEIGHT = 26;
    private static final int MAX_POPUP_HEIGHT = 240;
    private static final int MIN_QUERY_LENGTH = 1;

    public static void attach(JTextField textField, Function<String, List<String>> suggestionProvider) {
        // LRU cache — only stores authoritative API results, never filtered subsets
        final Map<String, List<String>> cache = new LinkedHashMap<String, List<String>>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };

        JPopupMenu popup = new JPopupMenu();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFocusable(false);

        // Styling
        list.setBackground(Color.WHITE);
        list.setForeground(new Color(33, 37, 41));
        list.setSelectionBackground(new Color(13, 110, 253));
        list.setSelectionForeground(Color.WHITE);
        list.setFont(textField.getFont());
        list.setFixedCellHeight(ITEM_ROW_HEIGHT);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(218, 224, 233)));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(ITEM_ROW_HEIGHT);
        popup.add(scrollPane);
        popup.setFocusable(false);

        final AtomicBoolean isSelecting = new AtomicBoolean(false);
        final String[] activeQuery = new String[1];
        final SwingWorker<?, ?>[] activeWorker = new SwingWorker<?, ?>[1];

        // Helper: show a list of suggestions in the popup (does NOT touch cache)
        final Consumer<List<String>> showInPopup = (suggestions) -> {
            if (suggestions == null || suggestions.isEmpty()) {
                popup.setVisible(false);
                return;
            }

            listModel.clear();
            int count = Math.min(suggestions.size(), MAX_DISPLAY_SUGGESTIONS);
            for (int i = 0; i < count; i++) {
                listModel.addElement(suggestions.get(i));
            }
            list.setSelectedIndex(0);

            int popupWidth = Math.max(textField.getWidth(), 280);
            int desiredHeight = Math.min(MAX_POPUP_HEIGHT, count * ITEM_ROW_HEIGHT + 4);
            scrollPane.setPreferredSize(new Dimension(popupWidth, desiredHeight));
            popup.pack();

            if (textField.isShowing() && textField.hasFocus()) {
                popup.show(textField, 0, textField.getHeight());
            }
        };

        // Debounce timer
        Timer debounceTimer = new Timer(DEBOUNCE_DELAY_MS, null);
        debounceTimer.setRepeats(false);

        // Document Listener
        DocumentListener docListener = new DocumentListener() {
            private void update() {
                if (isSelecting.get()) {
                    return;
                }

                if (debounceTimer.isRunning()) {
                    debounceTimer.stop();
                }

                // Cancel existing background fetch
                if (activeWorker[0] != null && !activeWorker[0].isDone()) {
                    activeWorker[0].cancel(true);
                }

                String text = textField.getText().trim();
                if (text.length() < MIN_QUERY_LENGTH) {
                    activeQuery[0] = null;
                    popup.setVisible(false);
                    return;
                }

                activeQuery[0] = text;
                String normalizedQuery = text.toLowerCase();

                // 1. Exact cache hit — show authoritative results immediately, no fetch needed
                if (cache.containsKey(normalizedQuery)) {
                    showInPopup.accept(cache.get(normalizedQuery));
                    return;
                }

                // 2. Quick interim results from a cached prefix (show immediately while fetch runs)
                //    This is display-only — we do NOT write the filtered subset into the cache.
                List<String> interimResults = null;
                for (Map.Entry<String, List<String>> entry : cache.entrySet()) {
                    if (normalizedQuery.startsWith(entry.getKey()) && !entry.getValue().isEmpty()) {
                        List<String> filtered = new ArrayList<>();
                        for (String item : entry.getValue()) {
                            if (item.toLowerCase().contains(normalizedQuery)) {
                                filtered.add(item);
                            }
                        }
                        if (!filtered.isEmpty() && (interimResults == null || filtered.size() > interimResults.size())) {
                            interimResults = filtered;
                        }
                    }
                }
                if (interimResults != null) {
                    showInPopup.accept(interimResults);
                }

                // 3. Always fetch from the server for the actual query (replaces interim results)
                for (ActionListener al : debounceTimer.getActionListeners()) {
                    debounceTimer.removeActionListener(al);
                }

                debounceTimer.addActionListener(e -> {
                    String queryToFetch = text;
                    SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
                        @Override
                        protected List<String> doInBackground() {
                            RetryHelper.setSilent(true);
                            try {
                                return suggestionProvider.apply(queryToFetch);
                            } catch (Exception ex) {
                                return new ArrayList<>();
                            } finally {
                                RetryHelper.setSilent(false);
                            }
                        }

                        @Override
                        protected void done() {
                            if (isCancelled()) {
                                return;
                            }
                            try {
                                List<String> results = get();
                                if (results == null) {
                                    results = new ArrayList<>();
                                }

                                // Store authoritative results in cache
                                cache.put(queryToFetch.toLowerCase(), results);

                                // Update popup only if the user hasn't moved on to a different query
                                if (queryToFetch.equals(activeQuery[0]) && textField.hasFocus() && textField.isShowing()) {
                                    showInPopup.accept(results);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    };

                    activeWorker[0] = worker;
                    worker.execute();
                });

                debounceTimer.start();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        };

        textField.getDocument().addDocumentListener(docListener);

        // Apply selected suggestion to text field
        Consumer<String> applySelection = (selected) -> {
            if (selected == null) return;
            isSelecting.set(true);
            try {
                textField.setText(selected);
                popup.setVisible(false);
                for (ActionListener al : textField.getActionListeners()) {
                    al.actionPerformed(new ActionEvent(textField, ActionEvent.ACTION_PERFORMED, ""));
                }
            } finally {
                isSelecting.set(false);
            }
        };

        // Mouse click on suggestion
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                String selected = list.getSelectedValue();
                if (selected != null) {
                    applySelection.accept(selected);
                }
            }
        });

        // Key navigation
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (popup.isShowing()) {
                    int keyCode = e.getKeyCode();
                    if (keyCode == KeyEvent.VK_DOWN) {
                        int index = list.getSelectedIndex();
                        if (index < listModel.getSize() - 1) {
                            list.setSelectedIndex(index + 1);
                            list.ensureIndexIsVisible(index + 1);
                        }
                        e.consume();
                    } else if (keyCode == KeyEvent.VK_UP) {
                        int index = list.getSelectedIndex();
                        if (index > 0) {
                            list.setSelectedIndex(index - 1);
                            list.ensureIndexIsVisible(index - 1);
                        }
                        e.consume();
                    } else if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_TAB) {
                        String selected = list.getSelectedValue();
                        if (selected != null) {
                            applySelection.accept(selected);
                        }
                        e.consume();
                    } else if (keyCode == KeyEvent.VK_ESCAPE) {
                        popup.setVisible(false);
                        e.consume();
                    }
                }
            }
        });

        // Focus listener to hide popup smoothly
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                Timer hideTimer = new Timer(200, evt -> {
                    if (!list.isFocusOwner() && !textField.isFocusOwner()) {
                        popup.setVisible(false);
                    }
                });
                hideTimer.setRepeats(false);
                hideTimer.start();
            }
        });
    }
}
