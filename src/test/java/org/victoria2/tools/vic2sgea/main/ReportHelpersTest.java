package org.victoria2.tools.vic2sgea.main;

import junit.framework.TestCase;

public class ReportHelpersTest extends TestCase {

    public void testGetLocalisationFilesHandlesNullAndBlankPath() {
        assertTrue(ReportHelpers.getLocalisationFiles(null).isEmpty());
        assertTrue(ReportHelpers.getLocalisationFiles("   ").isEmpty());
    }

    public void testReadProductsHandlesNullAndBlankPath() {
        assertTrue(ReportHelpers.readProducts(null).isEmpty());
        assertTrue(ReportHelpers.readProducts("   ").isEmpty());
    }
}
