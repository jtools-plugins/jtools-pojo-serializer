package com.lhstack.component;

import javax.swing.*;
import java.awt.*;

public class FlowPanel extends JPanel {

    public FlowPanel(int layout, JComponent... components) {
        super(new FlowLayout(layout));
        for (JComponent component : components) {
            this.add(component);
        }
    }
}