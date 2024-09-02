package com.lhstack.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.lhstack.constant.Icons;
import com.lhstack.listener.RenderObjectListener;

import java.util.ArrayList;

public class ClearHistoryAction extends DynamicIconAction {

    private final PsiClassHistoryAction action;
    private final Project project;

    public ClearHistoryAction(Project project, PsiClassHistoryAction action) {
        super(() -> "清除历史记录", () -> Icons.load("icons/clear", "svg"));
        this.action = action;
        this.project = project;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        action.setItems(new ArrayList<>(), null);
        action.clearSelection();
        project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Clear", null, null, null);
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
