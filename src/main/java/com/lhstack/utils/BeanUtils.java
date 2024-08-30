package com.lhstack.utils;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.util.lang.UrlClassLoader;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

public class BeanUtils {

    private static final PodamFactory PODAM_FACTORY = new PodamFactoryImpl();

    public static Object mockInstance(Class<?> clazz, Type... types) {
        return PODAM_FACTORY.manufacturePojo(clazz, types);
    }

    public static Object mockInstance(PsiClass psiClass, Project project) throws Throwable {
        UrlClassLoader pathClassLoader = ProjectUtils.projectClassloader(project);
        String className = PsiUtils.resolveClassName(psiClass);
        Class<?> clazz = pathClassLoader.loadClass(className);
        return mockInstance(clazz, clazz.getGenericSuperclass());
    }

    public static Class<?> loadClass(Project project, PsiClass psiClass) throws Exception {
        return ProjectUtils.projectClassloader(project).loadClass(PsiUtils.resolveClassName(psiClass));
    }

    public static Method findMethod(UrlClassLoader classLoader, PsiClass psiClass, PsiMethod method) throws Exception {
        Method classMethod = null;
        Class<?> clazz = classLoader.loadClass(PsiUtils.resolveClassName(psiClass));
        PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length == 0) {
            classMethod = clazz.getDeclaredMethod(method.getName());
        } else {
            Class[] parameterClass = Arrays.stream(parameters)
                    .map(PsiParameter::getType)
                    .map(PsiClassType.class::cast).map(PsiClassType::resolve)
                    .filter(Objects::nonNull)
                    .map(PsiUtils::resolveClassName)
                    .map(item -> {
                        try {
                            return classLoader.loadClass(item);
                        } catch (Throwable e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toArray(Class[]::new);
            classMethod = clazz.getDeclaredMethod(method.getName(), parameterClass);
        }
        return classMethod;
    }
}
