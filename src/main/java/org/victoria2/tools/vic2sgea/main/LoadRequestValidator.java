package org.victoria2.tools.vic2sgea.main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Validates user-provided paths before loading savegame data.
 */
public final class LoadRequestValidator {

    private LoadRequestValidator() {
    }

    public static Path normalizeOptionalPath(Path path) {
        if (path == null) {
            return null;
        }

        String value = path.toString().trim();
        if (value.isEmpty()) {
            return null;
        }

        return Paths.get(value);
    }

    public static void validate(Path savePath, Path gamePath, Path modPath) {
        validateSavePath(savePath);
        validateGamePath(gamePath);

        Path normalizedModPath = normalizeOptionalPath(modPath);
        if (normalizedModPath != null) {
            validateDirectoryPath(normalizedModPath, "Mod path");
        }
    }

    static void validateSavePath(Path savePath) {
        requireProvided(savePath, "Savegame path");

        if (!Files.exists(savePath)) {
            throw new IllegalArgumentException("Savegame path does not exist. Please choose a valid .v2 save file.");
        }

        if (!Files.isRegularFile(savePath)) {
            throw new IllegalArgumentException("Savegame path must point to a file, not a directory.");
        }
    }

    static void validateGamePath(Path gamePath) {
        validateDirectoryPath(gamePath, "Game path");

        Path goodsPath = gamePath.resolve("common").resolve("goods.txt");
        if (!Files.isRegularFile(goodsPath)) {
            throw new IllegalArgumentException("Game path is missing common/goods.txt. Please choose your Victoria II install directory.");
        }
    }

    static void validateDirectoryPath(Path path, String label) {
        requireProvided(path, label);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException(label + " does not exist. Please choose an existing directory.");
        }

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(label + " must point to a directory.");
        }
    }

    private static void requireProvided(Path path, String label) {
        if (path == null || path.toString().trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }
}
