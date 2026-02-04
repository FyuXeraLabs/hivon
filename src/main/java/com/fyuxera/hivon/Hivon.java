/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.fyuxera.hivon;

import javax.swing.UIManager;
import ui.LoginFrame;

/**
 *
 * @author Sanod D. Mendis
 */
public class Hivon {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatIntelliJLaf");
        } catch (Exception e) {
            e.printStackTrace();
        }
                
        LoginFrame login = new LoginFrame();
        login.setVisible(true);
    }
}
