/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui.dialogs;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import core.config.SettingsManager;

/**
 *
 * @author Sanod D. Mendis
 */
public class SettingsForm extends javax.swing.JFrame {

    // 2d array for themes
    private static final String[][] THEMES = {
        {"Light", "com.formdev.flatlaf.FlatLightLaf"},
        {"Dark", "com.formdev.flatlaf.FlatDarkLaf"},
        {"IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf"},
        {"Darcula", "com.formdev.flatlaf.FlatDarculaLaf"},
        {"macOS Light", "com.formdev.flatlaf.themes.FlatMacLightLaf"},
        {"macOS Dark", "com.formdev.flatlaf.themes.FlatMacDarkLaf"},
        {"Arc", "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme"},
        {"Arc - Orange", "com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme"},
        {"Arc Dark", "com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme"},
        {"Arc Dark - Orange", "com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme"},
        {"Carbon", "com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme"},
        {"Cobalt 2", "com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme"},
        {"Cyan Light", "com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme"},
        {"Dark Flat", "com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme"},
        {"Dark Purple", "com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme"},
        {"Dracula", "com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme"},
        {"Gradianto Dark Fuchsia", "com.formdev.flatlaf.intellijthemes.FlatGradiantoDarkFuchsiaIJTheme"},
        {"Gradianto Deep Ocean", "com.formdev.flatlaf.intellijthemes.FlatGradiantoDeepOceanIJTheme"},
        {"Gradianto Midnight Blue", "com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme"},
        {"Gradianto Nature Green", "com.formdev.flatlaf.intellijthemes.FlatGradiantoNatureGreenIJTheme"},
        {"Gray", "com.formdev.flatlaf.intellijthemes.FlatGrayIJTheme"},
        {"Gruvbox Dark Hard", "com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme"},
        {"Hiberbee Dark", "com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme"},
        {"High Contrast", "com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme"},
        {"Light Flat", "com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme"},
        {"Material Design Dark", "com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme"},
        {"Monocai", "com.formdev.flatlaf.intellijthemes.FlatMonocaiIJTheme"},
        {"Monokai Pro", "com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme"},
        {"Nord", "com.formdev.flatlaf.intellijthemes.FlatNordIJTheme"},
        {"One Dark", "com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme"},
        {"Solarized Dark", "com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme"},
        {"Solarized Light", "com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme"},
        {"Spacegray", "com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme"},
        {"Vuesion", "com.formdev.flatlaf.intellijthemes.FlatVuesionIJTheme"},
        {"Xcode Dark", "com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme"},
        {"Arc Dark (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTArcDarkIJTheme"},
        {"Atom One Dark (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTAtomOneDarkIJTheme"},
        {"Atom One Light (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTAtomOneLightIJTheme"},
        {"Dracula (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTDraculaIJTheme"},
        {"GitHub (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubIJTheme"},
        {"GitHub Dark (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubDarkIJTheme"},
        {"Light Owl (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTLightOwlIJTheme"},
        {"Material Darker (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme"},
        {"Material Deep Ocean (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDeepOceanIJTheme"},
        {"Material Lighter (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialLighterIJTheme"},
        {"Material Oceanic (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialOceanicIJTheme"},
        {"Material Palenight (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialPalenightIJTheme"},
        {"Monokai Pro (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMonokaiProIJTheme"},
        {"Moonlight (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMoonlightIJTheme"},
        {"Night Owl (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTNightOwlIJTheme"},
        {"Solarized Dark (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTSolarizedDarkIJTheme"},
        {"Solarized Light (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTSolarizedLightIJTheme"},
    };

    /**
     * Creates new form Settings
     */
    public SettingsForm() {
        initComponents();
        this.setLocationRelativeTo(null);

        // load existing settings
        soundToggleBtn.setSelected(SettingsManager.isSoundEnabled());
        soundToggleBtn.setText(SettingsManager.isSoundEnabled() ? "ON" : "OFF");
        int initialVol = SettingsManager.getSoundVolume();
        soundSlider.setValue(initialVol);
        lblVol.setText(initialVol + "%");

        // populate theme combobox
        cmbTheme.removeAllItems();
        String currentTheme = SettingsManager.getTheme();
        int selectedIndex = 0;
        for (int i = 0; i < THEMES.length; i++) {
            cmbTheme.addItem(THEMES[i][0]);
            if (THEMES[i][1].equals(currentTheme)) {
                selectedIndex = i;
            }
        }
        cmbTheme.setSelectedIndex(selectedIndex);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        soundToggleBtn = new javax.swing.JToggleButton();
        soundSlider = new javax.swing.JSlider();
        jLabel2 = new javax.swing.JLabel();
        cmbTheme = new javax.swing.JComboBox<>();
        lblVol = new javax.swing.JLabel();
        btnApply = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");
        setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());
        setResizable(false);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/sound-14.png"))); // NOI18N
        jLabel1.setText(" Sounds");

        soundToggleBtn.setText("ON");
        soundToggleBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                soundToggleBtnActionPerformed(evt);
            }
        });

        soundSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                soundSliderStateChanged(evt);
            }
        });

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/theme-14.png"))); // NOI18N
        jLabel2.setText(" Theme");

        cmbTheme.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lblVol.setText("vol");

        btnApply.setText("Apply");
        btnApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnApplyActionPerformed(evt);
            }
        });

        btnReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/refresh-2-14.png"))); // NOI18N
        btnReset.setText(" Reset");
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });

        btnCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/cancel-14.png"))); // NOI18N
        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCancel)
                .addGap(12, 12, 12))
            .addGroup(layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(soundToggleBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(soundSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblVol))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cmbTheme, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnApply)))
                .addContainerGap(32, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(soundToggleBtn)
                    .addComponent(soundSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblVol))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(cmbTheme, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnApply))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnReset)
                    .addComponent(btnCancel))
                .addGap(12, 12, 12))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnApplyActionPerformed
        int idx = cmbTheme.getSelectedIndex();
        if (idx >= 0 && idx < THEMES.length) {
            String themeClass = THEMES[idx][1];
            SettingsManager.setTheme(themeClass);
            try {
                javax.swing.UIManager.setLookAndFeel(themeClass);
                com.formdev.flatlaf.FlatLaf.updateUI();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to apply theme: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }//GEN-LAST:event_btnApplyActionPerformed

    private void soundToggleBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_soundToggleBtnActionPerformed
        boolean enabled = soundToggleBtn.isSelected();
        SettingsManager.setSoundEnabled(enabled);
        soundToggleBtn.setText(enabled ? "ON" : "OFF");
    }//GEN-LAST:event_soundToggleBtnActionPerformed

    private void soundSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_soundSliderStateChanged
        int value = soundSlider.getValue();
        SettingsManager.setSoundVolume(value);
        lblVol.setText(value + "%");
    }//GEN-LAST:event_soundSliderStateChanged

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        SettingsManager.resetToDefaults();
        
        // reset sound toggle
        soundToggleBtn.setSelected(SettingsManager.isSoundEnabled());
        soundToggleBtn.setText(SettingsManager.isSoundEnabled() ? "ON" : "OFF");
        
        // reset sound slider
        int vol = SettingsManager.getSoundVolume();
        soundSlider.setValue(vol);
        lblVol.setText(vol + "%");
        
        // reset theme combobox
        String currentTheme = SettingsManager.getTheme();
        int selectedIndex = 0;
        for (int i = 0; i < THEMES.length; i++) {
            if (THEMES[i][1].equals(currentTheme)) {
                selectedIndex = i;
                break;
            }
        }
        cmbTheme.setSelectedIndex(selectedIndex);
        
        // apply default theme instantly
        try {
            javax.swing.UIManager.setLookAndFeel(currentTheme);
            com.formdev.flatlaf.FlatLaf.updateUI();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Failed to apply default theme: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }//GEN-LAST:event_btnResetActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SettingsForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SettingsForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SettingsForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SettingsForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SettingsForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnApply;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnReset;
    private javax.swing.JComboBox<String> cmbTheme;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lblVol;
    private javax.swing.JSlider soundSlider;
    private javax.swing.JToggleButton soundToggleBtn;
    // End of variables declaration//GEN-END:variables
}
