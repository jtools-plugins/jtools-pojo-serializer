package com.lhstack;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.lang.UrlClassLoader;
import com.lhstack.constant.Group;
import com.lhstack.constant.Keys;
import com.lhstack.listener.RenderObjectListener;
import com.lhstack.utils.BeanUtils;
import com.lhstack.utils.ExceptionUtils;
import com.lhstack.utils.ProjectUtils;
import com.lhstack.utils.PsiUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RightClickMenuInitializer {

    private static final String EDITOR_POPUP_MENU = "EditorPopupMenu";

    private static final String FILE_MENU = "ProjectViewPopupMenu";

    private static AnAction beanSerializerAction;

    private static DefaultActionGroup methodSerializerAction;

    public static void init() {
        beanSerializerActionInit();
        methodSerializerActionInit();
        if (ActionManager.getInstance().getAction(EDITOR_POPUP_MENU) instanceof DefaultActionGroup group) {
            group.add(methodSerializerAction, Constraints.FIRST);
            group.add(beanSerializerAction, Constraints.FIRST);
            group.addSeparator();
        }

    }


    private static void methodSerializerActionInit() {

        methodSerializerAction = new DefaultActionGroup("MethodSerializer", "序列化方法返回值和入参", IconLoader.findIcon("icons/method.svg", RightClickMenuInitializer.class)) {
            @Override
            public void update(AnActionEvent e) {
                super.update(e);
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
                        group.add(new AnAction(() -> parameter.getName() + "Serializer", IconLoader.findIcon("icons/method-parameter.svg", RightClickMenuInitializer.class)) {
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
                                        } else {
                                            instance = BeanUtils.mockInstance(parameterType, Void.class);
                                        }
                                        project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", psiParameterClass.getClassName() + "_" + parameter.getNameIdentifier().getText(), parameterPsiClass, instance);
                                    } else {
                                        Constructor<?> constructor = BeanUtils.findConstructor(project, pathClassLoader, psiClass, method);
                                        Class<?>[] parameterTypes = constructor.getParameterTypes();
                                        Class<?> parameterType = parameterTypes[finalI];
                                        Type genericParameterType = constructor.getGenericParameterTypes()[finalI];
                                        Object instance = null;
                                        if (genericParameterType instanceof ParameterizedType parameterizedType) {
                                            instance = BeanUtils.mockInstance(parameterType, parameterizedType.getActualTypeArguments());
                                        } else {
                                            instance = BeanUtils.mockInstance(parameterType, Void.class);
                                        }
                                        project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", psiParameterClass.getClassName() + "_" + parameter.getNameIdentifier().getText(), parameterPsiClass, instance);
                                    }
                                } catch (Throwable e) {
                                    Notifications.Bus.notify(new Notification(Group.GROUP_ID, "转换对象异常", ExceptionUtils.extraStackMsg(e), NotificationType.ERROR), project);
                                }
                            }
                        });
                    }

                }

                if (parameter != null && parameter.getType() instanceof PsiPrimitiveType psiPrimitiveType) {
                    group.add(new AnAction(() -> parameter.getName() + "Serializer", IconLoader.findIcon("icons/method-parameter.svg", RightClickMenuInitializer.class)) {
                        @Override
                        public void actionPerformed(AnActionEvent anActionEvent) {
                            Class<?> clazz = PsiUtils.resolveClass(psiPrimitiveType);
                            Object instance = BeanUtils.mockInstance(clazz, Void.class);
                            project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", clazz.getSimpleName() + "_" + parameter.getNameIdentifier().getText(), null, instance);
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
                        group.add(new AnAction(() -> "MethodReturnSerializer", IconLoader.findIcon("icons/method-return.svg", RightClickMenuInitializer.class)) {

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
                                    } else {
                                        instance = BeanUtils.mockInstance(returnTypeClass, Void.class);
                                    }
                                    project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", method.getReturnTypeElement().getText(), resolve, instance);
                                } catch (Throwable e) {
                                    Notifications.Bus.notify(new Notification(Group.GROUP_ID, "转换对象异常", ExceptionUtils.extraStackMsg(e), NotificationType.ERROR), project);
                                }
                            }
                        });
                    }
                }
            }
            if (returnType instanceof PsiPrimitiveType psiPrimitiveType) {
                group.add(new AnAction(() -> "MethodReturnSerializer", IconLoader.findIcon("icons/method-return.svg", RightClickMenuInitializer.class)) {

                    @Override
                    public ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.EDT;
                    }

                    @Override
                    public void actionPerformed(AnActionEvent event) {
                        try {
                            Class<?> clazz = PsiUtils.resolveClass(psiPrimitiveType);
                            Object instance = BeanUtils.mockInstance(clazz, Void.class);
                            project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", method.getReturnTypeElement().getText(), null, instance);
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
        beanSerializerAction = new AnAction(() -> "BeanSerializer", IconLoader.findIcon("icons/bean-convert.svg", RightClickMenuInitializer.class)) {
            @Override
            public void update(AnActionEvent e) {
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
        }
    }
}
