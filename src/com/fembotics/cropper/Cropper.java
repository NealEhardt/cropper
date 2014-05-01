/*
 * To-Do list
 *
 *
 * choose crop resolution (megapixels)
 * show uncaught exceptions in a popup
 *
 */

package com.fembotics.cropper;

import java.awt.*;
import java.util.logging.*;
import javax.swing.*;

/**
 *
 * @author Neal Ehardt <nialsh@gmail.com>
 */
public class Cropper extends JFrame {
    /*
    JFileChooser chooser = null;

    JLabel statusbar; */
    FeaturePainter painter;
    
    Cropper() {
        super("Cropper"); // set frame title
        setSize(600, 500);
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            Logger.getLogger(Cropper.class.getName()).log(Level.SEVERE, null, ex);
        }

        Container c = getContentPane();

        c.setLayout(new CardLayout());

        c.add(painter = new FeaturePainter(), "the only effing card");

        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    static Cropper instance;
    public static void main(String[] args)
    {
        instance = new Cropper();
    }
}
