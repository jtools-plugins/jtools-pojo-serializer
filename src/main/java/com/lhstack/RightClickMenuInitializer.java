package com.lhstack;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.lang.UrlClassLoader;
import com.lhstack.actions.DynamicIconAction;
import com.lhstack.constant.Group;
import com.lhstack.constant.Icons;
import com.lhstack.constant.Keys;
import com.lhstack.listener.RenderObjectListener;
import com.lhstack.utils.BeanUtils;
import com.lhstack.utils.ExceptionUtils;
import com.lhstack.utils.ProjectUtils;
import com.lhstack.utils.PsiUtils;

import java.lang.reflect.*;

public class RightClickMenuInitializer {

    private static final String EDITOR_POPUP_MENU = "EditorPopupMenu";

    private static final String FILE_MENU = "ProjectViewPopupMenu";

    private static AnAction beanSerializerAction;

    private static DefaultActionGroup methodSerializerAction;

    private static DefaultActionGroup fieldSerializerAction;

    public static void init() {
        beanSerializerActionInit();
        methodSerializerActionInit();
        fieldSerializerActionInit();
        if (ActionManager.getInstance().getAction(EDITOR_POPUP_MENU) instanceof DefaultActionGroup group) {
            group.add(fieldSerializerAction, Constraints.FIRST);
            group.add(methodSerializerAction, Constraints.FIRST);
            group.add(beanSerializerAction, Constraints.FIRST);
            group.addSeparator();
        }

    }

    private static void fieldSerializerActionInit() {
        fieldSerializerAction = new DefaultActionGroup("FieldSerializer", "序列化字段", Icons.load("icons/field", "svg")) {
            @Override
            public void update(AnActionEvent e) {
                super.update(e);
                e.getPresentation().setIcon(Icons.load("icons/field", "svg"));
                Editor editor = e.getData(LangDataKeys.EDITOR);
                if (editor != null) {
                    PsiFile psiFile = PsiDocumentManager.getInstance(e.getProject()).getPsiFile(editor.getDocument());
                    PsiClass psiClass = PsiTreeUtil.findChildOfType(psiFile, PsiClass.class);
                    if (psiClass != null) {
                        if (e.getDataContext() instanceof UserDataHolder dataHolder) {
                            dataHolder.putUserData(Keys.PSI_CLASS_KEY, psiClass);
                            removeAll();
                            addFieldSerializerActions(e.getProject(), editor, psiClass, this);
                            return;
                        }
                    }
                }
                e.getPresentation().setEnabledAndVisible(false);
            }

            @Override
            public ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
        fieldSerializerAction.setPopup(true);
    }

    private static void addFieldSerializerActions(Project project, Editor editor, PsiClass psiClass, DefaultActionGroup group) {
        SelectionModel selectionModel = editor.getSelectionModel();
        int selectionStart = selectionModel.getSelectionStart();
        int selectionEnd = selectionModel.getSelectionEnd();
        for (PsiField psiField : psiClass.getAllFields()) {
            TextRange textRange = psiField.getTextRange();
            if (textRange.getStartOffset() <= selectionStart && textRange.getEndOffset() >= selectionEnd) {
                addFieldSerializerAction(project, editor, psiClass, psiField, group);
            }
        }
    }

    private static void addFieldSerializerAction(Project project, Editor editor, PsiClass psiClass, PsiField psiField, DefaultActionGroup group) {
        PsiType type = psiField.getType();
        if (type instanceof PsiPrimitiveType psiPrimitiveType) {
            group.add(new DynamicIconAction(() -> psiField.getName() + "Serializer", () -> Icons.load("icons/method-parameter", "svg")) {
                @Override
                public void actionPerformed(AnActionEvent event) {
                    Class<?> clazz = PsiUtils.resolveClass(psiPrimitiveType);
                    Object instance = BeanUtils.mockInstance(clazz, Void.class);
                    project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", psiPrimitiveType.getCanonicalText(), psiClass, instance);
                }

                @Override
                public ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.EDT;
                }
            });
        } else if (type instanceof PsiClassType psiClassType) {
            PsiClass resolve = psiClassType.resolve();
            if (resolve != null && PsiUtils.psiClassFilter(resolve)) {
                group.add(new DynamicIconAction(() -> psiField.getName() + "Serializer", () -> Icons.load("icons/method-parameter", "svg")) {
                    @Override
                    public void actionPerformed(AnActionEvent event) {
                        try {
                            UrlClassLoader urlClassLoader = ProjectUtils.projectClassloader(project);
                            Class<?> clazz = urlClassLoader.loadClass(PsiUtils.resolveClassName(psiClass));
                            Field field = clazz.getDeclaredField(psiField.getName());
                            Type genericType = field.getGenericType();
                            if (genericType instanceof ParameterizedType parameterizedType) {
                                Object instance = BeanUtils.mockInstance(field.getType(), parameterizedType.getActualTypeArguments());
                                project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", psiField.getTypeElement().getText(), psiClass, instance);
                            } else {
                                Object instance = BeanUtils.mockInstance(field.getType(), Void.class);
                                project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", psiField.getTypeElement().getText(), psiClass, instance);
                            }
                        } catch (Throwable e) {
                            Notifications.Bus.notify(new Notification(Group.GROUP_ID, "转换对象异常", ExceptionUtils.extraStackMsg(e), NotificationType.ERROR), project);
                        }
                    }

                    @Override
                    public ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.EDT;
                    }
                });
            }
        }
    }


    /**
     * 添加方法上的action和parameter的action
     *
     * @param project
     * @param editor
     * @param psiClass
     * @param group
     */
    private static void addMethodReturnAndParameterActions(Project project, Editor editor, PsiClass psiClass, DefaultActionGroup group) {
        SelectionModel selectionModel = editor.getSelectionModel();
        int selectionStart = selectionModel.getSelectionStart();
        int selectionEnd = selectionModel.getSelectionEnd();
        for (PsiMethod method : psiClass.getMethods()) {
            TextRange textRange = method.getTextRange();
            if (textRange.getStartOffset() <= selectionStart && textRange.getEndOffset() >= selectionEnd) {
                addMethodReturnAction(project, editor, psiClass, method, group);
                addMethodArgumentAction(project, editor, psiClass, method, group);
            }
        }
    }


    private static void methodSerializerActionInit() {

        methodSerializerAction = new DefaultActionGroup("MethodSerializer", "序列化方法返回值和入参", Icons.load("icons/method", "svg")) {
            @Override
            public void update(AnActionEvent e) {
                super.update(e);
                e.getPresentation().setIcon(Icons.load("icons/method", "svg"));
                Editor editor = e.getData(LangDataKeys.EDITOR);
                if (editor != null) {
                    PsiFile psiFile = PsiDocumentManager.getInstance(e.getProject()).getPsiFile(editor.getDocument());
                    PsiClass psiClass = PsiTreeUtil.findChildOfType(psiFile, PsiClass.class);
                    if (psiClass != null) {
                        if (e.getDataContext() instanceof UserDataHolder dataHolder) {
                            dataHolder.putUserData(Keys.PSI_CLASS_KEY, psiClass);
                            removeAll();
                            addMethodReturnAndParameterActions(e.getProject(), editor, psiClass, this);
                            return;
                        }
                    }
                }
                e.getPresentation().setEnabledAndVisible(false);
            }


            @Override
            public ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
        methodSerializerAction.setPopup(true);
    }


    /**
     * 添加方法参数操作
     *
     * @param project  项目
     * @param editor   编辑 器
     * @param psiClass PSI 级
     * @param method   方法
     * @param group    群
     */
    private static void addMethodArgumentAction(Project project, Editor editor, PsiClass psiClass, PsiMethod method, DefaultActionGroup group) {
        try {
            for (int i = 0; i < method.getParameterList().getParameters().length; i++) {
                PsiParameter parameter = method.getParameterList().getParameter(i);
                int finalI = i;
                if (parameter != null && parameter.getType() instanceof PsiClassType psiParameterClass) {
                    PsiClass parameterPsiClass = psiParameterClass.resolve();
                    if (parameterPsiClass != null && PsiUtils.psiClassFilter(parameterPsiClass)) {
                        group.add(new DynamicIconAction(() -> parameter.getName() + "Serializer", () -> Icons.load("icons/method-parameter", "svg")) {

                            @Override
                            public ActionUpdateThread getActionUpdateThread() {
                                return ActionUpdateThread.EDT;
                            }

                            @Override
                            public void actionPerformed(AnActionEvent event) {
                                try {
                                    UrlClassLoader pathClassLoader = ProjectUtils.projectClassloader(project);
                                    if (!method.isConstructor()) {
                                        Method classMethod = BeanUtils.findMethod(project, pathClassLoader, psiClass, method);
                                        Class<?>[] parameterTypes = classMethod.getParameterTypes();
                                        Class<?> parameterType = parameterTypes[finalI];
                                        Type genericParameterType = classMethod.getGenericParameterTypes()[finalI];
                                        Object instance = null;
                                        if (genericParameterType instanceof ParameterizedType parameterizedType) {
                                            instance = BeanUtils.mockInstance(parameterType, parameterizedType.getActualTypeArguments());
                                            project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", parameter.getNameIdentifier().getText(), parameterPsiClass, instance);
                                        } else {
                                            instance = BeanUtils.mockInstance(parameterType, Void.class);
                                            project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", psiParameterClass.getClassName(), parameterPsiClass, instance);
                                        }
                                    } else {
                                        Constructor<?> constructor = BeanUtils.findConstructor(project, pathClassLoader, psiClass, method);
                                        Class<?>[] parameterTypes = constructor.getParameterTypes();
                                        Class<?> parameterType = parameterTypes[finalI];
                                        Type genericParameterType = constructor.getGenericParameterTypes()[finalI];
                                        Object instance = null;
                                        if (genericParameterType instanceof ParameterizedType parameterizedType) {
                                            instance = BeanUtils.mockInstance(parameterType, parameterizedType.getActualTypeArguments());
                                            project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", parameter.getNameIdentifier().getText(), parameterPsiClass, instance);
                                        } else {
                                            instance = BeanUtils.mockInstance(parameterType, Void.class);
                                            project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", psiParameterClass.getClassName(), parameterPsiClass, instance);
                                        }
                                    }
                                } catch (Throwable e) {
                                    Notifications.Bus.notify(new Notification(Group.GROUP_ID, "转换对象异常", ExceptionUtils.extraStackMsg(e), NotificationType.ERROR), project);
                                }
                            }
                        });
                    }

                }

                if (parameter != null && parameter.getType() instanceof PsiPrimitiveType psiPrimitiveType) {
                    group.add(new DynamicIconAction(() -> parameter.getName() + "Serializer", () -> Icons.load("icons/method-parameter", "svg")) {
                        @Override
                        public void actionPerformed(AnActionEvent anActionEvent) {
                            Class<?> clazz = PsiUtils.resolveClass(psiPrimitiveType);
                            Object instance = BeanUtils.mockInstance(clazz, Void.class);
                            project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", parameter.getNameIdentifier().getText(), null, instance);
                        }

                        @Override
                        public ActionUpdateThread getActionUpdateThread() {
                            return ActionUpdateThread.EDT;
                        }
                    });
                }
            }
        } catch (Throwable e) {
            Notifications.Bus.notify(new Notification(Group.GROUP_ID, "生成MethodSerializer.ParameterSerializerMenu失败", ExceptionUtils.extraStackMsg(e), NotificationType.ERROR), project);
        }
    }

    /**
     * 添加方法返回值的序列化action
     *
     * @param project
     * @param editor
     * @param psiClass
     * @param method
     * @param group
     */
    private static void addMethodReturnAction(Project project, Editor editor, PsiClass psiClass, PsiMethod method, DefaultActionGroup group) {
        try {
            if (method.isConstructor()) {
                return;
            }
            PsiType returnType = method.getReturnType();
            if (returnType instanceof PsiClassType psiClassType) {
                PsiClass resolve = psiClassType.resolve();
                if (resolve != null) {
                    if (PsiUtils.psiClassFilter(resolve)) {
                        group.add(new DynamicIconAction(() -> "MethodReturnSerializer", () -> Icons.load("icons/method-return", "svg")) {

                            @Override
                            public ActionUpdateThread getActionUpdateThread() {
                                return ActionUpdateThread.EDT;
                            }

                            @Override
                            public void actionPerformed(AnActionEvent event) {
                                try {
                                    UrlClassLoader pathClassLoader = ProjectUtils.projectClassloader(project);
                                    Method classMethod = BeanUtils.findMethod(project, pathClassLoader, psiClass, method);
                                    Type genericReturnType = classMethod.getGenericReturnType();
                                    Class<?> returnTypeClass = classMethod.getReturnType();
                                    Object instance = null;
                                    if (genericReturnType instanceof ParameterizedType parameterizedType) {
                                        instance = BeanUtils.mockInstance(returnTypeClass, parameterizedType.getActualTypeArguments());
                                        project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", method.getReturnTypeElement().getText(), resolve, instance);
                                    } else {
                                        instance = BeanUtils.mockInstance(returnTypeClass, Void.class);
                                        project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", resolve.getQualifiedName(), resolve, instance);
                                    }

                                } catch (Throwable e) {
                                    Notifications.Bus.notify(new Notification(Group.GROUP_ID, "转换对象异常", ExceptionUtils.extraStackMsg(e), NotificationType.ERROR), project);
                                }
                            }
                        });
                    }
                }
            }
            if (returnType instanceof PsiPrimitiveType psiPrimitiveType) {
                if (PsiType.VOID.equals(psiPrimitiveType)) {
                    return;
                }
                group.add(new DynamicIconAction(() -> "MethodReturnSerializer", () -> Icons.load("icons/method-return", "svg")) {

                    @Override
                    public ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.EDT;
                    }

                    @Override
                    public void actionPerformed(AnActionEvent event) {
                        try {
                            Class<?> clazz = PsiUtils.resolveClass(psiPrimitiveType);
                            Object instance = BeanUtils.mockInstance(clazz, Void.class);
                            project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", psiPrimitiveType.getCanonicalText(), null, instance);
                        } catch (Throwable e) {
                            Notifications.Bus.notify(new Notification(Group.GROUP_ID, "转换对象异常", ExceptionUtils.extraStackMsg(e), NotificationType.ERROR), project);
                        }
                    }
                });
            }
        } catch (Throwable e) {
            Notifications.Bus.notify(new Notification(Group.GROUP_ID, "生成MethodSerializer.MethodReturnSerializerMenu失败", e.getMessage(), NotificationType.ERROR), project);
        }
    }

    private static void beanSerializerActionInit() {
        beanSerializerAction = new DynamicIconAction(() -> "PojoSerializer", () -> Icons.load("icons/bean-convert", "svg")) {
            @Override
            public void update(AnActionEvent e) {
                super.update(e);
                Editor editor = e.getData(LangDataKeys.EDITOR);
                if (editor != null) {
                    PsiFile psiFile = PsiDocumentManager.getInstance(e.getProject()).getPsiFile(editor.getDocument());
                    PsiClass psiClass = PsiTreeUtil.findChildOfType(psiFile, PsiClass.class);
                    if (psiClass != null) {
                        if (PsiUtils.psiClassFilter(psiClass)) {
                            if (e.getDataContext() instanceof UserDataHolder dataHolder) {
                                dataHolder.putUserData(Keys.PSI_CLASS_KEY, psiClass);
                                return;
                            }
                        }
                    }
                }
                e.getPresentation().setEnabledAndVisible(false);
            }

            @Override
            public void actionPerformed(AnActionEvent event) {
                if (event.getDataContext() instanceof UserDataHolder dataHolder) {
                    PsiClass psiClass = dataHolder.getUserData(Keys.PSI_CLASS_KEY);
                    if (psiClass != null) {
                        Project project = event.getProject();
                        try {
                            Object instance = BeanUtils.mockInstance(psiClass, event.getProject());
                            project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", psiClass.getQualifiedName(), psiClass, instance);
                        } catch (Throwable e) {
                            Notifications.Bus.notify(new Notification(Group.GROUP_ID, "转换对象异常", ExceptionUtils.extraStackMsg(e), NotificationType.ERROR), project);
                        }
                    }
                }
            }

            @Override
            public ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
    }

    public static void reset() {
        if (ActionManager.getInstance().getAction(EDITOR_POPUP_MENU) instanceof DefaultActionGroup group) {
            group.remove(beanSerializerAction);
            group.remove(methodSerializerAction);
            group.remove(fieldSerializerAction);
        }
    }
}
