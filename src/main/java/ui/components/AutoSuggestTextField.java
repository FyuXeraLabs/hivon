package ui.components;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AutoSuggestTextField {

    public static void attach(JTextField textField, Function<String, List<String>> suggestionProvider) {
        final Map<String, List<String>> cache = new HashMap<>();
        final int MAX_CACHE_SIZE = 100;
        JPopupMenu popup = new JPopupMenu();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFocusable(false);

        // Styling the list for rich aesthetics
        list.setBackground(Color.WHITE);
        list.setForeground(new Color(33, 37, 41));
        list.setSelectionBackground(new Color(0, 123, 255));
        list.setSelectionForeground(Color.WHITE);
        list.setFont(textField.getFont());
        list.setFixedCellHeight(24);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(218, 224, 233)));
        popup.add(scrollPane);
        popup.setFocusable(false);

        // Faster debounce timer (150ms instead of 300ms)
        Timer timer = new Timer(150, null);
        timer.setRepeats(false);

        // Thread-safe container to track the most recent active query
        final String[] activeQuery = new String[1];

        // Helper to update GUI with suggestions
        Runnable showSuggestionsRunnable = () -> {
            String currentQuery = activeQuery[0];
            if (currentQuery == null) return;
            
            List<String> suggestions = cache.get(currentQuery.toLowerCase());
            if (suggestions != null && !suggestions.isEmpty()) {
                listModel.clear();
                for (String suggestion : suggestions) {
                    listModel.addElement(suggestion);
                }
                list.setSelectedIndex(0);

                // Set size and show popup
                scrollPane.setPreferredSize(new Dimension(textField.getWidth(), Math.min(200, suggestions.size() * 24 + 5)));
                popup.pack();
                if (textField.isShowing() && textField.hasFocus()) {
                    popup.show(textField, 0, textField.getHeight());
                }
            } else {
                popup.setVisible(false);
            }
        };

        // Listener to detect typing
        DocumentListener docListener = new DocumentListener() {
            private void update() {
                if (timer.isRunning()) {
                    timer.stop();
                }
                
                String text = textField.getText().trim();
                if (text.length() < 2) {
                    activeQuery[0] = null;
                    popup.setVisible(false);
                    return;
                }

                activeQuery[0] = text;

                // Check cache first for instantaneous response
                if (cache.containsKey(text.toLowerCase())) {
                    showSuggestionsRunnable.run();
                    return;
                }

                // Setup action to run after debounce
                for (ActionListener al : timer.getActionListeners()) {
                    timer.removeActionListener(al);
                }
                timer.addActionListener(e -> {
                    String queryToFetch = text;
                    // Fetch suggestions in a background thread to prevent GUI lockup
                    new SwingWorker<List<String>, Void>() {
                        @Override
                        protected List<String> doInBackground() throws Exception {
                            return suggestionProvider.apply(queryToFetch);
                        }

                        @Override
                        protected void done() {
                            try {
                                List<String> suggestions = get();
                                SwingUtilities.invokeLater(() -> {
                                    // Save to cache (limit size if needed)
                                    if (cache.size() >= MAX_CACHE_SIZE) {
                                        cache.clear(); // Simple cache eviction
                                    }
                                    cache.put(queryToFetch.toLowerCase(), suggestions != null ? suggestions : new ArrayList<>());

                                    // Only show if the user hasn't typed anything else in the meantime (Prevents race conditions)
                                    if (queryToFetch.equals(activeQuery[0]) && textField.hasFocus()) {
                                        showSuggestionsRunnable.run();
                                    }
                                });
                            } catch (Exception ex) {
                                // Ignore or log
                            }
                        }
                    }.execute();
                });
                timer.start();
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

        // Mouse click on suggestion
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                String selected = list.getSelectedValue();
                if (selected != null) {
                    textField.getDocument().removeDocumentListener(docListener);
                    textField.setText(selected);
                    textField.getDocument().addDocumentListener(docListener);
                    popup.setVisible(false);
                }
            }
        });

        // Key navigation on the text field
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
                    } else if (keyCode == KeyEvent.VK_ENTER) {
                        String selected = list.getSelectedValue();
                        if (selected != null) {
                            textField.getDocument().removeDocumentListener(docListener);
                            textField.setText(selected);
                            textField.getDocument().addDocumentListener(docListener);
                            popup.setVisible(false);
                            // Trigger action listeners on text field (like search)
                            for (ActionListener al : textField.getActionListeners()) {
                                al.actionPerformed(new ActionEvent(textField, ActionEvent.ACTION_PERFORMED, ""));
                            }
                        }
                        e.consume();
                    } else if (keyCode == KeyEvent.VK_ESCAPE) {
                        popup.setVisible(false);
                        e.consume();
                    }
                }
            }
        });

        // Hide popup if the window loses focus
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Delay hiding popup to let list selection register clicks
                Timer hideTimer = new Timer(200, evt -> {
                    // Check if popup or text field still has focus conceptually
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
