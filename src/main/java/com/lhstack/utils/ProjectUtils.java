package com.lhstack.utils;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.lang.UrlClassLoader;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class ProjectUtils {

    public static <T> T getOrCreate(Key<T> key, Project project, Supplier<T> supplier) {
        T userData = project.getUserData(key);
        if (userData == null) {
            userData = supplier.get();
            project.putUserData(key, userData);
        }
        return userData;
    }


    public static UrlClassLoader projectClassloader(Project project) {
        Set<Path> classpaths = new HashSet<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            for (OrderEntry orderEntry : moduleRootManager.getOrderEntries()) {
                if (orderEntry instanceof LibraryOrderEntry) {
                    VirtualFile[] files = orderEntry.getFiles(OrderRootType.CLASSES);
                    for (VirtualFile file : files) {
                        classpaths.add(new File(file.getPresentableUrl()).toPath());
                    }
                }
                Optional.ofNullable(CompilerPaths.getModuleOutputPath(module, false)).map(File::new).map(File::toPath).ifPresent(classpaths::add);
                Optional.ofNullable(CompilerPaths.getModuleOutputPath(module, true)).map(File::new).map(File::toPath).ifPresent(classpaths::add);
            }
        }
        return UrlClassLoader.build().files(new ArrayList<>(classpaths)).allowLock(false).allowBootstrapResources(false).get();
    }
}
