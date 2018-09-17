package trace.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDrawer;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.effects.JFXDepthManager;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import trace.model.Data;
import trace.model.Layout;
import trace.subtitle.Caption;
import trace.subtitle.FormatSRT;
import trace.subtitle.TimedTextObject;
import trace.utils.CustomPagination;
import trace.utils.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TraceController implements Initializable {

    @FXML
    private AnchorPane rootPane;
    @FXML
    private HBox menu;
    @FXML
    private StackPane stackPane;
    @FXML
    private JFXDrawer drawer;
    @FXML
    private JFXButton menuButton;
    @FXML
    private TextField searchBox;
    private JFXPopup searchPopup;
    private List<String> reviewsResult;
    private List<String> familiarResult;
    private static Thread searchThread;
    private BooleanProperty hasResults = new SimpleBooleanProperty(false);

    private BooleanProperty hasResultsProperty() {
        return hasResults;
    }


    public HashMap<String, String> progressMap = new HashMap<>();
    public ArrayList<String> reviewsShuffleList;
    public ArrayList<String> familiarShuffleList;
    public ArrayList<String> markList = new ArrayList<>();
    public boolean isMouseClicked;

    private CustomPagination reviewsPagination = new CustomPagination(0, 0);
    private CustomPagination familiarPagination = new CustomPagination(0, 0);


    private String detailWord;
    public boolean reviewsChange = false;
    public boolean familiarChange = false;

    public Layout currentLayout;
    public Data dataLayout;
    public boolean isSearch;

    //Media File Path
    public String mediaPath;
    public String subtitlePath;
    public int sync;


    /**
     * 用户设置
     */
    public Path configPath = Paths.get("resources/config.properties");
    public Properties config;
    //    public SimpleIntegerProperty fontSize;
    public SimpleBooleanProperty subtitleOpen;
    public SimpleIntegerProperty interval;
    public SimpleStringProperty subtitleCoverColor;
    public SimpleStringProperty subtitleColor;
    public Font font;
    public SimpleBooleanProperty opacity;
    public SimpleDoubleProperty infiniteVBoxOpacity;
    public SimpleBooleanProperty cyclePopupOpen;
    public SimpleDoubleProperty newTranslateX = new SimpleDoubleProperty();
    public SimpleDoubleProperty newTranslateY = new SimpleDoubleProperty();
    public SimpleDoubleProperty volume;
    public SimpleDoubleProperty subtitleQuantity;




    /**
     * 不认识的单词
     * 需要复习的单词
     * 第一个 String 是单词，第二个 String 是 媒体，第三个 Integer 是字幕的开始时间。
     */
    public HashMap<String, TreeMap<String, TreeMap<Integer, Caption>>> reviews;
    /**
     * 用于 sidePane 显示单词数量
     */
    public SimpleStringProperty reviewSize = new SimpleStringProperty();
    /**
     * 我的词库，我认识的单词
     */
    public HashMap<String, TreeMap<String, TreeMap<Integer, Caption>>> familiar;
    /**
     * 用于SidePane 显示数量
     */
    public SimpleStringProperty familiarSize = new SimpleStringProperty();


    /**
     * 字典，用于判断字符串是否是单词。语料库 600000 + 拼写检查库  = 128047 个单词，
     */
    private List<String> dictionary;

    final CountDownLatch reviewDone = new CountDownLatch(1);
    final CountDownLatch familiarDone = new CountDownLatch(1);



    @Override
    public void initialize(URL url, ResourceBundle rb) {
        JFXDepthManager.setDepth(menu, 3);
        loadResource();
        initShuffList();
        initPagination();
        initDrawer();
        createSearchPopup();
    }

    private void loadResource() {

        Runnable readReviewDictionary = new Runnable() {
            @Override
            public void run() {
                File review = Paths.get("resources/word/review.json").toFile();
                if (review.exists()) {
                    String json = FileUtils.readJSON("resources/word/review.json");
                    Type type = new TypeToken<HashMap<String, TreeMap<String, TreeMap<Integer, Caption>>>>() {
                    }.getType();

                    Gson gson = new Gson();
                    reviews = gson.fromJson(json, type);
                    reviewDone.countDown();

                } else {
                    reviews = new HashMap<String, TreeMap<String, TreeMap<Integer, Caption>>>();
                    reviewDone.countDown();
                }
            }
        };
        Runnable readFamiliarDictionary = new Runnable() {
            @Override
            public void run() {
                File basic = Paths.get("resources/word/basic.json").toFile();
                if (basic.exists()) {

                    String json = FileUtils.readJSON("resources/word/basic.json");
                    Type type = new TypeToken<HashMap<String, TreeMap<String, TreeMap<Integer, Caption>>>>() {
                    }.getType();
                    Gson gson = new Gson();
                    familiar = gson.fromJson(json, type);
                    familiarDone.countDown();
                } else {
                    familiar = new HashMap<String, TreeMap<String, TreeMap<Integer, Caption>>>();
                    familiarDone.countDown();
                }

            }
        };
        Runnable readDictionary = new Runnable() {
            @Override
            public void run() {
                String json = FileUtils.readJSON("resources/dictionary.json");
                Gson gson = new Gson();
                dictionary = gson.fromJson(json, new ArrayList<>().getClass());
            }
        };
        Runnable readConfig = new Runnable() {
            @Override
            public void run() {

                config = new Properties();
                final boolean exists = configPath.toFile().exists();
                if (exists) {
                    try (BufferedReader in = Files.newBufferedReader(configPath, Charset.forName("UTF-8"))) {
                        config.load(in);
                    } catch (IOException exc) {
                        exc.printStackTrace();
                    }
                }

                if (!exists || config.isEmpty()) {
                    config.put("subtitleOpen", "false");
                    config.put("interval", "10");
                    config.put("subtitleBackgroundColor", "#424242");
                    config.put("subtitleColor", "#0F9D58");
                    config.put("fontSize", "26");
                    config.put("fontFamily", Font.getDefault().getFamily());
                    config.put("opacity", "false");
                    config.put("cyclePopupOpen", "false");
                    config.put("newTranslateX", "0.0");
                    config.put("newTranslateY", "0.0");
                    config.put("infiniteVBoxOpacity", "0.90");
                    config.put("volume", "100");
                    config.put("subtitleQuantity", "5");
                    try (PrintStream out = new PrintStream("resources/config.properties", "UTF-8")) {
                        config.list(out);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                opacity = new SimpleBooleanProperty(Boolean.parseBoolean(config.getProperty("opacity")));
                font = Font.font(config.getProperty("fontFamily"), Double.parseDouble(config.getProperty("fontSize")));
                subtitleOpen = new SimpleBooleanProperty(Boolean.parseBoolean(config.getProperty("subtitleOpen")));
                interval = new SimpleIntegerProperty(Integer.parseInt(config.getProperty("interval")));
                subtitleCoverColor = new SimpleStringProperty(config.getProperty("subtitleBackgroundColor"));
                subtitleColor = new SimpleStringProperty(config.getProperty("subtitleColor"));
                cyclePopupOpen = new SimpleBooleanProperty(Boolean.parseBoolean(config.getProperty("cyclePopupOpen")));
                infiniteVBoxOpacity = new SimpleDoubleProperty(Double.parseDouble(config.getProperty("infiniteVBoxOpacity")));
                volume = new SimpleDoubleProperty(Double.parseDouble(config.getProperty("volume")));
                subtitleQuantity = new SimpleDoubleProperty(Integer.parseInt(config.getProperty("subtitleQuantity")));
            }
        };
        Runnable progress = new Runnable() {
            @Override
            public void run() {
                File progress = Paths.get("resources/progress.json").toFile();
                if (progress.exists()) {
                    String json = FileUtils.readJSON("resources/progress.json");
                    Gson gson = new Gson();
                    progressMap = gson.fromJson(json, new HashMap<>().getClass());
                } else {
                    progressMap = new HashMap<>();
                }
            }
        };

        new Thread(readReviewDictionary).start();
        new Thread(readFamiliarDictionary).start();
        new Thread(readDictionary).start();
        new Thread(readConfig).start();
        new Thread(progress).start();
    }

    private void initShuffList() {
        try {
            reviewDone.await();
            familiarDone.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        updateShuffList();
    }

    public void updateShuffList() {
        updateReviewsList();
        updateFamiliarList();
    }

    public void updateReviewsList() {
        reviewsShuffleList = new ArrayList<>();
        final Iterator<String> iterator = reviews.keySet().iterator();
        while (iterator.hasNext()) {
            final String next = iterator.next();

            final String strings = progressMap.get(next);
            if (strings == null) {
                break;
            }
            final String[] split = strings.split("\\|");
            int hour = Integer.parseInt(split[0]);// 重复的间隔，
            final long lastTimeMillis = Long.parseLong(split[1]);
            final long currentTimeMillis = System.currentTimeMillis();
            final long interval = currentTimeMillis - lastTimeMillis;
            final long twelveHours = 12 * 60 * 60 * 1000;
            long intervalMillis;
            switch (hour) {
                case 0:
                    reviewsShuffleList.add(next);
//                    }
                    break;
                case 12:
                    intervalMillis = 12 * 60 * 60 * 1000;
                    if (interval > intervalMillis) {
                        reviewsShuffleList.add(next);
                    }
                    break;
                case 24:
                    intervalMillis = 24 * 60 * 60 * 1000;
                    if (interval > intervalMillis) {
                        reviewsShuffleList.add(next);
                    }
                    break;
                case 48:
                    intervalMillis = 48 * 60 * 60 * 1000;
                    if (interval > intervalMillis) {
                        reviewsShuffleList.add(next);
                    }
                    break;
                case 96:
                    intervalMillis = 96 * 60 * 60 * 1000;
                    if (interval > intervalMillis) {
                        reviewsShuffleList.add(next);
                    }
                    break;
                case 168:
                    intervalMillis = 168 * 60 * 60 * 1000;
                    if (interval > intervalMillis) {
                        reviewsShuffleList.add(next);
                    }
                    break;
                case 360:
                    intervalMillis = 360 * 60 * 60 * 1000;
                    if (interval > intervalMillis) {
                        reviewsShuffleList.add(next);
                    }
                    break;
                case 720:
                    intervalMillis = 720 * 60 * 60 * 1000;
                    if (interval > intervalMillis) {
                        reviewsShuffleList.add(next);
                    }
                    break;
                case 2160:
                    intervalMillis = 2160 * 60 * 60 * 1000;
                    if (interval > intervalMillis) {
                        reviewsShuffleList.add(next);
                    }
                    break;
            }
        }
        reviewSize.set(String.valueOf(reviewsShuffleList.size()));
        Collections.shuffle(reviewsShuffleList);

    }

    public void updateFamiliarList() {
        familiarShuffleList = new ArrayList<>(familiar.keySet());
        Collections.shuffle(familiarShuffleList);
    }

    public void updateReviewsPageFactory() {
        reviewsPagination.setPageFactory(new Callback<Integer, Node>() {
            public Node call(Integer pageIndex) {
                FlowPane flowPane = new FlowPane();
                flowPane.getStyleClass().add("flowPane");
                flowPane.setHgap(15);
                flowPane.setVgap(15);
                final double padding = reviewsPagination.widthProperty().get();
                flowPane.setPadding(new Insets(24, padding, 0, padding));
                int t = pageIndex * 200;
                final int size = reviewsShuffleList.size() - 1;
                if (!reviewsShuffleList.isEmpty()) {
                    reviewsPagination.setVisible(true);
                    for (int i = t; i < t + 200; i++) {
                        if (i > size) break;
                        final String str = reviewsShuffleList.get(i);

                        final Label word = decoratorLabel(str);
                        if (markList != null && markList.contains(str)) {
                            word.setStyle("-fx-background-color:#404040");
                        }
                        flowPane.getChildren().add(word);
                    }
                }

                ScrollPane scrollPane = new ScrollPane(flowPane);
                scrollPane.widthProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        Platform.runLater(() -> {
                            double padding = (newValue.doubleValue() - 875) / 2;
                            flowPane.setPadding(new Insets(24, padding, 0, padding));
                        });
                    }
                });
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scrollPane.setFocusTraversable(false);
                scrollPane.getStyleClass().add("scrollPane");
                reviewsPagination.setPageCount(reviewsShuffleList.size() / 200 + 1);
                return scrollPane;
            }

        });
    }

    public void updateFamiliarPageFactory() {
        familiarPagination.setPageFactory(new Callback<Integer, Node>() {
            public Node call(Integer pageIndex) {
                FlowPane flowPane = new FlowPane();
                flowPane.getStyleClass().add("flowPane");
                flowPane.setHgap(15);
                flowPane.setVgap(15);
                final double padding = familiarPagination.widthProperty().get();
                flowPane.setPadding(new Insets(24, padding, 0, padding));

                int t = pageIndex * 200;
                final int size = familiarShuffleList.size() - 1;
                if (!familiarShuffleList.isEmpty()) {
                    familiarPagination.setVisible(true);
                    for (int i = t; i < t + 200; i++) {
                        if (i > size) break;
                        final Label word = decoratorLabel(familiarShuffleList.get(i));
                        flowPane.getChildren().add(word);
                    }
                }


                ScrollPane scrollPane = new ScrollPane(flowPane);
                scrollPane.widthProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        Platform.runLater(() -> {
                            double padding = (newValue.doubleValue() - 875) / 2;
                            flowPane.setPadding(new Insets(24, padding, 0, padding));
                        });
                    }
                });
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scrollPane.setFocusTraversable(false);
                scrollPane.getStyleClass().add("scrollPane");
                familiarPagination.setPageCount(familiarShuffleList.size() / 200 + 1);
                return scrollPane;
            }
        });
    }

    public void initPagination() {

        AnchorPane.setTopAnchor(reviewsPagination, 65.0);
        AnchorPane.setRightAnchor(reviewsPagination, 0.0);
        AnchorPane.setBottomAnchor(reviewsPagination, 0.0);
        AnchorPane.setLeftAnchor(reviewsPagination, 0.0);

        familiarPagination.setVisible(false);
        AnchorPane.setTopAnchor(familiarPagination, 65.0);
        AnchorPane.setRightAnchor(familiarPagination, 0.0);
        AnchorPane.setBottomAnchor(familiarPagination, 0.0);
        AnchorPane.setLeftAnchor(familiarPagination, 0.0);

        rootPane.getChildren().add(0, reviewsPagination);
        rootPane.getChildren().add(1, familiarPagination);

        currentLayout = Layout.reviewsLayout;
        updateReviewsPageFactory();

    }


    private void initDrawer() {
        try {
            familiarDone.await();
            reviewDone.await();
            familiarSize.set(String.valueOf(familiar.size()));
            reviewSize.set(String.valueOf(reviewsShuffleList.size()));
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(TraceController.this.getClass().getResource("/trace/view/sidePane.fxml"));
            AnchorPane sidePane = loader.load();
            SidePaneController sidePaneController = loader.getController();
            sidePaneController.setMainController(this);
            JFXDepthManager.setDepth(sidePane, 3);
            drawer.setSidePane(sidePane);
        } catch (IOException ex) {
            Logger.getLogger(TraceController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createSearchPopup() {
        // 空的 Popup
        VBox box = new VBox();
        box.setPrefSize(300, 267);
        searchPopup = new JFXPopup(box);
        searchBox.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchThread != null && searchThread.isAlive()) {
                searchThread.interrupt();
            }
            String text = newValue.trim();
            if (text.equals("")) {
                if (searchPopup.isShowing()) searchPopup.hide();

            } else {
                searchThread = new Thread(() -> {
                    try {
                        hasResults.set(false);
                        String word = text.toUpperCase();
                        reviewsResult = reviews.keySet().stream().filter(str -> str.toUpperCase().contains(word))
                                .sorted((str1, str2) -> {
                                    boolean xMatch = str1.toUpperCase().equals(word);
                                    boolean yMatch = str2.toUpperCase().equals(word);
                                    if (xMatch && yMatch) return 0;
                                    if (xMatch) return -1;
                                    if (yMatch) return 1;

                                    boolean xStartWith = str1.toUpperCase().startsWith(word);
                                    boolean yStartWith = str2.toUpperCase().startsWith(word);
                                    if (xStartWith && yStartWith) return 0;
                                    if (xStartWith) return -1;
                                    if (yStartWith) return 1;

                                    boolean xContains = str1.toUpperCase().contains(" " + word);
                                    boolean yContains = str2.toUpperCase().contains(" " + word);
                                    if (xContains && yContains) return 0;
                                    if (xContains) return -1;
                                    if (yContains) return 1;
                                    return 0;
                                }).collect(Collectors.toList());
                        if (searchThread.isInterrupted()) {
                            throw new InterruptedException();
                        }
                        familiarResult = familiar.keySet().stream().filter(str -> str.toUpperCase().contains(word))
                                .sorted((str1, str2) -> {
                                    boolean xMatch = str1.toUpperCase().equals(word);
                                    boolean yMatch = str2.toUpperCase().equals(word);
                                    if (xMatch && yMatch) return 0;
                                    if (xMatch) return -1;
                                    if (yMatch) return 1;

                                    boolean xStartWith = str1.toUpperCase().startsWith(word);
                                    boolean yStartWith = str2.toUpperCase().startsWith(word);
                                    if (xStartWith && yStartWith) return 0;
                                    if (xStartWith) return -1;
                                    if (yStartWith) return 1;

                                    boolean xContains = str1.toUpperCase().contains(" " + word);
                                    boolean yContains = str2.toUpperCase().contains(" " + word);
                                    if (xContains && yContains) return 0;
                                    if (xContains) return -1;
                                    if (yContains) return 1;
                                    return 0;
                                }).collect(Collectors.toList());
                        if (searchThread.isInterrupted()) {
                            throw new InterruptedException();
                        }
                        if (reviewsResult.size() > 5) reviewsResult = reviewsResult.subList(0, 5);
                        if (familiarResult.size() > 5) familiarResult = familiarResult.subList(0, 5);
                        hasResults.set(true);
                    } catch (InterruptedException ex) {
                        // terminate thread
                        ex.printStackTrace();
                    }
                });
                searchThread.start();
            }

        });
        this.hasResultsProperty().addListener((observable, oldValue, hasResults) -> {
            if (hasResults) {
                Platform.runLater(() -> {
                    if (searchPopup != null && searchPopup.isShowing()) {
                        searchPopup.hide();
                    }
                    Label reviewsTitle = new Label("学习单词");
                    reviewsTitle.setStyle("-fx-background-color:#1F1F1F;");
                    reviewsTitle.setPrefWidth(300);
                    reviewsTitle.setAlignment(Pos.CENTER);
                    reviewsTitle.setFont(Font.font(16));
                    reviewsTitle.setTextFill(Paint.valueOf("#FFFFFF"));
                    VBox reviewsVBox = new VBox(reviewsTitle);
                    reviewsVBox.setMinHeight(135);
                    for (String str : reviewsResult) {
                        JFXButton word = new JFXButton(str);
                        word.setTextFill(Paint.valueOf("#FFFFFF"));
                        word.setFont(Font.font(16));
                        word.setPrefWidth(300);
                        word.setPrefHeight(32);
                        word.setAlignment(Pos.CENTER_LEFT);
                        word.getStyleClass().add("search");
                        word.setOnMouseClicked(new EventHandler<MouseEvent>() {
                            @Override
                            public void handle(MouseEvent event) {
                                rootPane.toBack();
                                rootPane.toFront();
                                menuButton.setVisible(false);
                                drawer.setVisible(false);
                                menu.setVisible(false);
                                isSearch = true;
                                dataLayout = Data.reviews;
                                searchPopup.hide();
                                setCurrentLayoutVisiable(false);
                                detailPane(str);
                            }
                        });
                        reviewsVBox.getChildren().add(word);
                    }

                    Label familiarTitle = new Label("熟悉单词");
                    familiarTitle.setStyle("-fx-background-color:#252525;");
                    familiarTitle.setPrefWidth(300);
                    familiarTitle.setAlignment(Pos.CENTER);
                    familiarTitle.setFont(Font.font(16));

                    familiarTitle.setTextFill(Paint.valueOf("#FFFFFF"));

                    VBox familiarVBox = new VBox(familiarTitle);
                    familiarVBox.setMinHeight(135);

                    for (String str : familiarResult) {
//                        Label word = new Label(str);
                        JFXButton word = new JFXButton(str);
                        word.setFont(Font.font(16));
                        word.setPrefWidth(300);
                        word.setAlignment(Pos.CENTER_LEFT);
                        word.getStyleClass().add("search");
                        word.setTextFill(Paint.valueOf("#FFFFFF"));
                        word.setPrefHeight(32);
                        word.setOnMouseClicked(new EventHandler<MouseEvent>() {
                            @Override
                            public void handle(MouseEvent event) {
                                rootPane.toBack();
                                menuButton.setVisible(false);
                                drawer.setVisible(false);
                                menu.setVisible(false);
                                dataLayout = Data.familiar;
                                searchPopup.hide();
                                setCurrentLayoutVisiable(false);
                                detailPane(str);
                            }
                        });
                        familiarVBox.getChildren().add(word);
                    }

                    VBox vbox = new VBox();
                    vbox.getChildren().addAll(reviewsVBox, familiarVBox);
                    vbox.setPrefWidth(300);
                    vbox.setStyle("-fx-background-color:#1F1F1F;");
                    searchPopup = new JFXPopup(vbox);
                    searchPopup.show(searchBox, JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 0, 35);
                });

            }
        });
    }

    public void setCurrentLayoutVisiable(boolean value) {
        if (currentLayout.equals(Layout.reviewsLayout)) {
            reviewsPagination.setVisible(value);
        } else if (currentLayout.equals(Layout.familiarLayout)) {
            familiarPagination.setVisible(value);
        } else {
            rootPane.getChildren().get(1).setVisible(value);
        }
    }




    @FXML
    void menuButton(ActionEvent event) {
        if (drawer.isShown() == false) {
            drawer.open();
        } else {
            drawer.close();
        }
    }

    @FXML
    private void openFile(ActionEvent event) {
        long start = System.currentTimeMillis();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("选择视频和字幕文件", "*.srt", "*.mp4"));
        List<File> files = fileChooser.showOpenMultipleDialog(null);
        if (files != null && files.size() == 2) {
            verifySubtitle(files);
        }
    }

    @FXML
    void dragDropped(DragEvent event) {
        Dragboard board = event.getDragboard();
        List<File> files = board.getFiles();
        verifySubtitle(files);
    }

    private void verifySubtitle(List<File> files) {
        final Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setMaximized(true);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(TraceController.this.getClass().getResource("/trace/view/VerifySubtitle.fxml"));
        Pane vrifyPane = null;
        try {
            vrifyPane = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        vrifyPane.prefWidthProperty().bind(rootPane.prefWidthProperty());
        vrifyPane.prefHeightProperty().bind(rootPane.prefHeightProperty());
        VerifySubtitleController selectWordController = loader.getController();
        selectWordController.init(files, this);
        stackPane.getChildren().add(vrifyPane);

    }

    @FXML
    void dragOver(DragEvent event) {
        Dragboard board = event.getDragboard();
        if (board.hasFiles()) {
            List<File> files = board.getFiles();
            String path1 = files.get(0).toPath().toString();
            String path2 = files.get(1).toPath().toString();
            if (path1.endsWith(".mp4") && path2.endsWith(".srt") || (path2.endsWith(".mp4") && path1.endsWith(".srt"))) {
                event.acceptTransferModes(TransferMode.ANY);
            }

        }
    }


    public void precessFiles() {

        FXMLLoader loader1 = new FXMLLoader();
        loader1.setLocation(TraceController.this.getClass().getResource("/trace/view/Spinner.fxml"));
        AnchorPane spinnerPane = null;
        try {
            spinnerPane = loader1.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stackPane.getChildren().add(spinnerPane);

        Runnable r = new Runnable() {
            @Override
            public void run() {

                Map<String, HashMap<Integer, Caption>> linkedHashMap = new LinkedHashMap<>();
                //分词 正则表达式
                final String regex = "[a-zA-Z]*-*'*[a-zA-Z]*-*[a-zA-Z]";
                final Pattern pattern = Pattern.compile(regex);
                Matcher matcher;

                final String charset = FileUtils.charsetDetector(subtitlePath);
                try (InputStream is = new FileInputStream(subtitlePath)) {
                    FormatSRT fsrt = new FormatSRT();
                    TimedTextObject tto = fsrt.parseFile(subtitlePath, is, Charset.forName(charset));
                    //用于 DetailPane 查找单词
                    Set<Integer> keySet = tto.captions.keySet();
                    Iterator<Integer> iterator = keySet.iterator();

                    while (iterator.hasNext()) {
                        int i = iterator.next();//字幕的编号。

                        Caption current = tto.captions.get(i);
                        current.content = current.content.replaceFirst(
                                "((^\\{\\\\an\\d\\})((┌|┐|└|┘)?)(\\{.*\\})?((m|M)? .*)?)|^m .*", "");
                        current.start.setMseconds(current.start.getMseconds() + sync);
                        current.end.setMseconds(current.end.getMseconds() + sync);
                        matcher = pattern.matcher(current.content);
                        while (matcher.find()) {
                            String string = matcher.group();
                            String toLowerCase = string.toLowerCase();
                            if (dictionary.contains(toLowerCase) == false) //判断词典里是否有这个单词
                                continue;

                            //处理字幕，把和单词匹配的字幕添加到容器，不区分大小写
                            //private HashMap<String, HashMap<Integer, Caption>> hashMap = new HashMap<>();
                            Iterator<String> itr = linkedHashMap.keySet().iterator();
                            boolean hasCaption = false;
                            while (itr.hasNext()) {
                                String s = itr.next();
                                if (s.equalsIgnoreCase(string)) {
                                    //如果有就获取后添加
                                    HashMap<Integer, Caption> oldMap = linkedHashMap.get(s);//一个数字编号对应一条字幕
                                    if (oldMap.size() < 10) {
                                        oldMap.put(i, current);
                                        linkedHashMap.put(s, oldMap);
                                    }
                                    hasCaption = true;
                                    break;
                                }
                            }
                            if (hasCaption == false) {
                                HashMap<Integer, Caption> newMap = new HashMap<>();
                                newMap.put(i, current);
                                linkedHashMap.put(string, newMap);
                            }

                        }
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                loadSelectPane(linkedHashMap);
            }
        };
        new Thread(r).start();
    }

    private void loadSelectPane(Map<String, HashMap<Integer, Caption>> linkedHashMap) {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(TraceController.this.getClass().getResource("/trace/view/selectWord.fxml"));
        Platform.runLater(() -> {
            AnchorPane selectWordPane = null;
            try {
                selectWordPane = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            selectWordPane.prefWidthProperty().bind(rootPane.widthProperty());
            selectWordPane.prefHeightProperty().bind(rootPane.heightProperty());
            SelectWordController selectWordController = loader.getController();
            selectWordController.init(TraceController.this, linkedHashMap);

            stackPane.getChildren().add(selectWordPane);
            stackPane.getChildren().remove(1);

        });
    }
    private Map<String, HashMap<Integer, Caption>> processSubtitle() {
        InputStream is = null;
        final long start = System.currentTimeMillis();
        Map<String, HashMap<Integer, Caption>> linkedHashMap = new LinkedHashMap<>();
        //分词 正则表达式
        final String regex = "[a-zA-Z]*-*'*[a-zA-Z]*-*[a-zA-Z]";
        final Pattern pattern = Pattern.compile(regex);
        Matcher matcher;

        try {
            is = new FileInputStream(subtitlePath);
            FormatSRT fsrt = new FormatSRT();
            TimedTextObject tto = fsrt.parseFile(subtitlePath, is, Charset.forName("UTF-8"));
            //用于 DetailPane 查找单词
            TreeMap<Integer, Caption> captions = tto.captions;
            Set<Integer> keySet = tto.captions.keySet();
            Iterator<Integer> iterator = keySet.iterator();

            while (iterator.hasNext()) {
                int i = iterator.next();//字幕的编号。

                Caption current = tto.captions.get(i);
                current.start.setMseconds(current.start.getMseconds() + sync);
                current.end.setMseconds(current.end.getMseconds() + sync);
                matcher = pattern.matcher(current.content);
                while (matcher.find()) {
                    String string = matcher.group();
                    String toLowerCase = string.toLowerCase();
                    if (dictionary.contains(toLowerCase) == false) //判断词典里是否有这个单词
                        continue;


                    //处理字幕，把和单词匹配的字幕添加到容器，不区分大小写
                    //private HashMap<String, HashMap<Integer, Caption>> hashMap = new HashMap<>();
                    Iterator<String> itr = linkedHashMap.keySet().iterator();
                    boolean hasCaption = false;
                    while (itr.hasNext()) {
                        String s = itr.next();
                        if (s.equalsIgnoreCase(string)) {
                            //如果有就获取后添加
                            HashMap<Integer, Caption> oldMap = linkedHashMap.get(s);//一个数字编号对应一条字幕
                            if (oldMap.size() < 10) {
                                oldMap.put(i, current);
                                linkedHashMap.put(s, oldMap);
                            }
                            hasCaption = true;
                            break;
                        }
                    }
                    if (hasCaption == false) {
                        HashMap<Integer, Caption> newMap = new HashMap<>();
                        newMap.put(i, current);
                        linkedHashMap.put(string, newMap);
                    }

                }
            }


        } catch (FileNotFoundException ex) {
            Logger.getLogger(TraceController.class
                    .getName()).log(Level.SEVERE, null, ex);

        } catch (IOException e) {
            Logger.getLogger(TraceController.class
                    .getName()).log(Level.SEVERE, null, e);
        } finally {
            try {
                is.close();

            } catch (IOException ex) {
                Logger.getLogger(TraceController.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }

        final long end = System.currentTimeMillis();
        return linkedHashMap;
    }

    /**
     * 将字幕加入已有的单词
     */

    private void addToExist(ArrayList<String> lists, HashMap<String, HashMap<Integer, Caption>> hashMap) {

        ArrayList<String> temp = new ArrayList<>();
        for (String word : lists) {
            if (familiar.containsKey(word)) {
                TreeMap<String, TreeMap<Integer, Caption>> medias = familiar.get(word);
                medias.put(mediaPath, new TreeMap<>(hashMap.get(word)));
                familiar.put(word, medias);
                temp.add(word);
            }

            if (reviews.containsKey(word)) {
                TreeMap<String, TreeMap<Integer, Caption>> medias = reviews.get(word);
                medias.put(mediaPath, new TreeMap<>(hashMap.get(word)));
                reviews.put(word, medias);
                temp.add(word);
            }
        }


    }

    private Callback<Integer, Node> familiarCallback = new Callback<Integer, Node>() {
        public Node call(Integer pageIndex) {
            FlowPane flowPane = new FlowPane();
            flowPane.getStyleClass().add("flowPane");
            flowPane.setHgap(15);
            flowPane.setVgap(15);
            final double padding = familiarPagination.widthProperty().get();
            flowPane.setPadding(new Insets(24, padding, 0, padding));

            int t = pageIndex * 200;
            final int size = familiarShuffleList.size() - 1;
            if (!familiarShuffleList.isEmpty()) {
                familiarPagination.setVisible(true);
                for (int i = t; i < t + 200; i++) {
                    if (i > size) break;
                    final Label word = decoratorLabel(familiarShuffleList.get(i));
                    flowPane.getChildren().add(word);
                }
            }


            ScrollPane scrollPane = new ScrollPane(flowPane);
            scrollPane.widthProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    Platform.runLater(() -> {
                        double padding = (newValue.doubleValue() - 875) / 2;
                        flowPane.setPadding(new Insets(24, padding, 0, padding));
                    });
                }
            });
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setFocusTraversable(false);
            scrollPane.getStyleClass().add("scrollPane");
            familiarPagination.setPageCount(familiarShuffleList.size() / 200 + 1);
            return scrollPane;
        }
    };
    private Callback<Integer, Node> reviewsCallback = new Callback<Integer, Node>() {
        public Node call(Integer pageIndex) {
            FlowPane flowPane = new FlowPane();
            flowPane.getStyleClass().add("flowPane");
            flowPane.setHgap(15);
            flowPane.setVgap(15);
            final double padding = reviewsPagination.widthProperty().get();
            flowPane.setPadding(new Insets(24, padding, 0, padding));

            int t = pageIndex * 200;
            final int size = reviewsShuffleList.size() - 1;
            if (!reviewsShuffleList.isEmpty()) {
                reviewsPagination.setVisible(true);
                for (int i = t; i < t + 200; i++) {
                    if (i > size) break;

                    final String str = reviewsShuffleList.get(i);
                    final Label word = decoratorLabel(str);
                    if (markList != null && markList.contains(str)) {
                        word.setStyle("-fx-background-color:#404040");
                    }
                    flowPane.getChildren().add(word);
                }
            }


            ScrollPane scrollPane = new ScrollPane(flowPane);
            scrollPane.widthProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    Platform.runLater(() -> {
                        double padding = (newValue.doubleValue() - 875) / 2;
                        flowPane.setPadding(new Insets(24, padding, 0, padding));
                    });
                }
            });
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setFocusTraversable(false);
            scrollPane.getStyleClass().add("scrollPane");
            reviewsPagination.setPageCount(reviewsShuffleList.size() / 200 + 1);
            return scrollPane;
        }

    };

    /**
     *
     * 显示掌握单词
     */
    public void BASICView() {
        currentLayout = Layout.familiarLayout;
        reviewsPagination.setVisible(false);
        familiarPagination.setVisible(true);
        if (!reviewsChange) {
            familiarPagination.setPageFactory(familiarCallback);
        } else {
            updateFamiliarList();
            updateFamiliarPageFactory();
            reviewsChange = false;
        }

    }



    /**
     * 显示学习单词
     */
    public void NOTBASICView() {
        currentLayout = Layout.reviewsLayout;
        familiarPagination.setVisible(false);
        reviewsPagination.setVisible(true);
        if (!reviewsChange) {
            reviewsPagination.setPageFactory(reviewsCallback);
        } else {
            updateReviewsList();
            updateReviewsPageFactory();
            reviewsChange = false;
        }

    }




    public void VideoListView() {

        if (currentLayout.equals(Layout.reviewsLayout)) {
            reviewsPagination.setVisible(false);
        } else {
            familiarPagination.setVisible(false);
        }
        currentLayout = Layout.videoLayout;
        ObservableList<String> data = FXCollections.observableArrayList();
        Set<String> temp = new LinkedHashSet<>();

        Collection<TreeMap<String, TreeMap<Integer, Caption>>> reviewsValues = reviews.values();
        final Iterator<TreeMap<String, TreeMap<Integer, Caption>>> itr = reviewsValues.iterator();
        while (itr.hasNext()) {
            final TreeMap<String, TreeMap<Integer, Caption>> next = itr.next();
            final Iterator<String> iterator = next.keySet().iterator();
            while (iterator.hasNext()) {
                final String[] strings = iterator.next().split("\\|");
                final String mediaPath = strings[0];
                File file = new File(mediaPath);
                final String name = file.getName();
                temp.add(name);
            }
        }

        Collection<TreeMap<String, TreeMap<Integer, Caption>>> familiarValues = familiar.values();
        final Iterator<TreeMap<String, TreeMap<Integer, Caption>>> itr1 = familiarValues.iterator();
        while (itr1.hasNext()) {
            final TreeMap<String, TreeMap<Integer, Caption>> next = itr1.next();
            final Iterator<String> iterator = next.keySet().iterator();
            while (iterator.hasNext()) {
                final String[] strings = iterator.next().split("\\|");
                final String mediaPath = strings[0];
                File file = new File(mediaPath);
                final String name = file.getName();
                temp.add(name);
            }
        }


        data.addAll(temp);
        Collections.sort(data);
        ListView<String> listView = new ListView<String>(data);
        listView.setEditable(true);
        listView.setItems(data);
        listView.setCellFactory((ListView<String> l) -> new ColorRectCell());
        listView.setPadding(new Insets(200, 0, 0, 0));
        AnchorPane anchorPane = new AnchorPane(listView);
        anchorPane.setStyle("-fx-background-color:#1F1F1F");
        AnchorPane.setTopAnchor(listView, 100.0);
        AnchorPane.setRightAnchor(listView, 0.0);
        AnchorPane.setBottomAnchor(listView, 0.0);
        AnchorPane.setLeftAnchor(listView, 0.0);

        AnchorPane.setTopAnchor(anchorPane, 0.0);
        AnchorPane.setRightAnchor(anchorPane, 0.0);
        AnchorPane.setBottomAnchor(anchorPane, 0.0);
        AnchorPane.setLeftAnchor(anchorPane, 0.0);
        rootPane.getChildren().add(1, anchorPane);
    }


    class ColorRectCell extends ListCell<String> {
        @Override
        public void updateItem(String item, boolean empty) {

            super.updateItem(item, empty);
            Label name = new Label(item);
            name.setFont(Font.font(14));
            name.setPrefSize(720, 31);
            name.setTextFill(Paint.valueOf("9E9E9E"));
            name.setAlignment(Pos.CENTER_LEFT);
            HBox.setMargin(name, new Insets(5, 10, 5, 140));


            JFXButton remove = new JFXButton("删除");
            remove.setFont(Font.font(14));
            remove.setPrefSize(50, 31);
            remove.setTextFill(Paint.valueOf("9E9E9E"));
            remove.setStyle("-fx-border-color:#404040;");
            HBox.setMargin(remove, new Insets(5, 20, 5, 0));


//            JFXButton sync = new JFXButton("调整字幕时间轴");
//            sync.setFont(Font.font(14));
//            sync.setPrefSize(120, 31);
//            sync.setTextFill(Paint.valueOf("#FFFFFF"));
//            sync.setStyle("-fx-border-color:#404040;");
//            sync.setVisible(false);

            HBox hbox = new HBox();
            hbox.getChildren().addAll(name, remove);
            hbox.getStyleClass().add("videoList");

            remove.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    getListView().getItems().remove(item);
                    removeWord(item, reviews);
                    removeWord(item, familiar);
                    updateFamiliarList();
                    updateReviewsList();
                    updateFamiliarPageFactory();
                    updateReviewsPageFactory();

                    reviewsPagination.setVisible(false);
                    familiarPagination.setVisible(false);
                    familiarSize.set(String.valueOf(familiar.size()));
                    reviewSize.set(String.valueOf(reviewsShuffleList.size()));

                    Platform.runLater(() -> {
                        FileUtils.write("resources/word/basic.json", familiar);
                        FileUtils.write("resources/word/review.json", reviews);
                        FileUtils.write("resources/progress.json", progressMap);
                    });

                }
            });

            if (item != null) {
                setGraphic(hbox);
            } else {
                setGraphic(null);
            }
        }

        private void removeWord(String item, HashMap<String, TreeMap<String, TreeMap<Integer, Caption>>> hashMap) {
            final Set<Map.Entry<String, TreeMap<String, TreeMap<Integer, Caption>>>> entries = hashMap.entrySet();
            final Iterator<Map.Entry<String, TreeMap<String, TreeMap<Integer, Caption>>>> entryIterator = entries.iterator();
            ArrayList<String> removedKey = new ArrayList<>();
            Map<String, String> removedMap = new TreeMap<>();
            while (entryIterator.hasNext()) {
                Map.Entry<String, TreeMap<String, TreeMap<Integer, Caption>>> next = entryIterator.next();// 第一个 Key 是单词，第二个 Key 是 媒体， 第三个 key 是字幕开始时间
                TreeMap<String, TreeMap<Integer, Caption>> mediaTreeMap = next.getValue(); // 第一个 key 是媒体， 第二个 key 是字幕开始时间
                final Iterator<String> mediaItr = mediaTreeMap.keySet().iterator();
                while (mediaItr.hasNext()) {
                    final String name = mediaItr.next();

                    final String[] strings = new StringBuilder(name).toString().replaceFirst("file:///", "file:/").split("\\|");
                    File file = new File(strings[0]);
                    final String fileName = file.getName();
                    if (fileName.equals(item)) {
                        final TreeMap<String, TreeMap<Integer, Caption>> treeMap = hashMap.get(next.getKey());
                        final Iterator<String> itr = treeMap.keySet().iterator();
                        removedMap.put(next.getKey(), name);
                    }
                }

            }

            final Iterator<Map.Entry<String, String>> iterator = removedMap.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<String, String> next = iterator.next();
                final TreeMap<String, TreeMap<Integer, Caption>> treeMap = hashMap.get(next.getKey());
                treeMap.remove(next.getValue());

                if (treeMap.isEmpty()) {
                    removedKey.add(next.getKey());
                }
            }

            for (String str : removedKey) {
                hashMap.remove(str);
                progressMap.remove(str);
            }

        }

    }


    /**
     * 装饰一个 Label
     */
    public Label decoratorLabel(String str) {
        Label word = new Label(str);
        word.getStyleClass().add("word");
        word.setFont(Font.font(14));
        word.setPrefSize(110, 67.98);
        word.setTextAlignment(TextAlignment.CENTER);
        word.setCursor(Cursor.HAND);
        word.setFocusTraversable(true);
        word.setAlignment(Pos.CENTER);
        word.setWrapText(true);
        word.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                menuButton.setVisible(false);
                menu.setVisible(false);
                drawer.setVisible(false);
                if (currentLayout == Layout.reviewsLayout) {
                    dataLayout = Data.reviews;
                } else {
                    dataLayout = Data.familiar;
                }
                setCurrentLayoutVisiable(false);
                detailPane(str);
            }
        });

        JFXDepthManager.setDepth(word, 1);
        return word;
    }

    /**
     * 显示详情页
     *
     * @param word
     */
    public void detailPane(String word) {
        try {
            detailWord = word;
            isMouseClicked = true;
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(TraceController.this.getClass().getResource("/trace/view/DetailPane.fxml"));
            BorderPane detailPane = loader.load();
            detailPane.maxWidthProperty().bind(rootPane.widthProperty());
            detailPane.maxHeightProperty().bind(rootPane.heightProperty().multiply(0.9));
            DetailPaneController detailPopupController = loader.getController();
            detailPopupController.init(TraceController.this);
            stackPane.getChildren().add(detailPane);
        } catch (IOException ex) {
            Logger.getLogger(TraceController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }


    //对 ArrayList frequency 进行排序
    private void sort(List<String> list, HashMap<String, HashMap<Integer, Caption>> hashMap) {
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String t, String t1) {
                return hashMap.get(t1).size() - hashMap.get(t).size();
            }
        });
    }


    public HBox getMenu() {
        return menu;
    }

    public StackPane getStackPane() {
        return stackPane;
    }

    public AnchorPane getRootPane() {
        return rootPane;
    }

    public String getDetailWord() {
        return detailWord;
    }

    public JFXButton getMenuButton() {
        return menuButton;
    }

    public JFXDrawer getDrawer() {
        return drawer;
    }

    public CustomPagination getReviewsPagination() {
        return reviewsPagination;
    }

    public CustomPagination getFamiliarPagination() {
        return familiarPagination;
    }

}

