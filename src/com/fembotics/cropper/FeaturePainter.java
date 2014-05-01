/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FeaturePainter.java
 *
 * Created on Nov 24, 2010, 7:13:39 PM
 */

package com.fembotics.cropper;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.undo.*;

/**
 *
 * @author Neal Ehardt <nialsh@gmail.com>
 */
public class FeaturePainter extends javax.swing.JPanel {

    /** image width */
    int W;
    /** image height */
    int H;
    
    BufferedImage rawImage, image;
    BufferedImage background, cropped, blurred;
    Segmenter segmenter;
    Path2D edgePath;
    
    File imageFile;
    
    Thread repainter, segmenterUpdater;
    UndoManager undoManager;

    Frame parentFrame;
    FileDialog opener, saver;
    String suffixFilter;
    
    double zoom = 1;
    boolean shrunken;

    /** Creates new form FeaturePainter */
    public FeaturePainter() {
        initComponents();

        setupDialogs();
        setupUndoStuff();
        setupPaintPanel();
        sleepUI();
    }
    
    
    private void setupPaintPanel() {
        paintPanel.addMouseListener(adapter);
        paintPanel.addMouseMotionListener(adapter);

        // repaint regularly so the background will move
        repainter = new Thread("slow repainter") {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(FeaturePainter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if(paintPanel.isShowing())
                        paintPanel.repaint();
                }
            }
        };
        repainter.setDaemon(true);
        repainter.start();


        // run the segmenter in a background thread
        segmenterUpdater = new Thread("segmenter updater") {
            @Override
            public void run() {
                while(true) {
                    Segmenter s = segmenter; // use a local in case the UI thread changes it
                    
                    try {
                        if(s != null && s.isOutOfDate()) {
                            progressBar.setVisible(true);
                            if(s.update()) {
                                cropped = s.getCropped();
                                edgePath = s.getEdgePath();
                                updateBlur();

                                statusLabel.setText("Done");
                                progressBar.setVisible(false);
                                paintPanel.repaint();
                            }
                        }
                        synchronized(this) {
                            wait();
                        }
                    } catch (Exception e) {
                        alertException(e, "Segmenter error");
                    }
                }
            }
        };
        segmenterUpdater.setDaemon(true);
        segmenterUpdater.setPriority(Thread.MIN_PRIORITY);
        segmenterUpdater.start();
    }


    private void setupDialogs() {
        // find my parent frame
        Container c = this;
        while(c != null && !(c instanceof Frame))
            c = c.getParent();
        parentFrame = (Frame)c;

        String[] suffixes = ImageIO.getReaderFileSuffixes();
        suffixFilter = "*."+suffixes[0];
        for(int i = 1; i < suffixes.length; ++i)
            suffixFilter += "; *."+suffixes[i];

        opener = new FileDialog(parentFrame, "Open image", FileDialog.LOAD);
        saver = new FileDialog(parentFrame, "Save cropped image", FileDialog.SAVE);


        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                alertException(e, "Uncaught Exception");
                throw (RuntimeException)e;
            }
        });
    }

    private void alertException(Throwable e, String title) {
        String message = e.toString();
        for(StackTraceElement i : e.getStackTrace())
            message += "\n    "+i;
        JOptionPane.showMessageDialog(parentFrame, message, title, JOptionPane.ERROR_MESSAGE);
    }


    private void setupUndoStuff() {
        
        undoManager = new UndoManager();
        updateUndoButtons();
        
        // the rest of this method just sets up the hotkeys. Brevity is not Java's thing
        String UNDO = "Undo action key";
        String REDO = "Redo action key";
        Action undoAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { undo(); }
        };
        Action redoAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { redo(); }
        };

        getActionMap().put(UNDO, undoAction);
        getActionMap().put(REDO, redoAction);

        int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        // these events are mutually exclusive so they must all be mapped
        InputMap[] inputMaps = new InputMap[] {
            getInputMap(JComponent.WHEN_FOCUSED),
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT),
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW),
        };
        for(InputMap i : inputMaps) {
            i.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, modifier), UNDO);
            i.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, modifier), REDO);
        }
    }

    void updateUndoButtons() {
        undoButton.setEnabled(undoManager.canUndo());
        redoButton.setEnabled(undoManager.canRedo());
    }
    

    public void undo() {
        if(undoManager.canUndo()) {
            System.out.println("undo");
            undoManager.undo();
            synchronized(segmenterUpdater) {
                segmenterUpdater.notify();
            }
            updateUndoButtons();
        }
    }
    public void redo() {
        if(undoManager.canRedo()) {
            System.out.println("redo");
            undoManager.redo();
            synchronized(segmenterUpdater) {
                segmenterUpdater.notify();
            }
            updateUndoButtons();
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel2 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        openButton = new javax.swing.JButton();
        jPanel12 = new javax.swing.JPanel();
        saveButton = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        openLabel = new javax.swing.JLabel();
        resetButton = new javax.swing.JButton();
        jPanel13 = new javax.swing.JPanel();
        blurPanel = new javax.swing.JPanel();
        blurSlider = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jPanel15 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        editButton = new javax.swing.JRadioButton();
        previewButton = new javax.swing.JRadioButton();
        jPanel7 = new javax.swing.JPanel();
        bgSlider = new javax.swing.JSlider();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        zoomSlider = new javax.swing.JSlider();
        paintScroller = new javax.swing.JScrollPane();
        paintPanel = new javax.swing.JPanel() {
            public void paint(Graphics g) {
                paintImage((Graphics2D)g);
            }
        };
        jPanel4 = new javax.swing.JPanel();
        undoButton = new javax.swing.JButton();
        redoButton = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        jPanel6 = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        setLayout(new java.awt.BorderLayout());

        jPanel2.setMinimumSize(new java.awt.Dimension(113, 50));
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel11.setAlignmentX(0.0F);
        jPanel11.setLayout(new javax.swing.BoxLayout(jPanel11, javax.swing.BoxLayout.LINE_AXIS));

        openButton.setText("Open Image...");
        openButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });
        jPanel11.add(openButton);

        jPanel12.setMaximumSize(new java.awt.Dimension(10, 10));

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 10, Short.MAX_VALUE)
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 10, Short.MAX_VALUE)
        );

        jPanel11.add(jPanel12);

        saveButton.setText("Save Cropped as PNG...");
        saveButton.setAlignmentX(1.0F);
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        jPanel11.add(saveButton);

        jPanel1.add(jPanel11);

        jPanel8.setAlignmentX(0.0F);
        jPanel8.setLayout(new javax.swing.BoxLayout(jPanel8, javax.swing.BoxLayout.LINE_AXIS));

        jPanel9.setMaximumSize(new java.awt.Dimension(10, 10));
        jPanel9.setMinimumSize(new java.awt.Dimension(0, 0));

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 10, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 10, Short.MAX_VALUE)
        );

        jPanel8.add(jPanel9);

        openLabel.setText("no image loaded yet...");
        jPanel8.add(openLabel);

        jPanel1.add(jPanel8);

        resetButton.setText("Reset");
        resetButton.setToolTipText("Reset the whole image and start cropping again from scratch");
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });
        jPanel1.add(resetButton);

        jPanel2.add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel13.setLayout(new javax.swing.BoxLayout(jPanel13, javax.swing.BoxLayout.LINE_AXIS));

        blurPanel.setLayout(new java.awt.BorderLayout());

        blurSlider.setMaximum(30);
        blurSlider.setMinimum(1);
        blurSlider.setPaintTicks(true);
        blurSlider.setValue(1);
        blurSlider.setPreferredSize(new java.awt.Dimension(100, 31));
        blurSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                blurSliderStateChanged(evt);
            }
        });
        blurPanel.add(blurSlider, java.awt.BorderLayout.CENTER);

        jLabel4.setText("Hard");
        blurPanel.add(jLabel4, java.awt.BorderLayout.LINE_START);

        jLabel5.setText("Soft");
        blurPanel.add(jLabel5, java.awt.BorderLayout.LINE_END);

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Edge Softness");
        blurPanel.add(jLabel6, java.awt.BorderLayout.PAGE_START);

        jPanel13.add(blurPanel);

        jPanel15.setMinimumSize(new java.awt.Dimension(0, 0));
        jPanel15.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel15.setLayout(new javax.swing.BoxLayout(jPanel15, javax.swing.BoxLayout.LINE_AXIS));
        jPanel13.add(jPanel15);

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel7.setText("View Mode");
        jLabel7.setAlignmentX(0.5F);
        jPanel3.add(jLabel7);

        jPanel14.setPreferredSize(new java.awt.Dimension(30, 5));

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 63, Short.MAX_VALUE)
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel3.add(jPanel14);

        jPanel16.setLayout(new java.awt.GridLayout(2, 0));

        buttonGroup1.add(editButton);
        editButton.setSelected(true);
        editButton.setText("Edit");
        jPanel16.add(editButton);

        buttonGroup1.add(previewButton);
        previewButton.setText("Preview");
        jPanel16.add(previewButton);

        jPanel3.add(jPanel16);

        jPanel13.add(jPanel3);

        jPanel2.add(jPanel13, java.awt.BorderLayout.EAST);

        add(jPanel2, java.awt.BorderLayout.NORTH);

        jPanel7.setPreferredSize(new java.awt.Dimension(64, 382));
        jPanel7.setRequestFocusEnabled(false);

        bgSlider.setMajorTickSpacing(25);
        bgSlider.setOrientation(javax.swing.JSlider.VERTICAL);
        bgSlider.setPaintTicks(true);
        bgSlider.setValue(75);
        bgSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                bgSliderStateChanged(evt);
            }
        });

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Background");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Zoom");

        zoomSlider.setMajorTickSpacing(10);
        zoomSlider.setMaximum(150);
        zoomSlider.setOrientation(javax.swing.JSlider.VERTICAL);
        zoomSlider.setPaintTicks(true);
        zoomSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                zoomSliderStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap(23, Short.MAX_VALUE)
                .addComponent(bgSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap(23, Short.MAX_VALUE)
                .addComponent(zoomSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(zoomSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bgSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        add(jPanel7, java.awt.BorderLayout.EAST);

        paintScroller.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                paintScrollerComponentResized(evt);
            }
        });

        paintPanel.setPreferredSize(new java.awt.Dimension(100, 100));

        javax.swing.GroupLayout paintPanelLayout = new javax.swing.GroupLayout(paintPanel);
        paintPanel.setLayout(paintPanelLayout);
        paintPanelLayout.setHorizontalGroup(
            paintPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 549, Short.MAX_VALUE)
        );
        paintPanelLayout.setVerticalGroup(
            paintPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 372, Short.MAX_VALUE)
        );

        paintScroller.setViewportView(paintPanel);

        add(paintScroller, java.awt.BorderLayout.CENTER);

        jPanel4.setAlignmentX(0.0F);
        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.LINE_AXIS));

        undoButton.setText("Undo");
        undoButton.setToolTipText("(Ctrl+Z)");
        undoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoButtonActionPerformed(evt);
            }
        });
        jPanel4.add(undoButton);

        redoButton.setText("Redo");
        redoButton.setToolTipText("(Ctrl+Y)");
        redoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redoButtonActionPerformed(evt);
            }
        });
        jPanel4.add(redoButton);

        jPanel10.setMinimumSize(new java.awt.Dimension(5, 5));

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );

        jPanel4.add(jPanel10);

        progressBar.setMaximumSize(new java.awt.Dimension(32767, 100));
        progressBar.setMinimumSize(new java.awt.Dimension(100, 20));
        progressBar.setPreferredSize(null);
        jPanel4.add(progressBar);

        jPanel6.setMinimumSize(new java.awt.Dimension(5, 5));
        jPanel6.setPreferredSize(new java.awt.Dimension(5, 5));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );

        jPanel4.add(jPanel6);

        statusLabel.setText("Ready");
        jPanel4.add(statusLabel);

        jPanel5.setPreferredSize(new java.awt.Dimension(30000, 0));

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 10));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("© 2011 Neal Ehardt");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(264, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
        );

        jPanel4.add(jPanel5);

        add(jPanel4, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        opener.setFile(suffixFilter);
        opener.show();
        if(opener.getFile() == null)
            return; // user hit cancel

        runWithModal("Opening image", "Opening image...", new Runnable() {
            public void run() {
                try {
                    File f = new File(opener.getDirectory() + opener.getFile());
                    rawImage = ImageIO.read(f);
                    if(rawImage == null)
                        throw new IOException();
                    imageFile = f;
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(parentFrame,
                            "Error opening file \""+opener.getFile()+"\"",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                setupImage();
            }
        });
    }//GEN-LAST:event_openButtonActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        runWithModal("Saving", "Saving image...",
                new Runnable() {
                    public void run() {
                        saveImage();
                    }
                });
    }//GEN-LAST:event_saveButtonActionPerformed

    private void undoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoButtonActionPerformed
        undo();
    }//GEN-LAST:event_undoButtonActionPerformed

    private void redoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_redoButtonActionPerformed
        redo();
    }//GEN-LAST:event_redoButtonActionPerformed

    private void bgSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_bgSliderStateChanged
        editButton.setSelected(true);
        updateBackground();
    }//GEN-LAST:event_bgSliderStateChanged

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonActionPerformed
        runWithModal("Resetting", "Resetting image...",
                new Runnable() {
                    public void run() {
                        setupImage();
                    }
                });
    }//GEN-LAST:event_resetButtonActionPerformed

    private void zoomSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_zoomSliderStateChanged
        updateZoom();
    }//GEN-LAST:event_zoomSliderStateChanged

    private void paintScrollerComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_paintScrollerComponentResized
        updateZoom();
    }//GEN-LAST:event_paintScrollerComponentResized

    private void blurSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_blurSliderStateChanged
        updateBlur();
    }//GEN-LAST:event_blurSliderStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider bgSlider;
    private javax.swing.JPanel blurPanel;
    private javax.swing.JSlider blurSlider;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JRadioButton editButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JButton openButton;
    private javax.swing.JLabel openLabel;
    private javax.swing.JPanel paintPanel;
    private javax.swing.JScrollPane paintScroller;
    private javax.swing.JRadioButton previewButton;
    protected javax.swing.JProgressBar progressBar;
    private javax.swing.JButton redoButton;
    private javax.swing.JButton resetButton;
    private javax.swing.JButton saveButton;
    protected javax.swing.JLabel statusLabel;
    private javax.swing.JButton undoButton;
    private javax.swing.JSlider zoomSlider;
    // End of variables declaration//GEN-END:variables

    
    void setupImage() {
        statusLabel.setText("loading image...");

        // free up some memory
        if(segmenter != null) {
            segmenter.destroy();
            segmenter = null;
        }
        background = null;
        blurred = null;
        cropped = null;
        edgePath = null;
        undoManager = new UndoManager();
        System.gc();

        W = rawImage.getWidth();
        H = rawImage.getHeight();
        
        final double aMilli = 1000000; // a milli a milli a milli a milli
        double mp = W*H / aMilli;
        double m = 2;

        shrunken = false;

        if(mp > m) {
            NumberFormat f = new DecimalFormat("#.0");
            int option = JOptionPane.showConfirmDialog(parentFrame,
                            "This is a "+f.format(mp)+" megapixel image.  "+
                            "Large images can cause Cropper to become unstable so it will be\n"+
                            "shrunk to "+f.format(m)+" megapixels for cropping",
                            "Large image",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.INFORMATION_MESSAGE);
            
            switch(option) {
                case JOptionPane.CANCEL_OPTION:
                case JOptionPane.CLOSED_OPTION:
                    sleepUI();
                    return;
                    
                default:
                    double r = (double)W / H;
                    W = (int)Math.sqrt(m*aMilli*r);
                    H = (int)Math.sqrt(m*aMilli/r);
                    shrunken = true;
                    System.out.println("new dimensions: "+W+", "+H);
                    break;
            }
        }

        // scale that mofo
        // (even if it doesn't need scaling, it will be copied to ensure ARGB)
        image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        AffineTransform xform = AffineTransform.getScaleInstance(
                                    (double)W / rawImage.getWidth(),
                                    (double)H / rawImage.getHeight());
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(rawImage, xform, null);
        g.dispose();


        // image ready; refresh the UI
        segmenter = new Segmenter(image, this);
        background = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        image.copyData(background.getRaster());
        blurred = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        image.copyData(blurred.getRaster());
        wakeUI();
    }

    void saveImage() {
        if(cropped == null) {
            JOptionPane.showMessageDialog(this,
                    "Please crop the image before saving it!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int choice = JOptionPane.NO_OPTION;
        if(shrunken)
            choice = JOptionPane.showConfirmDialog(parentFrame,
                    "You chose to crop a shrunken version of the original image.\n"+
                    "This lo-res crop can be enlarged and applied to the original image.\n"+
                    "(This will result in some quality loss but only in the transparency channel)\n\n"+
                    "Save at high resolution?",
                                                "Save to full resolution?",
                                                JOptionPane.YES_NO_CANCEL_OPTION);

        if(choice == JOptionPane.CANCEL_OPTION)
            return;
        
        String name = imageFile.getName();
        saver.setFile(name.substring(0, name.lastIndexOf('.')) + " (cropped).png");
        saver.setDirectory(imageFile.getParent());
        saver.setVisible(true);
        // ... and it blocks until the dialog closes...
        
        if(saver.getFile() == null)
            return; // user hit cancel

        name = saver.getDirectory() + saver.getFile();
        if(!name.endsWith(".png"))
            name += ".png";
        File f = new File(name);

        BufferedImage toSave = blurred;


        if(choice == JOptionPane.YES_OPTION) {
            // we want the alpha channel from the blurred image and RGB from rawImage

            // scale up the cropped, blurred image
            int w = rawImage.getWidth();
            int h = rawImage.getHeight();
            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            AffineTransform xform = AffineTransform.getScaleInstance(
                                                    (double)w / W,
                                                    (double)h / H);
            g.drawImage(blurred, xform, null);
            g.dispose();

            // copy all but the alpha channel back from the raw image
            WritableRaster in = scaled.getRaster(), out = rawImage.getRaster();
            int[] band = out.getSamples(0, 0, w, h, 0, (int[])null);
            in.setSamples(0, 0, w, h, 0, band);
            band = out.getSamples(0, 0, w, h, 1, band);
            in.setSamples(0, 0, w, h, 1, band);
            band = out.getSamples(0, 0, w, h, 2, band);
            in.setSamples(0, 0, w, h, 2, band);

            toSave = scaled;
        }
        

        try {
            ImageIO.write(toSave, "png", f);
        } catch (IOException ex) {
            alertException(ex, "File write failed");
        }
    }

    void paintImage(Graphics2D g) {
        g.clearRect(0, 0, paintPanel.getWidth(), paintPanel.getHeight());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw checkerboard
        final int S = 10;
        final double V = .5;
        double t = System.nanoTime() / 1000000000.0;
        double off = (t%(S/V)) * V;

        g.setColor(Color.lightGray);
        int n = paintPanel.getWidth() / S + 5;
        int m = paintPanel.getHeight() / S + 3;
        Rectangle2D.Double rect = new Rectangle2D.Double(0,0,S,S);
        for(int i = 0; i < n; i++)
            for(int j = 0; j < m; j++)
                if(i%2 == j%2) {
                    rect.x = (i-4)*S + off*4;
                    rect.y = (j-2)*S + off*2;
                    g.fill(rect);
                }


        // zoom in on everything drawn after this point
        g.scale(zoom, zoom);
        
        if(previewButton.isSelected()) {
            if(blurred != null)
                g.drawImage(blurred, null, null);
        } else {
            if(background != null)
                g.drawImage(background, null, null);

            if(blurred != null)
                g.drawImage(blurred, null, null);

            if(edgePath != null) {
                g.setColor(Color.blue);
                float fzoom = (float)zoom;
                float K = 2000;
                float phase = (float)((t*10/zoom)%K) + K;
                g.setStroke(new BasicStroke(1/fzoom,
                                            BasicStroke.CAP_BUTT,
                                            BasicStroke.JOIN_MITER, 10,
                                            new float[]{15/fzoom, 10/fzoom},
                                            phase));
                g.draw(edgePath);
            }

            if(segmenter != null)
                g.drawImage(segmenter.getOverlay(), null, null);
        }
    }

    void updateBlur() {
        if(cropped == null)
            return;

        int radius = blurSlider.getValue();

        alphaBuffer = cropped.getRaster().getSamples(0, 0, W, H, 3, alphaBuffer);
        int[] out = new int[alphaBuffer.length];

        blurChannel(alphaBuffer, out, W, H, radius);
        blurChannel(out, alphaBuffer, H, W, radius);

        blurred.getRaster().setSamples(0, 0, W, H, 3, alphaBuffer);
    }

    /** 1-D box blur + transpose (call it twice to get a 2D box blur) */
    public static void blurChannel(int[] in, int[] out, int width, int height, int radius) {
        int widthMinus1 = width-1;
        int tableSize = 2*radius+1;
        int divide[] = new int[256*tableSize];

        for ( int i = 0; i < 256*tableSize; i++ )
            divide[i] = i/tableSize;

        int inIndex = 0;

        for (int y = 0; y < height; y++) {
            int outIndex = y;
            //int ta = 0, tr = 0, tg = 0, tb = 0;
            int t = 0;

            for (int i = -radius; i <= radius; i++)
                t += in[inIndex + Math.min(Math.max(0, i), widthMinus1)];

            for ( int x = 0; x < width; x++ ) {
                out[outIndex] = divide[t];

                int i1 = inIndex + Math.min(x+radius+1, widthMinus1);
                int i2 = inIndex + Math.max(0, x-radius);

                t += in[i1] - in[i2];
                outIndex += height;
            }
            inIndex += width;
        }
    }

    int[] alphaBuffer;
    void updateBackground() {
        WritableRaster alphaChannel = background.getAlphaRaster();
        if(alphaBuffer == null || alphaBuffer.length != W*H)
            alphaBuffer = new int[W*H];
        int alpha = 255 * bgSlider.getValue() / bgSlider.getMaximum();
        Arrays.fill(alphaBuffer, alpha);
        alphaChannel.setPixels(0, 0, W, H, alphaBuffer);

        paintPanel.repaint();
    }


    void updateZoom() {
        JViewport viewport = paintScroller.getViewport();
        int vw = viewport.getWidth();
        int vh = viewport.getHeight();
        if(W <= 0 || H <= 0) {
            zoomSlider.setEnabled(false);
            paintPanel.setPreferredSize(new Dimension(vw, vh));
            return; // avoid those divides by zero
        }


        int minZoom = Math.min(100*vw/W, 100*vh/H);
        int maxZoom = zoomSlider.getMaximum();
        boolean enabled = minZoom < maxZoom;
        zoomSlider.setEnabled(enabled);
        if(enabled) {
            zoomSlider.setMinimum(minZoom);
            double oldZoom = zoom;
            zoom = zoomSlider.getValue() / 100.0;
            
            // why is this super shaky?
            JScrollBar h = paintScroller.getHorizontalScrollBar();
            double k = h.getVisibleAmount() / 2.0;
            h.setValue((int)Math.round((h.getValue() + k)*(zoom/oldZoom) - k));

            JScrollBar v = paintScroller.getVerticalScrollBar();
            k = v.getVisibleAmount() / 2.0;
            v.setValue((int)Math.round((v.getValue() + k)*(zoom/oldZoom) - k));
        } else {
            zoomSlider.setMinimum(maxZoom-1);
            zoomSlider.setValue(maxZoom);
            zoom = minZoom / 100.0;
        }
        paintPanel.setPreferredSize(new Dimension((int)(W*zoom), (int)(H*zoom)));
        paintPanel.revalidate();
    }

    void wakeUI() { wakeOrSleepUI(true); }
    void sleepUI() { wakeOrSleepUI(false); }
    void wakeOrSleepUI(boolean awake) {
        bgSlider.setEnabled(awake);
        blurSlider.setEnabled(awake);
        previewButton.setEnabled(awake);
        editButton.setEnabled(awake);
        resetButton.setEnabled(awake);
        saveButton.setEnabled(awake);

        editButton.setSelected(true);
        progressBar.setVisible(false);
        updateZoom();
        
        if(awake) {
            bgSlider.setValue(75);
            zoomSlider.setValue(0);
            updateBackground();
            openLabel.setText(imageFile.getName());
            statusLabel.setText("Ready");
        } else {
            image = null;
            imageFile = null;
            W = H = 0;
            openLabel.setText("");
            statusLabel.setText("Please open an image for cropping.");
        }
    }


    
    /** Shows a modal dialog and executes the runnable in a separate thread.
     * The modal is closed when the runnable completes.  */
    void runWithModal(String title, String message, final Runnable runnable) {
        final JDialog dialog = new JDialog(parentFrame, title, true);
        
        final Thread thread = new Thread(title) {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                    dialog.setVisible(false);
                }
            }
        };

        JOptionPane optionPane = new JOptionPane(
                                        message,
                                        JOptionPane.PLAIN_MESSAGE,
                                        JOptionPane.DEFAULT_OPTION,
                                        null,
                                        new String[0]);
        
        dialog.setContentPane(optionPane);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.pack();
        
        thread.start();
        dialog.setVisible(true);
    }

    MouseAdapter adapter = new MouseAdapter() {
        boolean positive;
        Point2D.Double oldPoint;
        CompoundEdit compound;

        @Override
        public void mousePressed(MouseEvent e) {
            if(segmenter == null || oldPoint != null || e.getButton() == MouseEvent.BUTTON2) {
                oldPoint = null;
                return;
            }
            
            positive = !e.isShiftDown() && e.getButton() == MouseEvent.BUTTON1;
            oldPoint = new Point2D.Double(e.getX()/zoom, e.getY()/zoom);
            compound = new CompoundEdit();
            mouseDragged(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if(oldPoint == null)
                return;

                
            Point2D.Double p = new Point2D.Double(e.getX()/zoom, e.getY()/zoom);

            Point2D.Double diff = new Point2D.Double(p.x - oldPoint.x, p.y - oldPoint.y);
            double dist = oldPoint.distance(p);
            if(dist == 0)
                dist = .000001; // don't divide by zero

            HashSet<Point> points = new HashSet<Point>();
            // follow the line from p to oldPoint
            for(int t = 0; t <= dist; ++t)
            {
                double x = p.x - diff.x * t / dist;
                double y = p.y - diff.y * t / dist;
                double R = 1.5 / zoom;

                // draw a circle centered at (x,y) with radius R
                for(double i = -R; i < R+1; ++i) {
                    if(i > R)
                        i = R;
                    int xe = (int)(x+i);
                    double r = Math.sqrt(R*R - i*i);
                    for(double j = -r; j < r+1; ++j) {
                        if(j > r)
                            j = r;
                        int ye = (int)(y+j);
                        points.add(new Point(xe, ye));
                    }
                }
            }
            for(Point i : points) {
                UndoableEdit edit = segmenter.addControlPoint(i.x, i.y, positive);
                synchronized(segmenterUpdater) {
                    segmenterUpdater.notify();
                }
                if(edit != null)
                    compound.addEdit(edit);
            }

            oldPoint = p;

            paintPanel.repaint();
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            if(oldPoint != null)
            {
                oldPoint = null;
                compound.end();
                undoManager.addEdit(compound);
                updateUndoButtons();
            }
        }
    };

}
