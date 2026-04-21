package org.victoria2.tools.vic2sgea.main;

import junit.framework.TestCase;

public class PropertiesTest extends TestCase {

    public void testNormalizeVersion() {
        assertNull(Properties.normalizeVersion(null));
        assertNull(Properties.normalizeVersion(""));
        assertNull(Properties.normalizeVersion("   "));
        assertNull(Properties.normalizeVersion("${project.version}"));
        assertEquals("0.15-SNAPSHOT", Properties.normalizeVersion(" 0.15-SNAPSHOT "));
    }

    public void testGetVersionHasSafeFallback() {
        Properties props = new Properties();
        String version = props.getVersion();

        assertNotNull(version);
        assertFalse(version.trim().isEmpty());
        assertFalse(version.contains("${"));
    }
}
