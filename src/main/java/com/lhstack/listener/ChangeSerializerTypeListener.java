package com.lhstack.listener;

import com.intellij.util.messages.Topic;

public interface ChangeSerializerTypeListener {

    Topic<ChangeSerializerTypeListener> TOPIC = Topic.create("Tools@PojoSerializer@ChangeFileTypeListener", ChangeSerializerTypeListener.class);


    void changeType(String type);
}
