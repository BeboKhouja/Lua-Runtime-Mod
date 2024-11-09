package com.mokkachocolata.minecraft.mod.luaruntime.client;

import com.mokkachocolata.minecraft.mod.luaruntime.Consts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ScriptError {
    private final Frame awtFrame = new Frame("Lua Runtime " + Consts.Version);
    private final Label awtErrorTitle = new Label("A script error occurred during startup!");
    private final TextArea cause;
    private final Button closeButton = new Button("Close");
    public ScriptError(String cause) {
        TextArea causeTextArea = new TextArea(cause);
        causeTextArea.setEditable(false);
        this.cause = causeTextArea;
    }
    public void Display() {
        awtFrame.setSize(640, 280);
        awtFrame.setLayout(new FlowLayout());
        awtFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(-1);
            }
        });
        awtErrorTitle.setAlignment(Label.CENTER);
        awtFrame.add(awtErrorTitle);
        awtFrame.add(Box.createHorizontalStrut(10));
        awtFrame.add(cause);
        awtFrame.add(Box.createHorizontalStrut(20));
        closeButton.addActionListener(e -> {
            awtFrame.dispose();
            System.exit(-1);
        });
        awtFrame.add(closeButton);
        awtFrame.setVisible(true);
    }
}