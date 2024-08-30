package com.lhstack.actions;

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.lang.UrlClassLoader;
import com.lhstack.constant.Group;
import com.lhstack.listener.RenderObjectListener;
import com.lhstack.utils.BeanUtils;
import com.lhstack.utils.ExceptionUtils;
import com.lhstack.utils.ProjectUtils;
import com.lhstack.utils.PsiUtils;

public class SelectClassAction extends AnAction implements Disposable {

    public SelectClassAction() {
        super(() -> "选中要转换的Class", IconLoader.findIcon("icons/gen.svg", SelectClassAction.class));
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(LangDataKeys.PROJECT);
        if (project != null) {
            TreeClassChooserFactory instance = TreeClassChooserFactory.getInstance(project);
            TreeClassChooser chooser = instance.createNoInnerClassesScopeChooser("选择要转换的对象", GlobalSearchScope.allScope(project), PsiUtils::psiClassFilter, null);
            chooser.showDialog();
            PsiClass psiClass = chooser.getSelected();
            if (psiClass != null) {
                try {
                    UrlClassLoader pathClassLoader = ProjectUtils.projectClassloader(project);
                    Class<?> clazz = pathClassLoader.loadClass(PsiUtils.resolveClassName(psiClass));
                    Object mockInstance = BeanUtils.mockInstance(clazz, clazz.getGenericSuperclass());
                    project.getMessageBus().syncPublisher(RenderObjectListener.TOPIC).render("Select", psiClass.getQualifiedName(), psiClass, mockInstance);
                } catch (Throwable e) {
                    Notifications.Bus.notify(new Notification(Group.GROUP_ID, "转换对象异常", ExceptionUtils.extraStackMsg(e), NotificationType.ERROR), project);
                }
            }
        } else {
            Notifications.Bus.notify(new Notification(Group.GROUP_ID, "BeanSerializer选择对象提示", "没有项目", NotificationType.ERROR));
        }
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void dispose() {

    }
}
