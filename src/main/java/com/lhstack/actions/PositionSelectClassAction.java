package com.lhstack.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.lhstack.constant.Icons;

public class PositionSelectClassAction extends DynamicIconAction {

    private final Project project;
    private final PsiClassHistoryAction historyAction;

    public PositionSelectClassAction(Project project, PsiClassHistoryAction action) {
        super(() -> "打开选择的Class", () -> Icons.load("icons/position", "svg"));
        this.project = project;
        this.historyAction = action;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        PsiClassHistoryAction.Entry selection = historyAction.getSelection();
        if (selection != null) {
            PsiClass psiClass = selection.getPsiClass();
            VirtualFile virtualFile = psiClass.getContainingFile().getVirtualFile();
            FileEditorManager.getInstance(project).openFile(virtualFile, true);
        }
    }
}
