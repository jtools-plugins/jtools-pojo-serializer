package com.lhstack;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.intellij.json.json5.Json5FileType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.messages.MessageBusConnection;
import com.lhstack.actions.ClearHistoryAction;
import com.lhstack.actions.PositionSelectClassAction;
import com.lhstack.actions.PsiClassHistoryAction;
import com.lhstack.actions.SelectClassAction;
import com.lhstack.component.FlowPanel;
import com.lhstack.component.MultiLanguageTextField;
import com.lhstack.constant.Group;
import com.lhstack.listener.RenderObjectListener;
import com.lhstack.tools.plugins.IPlugin;
import com.lhstack.utils.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class PluginImpl implements IPlugin {

    private final Map<String, List<Disposable>> disposableMap = new HashMap<>();

    private final Map<String, MessageBusConnection> messageBusConnectionMap = new HashMap<>();

    private final Map<String, JComponent> panelMap = new HashMap<>();

    private final Map<String, Runnable> openThisPageMap = new HashMap<>();

    @Override
    public JComponent createPanel(Project project) {
        try {
            return panelMap.get(project.getLocationHash());
        } catch (Throwable e) {
            Notifications.Bus.notify(new Notification(Group.GROUP_ID, "打开面板失败: " + pluginName(), ExceptionUtils.extraStackMsg(e), NotificationType.ERROR), project);
            return new JLabel("错误");
        }
    }

    public List<Disposable> getDisposables(Project project) {
        return disposableMap.computeIfAbsent(project.getLocationHash(), key -> new ArrayList<>());
    }

    /**
     * 渲染
     *
     * @param type
     * @param field
     * @param instance
     * @param project
     */
    private void render(String type, MultiLanguageTextField field, Object instance, Project project) {
        try {
            if (StringUtils.equals(type, "Clear")) {
                field.setText("");
                return;
            }
            if (field.getLanguageFileType() == Json5FileType.INSTANCE) {
                field.setText(JSONObject.toJSONString(instance, JSONWriter.Feature.PrettyFormat));
            }
            Optional.ofNullable(openThisPageMap.get(project.getLocationHash())).ifPresent(Runnable::run);
        } catch (Throwable e) {
            Notifications.Bus.notify(new Notification(Group.GROUP_ID, "Bean转换失败", e.getMessage(), NotificationType.ERROR), project);
        }
    }

    /**
     * 需要所有项目都要初始化面板
     */
    @Override
    public void install() {
        RightClickMenuInitializer.init();
    }

    @Override
    public void openProject(Project project, Runnable openThisPage) {
        messageBusConnectionMap.computeIfAbsent(project.getLocationHash(), key -> {
            MessageBusConnection connect = project.getMessageBus().connect();
            Disposer.register(project, connect);
            return connect;
        });
        openThisPageMap.computeIfAbsent(project.getLocationHash(), key -> openThisPage);
        panelMap.computeIfAbsent(project.getLocationHash(), key -> {
            List<Disposable> disposables = getDisposables(project);
            SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, false);
            DefaultActionGroup group = new DefaultActionGroup();

            PsiClassHistoryAction psiClassHistoryAction = new PsiClassHistoryAction(project);
            disposables.add(psiClassHistoryAction);
            SelectClassAction selectClassAction = new SelectClassAction();
            disposables.add(selectClassAction);

            group.add(psiClassHistoryAction);
            group.add(new PositionSelectClassAction(project, psiClassHistoryAction));
            group.add(new ClearHistoryAction(project, psiClassHistoryAction));
            group.add(selectClassAction);
            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Tools@BeanSerializer", group, true);
            actionToolbar.setTargetComponent(panel);
            JComponent component = actionToolbar.getComponent();
            panel.setToolbar(new FlowPanel(FlowLayout.RIGHT, component));
            MultiLanguageTextField languageTextField = new MultiLanguageTextField(Json5FileType.INSTANCE, project);
            MessageBusConnection messageBusConnection = messageBusConnectionMap.get(project.getLocationHash());
            messageBusConnection.subscribe(RenderObjectListener.TOPIC, (RenderObjectListener) (type, uniqueKey, psiClass, instance) -> render(type, languageTextField, instance, project));
            panel.setContent(languageTextField);
            return panel;
        });
    }

    @Override
    public void closeProject(Project project) {
        List<Disposable> disposables = this.disposableMap.remove(project.getLocationHash());
        Optional.ofNullable(disposables).ifPresent(items -> items.forEach(Disposable::dispose));
        MessageBusConnection messageBusConnection = messageBusConnectionMap.remove(project.getLocationHash());
        Optional.ofNullable(messageBusConnection).ifPresent(MessageBusConnection::dispose);
        panelMap.remove(project.getLocationHash());
    }

    @Override
    public void unInstall() {
        this.disposableMap.values().forEach(items -> items.forEach(Disposable::dispose));
        this.disposableMap.clear();
        this.messageBusConnectionMap.forEach((k, v) -> v.dispose());
        this.messageBusConnectionMap.clear();
        RightClickMenuInitializer.reset();
        panelMap.clear();
    }

    @Override
    public Icon pluginIcon() {
        return IconLoader.findIcon("console-48x48.svg", PluginImpl.class);
    }

    @Override
    public Icon pluginTabIcon() {
        return IconLoader.findIcon("console-13x13.svg", PluginImpl.class);
    }

    @Override
    public String pluginName() {
        return "BeanSerializer";
    }

    @Override
    public String pluginDesc() {
        return "Java,Kotlin,Scala,Groovy等Jvm语言转换成Json,Xml,Yaml";
    }

    @Override
    public String pluginVersion() {
        return "v0.0.1";
    }

}