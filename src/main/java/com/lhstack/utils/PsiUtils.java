package com.lhstack.utils;

import com.intellij.psi.PsiClass;

import java.util.Arrays;

public class PsiUtils {

    public static Boolean psiClassFilter(PsiClass psiClass) {
        if (psiClass.hasModifierProperty("abstract")) {
            return false;
        }
        if (psiClass.isEnum() || psiClass.isAnnotationType() || psiClass.isInterface()) {
            return false;
        }
        if (!psiClass.hasModifierProperty("public")) {
            return false;
        }

        long count = Arrays.stream(psiClass.getFields()).filter(field -> !field.hasModifierProperty("static")).count();
        if (count == 0) {
            return false;
        }
        return true;
    }

    public static String resolveClassName(PsiClass psiClass) {
        String qualifiedName = "";
        //这是一个内部类
        if (psiClass.getContainingClass() != null) {
            String name = psiClass.getQualifiedName();
            String left = name.substring(0, name.lastIndexOf("."));
            qualifiedName = left + "$" + name.substring(name.lastIndexOf(".") + 1);
        } else {
            //class全类限定名
            qualifiedName = psiClass.getQualifiedName();
        }
        return qualifiedName;
    }
}
