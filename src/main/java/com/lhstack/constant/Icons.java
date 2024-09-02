package com.lhstack.constant;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

public interface Icons {

    static Icon load(String path, String ext) {
        if (StringUtils.isEmpty(ext)) {
            ext = "svg";
        }
        String iconPath;
        //如果是深色主题
        if (UIUtil.isUnderDarcula()) {
            iconPath = path + "_light." + ext;
        } else {
            iconPath = path + "_dark." + ext;
        }
        Icon icon = IconLoader.findIcon(iconPath, Icons.class.getClassLoader());
        if (icon == null) {
            icon = IconLoader.findIcon(path + "." + ext, Icons.class);
        }
        return icon;
    }
}
