package org.victoria2.tools.vic2sgea.watcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.victoria2.tools.vic2sgea.main.Report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WatchUtil {

    public static void addState(Watch watch, Path savePath) {
        Report report = new Report(savePath.toString(), null, null);
        WorldState state = new WorldState(report);
        watch.addState(report.getCurrentDate(), state);
    }

    public static Watch fromExisting(List<Path> saveFiles) {
        Watch watch = new Watch();
        for (Path saveFile : saveFiles) {
            addState(watch, saveFile);
        }
        return watch;
    }

    public static Watch read(Path historyPath) throws IOException {
        Gson gson = new GsonBuilder()
                .addDeserializationExclusionStrategy(new NonSerializableExclusionStrategy())
                .create();

        return gson.fromJson(Files.newBufferedReader(historyPath), Watch.class);
    }
}
