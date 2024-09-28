package com.lhstack;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlWriter;
import com.esotericsoftware.yamlbeans.scalar.ScalarSerializer;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.intellij.designer.actions.AbstractComboBoxAction;
import com.intellij.json.json5.Json5FileType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
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
import com.lhstack.listener.ChangeSerializerTypeListener;
import com.lhstack.listener.RenderObjectListener;
import com.lhstack.tools.plugins.IPlugin;
import com.lhstack.tools.plugins.Logger;
import com.lhstack.utils.ExceptionUtils;
import com.lhstack.utils.FileTypeUtils;
import com.moandjiezana.toml.TomlWriter;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

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
     * 需要所有项目都要初始化面板
     */
    @Override
    public void install() {
        RightClickMenuInitializer.init();
    }

    @Override
    public void openProject(Project project, Logger logger, Runnable openThisPage) {
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
            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Tools@PojoSerializerRightMenus", group, true);
            actionToolbar.setTargetComponent(panel);
            JComponent component = actionToolbar.getComponent();
            DefaultActionGroup leftGroup = new DefaultActionGroup();
            AbstractComboBoxAction<String> serializerTypeChangeAction = createFileTypeChangeAction(project);
            leftGroup.add(serializerTypeChangeAction);
            ActionToolbar leftToolbar = ActionManager.getInstance().createActionToolbar("Tools@PojoSerializerLeftMenus", leftGroup, true);
            leftToolbar.setTargetComponent(panel);
            JPanel jPanel = new JPanel();
            jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.X_AXIS));
            jPanel.add(new FlowPanel(FlowLayout.LEFT, leftToolbar.getComponent()));
            jPanel.add(new FlowPanel(FlowLayout.RIGHT, component));
            panel.setToolbar(jPanel);
            MultiLanguageTextField languageTextField = new MultiLanguageTextField(Json5FileType.INSTANCE, project);
            disposables.add(languageTextField);
            MessageBusConnection messageBusConnection = messageBusConnectionMap.get(project.getLocationHash());
            messageBusConnection.subscribe(RenderObjectListener.TOPIC, (RenderObjectListener) (type, uniqueKey, psiClass, instance) -> render(type, serializerTypeChangeAction.getSelection(), languageTextField, instance, project));
            messageBusConnection.subscribe(ChangeSerializerTypeListener.TOPIC, (ChangeSerializerTypeListener) type -> {
                languageTextField.changeLanguageFileType(FileTypeUtils.resolve(type));
                PsiClassHistoryAction.Entry selection = psiClassHistoryAction.getSelection();
                if (selection != null) {
                    //转换
                    render("Change", type, languageTextField, selection.getInstance(), project);
                }
            });
            panel.setContent(languageTextField);
            return panel;
        });
    }


    /**
     * 渲染
     *
     * @param type
     * @param serializerType 序列化器类型
     * @param field
     * @param instance
     * @param project        项目
     */
    private void render(String type, String serializerType, MultiLanguageTextField field, Object instance, Project project) {
        try {
            if (StringUtils.equals(type, "Clear")) {
                field.setText("");
                return;
            }
            switch (serializerType) {
                case "json": {
                    field.setText(JSONObject.toJSONString(instance, JSONWriter.Feature.PrettyFormat));
                }
                break;
                case "xml": {
                    XStream xstream = new XStream();
                    field.setText(xstream.toXML(instance));
                }
                break;
                case "yaml": {
                    StringWriter sw = new StringWriter();
                    YamlConfig yamlConfig = new YamlConfig();
                    yamlConfig.writeConfig.setQuoteChar(YamlConfig.Quote.LITERAL);
                    yamlConfig.writeConfig.setAutoAnchor(true);
                    yamlConfig.writeConfig.setWriteClassname(YamlConfig.WriteClassName.ALWAYS);
                    yamlConfig.setAllowDuplicates(false);
                    yamlConfig.setBeanProperties(true);
                    yamlConfig.setPrivateFields(false);
                    yamlConfig.setScalarSerializer(BigDecimal.class, new ScalarSerializer<BigDecimal>() {

                        @Override
                        public String write(BigDecimal bigDecimal) throws YamlException {
                            return bigDecimal != null ? bigDecimal.setScale(2, RoundingMode.HALF_UP).toPlainString() : "";
                        }

                        @Override
                        public BigDecimal read(String s) throws YamlException {
                            return s != null ? new BigDecimal(s).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                        }
                    });
                    YamlWriter yamlWriter = new YamlWriter(sw, yamlConfig);
                    yamlWriter.write(instance);
                    yamlWriter.close();
                    field.setText(sw.toString());
                }
                break;
                case "properties": {
                    Map<String, Object> map = JsonFlattener.flattenAsMap(JSONObject.toJSONString(instance));
                    String text = map.entrySet().stream().map(item -> String.format("%s=%s", item.getKey(), item.getValue()))
                            .collect(Collectors.joining("\r\n"));
                    field.setText(text);
                }
                break;
                case "csv": {
                    StringWriter sw = new StringWriter();
                    StatefulBeanToCsv<Object> beanToCsv = new StatefulBeanToCsvBuilder<Object>(sw)
                            .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                            .withOrderedResults(true)
                            .build();
                    beanToCsv.write(instance);
                    field.setText(sw.toString());
                }
                break;
                case "toml": {
                    TomlWriter tomlWriter = new TomlWriter.Builder().build();
                    field.setText(tomlWriter.write(instance));
                }
                break;
            }
            Optional.ofNullable(openThisPageMap.get(project.getLocationHash())).ifPresent(Runnable::run);
        } catch (Throwable e) {
            Notifications.Bus.notify(new Notification(Group.GROUP_ID, "Bean转换失败", ExceptionUtils.extraStackMsg(e), NotificationType.ERROR), project);
        }
    }

    private AbstractComboBoxAction<String> createFileTypeChangeAction(Project project) {
        AbstractComboBoxAction<String> comboBoxAction = new AbstractComboBoxAction<>() {

            @Override
            protected void update(String type, Presentation presentation, boolean b) {
                presentation.setText(type);
                presentation.setDescription("序列化类型");
            }

            @Override
            protected boolean selectionChanged(String type) {
                if (!StringUtils.equals(this.getSelection(), type)) {
                    project.getMessageBus().syncPublisher(ChangeSerializerTypeListener.TOPIC).changeType(type);
                    return true;
                }
                return false;
            }
        };
        comboBoxAction.setItems(Arrays.asList("json", "xml", "properties", "yaml", "csv", "toml"), "json");
        return comboBoxAction;
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
        return "PojoSerializer";
    }

    @Override
    public String pluginDesc() {
        return "Java,Kotlin,Scala,Groovy等Jvm语言转换成Json,Xml,Yaml,Toml,Csv,Properties";
    }

    @Override
    public String pluginVersion() {
        return "v0.0.1";
    }

}