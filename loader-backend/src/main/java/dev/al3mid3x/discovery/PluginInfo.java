package dev.al3mid3x.discovery;

import java.io.File;

/**
 * A class to hold discovered information about a plugin JAR.
 * (Using a class instead of a record for broader Java version compatibility).
 */
public final class PluginInfo {
    private final String name;
    private final String mainClass;
    private final File file;

    public PluginInfo(String name, String mainClass, File file) {
        this.name = name;
        this.mainClass = mainClass;
        this.file = file;
    }

    public String name() {
        return name;
    }

    public String mainClass() {
        return mainClass;
    }

    public File file() {
        return file;
    }

    @Override
    public String toString() {
        return "PluginInfo[" +
                "name='" + name + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", file=" + file.getName() +
                ']';
    }
}
