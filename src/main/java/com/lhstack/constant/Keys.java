package com.lhstack.constant;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;

import javax.swing.*;

public interface Keys {
    Key<JComponent> PANEL_KEY = Key.create("PANEL_KEY");

    Key<PsiClass> PSI_CLASS_KEY = Key.create("PSI_CLASS_KEY");
}
