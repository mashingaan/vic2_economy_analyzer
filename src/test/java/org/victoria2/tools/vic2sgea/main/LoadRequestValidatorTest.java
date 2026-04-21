package org.victoria2.tools.vic2sgea.main;

import junit.framework.TestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class LoadRequestValidatorTest extends TestCase {

    private Path tempRoot;

    @Override
    protected void setUp() throws Exception {
        tempRoot = Files.createTempDirectory("vic2-sgea-validator-test");
    }

    @Override
    protected void tearDown() throws Exception {
        if (tempRoot != null && Files.exists(tempRoot)) {
            Files.walk(tempRoot)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    public void testNormalizeOptionalPath() {
        assertNull(LoadRequestValidator.normalizeOptionalPath(null));
        assertNull(LoadRequestValidator.normalizeOptionalPath(Path.of("")));
        assertEquals(Path.of("mod"), LoadRequestValidator.normalizeOptionalPath(Path.of("mod")));
    }

    public void testValidateAcceptsNullModPath() throws IOException {
        Path saveFile = Files.createFile(tempRoot.resolve("savegame.v2"));
        Path gameDir = Files.createDirectories(tempRoot.resolve("game"));
        Files.createDirectories(gameDir.resolve("common"));
        Files.createFile(gameDir.resolve("common").resolve("goods.txt"));

        LoadRequestValidator.validate(saveFile, gameDir, null);
    }

    public void testValidateRejectsMissingSavePath() throws IOException {
        Path gameDir = Files.createDirectories(tempRoot.resolve("game"));
        Files.createDirectories(gameDir.resolve("common"));
        Files.createFile(gameDir.resolve("common").resolve("goods.txt"));

        try {
            LoadRequestValidator.validate(null, gameDir, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Savegame path is required.", e.getMessage());
        }
    }

    public void testValidateRejectsMissingGoodsFile() throws IOException {
        Path saveFile = Files.createFile(tempRoot.resolve("savegame.v2"));
        Path gameDir = Files.createDirectories(tempRoot.resolve("game"));

        try {
            LoadRequestValidator.validate(saveFile, gameDir, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Game path is missing common/goods.txt. Please choose your Victoria II install directory.", e.getMessage());
        }
    }

    public void testValidateRejectsInvalidModPath() throws IOException {
        Path saveFile = Files.createFile(tempRoot.resolve("savegame.v2"));
        Path gameDir = Files.createDirectories(tempRoot.resolve("game"));
        Files.createDirectories(gameDir.resolve("common"));
        Files.createFile(gameDir.resolve("common").resolve("goods.txt"));

        Path modFile = Files.createFile(tempRoot.resolve("mod-not-directory"));

        try {
            LoadRequestValidator.validate(saveFile, gameDir, modFile);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Mod path must point to a directory.", e.getMessage());
        }
    }
}
