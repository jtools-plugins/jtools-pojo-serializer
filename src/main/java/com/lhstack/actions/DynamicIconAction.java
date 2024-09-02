package com.lhstack.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import javax.swing.*;
import java.util.function.Supplier;

public abstract class DynamicIconAction extends AnAction {

    private final Supplier<Icon> dynamicIcon;

    public DynamicIconAction(Supplier<String> dynamicText, Supplier<Icon> dynamicIcon) {
        super(dynamicText, dynamicIcon.get());
        this.dynamicIcon = dynamicIcon;
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setIcon(dynamicIcon.get());
    }
}
