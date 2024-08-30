package com.lhstack.component;

import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.ui.LanguageTextField;

public class MultiLanguageTextField extends LanguageTextField implements Disposable {

    private final SimpleDocumentCreator documentCreator;

    private LanguageFileType languageFileType;

    public MultiLanguageTextField(LanguageFileType languageFileType, Project project, String title) {
        this(languageFileType, project, new SimpleDocumentCreator() {
            @Override
            public void customizePsiFile(PsiFile file) {
                file.setName(title);
            }
        });
    }

    public MultiLanguageTextField(LanguageFileType languageFileType, Project project) {
        this(languageFileType, project, new SimpleDocumentCreator());
    }

    public MultiLanguageTextField(LanguageFileType languageFileType, Project project, SimpleDocumentCreator documentCreator) {
        super(languageFileType.getLanguage(), project, "", documentCreator, false);
        this.documentCreator = documentCreator;
        this.languageFileType = languageFileType;
    }


    public void changeLanguageFileType(LanguageFileType languageFileType) {
        if (this.languageFileType != languageFileType) {
            this.setNewDocumentAndFileType(languageFileType, this.documentCreator.createDocument(this.getDocument().getText(), languageFileType.getLanguage(), this.getProject()));
            this.languageFileType = languageFileType;
            Editor editor = this.getEditor();
            if (editor instanceof EditorEx) {
                EditorEx editorEx = (EditorEx) editor;
                editorEx.setHighlighter(HighlighterFactory.createHighlighter(this.getProject(), this.languageFileType));
            }
        }
    }

    public LanguageFileType getLanguageFileType() {
        return languageFileType;
    }

    @Override
    protected EditorEx createEditor() {
        EditorEx editor = super.createEditor();
        editor.setHorizontalScrollbarVisible(true);
        editor.setVerticalScrollbarVisible(true);
        editor.setHighlighter(HighlighterFactory.createHighlighter(this.getProject(), this.languageFileType));
        editor.setEmbeddedIntoDialogWrapper(true);
        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setIndentGuidesShown(true);
        settings.setCaretRowShown(false);
        return editor;
    }

    @Override
    public void dispose() {
        Editor editor = this.getEditor();
        if (editor != null) {
            EditorFactory.getInstance().releaseEditor(editor);
        }
    }
}
