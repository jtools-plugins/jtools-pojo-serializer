package com.lhstack.utils;

import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;

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

    public static Class<?> resolveClass(PsiPrimitiveType psiPrimitiveType) {
        if (psiPrimitiveType.equals(PsiType.BOOLEAN)) {
            return boolean.class;
        }
        if (psiPrimitiveType.equals(PsiType.BYTE)) {
            return byte.class;
        }
        if (psiPrimitiveType.equals(PsiType.CHAR)) {
            return char.class;
        }
        if (psiPrimitiveType.equals(PsiType.SHORT)) {
            return short.class;
        }
        if (psiPrimitiveType.equals(PsiType.INT)) {
            return int.class;
        }

        if (psiPrimitiveType.equals(PsiType.FLOAT)) {
            return float.class;
        }

        if (psiPrimitiveType.equals(PsiType.LONG)) {
            return long.class;
        }

        if (psiPrimitiveType.equals(PsiType.DOUBLE)) {
            return double.class;
        }

        if (psiPrimitiveType.equals(PsiType.VOID)) {
            return void.class;
        }
        return null;
    }
}
