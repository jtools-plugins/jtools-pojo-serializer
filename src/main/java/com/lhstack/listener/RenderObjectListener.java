package com.lhstack.listener;

import com.intellij.psi.PsiClass;
import com.intellij.util.messages.Topic;

public interface RenderObjectListener {

    Topic<RenderObjectListener> TOPIC = Topic.create("Tools@BeanSerializer@RenderObjectListener", RenderObjectListener.class);

    void render(String type,String key, PsiClass psiClass, Object instance);
}
