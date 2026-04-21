package org.victoria2.tools.vic2sgea.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.victoria2.tools.vic2sgea.entities.Country;
import org.victoria2.tools.vic2sgea.main.LoadRequestValidator;
import org.victoria2.tools.vic2sgea.main.PathKeeper;
import org.victoria2.tools.vic2sgea.main.Report;
import org.victoria2.tools.vic2sgea.main.TableRowDoubleClickFactory;
import org.victoria2.tools.vic2sgea.main.Wrapper;
import org.victoria2.tools.vic2sgea.watcher.Watch;
import org.victoria2.tools.vic2sgea.watcher.WatchUtil;
import org.victoria2.tools.vic2sgea.watcher.Watcher;
import org.victoria2.tools.vic2sgea.watcher.WatcherManager;
import org.victoria2.tools.vic2sgea.watcher.WorldState;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * @author nashet
 */
public class WindowController extends BaseController implements Initializable {

    private static final int MAX_HISTORY_CHART_POINTS = 1400;
    private static final int HISTORY_SERIES_CACHE_SIZE = 64;
    private static final ObservableList<Country> countryTableContent = FXCollections.observableArrayList();

    @FXML
    Button btnLoad;
    @FXML
    Button btnGoods;
    @FXML
    public Label lblStartDate;
    @FXML
    public Label lblCurrentDate;
    @FXML
    public Label lblPlayer;
    @FXML
    public Label lblPopCount;
    @FXML
    TableView<Country> mainTable;

    @FXML
    FilePrompt fpSaveGame;
    @FXML
    FilePrompt fpGamePath;
    @FXML
    FilePrompt fpModPath;
    @FXML
    public Pane progressWrap;
    @FXML
    ProgressIndicator piLoad;

    @FXML
    ChoiceBox<Watcher> cbHistoryWatcher;
    @FXML
    FilePrompt fpHistoryFile;
    @FXML
    Label lblHistoryStatus;
    @FXML
    TextField tfHistoryCountryFilter;
    @FXML
    ListView<HistoryCountryOption> lvHistoryCountries;
    @FXML
    VBox historyChartsBox;

    private ProductListController productListController;
    private final ObservableList<HistoryCountryOption> historyCountries = FXCollections.observableArrayList();
    private FilteredList<HistoryCountryOption> filteredHistoryCountries;
    private final EnumMap<HistoryMetric, LineChart<Number, Number>> historyCharts = new EnumMap<>(HistoryMetric.class);
    private HistoryData historyData = HistoryData.empty();
    private String historyStatusPrefix = "No history loaded";

    @FXML
    public TableColumn<Country, ImageView> colImage;
    @FXML
    public TableColumn<Country, String> colCountry;
    @FXML
    public TableColumn<Country, Long> colPopulation;
    @FXML
    TableColumn<Country, Float> colConsumption;
    @FXML
    TableColumn<Country, Float> colActualSupply;
    @FXML
    TableColumn<Country, Float> colGdp;
    @FXML
    TableColumn<Country, Float> colGDPPer;
    @FXML
    TableColumn<Country, Integer> colGDPPlace;
    @FXML
    TableColumn<Country, Float> colGDPPart;
    @FXML
    TableColumn<Country, Long> colGoldIncome;
    @FXML
    TableColumn<Country, Long> colWorkForceRgo;
    @FXML
    TableColumn<Country, Long> colWorkForceFactory;
    @FXML
    TableColumn<Country, Long> colEmployment;
    @FXML
    TableColumn<Country, Float> colExport;
    @FXML
    TableColumn<Country, Float> colImport;
    @FXML
    TableColumn<Country, Float> colUnemploymentRate;
    @FXML
    TableColumn<Country, Float> colUnemploymentRateFactory;

    @FXML
    TableColumn<Country, Double> colWageRgo;
    @FXML
    TableColumn<Country, Double> colWageFactory;

    private Report report;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {

        // add row click handler
        mainTable.setRowFactory(new TableRowDoubleClickFactory<>(country -> Main.showCountry(report, country)));
        mainTable.setItems(countryTableContent);
        mainTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        colImage.setCellValueFactory(features -> {
            String tag = features.getValue().getTag();
            URL url = getClass().getResource("/flags/" + tag + ".png");
            if (url == null) {
                return null;
            }

            Image image = new Image(url.toString());
            ImageView iv = new ImageView(image);
            iv.setPreserveRatio(true);
            iv.setFitHeight(20);
            iv.getStyleClass().add("flag");
            return new SimpleObjectProperty<>(iv);
        });

        setFactory(colCountry, Country::getOfficialName);
        setFactory(colPopulation, Country::getPopulation);
        setFactory(colActualSupply, Country::getSold);
        setFactory(colGdp, Country::getGdp);
        setFactory(colConsumption, Country::getBought);
        setFactory(colGDPPer, Country::getGdpPerCapita);
        setFactory(colGDPPlace, Country::getGDPPlace);
        setFactory(colGDPPart, Country::getGDPPart);
        setFactory(colGoldIncome, Country::getGoldIncome);

        setFactory(colWorkForceRgo, Country::getWorkforceRgo);
        setFactory(colWorkForceFactory, Country::getWorkforceFactory);
        setFactory(colEmployment, Country::getEmployment);

        setFactory(colExport, Country::getExported);
        setFactory(colImport, Country::getImported);

        setFactory(colUnemploymentRate, Country::getUnemploymentRateRgo);
        setFactory(colUnemploymentRateFactory, Country::getUnemploymentRateFactory);

        setFactory(colWageRgo, country -> country.wagesRgo * 100 / country.getEmploymentRGO());
        setFactory(colWageFactory, country -> country.wagesFactory * 100 / country.getEmploymentFactory());

        setCellFactory(colPopulation, new KmgConverter<>());
        setCellFactory(colActualSupply, new KmgConverter<>());
        setCellFactory(colGdp, new KmgConverter<>());
        setCellFactory(colGDPPart, new PercentageConverter());
        setCellFactory(colGDPPer, new NiceNumberConverter<>());
        setCellFactory(colWorkForceRgo, new KmgConverter<>());
        setCellFactory(colWorkForceFactory, new KmgConverter<>());
        setCellFactory(colEmployment, new KmgConverter<>());
        setCellFactory(colUnemploymentRate, new PercentageConverter());
        setCellFactory(colUnemploymentRateFactory, new PercentageConverter());
        setCellFactory(colWageRgo, new NiceNumberConverter<>());
        setCellFactory(colWageFactory, new NiceNumberConverter<>());

        colConsumption.setVisible(false);
        colActualSupply.setVisible(false);
        colWorkForceRgo.setVisible(false);
        colWorkForceFactory.setVisible(false);
        colEmployment.setVisible(false);
        colExport.setVisible(false);
        colImport.setVisible(false);

        PathKeeper.init();

        PathKeeper.getSavePath().ifPresent(fpSaveGame::setPath);
        PathKeeper.getLocalisationPath().ifPresent(fpGamePath::setPath);
        PathKeeper.getModPath().ifPresent(fpModPath::setPath);

        lblPlayer.setOnMouseClicked(e -> {
            if (report != null) {
                Main.showCountry(report, report.getPlayerCountry());
            }
        });

        setupHistoryUi();
    }

    private void setupHistoryUi() {
        cbHistoryWatcher.setItems(WatcherManager.getInstance().getWatcherList());
        cbHistoryWatcher.setConverter(new StringConverter<Watcher>() {
            @Override
            public String toString(Watcher watcher) {
                if (watcher == null) {
                    return "";
                }
                return watcher.getHistoryFile().toString();
            }

            @Override
            public Watcher fromString(String string) {
                return null;
            }
        });

        cbHistoryWatcher.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                fpHistoryFile.setPath(newValue.getHistoryFile());
            }
        });

        WatcherManager.getInstance().getWatcherList().addListener((ListChangeListener<Watcher>) change -> {
            if (cbHistoryWatcher.getSelectionModel().getSelectedItem() == null && !cbHistoryWatcher.getItems().isEmpty()) {
                cbHistoryWatcher.getSelectionModel().selectFirst();
            }
        });

        if (!cbHistoryWatcher.getItems().isEmpty()) {
            cbHistoryWatcher.getSelectionModel().selectFirst();
        }

        lvHistoryCountries.setCellFactory(list -> new ListCell<HistoryCountryOption>() {
            @Override
            protected void updateItem(HistoryCountryOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDisplayName());
            }
        });
        lvHistoryCountries.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        filteredHistoryCountries = new FilteredList<>(historyCountries, option -> true);
        lvHistoryCountries.setItems(filteredHistoryCountries);
        lvHistoryCountries.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<HistoryCountryOption>) change -> refreshHistoryCharts());

        tfHistoryCountryFilter.textProperty().addListener((observable, oldValue, newValue) -> applyHistoryCountryFilter(newValue));

        createHistoryCharts();
        refreshHistoryStatus();
        tryAutoLoadHistoryFromWatcher();
    }

    private void createHistoryCharts() {
        historyChartsBox.getChildren().clear();
        historyCharts.clear();

        for (HistoryMetric metric : HistoryMetric.values()) {
            LineChart<Number, Number> chart = createHistoryChart(metric);
            historyCharts.put(metric, chart);
            historyChartsBox.getChildren().add(chart);
        }
    }

    private LineChart<Number, Number> createHistoryChart(HistoryMetric metric) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setForceZeroInRange(false);
        xAxis.setLabel("Date");
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                if (object == null) {
                    return "";
                }
                return historyData.getDateLabel(object.intValue());
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });

        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(metric.forceZeroInRange);
        yAxis.setLabel(metric.axisLabel);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(true);
        chart.setTitle(metric.title);
        chart.setMinHeight(210);
        chart.setPrefHeight(210);
        chart.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(chart, Priority.NEVER);
        return chart;
    }

    public final void onGoods(ActionEvent event) {
        Main.showProductList();
    }

    private void setInterfaceEnabled(boolean isEnabled) {
        progressWrap.setVisible(!isEnabled);
        progressWrap.toFront();
    }

    public void onLoad() {
        Path savePath = fpSaveGame.getPath();
        Path gamePath = fpGamePath.getPath();
        Path modPath = LoadRequestValidator.normalizeOptionalPath(fpModPath.getPath());

        try {
            LoadRequestValidator.validate(savePath, gamePath, modPath);
        } catch (IllegalArgumentException e) {
            errorAlert(e.getMessage());
            return;
        }

        setInterfaceEnabled(false);

        Task<Integer> task = new Task<Integer>() {
            @Override
            protected Integer call() {
                System.out.println();
                System.out.println("Nash: calc thread started...");
                float startTime = System.nanoTime();

                try {
                    PathKeeper.save(savePath, gamePath, modPath);

                    report = new Report(savePath.toString(), gamePath.toString(), modPath != null ? modPath.toString() : null);

                    countryTableContent.setAll(report.getCountryList());
                    if (productListController != null) {
                        productListController.setReport(report);
                    }

                    float res = ((float) System.nanoTime() - startTime) / 1000000000;
                    System.out.println("Nash: total time is " + res + " seconds");
                    Platform.runLater(() -> {
                        setLabels(report);
                        selectHistoryCountriesByTags(getPreferredHistoryTags());
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    errorAlert(e, getLoadErrorMessage(e));
                } finally {
                    Platform.runLater(() -> setInterfaceEnabled(true));
                }

                return 0;
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private static String getLoadErrorMessage(Exception e) {
        if (e instanceof FileNotFoundException) {
            return "Could not open the selected savegame file. Check the Savegame path and file permissions.";
        }

        if (e instanceof InvalidPathException) {
            return "One of the selected paths is invalid. Please reselect Savegame, Game, and Mod paths.";
        }

        return "Could not load the selected savegame data. Please verify Savegame path, Game path, and optional Mod path.";
    }

    private void setLabels(Report report) {
        lblCurrentDate.setText(report.getCurrentDate());
        lblPlayer.setText(report.getPlayerCountry().getOfficialName());
        lblStartDate.setText(report.getStartDate());
        lblPopCount.setText(report.popCount + " pops total");
    }

    public void setProductListController(ProductListController productListController) {
        this.productListController = productListController;
    }

    public void createNewHistory() {
    }

    public void onWatcherWindow() {
        Main.showWatcherWindow();
    }

    public void onManualExport() {
        FileChooser chooser = new FileChooser();
        List<File> files = chooser.showOpenMultipleDialog(null);
        if (files != null) {
            Watch watch = WatchUtil.fromExisting(
                    files.stream().map(File::toPath).collect(Collectors.toList())
            );
            Main.showManualExportWindow(watch);
        }
    }

    public void onUseWatcherHistory() {
        Watcher watcher = cbHistoryWatcher.getValue();
        if (watcher == null) {
            errorAlert("No active watcher selected");
            return;
        }

        loadHistoryAsync(watcher::getWatch, "watcher " + watcher.getHistoryFile());
    }

    public void onLoadHistoryFile() {
        Path historyPath = fpHistoryFile.getPath();
        if (historyPath == null) {
            errorAlert("Select a history file first");
            return;
        }
        if (!Files.exists(historyPath)) {
            errorAlert("History file does not exist: " + historyPath);
            return;
        }

        loadHistoryAsync(() -> WatchUtil.read(historyPath), "file " + historyPath);
    }

    public void onSelectAllHistoryCountries() {
        lvHistoryCountries.getSelectionModel().selectAll();
    }

    public void onClearHistorySelection() {
        lvHistoryCountries.getSelectionModel().clearSelection();
    }

    public void onUseTableSelectionForHistory() {
        Set<String> selectedTags = mainTable.getSelectionModel().getSelectedItems().stream()
                .map(Country::getTag)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (selectedTags.isEmpty()) {
            errorAlert("Select one or more countries in the Overview tab first");
            return;
        }

        tfHistoryCountryFilter.clear();
        selectHistoryCountriesByTags(selectedTags);
    }

    public void onExportHistoryCsv() {
        if (historyData.isEmpty()) {
            errorAlert("No history loaded");
            return;
        }

        List<HistoryCountryOption> selectedCountries = new ArrayList<>(lvHistoryCountries.getSelectionModel().getSelectedItems());
        if (selectedCountries.isEmpty()) {
            errorAlert("Select at least one country to export");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("history-series.csv");
        File outputFile = chooser.showSaveDialog(null);
        if (outputFile == null) {
            return;
        }

        setInterfaceEnabled(false);
        Task<Void> exportTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                writeHistoryCsv(outputFile.toPath(), selectedCountries);
                return null;
            }
        };

        exportTask.setOnSucceeded(event -> {
            setInterfaceEnabled(true);
            infoAlert("History exported to " + outputFile.getAbsolutePath());
        });
        exportTask.setOnFailed(event -> {
            setInterfaceEnabled(true);
            Throwable e = exportTask.getException();
            if (e instanceof Exception) {
                errorAlert((Exception) e, "Could not export history CSV");
            } else {
                errorAlert("Could not export history CSV");
            }
        });

        Thread thread = new Thread(exportTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void writeHistoryCsv(Path outputPath, List<HistoryCountryOption> selectedCountries) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write("date,country_tag,country_name,gdp,gdp_per_capita,population,imports,exports,rgo_unemployment,factory_unemployment");
            writer.newLine();

            for (HistoryCountryOption countryOption : selectedCountries) {
                CountryHistorySeries series = historyData.getSeries(countryOption.getTag());
                for (int idx = 0; idx < historyData.getSnapshotCount(); idx++) {
                    if (!series.hasAnyValue(idx)) {
                        continue;
                    }

                    writer.write(csvEscape(historyData.getDate(idx)));
                    writer.write(",");
                    writer.write(csvEscape(countryOption.getTag()));
                    writer.write(",");
                    writer.write(csvEscape(countryOption.getName()));
                    writer.write(",");
                    writer.write(formatCsvNumber(series.getMetricValue(HistoryMetric.GDP, idx)));
                    writer.write(",");
                    writer.write(formatCsvNumber(series.getMetricValue(HistoryMetric.GDP_PER_CAPITA, idx)));
                    writer.write(",");
                    writer.write(series.hasPopulation(idx) ? Long.toString(series.getPopulation(idx)) : "");
                    writer.write(",");
                    writer.write(formatCsvNumber(series.getMetricValue(HistoryMetric.IMPORTS, idx)));
                    writer.write(",");
                    writer.write(formatCsvNumber(series.getMetricValue(HistoryMetric.EXPORTS, idx)));
                    writer.write(",");
                    writer.write(formatCsvNumber(series.getMetricValue(HistoryMetric.RGO_UNEMPLOYMENT, idx)));
                    writer.write(",");
                    writer.write(formatCsvNumber(series.getMetricValue(HistoryMetric.FACTORY_UNEMPLOYMENT, idx)));
                    writer.newLine();
                }
            }
        }
    }

    private static String formatCsvNumber(double value) {
        if (!Double.isFinite(value)) {
            return "";
        }
        return String.format(Locale.US, "%.6f", value);
    }

    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private void loadHistoryAsync(Callable<Watch> source, String sourceDescription) {
        setInterfaceEnabled(false);
        historyStatusPrefix = "Loading history...";
        refreshHistoryStatus();

        Task<HistoryData> task = new Task<HistoryData>() {
            @Override
            protected HistoryData call() throws Exception {
                Watch watch = source.call();
                return HistoryData.fromWatch(watch);
            }
        };

        task.setOnSucceeded(event -> {
            setInterfaceEnabled(true);
            historyData = task.getValue();
            historyStatusPrefix = historyData.isEmpty()
                    ? "History loaded but no snapshots were found"
                    : "Loaded history from " + sourceDescription;
            refreshHistoryCountryListAndSelection();
            refreshHistoryCharts();
        });

        task.setOnFailed(event -> {
            setInterfaceEnabled(true);
            historyStatusPrefix = "History loading failed";
            refreshHistoryStatus();
            Throwable e = task.getException();
            if (e instanceof Exception) {
                errorAlert((Exception) e, "Could not load history");
            } else {
                errorAlert("Could not load history");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void tryAutoLoadHistoryFromWatcher() {
        Watcher selectedWatcher = cbHistoryWatcher.getValue();
        if (selectedWatcher == null || selectedWatcher.getWatch() == null) {
            return;
        }
        Watch watch = selectedWatcher.getWatch();
        if (watch.getHistory() == null || watch.getHistory().isEmpty()) {
            return;
        }
        loadHistoryAsync(selectedWatcher::getWatch, "watcher " + selectedWatcher.getHistoryFile());
    }

    private void refreshHistoryCountryListAndSelection() {
        Set<String> previousSelection = lvHistoryCountries.getSelectionModel().getSelectedItems().stream()
                .map(HistoryCountryOption::getTag)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        historyCountries.setAll(historyData.getCountryOptions());
        applyHistoryCountryFilter(tfHistoryCountryFilter.getText());

        if (!previousSelection.isEmpty()) {
            selectHistoryCountriesByTags(previousSelection);
        } else {
            selectHistoryCountriesByTags(getPreferredHistoryTags());
        }
    }

    private Set<String> getPreferredHistoryTags() {
        LinkedHashSet<String> preferred = new LinkedHashSet<>();
        if (report != null && report.getPlayerCountry() != null) {
            preferred.add(report.getPlayerCountry().getTag());
        }
        if (preferred.isEmpty() && !historyCountries.isEmpty()) {
            preferred.add(historyCountries.get(0).getTag());
        }
        return preferred;
    }

    private void selectHistoryCountriesByTags(Set<String> tags) {
        if (tags == null || tags.isEmpty() || historyCountries.isEmpty()) {
            refreshHistoryStatus();
            return;
        }

        lvHistoryCountries.getSelectionModel().clearSelection();
        for (HistoryCountryOption option : historyCountries) {
            if (tags.contains(option.getTag())) {
                lvHistoryCountries.getSelectionModel().select(option);
            }
        }
        refreshHistoryStatus();
    }

    private void applyHistoryCountryFilter(String value) {
        String filter = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        filteredHistoryCountries.setPredicate(option -> {
            if (filter.isEmpty()) {
                return true;
            }
            return option.getTag().toLowerCase(Locale.ROOT).contains(filter)
                    || option.getName().toLowerCase(Locale.ROOT).contains(filter);
        });
    }

    private void refreshHistoryCharts() {
        for (LineChart<Number, Number> chart : historyCharts.values()) {
            chart.getData().clear();
        }

        if (historyData.isEmpty()) {
            refreshHistoryStatus();
            return;
        }

        List<HistoryCountryOption> selectedCountries = new ArrayList<>(lvHistoryCountries.getSelectionModel().getSelectedItems());
        if (selectedCountries.isEmpty()) {
            refreshHistoryStatus();
            return;
        }

        int[] indicesToDisplay = historyData.getDisplayIndices(MAX_HISTORY_CHART_POINTS);
        for (HistoryMetric metric : HistoryMetric.values()) {
            LineChart<Number, Number> chart = historyCharts.get(metric);
            ObservableList<XYChart.Series<Number, Number>> lines = FXCollections.observableArrayList();

            for (HistoryCountryOption countryOption : selectedCountries) {
                CountryHistorySeries seriesData = historyData.getSeries(countryOption.getTag());
                XYChart.Series<Number, Number> line = new XYChart.Series<>();
                line.setName(countryOption.getDisplayName());

                for (int idx : indicesToDisplay) {
                    double value = seriesData.getMetricValue(metric, idx);
                    if (Double.isFinite(value)) {
                        line.getData().add(new XYChart.Data<>(idx, value));
                    }
                }

                if (!line.getData().isEmpty()) {
                    lines.add(line);
                }
            }

            chart.setData(lines);
        }

        refreshHistoryStatus();
    }

    private void refreshHistoryStatus() {
        if (historyData.isEmpty()) {
            lblHistoryStatus.setText(historyStatusPrefix);
            return;
        }
        lblHistoryStatus.setText(String.format("%s | snapshots: %d | countries: %d | selected: %d",
                historyStatusPrefix,
                historyData.getSnapshotCount(),
                historyData.getCountryCount(),
                lvHistoryCountries.getSelectionModel().getSelectedItems().size()));
    }

    private enum HistoryMetric {
        GDP("GDP", "GDP", true),
        GDP_PER_CAPITA("GDP per capita", "GDP per capita", true),
        POPULATION("Population", "Population", true),
        IMPORTS("Imports", "Import value", true),
        EXPORTS("Exports", "Export value", true),
        RGO_UNEMPLOYMENT("RGO unemployment", "Percent", true),
        FACTORY_UNEMPLOYMENT("Factory unemployment", "Percent", true);

        private final String title;
        private final String axisLabel;
        private final boolean forceZeroInRange;

        HistoryMetric(String title, String axisLabel, boolean forceZeroInRange) {
            this.title = title;
            this.axisLabel = axisLabel;
            this.forceZeroInRange = forceZeroInRange;
        }
    }

    private static class HistoryCountryOption {
        private final String tag;
        private final String name;

        private HistoryCountryOption(String tag, String name) {
            this.tag = tag;
            this.name = name;
        }

        public String getTag() {
            return tag;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            if (Objects.equals(tag, name)) {
                return tag;
            }
            return tag + " - " + name;
        }

        @Override
        public String toString() {
            return getDisplayName();
        }

        @Override
        public int hashCode() {
            return Objects.hash(tag);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof HistoryCountryOption)) {
                return false;
            }
            HistoryCountryOption other = (HistoryCountryOption) obj;
            return Objects.equals(tag, other.tag);
        }
    }

    private static class CountryHistorySeries {
        private final float[] gdp;
        private final float[] gdpPerCapita;
        private final long[] population;
        private final boolean[] populationPresent;
        private final float[] imports;
        private final float[] exports;
        private final float[] rgoUnemployment;
        private final float[] factoryUnemployment;

        private CountryHistorySeries(int size) {
            gdp = new float[size];
            Arrays.fill(gdp, Float.NaN);
            gdpPerCapita = new float[size];
            Arrays.fill(gdpPerCapita, Float.NaN);
            population = new long[size];
            populationPresent = new boolean[size];
            imports = new float[size];
            Arrays.fill(imports, Float.NaN);
            exports = new float[size];
            Arrays.fill(exports, Float.NaN);
            rgoUnemployment = new float[size];
            Arrays.fill(rgoUnemployment, Float.NaN);
            factoryUnemployment = new float[size];
            Arrays.fill(factoryUnemployment, Float.NaN);
        }

        private void put(int index, Country country) {
            gdp[index] = safeFloat(country.getGdp());
            gdpPerCapita[index] = safeFloat(country.getGdpPerCapita());

            population[index] = country.getPopulation();
            populationPresent[index] = true;

            imports[index] = safeFloat(country.getImported());
            exports[index] = safeFloat(country.getExported());
            rgoUnemployment[index] = safeFloat(country.getUnemploymentRateRgo());
            factoryUnemployment[index] = safeFloat(country.getUnemploymentRateFactory());
        }

        public boolean hasAnyValue(int index) {
            return populationPresent[index]
                    || Float.isFinite(gdp[index])
                    || Float.isFinite(gdpPerCapita[index])
                    || Float.isFinite(imports[index])
                    || Float.isFinite(exports[index])
                    || Float.isFinite(rgoUnemployment[index])
                    || Float.isFinite(factoryUnemployment[index]);
        }

        public boolean hasPopulation(int index) {
            return populationPresent[index];
        }

        public long getPopulation(int index) {
            return population[index];
        }

        public double getMetricValue(HistoryMetric metric, int index) {
            if (index < 0 || index >= gdp.length) {
                return Double.NaN;
            }
            switch (metric) {
                case GDP:
                    return gdp[index];
                case GDP_PER_CAPITA:
                    return gdpPerCapita[index];
                case POPULATION:
                    return populationPresent[index] ? population[index] : Double.NaN;
                case IMPORTS:
                    return imports[index];
                case EXPORTS:
                    return exports[index];
                case RGO_UNEMPLOYMENT:
                    return rgoUnemployment[index];
                case FACTORY_UNEMPLOYMENT:
                    return factoryUnemployment[index];
                default:
                    return Double.NaN;
            }
        }
    }

    private static class HistoryData {
        private static final HistoryData EMPTY = new HistoryData(
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        private final List<String> dates;
        private final List<WorldState> states;
        private final List<HistoryCountryOption> countryOptions;
        private final Map<String, String> countryNames;
        private final Map<String, CountryHistorySeries> seriesCache = new LinkedHashMap<String, CountryHistorySeries>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CountryHistorySeries> eldest) {
                return size() > HISTORY_SERIES_CACHE_SIZE;
            }
        };

        private HistoryData(List<String> dates,
                            List<WorldState> states,
                            List<HistoryCountryOption> countryOptions,
                            Map<String, String> countryNames) {
            this.dates = dates;
            this.states = states;
            this.countryOptions = countryOptions;
            this.countryNames = countryNames;
        }

        public static HistoryData empty() {
            return EMPTY;
        }

        public static HistoryData fromWatch(Watch watch) {
            if (watch == null || watch.getHistory() == null || watch.getHistory().isEmpty()) {
                return empty();
            }

            List<Map.Entry<String, WorldState>> entries = snapshotEntries(watch);
            if (entries.isEmpty()) {
                return empty();
            }

            entries.sort((left, right) -> compareVicDate(left.getKey(), right.getKey()));

            List<String> dates = new ArrayList<>(entries.size());
            List<WorldState> states = new ArrayList<>(entries.size());
            Map<String, String> namesByTag = new HashMap<>();

            for (Map.Entry<String, WorldState> entry : entries) {
                dates.add(entry.getKey());
                WorldState state = entry.getValue();
                states.add(state);
                if (state == null) {
                    continue;
                }

                Collection<Country> countries = state.getCountries();
                if (countries == null) {
                    continue;
                }

                for (Country country : countries) {
                    if (country == null || country.getTag() == null) {
                        continue;
                    }
                    String tag = country.getTag();
                    String officialName = country.getOfficialName();
                    namesByTag.put(tag, normalizeName(tag, officialName));
                }
            }

            List<HistoryCountryOption> options = namesByTag.entrySet().stream()
                    .map(entry -> new HistoryCountryOption(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(HistoryCountryOption::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            return new HistoryData(
                    List.copyOf(dates),
                    List.copyOf(states),
                    List.copyOf(options),
                    Map.copyOf(namesByTag)
            );
        }

        public boolean isEmpty() {
            return dates.isEmpty();
        }

        public int getSnapshotCount() {
            return dates.size();
        }

        public int getCountryCount() {
            return countryOptions.size();
        }

        public String getDate(int index) {
            return dates.get(index);
        }

        public String getDateLabel(int index) {
            if (dates.isEmpty()) {
                return "";
            }
            int safeIndex = Math.max(0, Math.min(index, dates.size() - 1));
            return dates.get(safeIndex);
        }

        public List<HistoryCountryOption> getCountryOptions() {
            return countryOptions;
        }

        public int[] getDisplayIndices(int maxPoints) {
            if (dates.isEmpty()) {
                return new int[0];
            }
            return buildDisplayIndices(dates.size(), maxPoints);
        }

        public CountryHistorySeries getSeries(String tag) {
            CountryHistorySeries cached = seriesCache.get(tag);
            if (cached != null) {
                return cached;
            }

            CountryHistorySeries series = new CountryHistorySeries(dates.size());

            for (int i = 0; i < states.size(); i++) {
                WorldState state = states.get(i);
                if (state == null || state.getCountries() == null) {
                    continue;
                }
                Country country = findCountry(state.getCountries(), tag);
                if (country != null) {
                    series.put(i, country);
                }
            }

            seriesCache.put(tag, series);
            return series;
        }

        private static List<Map.Entry<String, WorldState>> snapshotEntries(Watch watch) {
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    return new ArrayList<>(watch.getHistory().entrySet());
                } catch (ConcurrentModificationException ignored) {
                    Thread.yield();
                }
            }
            return List.of();
        }

        private static Country findCountry(Collection<Country> countries, String tag) {
            for (Country country : countries) {
                if (country != null && tag.equals(country.getTag())) {
                    return country;
                }
            }
            return null;
        }

        private static String normalizeName(String tag, String officialName) {
            if (officialName == null || officialName.trim().isEmpty()) {
                return tag;
            }
            return officialName.trim();
        }

        private static int[] buildDisplayIndices(int size, int maxPoints) {
            if (size <= 0) {
                return new int[0];
            }
            if (size <= maxPoints) {
                int[] all = new int[size];
                for (int i = 0; i < size; i++) {
                    all[i] = i;
                }
                return all;
            }

            int[] indices = new int[maxPoints];
            double step = (double) (size - 1) / (maxPoints - 1);
            int writePos = 0;
            int previous = -1;

            for (int i = 0; i < maxPoints; i++) {
                int idx = (int) Math.round(i * step);
                if (idx <= previous) {
                    idx = Math.min(size - 1, previous + 1);
                }
                indices[writePos++] = idx;
                previous = idx;
                if (idx == size - 1) {
                    break;
                }
            }

            if (indices[writePos - 1] != size - 1) {
                if (writePos < indices.length) {
                    indices[writePos++] = size - 1;
                } else {
                    indices[indices.length - 1] = size - 1;
                }
            }

            return writePos == indices.length ? indices : Arrays.copyOf(indices, writePos);
        }

        private static int compareVicDate(String left, String right) {
            int[] leftParts = parseVicDate(left);
            int[] rightParts = parseVicDate(right);

            for (int i = 0; i < 3; i++) {
                int cmp = Integer.compare(leftParts[i], rightParts[i]);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return left.compareTo(right);
        }

        private static int[] parseVicDate(String value) {
            int[] parts = new int[]{0, 0, 0};
            if (value == null || value.isEmpty()) {
                return parts;
            }

            String[] split = value.split("\\.");
            for (int i = 0; i < 3 && i < split.length; i++) {
                parts[i] = parseDatePart(split[i]);
            }
            return parts;
        }

        private static int parseDatePart(String value) {
            if (value == null || value.isEmpty()) {
                return 0;
            }
            int result = 0;
            boolean hasDigit = false;
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                if (ch >= '0' && ch <= '9') {
                    result = result * 10 + (ch - '0');
                    hasDigit = true;
                } else if (hasDigit) {
                    break;
                }
            }
            return result;
        }
    }

    private static float safeFloat(float value) {
        return Float.isFinite(value) ? value : Float.NaN;
    }
}

class KmgConverter<T extends Number> extends StringConverter<T> {

    @Override
    public String toString(T object) {
        return Wrapper.toKMG(object);
    }

    // don't need this
    @Override
    public T fromString(String string) {
        return null;
    }
}

class PercentageConverter extends StringConverter<Float> {

    @Override
    public String toString(Float object) {
        return Wrapper.toPercentage(object);
    }

    // don't need this
    @Override
    public Float fromString(String string) {
        return null;
    }
}

class NiceNumberConverter<T extends Number> extends StringConverter<T> {

    @Override
    public String toString(T object) {
        return String.format("%.3f", object.doubleValue());
    }

    // don't need this
    @Override
    public T fromString(String string) {
        return null;
    }
}
