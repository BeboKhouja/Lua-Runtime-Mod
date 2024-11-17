package com.mokkachocolata.minecraft.mod.luaruntime.client;

import com.mokkachocolata.minecraft.mod.luaruntime.Consts;

import javax.swing.*;
import java.awt.*;

/**
 * A GUI designed for when a script error occurs.
 *
 * @author Mokka Chocolata
 * @apiNote This is designed for when the game has not started yet.
 */
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

    /**
     * Display this script error.
     */
    public void Display() {
        awtFrame.setSize(640, 280);
        awtFrame.setLayout(new FlowLayout());
        awtErrorTitle.setAlignment(Label.CENTER);
        awtFrame.add(awtErrorTitle);
        awtFrame.add(Box.createHorizontalStrut(10));
        awtFrame.add(cause);
        awtFrame.add(Box.createHorizontalStrut(20));
        closeButton.addActionListener(e -> awtFrame.dispose());
        awtFrame.add(closeButton);
        awtFrame.setVisible(true);
    }
}
