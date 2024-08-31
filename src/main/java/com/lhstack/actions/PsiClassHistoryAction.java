package com.lhstack.actions;

import com.intellij.designer.actions.AbstractComboBoxAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.util.messages.MessageBusConnection;
import com.lhstack.listener.RenderObjectListener;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PsiClassHistoryAction extends AbstractComboBoxAction<PsiClassHistoryAction.Entry> implements Disposable {

    private final MessageBusConnection connect;
    private final Project project;

    public static class Entry {

        private String key;

        private PsiClass psiClass;

        private Object instance;

        public Entry(String key, PsiClass psiClass, Object instance) {
            this.key = key;
            this.psiClass = psiClass;
            this.instance = instance;
        }

        public String getKey() {
            return key;
        }

        public Entry setKey(String key) {
            this.key = key;
            return this;
        }

        public PsiClass getPsiClass() {
            return psiClass;
        }

        public Entry setPsiClass(PsiClass psiClass) {
            this.psiClass = psiClass;
            return this;
        }

        public Object getInstance() {
            return instance;
        }

        public Entry setInstance(Object instance) {
            this.instance = instance;
            return this;
        }
    }

    public PsiClassHistoryAction(Project project) {
        this.project = project;
        this.connect = project.getMessageBus().connect();
        this.connect.subscribe(RenderObjectListener.TOPIC, (RenderObjectListener) (type, key, psiClass, instance) -> {
            if (StringUtils.equals(type, "Change")) {
                return;
            }
            if (StringUtils.equals(type, "Clear")) {
                PsiClassHistoryAction.this.setItems(new ArrayList<>(), null);
                PsiClassHistoryAction.this.clearSelection();
                return;
            }
            List<PsiClassHistoryAction.Entry> items = PsiClassHistoryAction.this.myItems;
            if (items.isEmpty()) {
                ArrayList<PsiClassHistoryAction.Entry> entries = new ArrayList<>();
                entries.add(new PsiClassHistoryAction.Entry(key, psiClass, instance));
                PsiClassHistoryAction.this.setItems(entries, entries.get(0));
                return;
            }
            Optional<Entry> optionalEntry = items.stream().filter(item -> Objects.equals(item.key, key)).findFirst();
            if (optionalEntry.isPresent()) {
                setSelection(optionalEntry.get());
                return;
            }
            if (items.size() >= 20) {
                items.remove(0);
            }
            Entry entry = new Entry(key, psiClass, instance);
            items.add(entry);
            setSelection(entry);
        });
    }

    @Override
    protected void update(PsiClassHistoryAction.Entry entry, Presentation presentation, boolean b) {
        if (entry == null) {
            presentation.setText("当前没有选择对象");
            return;
        }
        if (b) {
            presentation.setText(entry.getKey());
            PsiClass psiClass = entry.psiClass;
            if (psiClass != null) {
                presentation.setText(entry.getKey());
                presentation.setDescription(entry.psiClass.getQualifiedName());
            }
        } else {
            PsiClass psiClass = entry.psiClass;
            presentation.setText(entry.getKey());
            if (psiClass != null) {
                presentation.setDescription(psiClass.getQualifiedName());
            }
        }
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    protected boolean selectionChanged(PsiClassHistoryAction.Entry entry) {
        if (!Objects.equals(this.getSelection().key, entry.key)) {
            project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Change", entry.key, entry.psiClass, entry.instance);
            return true;
        }
        return false;
    }

    @Override
    public void dispose() {
        connect.dispose();
    }
}
