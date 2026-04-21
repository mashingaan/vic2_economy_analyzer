package org.victoria2.tools.vic2sgea.main;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by anth on 05.02.2017.
 */
public class Properties {

    static final String DEFAULT_VERSION = "dev";

    private java.util.Properties classPathProperties = new java.util.Properties();

    public Properties() {
        try (InputStream in = getClass().getResourceAsStream("/build.properties")) {
            if (in != null) {
                classPathProperties.load(in);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getVersion() {
        String version = normalizeVersion(classPathProperties.getProperty("build.version"));
        if (version != null) {
            return version;
        }

        Package pkg = getClass().getPackage();
        if (pkg != null) {
            version = normalizeVersion(pkg.getImplementationVersion());
            if (version != null) {
                return version;
            }
        }

        return DEFAULT_VERSION;
    }

    static String normalizeVersion(String version) {
        if (version == null) {
            return null;
        }

        String value = version.trim();
        if (value.isEmpty()) {
            return null;
        }

        if (value.startsWith("${") && value.endsWith("}")) {
            return null;
        }

        return value;
    }
}
