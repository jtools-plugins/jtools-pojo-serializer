package com.lhstack.state;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;

@State(name = "data", storages = @Storage("Tools@PojoSerializer.xml"))
public class ProjectState implements PersistentStateComponent<ProjectState.State> {

    private State state = new State();

    public static ProjectState.State getInstance(Project project) {
        return new ProjectState().state;
    }

    @Override
    public ProjectState.State getState() {
        return state;
    }

    @Override
    public void loadState(State state) {
        this.state = state;
    }

    public static class State {

        private String fileType = "json";

        public String getFileType() {
            return fileType;
        }

        public State setFileType(String fileType) {
            this.fileType = fileType;
            return this;
        }
    }
}
