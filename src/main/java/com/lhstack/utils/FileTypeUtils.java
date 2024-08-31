package com.lhstack.utils;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.json.json5.Json5FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;

public class FileTypeUtils {

    public static LanguageFileType resolve(String type) {
        return switch (type) {
            case "json" -> Json5FileType.INSTANCE;
            case "xml" -> XmlFileType.INSTANCE;
            case "yaml" -> (LanguageFileType) FileTypeManager.getInstance().getFileTypeByExtension("yml");
            case "properties" -> (LanguageFileType) FileTypeManager.getInstance().getFileTypeByExtension("properties");
            case "toml" -> (LanguageFileType) FileTypeManager.getInstance().getFileTypeByExtension("toml");
            case "csv" -> (LanguageFileType) FileTypeManager.getInstance().getFileTypeByExtension("csv");
            default -> PlainTextFileType.INSTANCE;
        };
    }
}
