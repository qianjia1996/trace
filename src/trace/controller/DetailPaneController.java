package trace.controller;

import com.jfoenix.controls.*;
import com.jfoenix.effects.JFXDepthManager;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaMarkerEvent;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import trace.model.Data;
import trace.subtitle.Caption;
import trace.subtitle.FormatSRT;
import trace.subtitle.Time;
import trace.subtitle.TimedTextObject;
import trace.utils.CustomPaginationSkin;
import trace.utils.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class DetailPaneController {
    private final ImageView pauseImageView = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/Pause_48.png")));
    private final ImageView playImageView = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/Play_48.png")));
    private final ImageView notMuteImage = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/Audio_48.png")));
    private final ImageView muteImage = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/No_Audio_48.png")));
    private final ImageView starFilledImage = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/Star Filled_96px.png")));
    private final ImageView starImage = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/Star_96px.png")));

    private MediaPlayer mediaPlayer;
    private TraceController mainController;
    private Duration duration;
    private EventHandler<KeyEvent> spaceHandler;
    private JFXPopup infinitePopup;
    private JFXPopup settingsPopup;
    private JFXPopup settingsTooltipPopup;
    private JFXPopup subtitleCoverPopup;
    private JFXPopup copyPopup;
    private JFXPopup startPopup;
    private JFXPopup endPopup;
    private JFXPopup starPopup;
    private JFXPopup starFilledPopup;
    private Time start;
    private Time end;
    private SimpleIntegerProperty interval = new SimpleIntegerProperty();
    private int tempInterval = 1;
    private StringProperty startProperty = new SimpleStringProperty("00:00");
    private StringProperty stopProperty = new SimpleStringProperty("00:00");
    private boolean starFilled = false;
    private AnchorPane rectangle;
    private Text prePreviousSubtitle;
    private Text previousSubtitle;
    private Text currentSubtitle;
    private Text nextSubtitle;
    private Text nextNextSubtitle;
    private TreeMap<Integer, Caption> currentCaptions;
    private ArrayList<MediaPlayer> players;

    boolean nextStatus;
    private JFXButton currentSubtitleButton;
    private double maxHBoxWidth;
    private String detailString;
    private String previousString;
    private String nextString;
    private double orgSceneX, orgSceneY;
    private double orgTranslateX, orgTranslateY;
    private double newTranslateX, newTranslateY;
    private boolean coverOpen = false;
    private Font font;
    @FXML
    private Label startTimeLabel;
    @FXML
    private Label currentTime;
    @FXML
    private Label endTimeLabel;
    @FXML
    private Label totalTimeLabel;
    @FXML
    private HBox timeHBox;
    @FXML
    private HBox controllHBox;
    @FXML
    private JFXSlider volumeSlider;
    @FXML
    private BorderPane rootPane;
    @FXML
    private MediaView mediaView;
    @FXML
    private Text detailWord;
    @FXML
    private VBox subtitlesVBox;
    @FXML
    private JFXButton playButton;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox detailWordVBox;
    @FXML
    private JFXButton infiniteButton;

    @FXML
    private VBox mediaControll;
    @FXML
    private JFXButton volume;
    @FXML
    private VBox detailVBox;
    @FXML
    private HBox settingsHBox;
    @FXML
    private ChoiceBox<String> choicebox;
    @FXML
    private JFXButton copy;
    @FXML
    private Pane mediaViewPane;
    @FXML
    private JFXButton subtitleCover;
    @FXML
    private AnchorPane detailAnchorPane;
    private int circledTime;
    @FXML
    private AnchorPane timeLinePane;
    @FXML
    private JFXProgressBar timeProgress;
    @FXML
    private AnchorPane mediaPane;
    @FXML
    private Text title;
    @FXML
    private JFXButton star;
    @FXML
    private JFXButton settings;
    @FXML
    private JFXProgressBar progress;
    @FXML
    private Label progressLabel;
    @FXML
    private Label day;
    @FXML
    private JFXButton previousButton;
    @FXML
    private JFXButton nextButton;

    private ArrayList<String> reviewsKeys;
    private ArrayList<String> familiarKeys;

    private Properties config;


    public void init(TraceController mainController) {
        this.mainController = mainController;
        this.config = mainController.config;
        if (mainController.isSearch) {
            previousButton.setVisible(false);
            nextButton.setVisible(false);
            mainController.isSearch = false;
        }
        initMediaView();
        initScrollPane();
        initDetailWord();
        createSubtitleVBox(mainController.getDetailWord()); //VBox 里面装的 开始时间 和字幕
        initChoiceBox();
        Runnable settings = (this::createSettingsPopup);
        Runnable loop = (this::createLoopPopup);
        Runnable Tooltip = (this::createTooltipPopup);
        Runnable shortcuts = (this::initSceneShortcuts);

        new Thread(settings).start();
        new Thread(loop).start();
        new Thread(Tooltip).start();
        new Thread(shortcuts).start();

    }

    private void createLoopPopup() {
        ImageView rewindImageView = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/Rewind_48.png")));
        rewindImageView.setFitWidth(34);
        rewindImageView.setFitHeight(34);
        ImageView fastForwardImageView = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/Fast_Forward_48.png")));
        fastForwardImageView.setFitWidth(34);
        fastForwardImageView.setFitHeight(34);

        ImageView rewindImageViewB = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/Rewind_48.png")));
        rewindImageViewB.setFitWidth(34);
        rewindImageViewB.setFitHeight(34);
        ImageView fastForwardImageViewB = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/Fast_Forward_48.png")));
        fastForwardImageViewB.setFitWidth(34);
        fastForwardImageViewB.setFitHeight(34);
        ImageView closeImage = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/Delete_48.png")));
        closeImage.setFitWidth(34);
        closeImage.setFitHeight(34);


        Label title = new Label("调整循环时间");
        title.setTextFill(Paint.valueOf("#FFFFFF"));
        title.setFont(Font.font(16));

        JFXButton close = new JFXButton();
        close.setGraphic(closeImage);
        close.getStyleClass().add("close-button");
        close.setRipplerFill(Paint.valueOf("#000000"));
        close.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                infinitePopup.hide();
                Runnable r = () -> {
                    mainController.cyclePopupOpen.set(false);
                    config.setProperty("cyclePopupOpen", String.valueOf(false));
                    saveConfig();
                };
                new Thread(r).start();
            }
        });

        HBox titleHbox = new HBox();
        titleHbox.setAlignment(Pos.CENTER_LEFT);
        titleHbox.getChildren().addAll(title, close);
        titleHbox.setMinWidth(296);
        HBox.setMargin(title, new Insets(0, 50, 0, 100));


        JFXSlider opacitySlider = new JFXSlider();
        opacitySlider.setPrefWidth(164);
        opacitySlider.valueProperty().bindBidirectional(mainController.infiniteVBoxOpacity);
        opacitySlider.setMin(0.0);
        opacitySlider.setMax(1.0);
        opacitySlider.setBlockIncrement(0.1);
        opacitySlider.setValueFactory(slider ->
                Bindings.createStringBinding(
                        () -> Math.ceil(opacitySlider.getValue() * 100) / 100 + "",
                        opacitySlider.valueProperty()
                )
        );
        opacitySlider.valueProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                if (opacitySlider.isValueChanging()) {
                    Platform.runLater(() -> {
                        infinitePopup.setOpacity(opacitySlider.getValue());
                        Runnable r = () -> {
                            config.setProperty("infiniteVBoxOpacity", String.valueOf(opacitySlider.getValue()));
                            saveConfig();
                        };
                        new Thread(r).start();
                    });
                }
            }
        });
        Label opacityLabel = new Label("不透明度");
        opacityLabel.setTextFill(Paint.valueOf("#FFFFFF"));
        opacityLabel.setFont(Font.font(16));
        HBox.setMargin(opacityLabel, new Insets(0, 10, 0, 10));
        HBox opacityHbox = new HBox(opacityLabel, opacitySlider);
        VBox.setMargin(opacityHbox, new Insets(10, 0, 10, 0));
        opacityHbox.setAlignment(Pos.CENTER_LEFT);
        //图标A Button 的弹窗设置，滑块向前移动字幕的位置
        HBox circleAHBox = new HBox();
        VBox.setMargin(circleAHBox, new Insets(10, 0, 10, 0));
        //向前滑动
        JFXButton rewindA_Button = new JFXButton();
        rewindA_Button.setCursor(Cursor.HAND);
        rewindA_Button.setRipplerFill(Paint.valueOf("#000000"));
        rewindA_Button.setGraphic(rewindImageView);
        rewindA_Button.setContentDisplay(ContentDisplay.CENTER);
        rewindA_Button.setPrefSize(56, 42);
        HBox.setMargin(rewindA_Button, new Insets(0, 0, 0, 10));
        rewindA_Button.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Duration subtract = mediaPlayer.getStartTime().subtract(Duration.seconds(tempInterval));
                if (subtract.lessThan(Duration.ZERO)) subtract = Duration.ZERO;
                mediaPlayer.seek(subtract);
                mediaPlayer.setStartTime(subtract);
                mediaPlayer.play();
                Platform.runLater(() -> {
                    startProperty.set(formatTime(mediaPlayer.getStartTime()));
                });
            }
        });
        //向后滑动
        JFXButton fastForwardA_Button = new JFXButton();
        fastForwardA_Button.setCursor(Cursor.HAND);
        fastForwardA_Button.setRipplerFill(Paint.valueOf("#000000"));
        fastForwardA_Button.setGraphic(fastForwardImageView);
        fastForwardA_Button.setContentDisplay(ContentDisplay.CENTER);
        fastForwardA_Button.setPrefSize(56, 42);
        HBox.setMargin(fastForwardA_Button, new Insets(0, 30, 0, 0));

        fastForwardA_Button.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Duration add = mediaPlayer.getStartTime().add(Duration.seconds(tempInterval));
                final Duration duration = mediaPlayer.getMedia().getDuration();
                if (add.greaterThan(duration)) add = duration;

                mediaPlayer.seek(add);
                mediaPlayer.setStartTime(add);
                mediaPlayer.play();
                Platform.runLater(() -> {
                    startProperty.set(formatTime(mediaPlayer.getStartTime()));
                });
            }
        });
        rewindA_Button.getStyleClass().add("speed");
        fastForwardA_Button.getStyleClass().add("speed");
        circleAHBox.getStyleClass().add("circlePopup");
        Label startLabel = new Label("开始");
        startLabel.setTextFill(Paint.valueOf("#FFFFFF"));
        startLabel.setFont(Font.font(16));
        HBox.setMargin(startLabel, new Insets(0, 0, 0, 10));


        Label startTime = new Label();
        startTime.setTextFill(Paint.valueOf("#FFFFFF"));
        startTime.setFont(Font.font(16));
        startTime.setAlignment(Pos.CENTER);
        startTime.textProperty().bind(startProperty);
        startTime.setPrefSize(56, 42);
        HBox.setMargin(startTime, new Insets(0, 10, 0, 10));
        circleAHBox.setAlignment(Pos.CENTER_LEFT);
        circleAHBox.getChildren().addAll(startLabel, rewindA_Button, startTime, fastForwardA_Button);

        //图标B Button 的弹窗设置,滑块向后移动字幕的位置
        HBox circleBHBox = new HBox();
        VBox.setMargin(circleBHBox, new Insets(10, 0, 10, 0));


        //向前滑动
        JFXButton rewindB_Button = new JFXButton();
        rewindB_Button.setCursor(Cursor.HAND);
        rewindB_Button.setRipplerFill(Paint.valueOf("#000000"));
        rewindB_Button.setGraphic(rewindImageViewB);
        rewindB_Button.setContentDisplay(ContentDisplay.CENTER);
        rewindB_Button.setPrefSize(56, 42);
        HBox.setMargin(rewindB_Button, new Insets(0, 0, 0, 10));
        rewindB_Button.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Duration subtract = mediaPlayer.getStopTime().subtract(Duration.seconds(tempInterval));
                if (subtract.lessThan(Duration.ZERO)) subtract = Duration.ZERO;
                mediaPlayer.seek(subtract.subtract(Duration.seconds(2)));
                mediaPlayer.play();
                mediaPlayer.setStopTime(subtract);
                Platform.runLater(() -> {
                    stopProperty.set(formatTime(mediaPlayer.getStopTime()));
                });
            }
        });
        //向后滑动
        JFXButton fastForwardB_Button = new JFXButton();
        fastForwardB_Button.setCursor(Cursor.HAND);
        fastForwardB_Button.setRipplerFill(Paint.valueOf("#000000"));
        fastForwardB_Button.setGraphic(fastForwardImageViewB);
        fastForwardB_Button.setContentDisplay(ContentDisplay.CENTER);
        fastForwardB_Button.setPrefSize(56, 42);
        HBox.setMargin(fastForwardB_Button, new Insets(0, 30, 0, 0));
        fastForwardB_Button.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Duration add = mediaPlayer.getStopTime().add(Duration.seconds(tempInterval));
                if (add.greaterThan(duration)) add = duration;
                mediaPlayer.seek(add.subtract(Duration.seconds(2)));
                mediaPlayer.setStopTime(add);
                mediaPlayer.play();
                Platform.runLater(() -> {
                    stopProperty.set(formatTime(mediaPlayer.getStopTime()));
                });
            }
        });
        rewindB_Button.getStyleClass().add("speed");
        fastForwardB_Button.getStyleClass().add("speed");
        circleBHBox.getStyleClass().add("circlePopup");

        Label endLabel = new Label("结束");
        endLabel.setTextFill(Paint.valueOf("#FFFFFF"));
        endLabel.setFont(Font.font(16));
        HBox.setMargin(endLabel, new Insets(0, 0, 0, 10));


        Label endTime = new Label();
        endTime.setTextFill(Paint.valueOf("#FFFFFF"));
        endTime.setFont(Font.font(16));
        endTime.textProperty().bind(stopProperty);
        endTime.setAlignment(Pos.CENTER);
        endTime.setPrefSize(56, 42);
        HBox.setMargin(endTime, new Insets(0, 10, 0, 10));
        circleBHBox.setAlignment(Pos.CENTER_LEFT);
        circleBHBox.getChildren().addAll(endLabel, rewindB_Button, endTime, fastForwardB_Button);

        Label intervalL = new Label("调整单位");
        intervalL.setFont(Font.font(16));
        intervalL.setTextFill(Paint.valueOf("#FFFFFF"));
        intervalL.setAlignment(Pos.CENTER);
        HBox.setMargin(intervalL, new Insets(0, 42, 20, 10));

        ChoiceBox intervalChoice = new ChoiceBox();
        HBox.setMargin(intervalChoice, new Insets(0, 0, 18, 0));

        int[] values = new int[]{1, 5, 10, 15, 20, 25, 30, 60};
        String[] arrays = new String[8];
        for (int i = 0; i < values.length; i++) {
            arrays[i] = values[i] + " 秒 ";
        }
        ObservableList<String> observableArrayList = FXCollections.observableArrayList(arrays);
        intervalChoice.setItems(observableArrayList);

        intervalChoice.getSelectionModel().select(0);
        intervalChoice.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    tempInterval = values[newValue.intValue()];

            }

        });
        HBox intervalHBox = new HBox();
        VBox.setMargin(intervalHBox, new Insets(10, 0, 0, 0));
        intervalHBox.getChildren().addAll(intervalL, intervalChoice);
        intervalHBox.setAlignment(Pos.CENTER_LEFT);

        Separator separator1 = new Separator();
        separator1.setOrientation(Orientation.HORIZONTAL);
        separator1.getStyleClass().add("-fx-border-color:#313131;");
        Separator separator2 = new Separator();
        separator2.setOrientation(Orientation.HORIZONTAL);
        separator2.getStyleClass().add("-fx-border-color:#313131;");
        Separator separator3 = new Separator();
        separator3.setOrientation(Orientation.HORIZONTAL);
        separator3.getStyleClass().add("-fx-border-color:#313131;");
        Separator separator4 = new Separator();
        separator4.setOrientation(Orientation.HORIZONTAL);
        separator4.getStyleClass().add("-fx-border-color:#313131;");

        VBox infiniteVBox = new VBox();
        infiniteVBox.setAlignment(Pos.CENTER);
        infiniteVBox.getStyleClass().add("circlePopup");
        infiniteVBox.getChildren().addAll(titleHbox, separator1, opacityHbox, separator2, circleAHBox, separator3, circleBHBox, separator4, intervalHBox);
        infiniteVBox.setFocusTraversable(true);

        // Mouse Pressed
        infiniteVBox.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                orgSceneX = event.getSceneX();
                orgSceneY = event.getSceneY();

                orgTranslateX = ((VBox) (event.getSource())).getTranslateX();
                orgTranslateY = ((VBox) (event.getSource())).getTranslateY();
            }
        });
        // Mouse Dragged
        infiniteVBox.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                double offsetX = event.getSceneX() - orgSceneX;
                double offsetY = event.getSceneY() - orgSceneY;
                newTranslateX = orgTranslateX + offsetX;
                newTranslateY = orgTranslateY + offsetY;
                ((VBox) (event.getSource())).setTranslateX(newTranslateX);
                ((VBox) (event.getSource())).setTranslateY(newTranslateY);
                Runnable r = () -> {
                    mainController.newTranslateX.set(newTranslateX);
                    mainController.newTranslateY.setValue(newTranslateY);
                    config.setProperty("newTranslateX", String.valueOf(newTranslateX));
                    config.setProperty("newTranslateY", String.valueOf(newTranslateY));
                    saveConfig();
                };
                new Thread(r).start();
            }
        });


        infinitePopup = new JFXPopup(infiniteVBox);
        infinitePopup.setOpacity(opacitySlider.getValue());
        infinitePopup.setAutoHide(false);
        infinitePopup.setOnHidden(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                infiniteButton.setDisable(false);
                if (mediaPlayer != null) {
                    if (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
                        mediaPlayer.play();
                    }
                }

            }
        });
        if (mainController.cyclePopupOpen.get()) {
            infiniteAction();
        }
    }

    private void createSettingsPopup() {
        VBox settingVBox = new VBox();
        settingVBox.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("设置字幕");
        title.setTextFill(Paint.valueOf("#FFFFFF"));
        title.setFont(Font.font(16));
        JFXButton close = new JFXButton();
        ImageView closeImage = new ImageView(new Image(getClass().getResourceAsStream("/trace/images/Delete_48.png")));
        closeImage.setFitWidth(34);
        closeImage.setFitHeight(34);
        close.setGraphic(closeImage);
        close.getStyleClass().add("close-button");
        close.setRipplerFill(Paint.valueOf("#000000"));
        close.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                settingsPopup.hide();
            }
        });
        HBox titleHBox = new HBox(title, close);
        titleHBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setMargin(title, new Insets(0, 77, 0, 0));

        JFXToggleButton toogle = new JFXToggleButton();
        JFXDepthManager.setDepth(toogle, 1);
        toogle.setText("背景透明");
        toogle.setTextFill(Paint.valueOf("#FFFFFF"));
        toogle.setContentDisplay(ContentDisplay.RIGHT);
        toogle.selectedProperty().bindBidirectional(mainController.opacity);
        toogle.setGraphicTextGap(67);
        toogle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (rectangle != null) {
                if (newValue) {
                    rectangle.setBackground(new Background(new BackgroundFill(Paint.valueOf("transparent"), CornerRadii.EMPTY, Insets.EMPTY)));
                    config.setProperty("opacity", "true");
                    saveConfig();
                } else {
                    rectangle.setBackground(new Background(new BackgroundFill(Paint.valueOf(mainController.subtitleCoverColor.getValue()), CornerRadii.EMPTY, Insets.EMPTY)));
                    config.setProperty("opacity", "false");
                    saveConfig();
                }
            }
        });


        Label colorLabel = new Label("背景颜色");
        colorLabel.setPrefHeight(25);
        colorLabel.setAlignment(Pos.CENTER);
        colorLabel.setTextFill(Paint.valueOf("#FFFFFF"));
        Label fontLabel = new Label("字体大小");
        fontLabel.setTextFill(Paint.valueOf("#FFFFFF"));
        HBox.setMargin(colorLabel, new Insets(0, 57, 0, 0));

        JFXColorPicker colorPicker = new JFXColorPicker(Color.web(mainController.subtitleCoverColor.get()));

        colorPicker.setPrefWidth(70);
        colorPicker.setOnAction((ActionEvent e) -> {
            Color background = colorPicker.getValue();
            String color = "#" + background.toString().substring(2, 8).toUpperCase();
            mainController.subtitleCoverColor.set(color);
            config.setProperty("subtitleBackgroundColor", color);
            saveConfig();
            if (rectangle != null) {
                rectangle.setBackground(new Background(new BackgroundFill(Paint.valueOf(background.toString()), CornerRadii.EMPTY, Insets.EMPTY)));
            }
        });
        HBox colorHbox = new HBox();
        colorHbox.setAlignment(Pos.CENTER_LEFT);
        colorHbox.getChildren().addAll(colorLabel, colorPicker);

        Label subtitleColor = new Label("字幕颜色");
        subtitleColor.setPrefHeight(25);
        subtitleColor.setAlignment(Pos.CENTER);
        subtitleColor.setTextFill(Paint.valueOf("#FFFFFF"));
        HBox.setMargin(subtitleColor, new Insets(0, 57, 0, 0));

        JFXColorPicker subtitleColorPicker = new JFXColorPicker(Color.web(mainController.subtitleColor.get()));
        subtitleColorPicker.setPrefWidth(70);
        subtitleColorPicker.setOnAction((ActionEvent e) -> {
            Color background = subtitleColorPicker.getValue();
            String color = "#" + background.toString().substring(2, 8).toUpperCase();
            mainController.subtitleColor.set(color);
            config.setProperty("subtitleColor", color);
            saveConfig();
            if (rectangle != null) {
                currentSubtitle.setFill(Paint.valueOf(color));
            }
            if (currentSubtitleButton != null) {
                currentSubtitleButton.setTextFill(Paint.valueOf(color));
            }
        });
        HBox subtitleColorHbox = new HBox();
        subtitleColorHbox.setAlignment(Pos.CENTER_LEFT);
        subtitleColorHbox.getChildren().addAll(subtitleColor, subtitleColorPicker);

        Label subtitleNumbers = new Label("字幕条数");
        subtitleNumbers.setPrefHeight(25);
        subtitleNumbers.setAlignment(Pos.CENTER);
        HBox.setMargin(subtitleNumbers, new Insets(0, 57, 0, 0));
        subtitleNumbers.setTextFill(Paint.valueOf("#FFFFFF"));
        ChoiceBox<String> quantity = new ChoiceBox<>(FXCollections.observableArrayList(" 1条 ", " 3条 ", " 5条 "));
        JFXDepthManager.setDepth(quantity, 1);
        if (mainController.subtitleQuantity.get() == 1) {
            quantity.getSelectionModel().select(0);
        } else if (mainController.subtitleQuantity.get() == 3) {
            quantity.getSelectionModel().select(1);
        } else if (mainController.subtitleQuantity.get() == 5) {
            quantity.getSelectionModel().select(2);
        }

        quantity.getStyleClass().add("subtitle-choice-box");
        quantity.setPrefWidth(71);
        quantity.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (newValue.intValue() == 0) {
                    mainController.subtitleQuantity.set(1);

                    if (mediaViewPane.getChildren().size() == 2) {
                        initRectangle();
                    } else {
                        mediaViewPane.getChildren().remove(rectangle);
                        initRectangle();
                    }
                    config.setProperty("subtitleQuantity", String.valueOf(1));
                    saveConfig();
                } else if (newValue.intValue() == 1) {

                    mainController.subtitleQuantity.set(3);
                    if (mediaViewPane.getChildren().size() == 2) {
                        initRectangle();
                    } else {
                        mediaViewPane.getChildren().remove(rectangle);
                        initRectangle();
                    }
                    config.setProperty("subtitleQuantity", String.valueOf(3));
                    saveConfig();
                }
                if (newValue.intValue() == 2) {
                    mainController.subtitleQuantity.set(5);
                    if (mediaViewPane.getChildren().size() == 2) {
                        initRectangle();
                    } else {
                        mediaViewPane.getChildren().remove(rectangle);
                        initRectangle();
                    }
                    config.setProperty("subtitleQuantity", String.valueOf(5));
                    saveConfig();
                }
            }
        });
        HBox numbersHbox = new HBox(subtitleNumbers, quantity);

        int[] values = new int[]{14, 18, 20, 24, 26, 28, 30, 32, 34, 36};
        String[] arrays = new String[10];
        for (int i = 0; i < values.length; i++) {
            arrays[i] = values[i] + "";
        }
        ObservableList<String> observableArrayList = FXCollections.observableArrayList(arrays);
        ChoiceBox<String> fontSizeChoice = new ChoiceBox<>(observableArrayList);
        for (int i = 0; i < values.length; i++) {
            if (values[i] == mainController.font.getSize()) {
                fontSizeChoice.getSelectionModel().select(i);
            }
        }

        fontSizeChoice.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                int fontSize = values[newValue.intValue()];
                mainController.font = Font.font(fontSize);
                config.setProperty("fontSize", String.valueOf(fontSize));
                saveConfig();
                if (currentSubtitle != null) {
                    currentSubtitle.setFont(mainController.font);
                    if (previousSubtitle != null) {
                        previousSubtitle.setFont(mainController.font);
                        nextSubtitle.setFont(mainController.font);
                    }

                    if (prePreviousSubtitle != null) {
                        prePreviousSubtitle.setFont(mainController.font);
                        nextNextSubtitle.setFont(mainController.font);
                    }

                    final double subtitleHeight = currentSubtitle.getLayoutBounds().getHeight();
                    double top = (rectangle.getPrefHeight() - subtitleHeight) / 2.0;
                    AnchorPane.setTopAnchor(currentSubtitle, top);
                    AnchorPane.setLeftAnchor(currentSubtitle, 0.0);
                }
            }
        });
        HBox fontSizeHbox = new HBox();
        fontSizeHbox.getChildren().addAll(fontLabel, fontSizeChoice);

        Label fontFamiliy = new Label("字体");
        fontFamiliy.setPrefHeight(22);
        fontFamiliy.setAlignment(Pos.CENTER);
        fontFamiliy.setTextFill(Paint.valueOf("#FFFFFF"));
        final List<String> families = Font.getFamilies();
        final ObservableList<String> observableFamilies = FXCollections.observableArrayList(families);
        ChoiceBox<String> familiesChoice = new ChoiceBox<String>(observableFamilies);
        final int index = observableFamilies.indexOf(mainController.font.getFamily());

        familiesChoice.getSelectionModel().select(index);
        familiesChoice.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                final String family = observableFamilies.get(newValue.intValue());
                config.setProperty("fontFamily", family);
                saveConfig();
                mainController.font = Font.font(family, mainController.font.getSize());
                if (currentSubtitle != null) {
                    currentSubtitle.setFont(mainController.font);
                    if (previousSubtitle != null) {
                        previousSubtitle.setFont(mainController.font);
                        nextSubtitle.setFont(mainController.font);
                    }
                    if (prePreviousSubtitle != null) {
                        prePreviousSubtitle.setFont(mainController.font);
                        nextNextSubtitle.setFont(mainController.font);
                    }

                }

            }
        });
        HBox fontFamililyHbox = new HBox();
        HBox.setMargin(fontFamiliy, new Insets(0, 10, 0, 0));
        HBox.setMargin(familiesChoice, new Insets(0, 10, 0, 0));
        fontFamililyHbox.getChildren().addAll(fontFamiliy, familiesChoice, fontSizeChoice);

        settingVBox.getChildren().addAll(titleHBox, toogle, colorHbox, subtitleColorHbox, numbersHbox, fontFamililyHbox);
        settingVBox.setStyle("-fx-background-color:#353535");
        VBox.setMargin(colorHbox, new Insets(0, 0, 10, 10));
        VBox.setMargin(subtitleColorHbox, new Insets(0, 0, 10, 10));
        VBox.setMargin(numbersHbox, new Insets(0, 0, 10, 10));
        VBox.setMargin(toogle, new Insets(0, 0, 0, 2));
        VBox.setMargin(fontFamililyHbox, new Insets(0, 10, 30, 10));
        settingVBox.setFocusTraversable(true);
        settingsPopup = new JFXPopup(settingVBox);
        settingsPopup.setAutoHide(false);
        settings.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (!settingsPopup.isShowing()) {
                    settingsPopup.show(settings, JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, -128, -270);
                    settingVBox.requestFocus();
                }
            }
        });
    }

    private void saveConfig() {
        try (PrintStream out = new PrintStream("resources/config.properties", "UTF-8")) {
            config.list(out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    private void createTooltipPopup() {

        Label subtitle = new Label("字幕");
        Label setting = new Label("设置");
        Label start = new Label("循环开始");
        Label end = new Label("调整循环时间");
        Label c = new Label("复制");
        Label starLabel = new Label("学会了");
        Label starLabel2 = new Label("重新学习");


        start.getStyleClass().add("copyPopup-label");
        setting.getStyleClass().add("copyPopup-label");
        end.getStyleClass().add("copyPopup-label");
        subtitle.getStyleClass().add("copyPopup-label");
        c.getStyleClass().add("copyPopup-label");
        starLabel.getStyleClass().add("copyPopup-label");
        starLabel2.getStyleClass().add("copyPopup-label");

        HBox starthbox = new HBox(start);
        HBox settinghbox = new HBox(setting);
        HBox endhbox = new HBox(end);
        HBox subtitlehbox = new HBox(subtitle);
        HBox hbox = new HBox(c);
        HBox starhbox = new HBox(starLabel);
        HBox starFillhbox = new HBox(starLabel2);

        starthbox.getStyleClass().add("copyPopup");
        settinghbox.getStyleClass().add("copyPopup");
        endhbox.getStyleClass().add("copyPopup");
        subtitlehbox.getStyleClass().add("copyPopup");
        hbox.getStyleClass().add("copyPopup");
        starhbox.getStyleClass().add("copyPopup");
        starFillhbox.getStyleClass().add("copyPopup");

        copyPopup = new JFXPopup(hbox);
        copyPopup.setAutoHide(false);

        settingsTooltipPopup = new JFXPopup(settinghbox);
        settingsTooltipPopup.setAutoHide(false);

        subtitleCoverPopup = new JFXPopup(subtitlehbox);
        subtitleCoverPopup.setAutoHide(false);

        startPopup = new JFXPopup(starthbox);
        startPopup.setAutoHide(false);

        endPopup = new JFXPopup(endhbox);
        endPopup.setAutoHide(false);

        starPopup = new JFXPopup(starhbox);
        starPopup.setAutoHide(false);

        starFilledPopup = new JFXPopup(starFillhbox);
        starFilledPopup.setAutoHide(false);


        copy.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                copyPopup.show(copy, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.LEFT, 0, 35);
            }
        });

        copy.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                copyPopup.hide();
            }
        });


        star.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (starFilled) {
                    starFilledPopup.show(star, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.LEFT, -6, 35);
                } else {
                    starPopup.show(star, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.LEFT, -6, 35);
                }
            }
        });

        star.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                if (starFilledPopup.isShowing()) starFilledPopup.hide();
                if (starPopup.isShowing()) starPopup.hide();


            }
        });


        subtitleCover.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                subtitleCoverPopup.show(subtitleCover, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.LEFT, 8, 30);
            }
        });

        subtitleCover.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                subtitleCoverPopup.hide();
            }
        });


        infiniteButton.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                endPopup.show(infiniteButton, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.LEFT, -5, 30);
            }
        });

        infiniteButton.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                endPopup.hide();
            }
        });
        settings.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                settingsTooltipPopup.show(settings, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.LEFT, 8, 30);
            }
        });

        settings.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                settingsTooltipPopup.hide();
            }
        });

    }

    private void initSceneShortcuts() {

        rootPane.setFocusTraversable(true);

            spaceHandler = new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    if (event.getCode() == KeyCode.SPACE) {
                        PlayAction();
                    }
                }
            };
        Platform.runLater(() -> {
            rootPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, spaceHandler);
        });


    }

    private void initMediaView() {
        pauseImageView.setFitWidth(34);
        pauseImageView.setFitHeight(34);
        playImageView.setFitWidth(34);
        playImageView.setFitHeight(34);
        notMuteImage.setFitWidth(34);
        notMuteImage.setFitHeight(34);
        muteImage.setFitWidth(34);
        muteImage.setFitHeight(34);
        starFilledImage.setFitWidth(25);
        starFilledImage.setFitHeight(25);
        starImage.setFitWidth(25);
        starImage.setFitHeight(25);
        mediaView.setFitWidth(rootPane.getMaxWidth() - 590);
        mediaView.fitHeightProperty().bind(mediaView.fitWidthProperty().multiply(0.5615));
        mediaView.fitWidthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Platform.runLater(() -> {
                    mediaControll.setLayoutX(0);
                    mediaControll.setPrefWidth(newValue.doubleValue());
                    if (rectangle != null) {
                        rectangle.setPrefWidth(newValue.doubleValue());
                        rectangle.setLayoutX(0);
                        mediaControll.setLayoutX(0);
                    }
                });

            }
        });
        mediaView.fitHeightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Platform.runLater(() -> {

                    if (rectangle != null) {
                        mediaControll.setLayoutY(newValue.doubleValue() * 0.87 - 64);
                        rectangle.setPrefHeight((newValue.doubleValue() * 0.13));

                        rectangle.setLayoutY(newValue.doubleValue() * 0.87);
                        final double subtitleHeight = currentSubtitle.getLayoutBounds().getHeight();
                        double top = (rectangle.getPrefHeight() - subtitleHeight) / 2.0;
                        AnchorPane.setTopAnchor(currentSubtitle, top);
                        AnchorPane.setLeftAnchor(currentSubtitle, 0.0);
                    } else {
                        mediaControll.setLayoutY(newValue.doubleValue());
                    }
                });

            }
        });

        rootPane.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Platform.runLater(() -> {
                    mediaView.setFitWidth(newValue.doubleValue() - 590);
                });
            }

        });

        mediaPane.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (mediaView.getMediaPlayer() != null) {
                    timeProgress.setVisible(true);
                    double toProgress = mediaView.getMediaPlayer().getCurrentTime().divide(duration.toMillis()).toMillis();
                    timeProgress.setProgress(toProgress);
                    totalTimeLabel.setVisible(true);
                    controllHBox.setVisible(true);
                }

            }
        });

        mediaPane.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                timeProgress.setVisible(false);

                totalTimeLabel.setVisible(false);
                controllHBox.setVisible(false);
            }
        });

    }

    public void initChoiceBox() {
        interval.bind(mainController.interval);
        int[] values = new int[]{1, 5, 10, 15, 20, 25, 30, 60};
        String[] arrays = new String[8];
        for (int i = 0; i < values.length; i++) {
            arrays[i] = values[i] + " 秒 ";
        }
        ObservableList<String> observableArrayList = FXCollections.observableArrayList(arrays);
        choicebox.setItems(observableArrayList);


        for (int i = 0; i < values.length; i++) {
            if (values[i] == interval.intValue()) {
                choicebox.getSelectionModel().select(i);
            }
        }


        choicebox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                mainController.interval.set(values[newValue.intValue()]);
                config.setProperty("interval", String.valueOf(interval.get()));
                saveConfig();
                    if (mediaPlayer != null) {
                        setLoop(interval.intValue());
                        MediaPlayer.Status status = mediaPlayer.getStatus();
                        if (status == MediaPlayer.Status.PAUSED
                                || status == MediaPlayer.Status.READY
                                || status == MediaPlayer.Status.STOPPED
                                || status == MediaPlayer.Status.PLAYING) {
                            mediaPlayer.seek(mediaPlayer.getStartTime());
                            mediaPlayer.play();
                        }
                    }
            }

        });

    }

    private void initScrollPane() {
        JFXScrollPane.smoothScrolling(scrollPane);
        scrollPane.minHeightProperty().bind(rootPane.heightProperty().subtract(300));
        scrollPane.maxHeightProperty().bind(rootPane.heightProperty().subtract(300));

    }

    private void initDetailWord() {
        detailWord.setWrappingWidth(450);
        detailWord.setFill(Paint.valueOf("#808080"));
    }

    public void createSubtitleVBox(String word) {
        subtitlesVBox.getChildren().clear();
        detailWord.setText(word);
        detailString = word;
        markLabel(word);
        TreeMap<String, TreeMap<Integer, Caption>> medias;
        if (mainController.dataLayout.equals(Data.reviews)) {
            medias = mainController.reviews.get(word);
            computeProgress(word);
            progressLabel.setVisible(true);
            day.setVisible(true);
            star.setGraphic(starImage);
        } else {
            medias = mainController.familiar.get(word);
            star.setGraphic(starFilledImage);
            progress.setProgress(1.0);
            progressLabel.setVisible(false);
            day.setVisible(false);
            starFilled = true;
        }

        reviewsKeys = mainController.reviewsShuffleList;
        familiarKeys = mainController.familiarShuffleList;
        computeNextWord(word);
        computePreviousWord(word);
        players = new ArrayList<>();
        if (medias != null) {
            Iterator<Map.Entry<String, TreeMap<Integer, Caption>>> iterator = medias.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, TreeMap<Integer, Caption>> next = iterator.next();
                final String[] strings = next.getKey().replaceFirst("file:///", "file:/").split("\\|");
                final String mediaPath = strings[0];
                Media media = new Media(mediaPath);
                MediaPlayer player = new MediaPlayer(media);
                players.add(player);

                final String subtitlePath = strings[1];
                final int sync = Integer.parseInt(strings[2]);
                new Thread(() -> {
                    setSubtitle(player, subtitlePath, sync);
                }).start();

                Set<Map.Entry<Integer, Caption>> captions = next.getValue().entrySet();
                Iterator<Map.Entry<Integer, Caption>> itr = captions.iterator();
                while (itr.hasNext()) {

                    Map.Entry<Integer, Caption> captionEntry = itr.next();
                    Caption current = captionEntry.getValue();
                    String content = new String(current.content);
                    String start = new String(current.start.toString().substring(0, 8));
                    JFXButton button = new JFXButton();
                    String subtitle = start + "  " + content;
                    button.setText(subtitle);
                    button.setTextFill(Paint.valueOf("#FFFFFF"));
                    button.setFont(Font.font(14));
                    button.setWrapText(true);
                    button.setPrefHeight(50);
                    button.setPrefWidth(470);
                    button.setMaxHeight(50);
                    button.setAlignment(Pos.CENTER_LEFT);
                    button.setCursor(Cursor.HAND);
                    button.setPadding(new Insets(5, 10, 10, 5));
                    button.getStyleClass().add("subtitleButton");
                    VBox.setVgrow(button, Priority.ALWAYS);
                    button.setFocusTraversable(true);
                    button.setOnMouseClicked(new EventHandler<MouseEvent>() {
                        @Override
                        public void handle(MouseEvent event) {
                            setPlayer(player);
                            setCurrentSubtitle(subtitlePath);
                            setLoop(current);
                            setCSS(button);
                            setTitle();
                            clearSubtitle();

                            if (!coverOpen) {
                                if (mainController.subtitleOpen.get()) {
                                    if (mediaViewPane.getChildren().size() == 2) {
                                        initRectangle();
                                    }
                                }
                            }
                            if (mediaPlayer.getStatus().equals(MediaPlayer.Status.UNKNOWN)) {
                                resetCurrentPage();
                                return;
                            }

                            mediaPlayer.play();
                        }
                    });
                    subtitlesVBox.getChildren().add(button);
                }
            }
        }
    }


    private void setCurrentSubtitle(String currentSubtitlePath) {
        final String charset = FileUtils.charsetDetector(currentSubtitlePath);
        try (InputStream is = new FileInputStream(currentSubtitlePath)) {
            FormatSRT fsrt = new FormatSRT();
            TimedTextObject tto = fsrt.parseFile(currentSubtitlePath, is, Charset.forName(charset));
            currentCaptions = tto.captions;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void markLabel(String word) {
        CustomPaginationSkin skin = (CustomPaginationSkin) mainController.getReviewsPagination().getSkin();
        StackPane stackPane = (StackPane) skin.getChildren().get(0);
        StackPane nextStackPane = null;
        ScrollPane scrollPane = null;
        if (stackPane.getChildren().isEmpty()) {
            nextStackPane = (StackPane) skin.getChildren().get(1);
            scrollPane = (ScrollPane) nextStackPane.getChildren().get(0);
        } else {
            scrollPane = (ScrollPane) stackPane.getChildren().get(0);
        }

        FlowPane flowPane = (FlowPane) scrollPane.getContent();
        final Iterator<Node> itr = flowPane.getChildren().iterator();
        int index = 0;
        while (itr.hasNext()) {
            final Label next = (Label) itr.next();
            if (next.getText().equals(word)) {
                mainController.markList.add(next.getText());
                next.setStyle("-fx-background-color:#404040");
                index = flowPane.getChildren().indexOf(next);
                break;
            }
        }


        if (mainController.isMouseClicked && index == (flowPane.getChildren().size() - 1)) {
            nextStatus = false;
            skin.selectNext();
            mainController.getReviewsPagination().setVisible(false);
            mainController.isMouseClicked = false;
        }

        if (!nextStatus) {
            if (index == 0 && !mainController.isMouseClicked) {
                skin.selectPrevious();
                mainController.getReviewsPagination().setVisible(false);
                mainController.isMouseClicked = false;
            }
        }
    }

    private void computeProgress(String word) {
        final String string = mainController.progressMap.get(word);
        final String[] split = string.split("\\|");
        int hour = Integer.parseInt(split[0]);// 重复的间隔，
        final long lastTimeMillis = Long.parseLong(split[1]);
        long intervalMillis;
        final long currentTimeMillis = System.currentTimeMillis();
        final long interval = currentTimeMillis - lastTimeMillis;
        int time = 1; // 学习的次数

        switch (hour) {
            case 0:
                time = 1;
                hour = 12;//12 小时
                day.setText("12小时");
                mainController.progressMap.put(word, String.valueOf(hour + "|" + System.currentTimeMillis()));
                break;
            case 12:
                intervalMillis = 12 * 60 * 60 * 1000;
                if (interval > intervalMillis) {
                    hour = 24;//1天
                    time = 2;
                    day.setText("1天");
                    mainController.progressMap.put(word, String.valueOf(hour + "|" + System.currentTimeMillis()));
                } else {
                    time = 1;
                    day.setText("12小时");
                }
                break;
            case 24:
                intervalMillis = 24 * 60 * 60 * 1000;
                if (interval > intervalMillis) {
                    hour = 48;// 2天
                    time = 3;
                    day.setText("2天");
                    mainController.progressMap.put(word, String.valueOf(hour + "|" + System.currentTimeMillis()));
                } else {
                    time = 2;
                    day.setText("1天");
                }
                break;
            case 48:
                intervalMillis = 48 * 60 * 60 * 1000;
                if (interval > intervalMillis) {
                    hour = 96;// 4天
                    time = 4;
                    day.setText("4天");
                    mainController.progressMap.put(word, String.valueOf(hour + "|" + System.currentTimeMillis()));
                } else {
                    time = 3;
                    day.setText("2天");
                }
                break;
            case 96:
                intervalMillis = 96 * 60 * 60 * 1000;
                if (interval > intervalMillis) {
                    hour = 168;// 7天
                    time = 5;
                    day.setText("7天");
                    mainController.progressMap.put(word, String.valueOf(hour + "|" + System.currentTimeMillis()));
                } else {
                    time = 4;
                    day.setText("4天");
                }
                break;
            case 168:
                intervalMillis = 168 * 60 * 60 * 1000;
                if (interval > intervalMillis) {
                    hour = 360;//15天
                    time = 6;
                    day.setText("15天");
                    mainController.progressMap.put(word, String.valueOf(hour + "|" + System.currentTimeMillis()));
                } else {
                    time = 5;
                    day.setText("7天");
                }
                break;
            case 360:
                intervalMillis = 360 * 60 * 60 * 1000;
                if (interval > intervalMillis) {
                    hour = 720;// 30 天
                    time = 7;
                    day.setText("1个月");
                    mainController.progressMap.put(word, String.valueOf(hour + "|" + System.currentTimeMillis()));
                } else {
                    time = 6;
                    day.setText("15天");
                }
                break;
            case 720:
                intervalMillis = 720 * 60 * 60 * 1000;
                if (interval > intervalMillis) {
                    hour = 2160;// 3个月
                    time = 8;
                    day.setText("3个月");
                    mainController.progressMap.put(word, String.valueOf(hour + "|" + System.currentTimeMillis()));
                } else {
                    time = 7;
                    day.setText("1个月");
                }
                break;
            case 2160:
                intervalMillis = 2160 * 60 * 60 * 1000;
                if (interval > intervalMillis) {
                    hour = 4320;// 6个月
                    time = 9;
                    day.setText("6个月");
                    mainController.progressMap.put(word, String.valueOf(hour + "|" + System.currentTimeMillis()));
                } else {
                    time = 8;
                    day.setText("3个月");
                }
                break;
            case 4320:
                time = 10;
                day.setVisible(false);
        }
        progressLabel.setText(time + "/10");
        progress.setProgress(time / 10.0);
        FileUtils.write("resources/progress.json", mainController.progressMap);

    }

    public void setCSS(JFXButton button) {
        if (currentSubtitleButton != null) {
            currentSubtitleButton.setTextFill(Paint.valueOf("#FFFFFF"));
            currentSubtitleButton.setStyle("-fx-background-color:#353535;");
            currentSubtitleButton.setStyle(":hover{-fx-background-color:#3D3D3D;}");
        }
        button.setTextFill(Paint.valueOf(mainController.subtitleColor.getValue()));
        button.setStyle("-fx-background-color:#3D3D3D;");
        button.setStyle(":focused{-fx-background-color:#3D3D3D;}");
        button.setStyle(":focus{-fx-background-color:#3D3D3D;}");
        currentSubtitleButton = button;
    }

    public void setSubtitle(MediaPlayer player, String subtitlePath, int sync) {
        final String charset = FileUtils.charsetDetector(subtitlePath);
        try (InputStream is = new FileInputStream(subtitlePath)) {
            FormatSRT fsrt = new FormatSRT();
            TimedTextObject tto = fsrt.parseFile(subtitlePath, is, Charset.forName(charset));
            TreeMap<Integer, Caption> captions = tto.captions;
            //播放字幕
            ObservableMap<String, Duration> makers = player.getMedia().getMarkers();
            final Set<Map.Entry<Integer, Caption>> entries = captions.entrySet();
            final Iterator<Map.Entry<Integer, Caption>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                final Map.Entry<Integer, Caption> next = iterator.next();
                if (sync == 0) {
                    makers.put(next.getKey().toString(), Duration.millis(next.getKey()));
                } else {
                    makers.put(next.getKey().toString(), Duration.millis(next.getKey() + sync));
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        player.setOnMarker(new EventHandler<MediaMarkerEvent>() {
            @Override
            public void handle(MediaMarkerEvent event) {
                Platform.runLater(() -> {
                    final String key = event.getMarker().getKey();
                    final int i = Integer.parseInt(key);
                    if (mainController.subtitleOpen.get()) {
                        if (mainController.subtitleQuantity.get() == 1) {
                            if (currentCaptions.get(i) != null) {
                                currentSubtitle.setText(currentCaptions.get(i).content);
                            }

                        } else if (mainController.subtitleQuantity.get() == 3) {

                            if (currentCaptions.lowerEntry(i) != null) {
                                previousSubtitle.setText(currentCaptions.lowerEntry(i).getValue().content);
                            }
                            if (currentCaptions.get(i) != null) {
                                currentSubtitle.setText(currentCaptions.get(i).content);
                            }

                            if (currentCaptions.higherEntry(i) != null) {
                                nextSubtitle.setText(currentCaptions.higherEntry(i).getValue().content);
                            }

                        } else {//mainController.subtitleQuantity.get() == 5


                            if (currentCaptions.lowerEntry(i) != null) {
                                previousSubtitle.setText(currentCaptions.lowerEntry(i).getValue().content);

                                if (currentCaptions.lowerEntry(currentCaptions.lowerKey(i)) != null) {
                                    prePreviousSubtitle.setText(currentCaptions.lowerEntry(currentCaptions.lowerKey(i)).getValue().content);
                                }
                            }

                            if (currentCaptions.get(i) != null) {
                                currentSubtitle.setText(currentCaptions.get(i).content);
                            }


                            if (currentCaptions.higherEntry(i) != null) {
                                nextSubtitle.setText(currentCaptions.higherEntry(i).getValue().content);
                                if (currentCaptions.higherEntry(currentCaptions.higherKey(i)) != null) {
                                    nextNextSubtitle.setText(currentCaptions.higherEntry(currentCaptions.higherKey(i)).getValue().content);
                                }
                            }


                        }
                    }

                });
            }
        });
    }

    public void setPlayer(MediaPlayer player) {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            if (mediaPlayer.equals(player)) return;
            try {
                Thread.currentThread().join(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mediaPlayer = player;
        mediaView.setMediaPlayer(mediaPlayer);
        duration = mediaPlayer.getMedia().getDuration();
        totalTimeLabel.setText(formatTime(duration));

        mediaPlayer.setOnPlaying(new Runnable() {
            @Override
            public void run() {
                playButton.setGraphic(pauseImageView);
            }
        });

        mediaPlayer.setOnPaused(new Runnable() {
            @Override
            public void run() {
                playButton.setGraphic(playImageView);
            }
        });

        mediaPlayer.setOnRepeat(new Runnable() {
            @Override
            public void run() {
                clearSubtitle();
            }
        });

        volumeSlider.valueProperty().bindBidirectional(mainController.volume);
        mediaPlayer.volumeProperty().bind(mainController.volume.divide(100));
        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Platform.runLater(() -> {
                    config.setProperty("volume", String.valueOf(mainController.volume.get()));
                    saveConfig();
                });
            }
        });

        mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {
            @Override
            public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
//                    updateTimeValues();
                if (currentTime != null && duration != null && mediaPlayer != null) {
                    Platform.runLater(() -> {
                        currentTime.setText(formatTime(newValue));
                    });
                }
            }
        });

        mediaControll.setPrefWidth(mediaControll.getParent().getLayoutBounds().getWidth());
        mediaControll.setLayoutX(0);
        if (mainController.subtitleOpen.get()) {
            mediaControll.setLayoutY(mediaView.fitHeightProperty().multiply(0.87).get() - 64);
        } else {
            mediaControll.setLayoutY(mediaView.fitHeightProperty().getValue());
        }


    }

    private void clearSubtitle() {
        if (currentSubtitle != null) {
            currentSubtitle.setText(" ");
        }
        if (previousSubtitle != null) {
            previousSubtitle.setText(" ");
            nextSubtitle.setText(" ");
        }

        if (prePreviousSubtitle != null) {
            prePreviousSubtitle.setText(" ");
            nextNextSubtitle.setText(" ");
        }
    }
    private void setTitle() {
        File file = new File(mediaPlayer.getMedia().getSource());
        title.setText(file.getName());
    }

    private void setLoop(Caption current) {
        if (current != null) {
            start = current.start;
            end = current.end;
            mediaPlayer.setStartTime(Duration.millis(start.getMseconds() - interval.get() * 1000));
            mediaPlayer.setStopTime(Duration.millis(end.getMseconds() + interval.get() * 1000));

            mediaPlayer.setCycleCount(Integer.MAX_VALUE);
            startProperty.set(formatTime(mediaPlayer.getStartTime()));
            stopProperty.set(formatTime(mediaPlayer.getStopTime()));

            startTimeLabel.textProperty().bind(startProperty);
            endTimeLabel.textProperty().bind(stopProperty);
        }


    }

    private void setLoop(int interval) {
        mediaPlayer.setStartTime(Duration.millis(start.getMseconds() - interval * 1000));
        mediaPlayer.setStopTime(Duration.millis(end.getMseconds() + interval * 1000));
        mediaPlayer.setCycleCount(Integer.MAX_VALUE);
        startProperty.set(formatTime(mediaPlayer.getStartTime()));
        stopProperty.set(formatTime(mediaPlayer.getStopTime()));
    }

    protected void updateTimeValues() {
        if (currentTime != null && duration != null && mediaPlayer != null) {
            Duration currentTime = mediaPlayer.getCurrentTime();
            this.currentTime.setText(formatTime(currentTime));
        }
    }

    public String formatTime(Duration elapsed, Duration duration) {
        int intElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        int elapsedMinutes = intElapsed / 60 - elapsedHours * 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60
                - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) Math.floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            int durationMinutes = intDuration / 60 - durationHours * 60;
            int durationSeconds = intDuration - durationHours * 60 * 60
                    - durationMinutes * 60;
            if (durationHours > 0) {
                return String.format("%d:%02d:%02d/%d:%02d:%02d", elapsedHours,
                        elapsedMinutes, elapsedSeconds, durationHours,
                        durationMinutes, durationSeconds);
            } else {
                return String.format("%02d:%02d/%02d:%02d", elapsedMinutes,
                        elapsedSeconds, durationMinutes, durationSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d:%02d:%02d", elapsedHours,
                        elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d", elapsedMinutes,
                        elapsedSeconds);
            }
        }
    }


    public String formatTime(Duration duration) {
        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) Math.floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            int durationMinutes = intDuration / 60 - durationHours * 60;
            int durationSeconds = intDuration - durationHours * 60 * 60
                    - durationMinutes * 60;
            if (durationHours > 0) {
                return String.format("%d:%02d:%02d", durationHours, durationMinutes, durationSeconds);
            } else {
                return String.format("%02d:%02d", durationMinutes, durationSeconds);
            }
        } else {
            return "00:00";
        }
    }


    @FXML
    private void closePopup(MouseEvent event) {
        if (spaceHandler != null) {
            rootPane.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, spaceHandler);
            spaceHandler = null;
        }
        if (infinitePopup.isShowing()) {
            infinitePopup.hide();
        }
        if (settingsPopup.isShowing()) {
            settingsPopup.hide();
        }
        pauseVideo();
        disposePlayers();

        mainController.getMenuButton().setVisible(true);
        mainController.getMenu().setVisible(true);
        mainController.getDrawer().setVisible(true);
        mainController.setCurrentLayoutVisiable(true);

        currentSubtitleButton = null;
        reviewsKeys = null;
        familiarKeys = null;
        subtitlesVBox.getChildren().clear();
        mainController.getStackPane().getChildren().remove(1);
        rootPane = null;
    }

    private void disposePlayers() {
        for (MediaPlayer player : players) {
            player.dispose();
        }
    }

    private void closePopup() {

        if (spaceHandler != null) {
            rootPane.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, spaceHandler);
            spaceHandler = null;
        }
        if (infinitePopup.isShowing()) {
            infinitePopup.hide();
        }
        if (settingsPopup.isShowing()) {
            settingsPopup.hide();
        }
        pauseVideo();
        disposePlayers();
        currentSubtitleButton = null;
        reviewsKeys = null;
        familiarKeys = null;
        subtitlesVBox.getChildren().clear();
        mainController.getStackPane().getChildren().remove(1);
        rootPane = null;

    }

    @FXML
    private void PlayAction() {
        if (mediaPlayer != null) {
            MediaPlayer.Status status = mediaPlayer.getStatus();
            if (status == MediaPlayer.Status.PAUSED
                    || status == MediaPlayer.Status.READY
                    || status == MediaPlayer.Status.STOPPED
                    || status == MediaPlayer.Status.PLAYING) {
                mediaPlayer.play();
            }

            if (status == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
            }
        }

    }

    private void pauseVideo() {
        if (mediaPlayer != null) {
            if ((mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) ||
                    mediaPlayer.getStatus().equals(MediaPlayer.Status.PAUSED))
                mediaPlayer.pause();
        }
        }

    @FXML
    private void MediaViewClicked(MouseEvent event) {
        PlayAction();
    }



    @FXML
    private void infiniteAction(ActionEvent event) {
        infiniteAction();
    }

    private void infiniteAction() {

        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                pauseVideo();
            }
            final Scene scene = mainController.getRootPane().getScene();
            Bounds rootBounds = scene.getRoot().getLayoutBounds();

            infinitePopup.show(scene.getRoot(), JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT,
                    (rootBounds.getWidth() - 755),
                    (rootBounds.getHeight() - 390));
            infiniteButton.setDisable(true);

            if (mainController.newTranslateX.get() != 0.0) {
                infinitePopup.getPopupContent().setTranslateX(mainController.newTranslateX.get());
                infinitePopup.getPopupContent().setTranslateY(mainController.newTranslateY.get());
            }

            Runnable r = () -> {
                mainController.cyclePopupOpen.set(true);
                config.setProperty("cyclePopupOpen", String.valueOf(true));
                saveConfig();
            };
            new Thread(r).start();
        });
    }



    @FXML
    private void previousAction(MouseEvent event) {
        pauseVideo();
        disposePlayers();
        mediaPlayer = null;
        createSubtitleVBox(previousString);
    }

    private void resetCurrentPage() {
        pauseVideo();
        disposePlayers();
        mediaPlayer = null;
        createSubtitleVBox(detailString);
    }
    @FXML
    private void nextAction(MouseEvent event) {
        pauseVideo();
        nextStatus = true;

        disposePlayers();
        mediaPlayer = null;
        createSubtitleVBox(nextString);

    }


    private void computePreviousWord(String current) {
        if (mainController.dataLayout.equals(Data.familiar)) {
            int i = familiarKeys.indexOf(current);
            i--;
            if (i > -1) {
                previousString = familiarKeys.get(i);
            } else {
                i = familiarKeys.size() - 1;
                previousString = familiarKeys.get(i);
            }
        } else {
            int j = reviewsKeys.indexOf(current);
            j--;
            if (j > -1) {
                previousString = reviewsKeys.get(j);
            } else {
                j = reviewsKeys.size() - 1;
                previousString = reviewsKeys.get(j);
            }
        }
    }

    private void computeNextWord(String current) {
        if (mainController.dataLayout.equals(Data.familiar)) {
            int i = familiarKeys.indexOf(current);
            i++;
            if (i < familiarKeys.size()) {
                nextString = familiarKeys.get(i);
            } else {
                i = 0;
                nextString = familiarKeys.get(i);
            }

        } else {
            int j = reviewsKeys.indexOf(current);
            j++;
            if (j < reviewsKeys.size()) {
                nextString = reviewsKeys.get(j);
            } else {
                j = 0;
                nextString = reviewsKeys.get(j);
            }
        }
    }
    @FXML
    private void copyAction(ActionEvent event) {
        Platform.runLater(() -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent cc = new ClipboardContent();
            cc.putString(detailString);
            clipboard.setContent(cc);
        });
    }

    @FXML
    void starAction(ActionEvent event) {
        if (!starFilled) {
            star.setGraphic(starFilledImage);
            starFilled = true;
            final TreeMap<String, TreeMap<Integer, Caption>> medias = mainController.reviews.remove(detailString);
            final boolean remove = reviewsKeys.remove(detailString);
            if (familiarKeys != null) {
                familiarKeys.add(detailString);
            }
            mainController.familiar.put(detailString, medias);
            mainController.familiarSize.set(String.valueOf(mainController.familiar.size()));
            mainController.reviewSize.set(String.valueOf(mainController.reviewsShuffleList.size()));

            progress.setProgress(1.0);
            progressLabel.setVisible(false);
            day.setVisible(false);
            mainController.progressMap.remove(detailString);


            CustomPaginationSkin skin = (CustomPaginationSkin) mainController.getReviewsPagination().getSkin();
            StackPane stackPane = (StackPane) skin.getChildren().get(0);
            StackPane nextStackPane = null;
            ScrollPane scrollPane = null;
            if (stackPane.getChildren().isEmpty()) {
                nextStackPane = (StackPane) skin.getChildren().get(1);
                scrollPane = (ScrollPane) nextStackPane.getChildren().get(0);
            } else {
                scrollPane = (ScrollPane) stackPane.getChildren().get(0);
            }

            FlowPane flowPane = (FlowPane) scrollPane.getContent();
            final Iterator<Node> iterator = flowPane.getChildren().iterator();
            Label current = null;
            while (iterator.hasNext()) {
                final Label next = (Label) iterator.next();
                if (next.getText().equals(detailString)) {
                    current = next;
                    break;
                }
            }
            flowPane.getChildren().remove(current);
            mainController.reviewsChange = true;
            mainController.familiarChange = true;
            Platform.runLater(() -> {
                FileUtils.write("resources/word/basic.json", mainController.familiar);
                FileUtils.write("resources/word/review.json", mainController.reviews);
                FileUtils.write("resources/progress.json", mainController.progressMap);
            });

        } else {
            star.setGraphic(starImage);
            starFilled = false;
            final TreeMap<String, TreeMap<Integer, Caption>> medias = mainController.familiar.remove(detailString);
            mainController.reviews.put(detailString, medias);
            reviewsKeys.add(detailString);

            if (familiarKeys != null) {
                familiarKeys.remove(detailString);
            }
            mainController.familiarSize.set(String.valueOf(mainController.familiar.size()));
            mainController.reviewSize.set(String.valueOf(mainController.reviewsShuffleList.size()));
            progress.setProgress(0.1);
            mainController.progressMap.put(detailString, "0|" + System.currentTimeMillis());
            progressLabel.setText("1/10");
            progressLabel.setVisible(true);
            day.setText("12小时");
            day.setVisible(true);

            CustomPaginationSkin skin = (CustomPaginationSkin) mainController.getFamiliarPagination().getSkin();
            StackPane stackPane = (StackPane) skin.getChildren().get(0);
            StackPane nextStackPane = null;
            ScrollPane scrollPane = null;
            if (stackPane.getChildren().isEmpty()) {
                nextStackPane = (StackPane) skin.getChildren().get(1);
                if (!nextStackPane.getChildren().isEmpty()) {
                    scrollPane = (ScrollPane) nextStackPane.getChildren().get(0);
                }

            } else {
                scrollPane = (ScrollPane) stackPane.getChildren().get(0);
            }

            if (scrollPane != null) {
                FlowPane flowPane = (FlowPane) scrollPane.getContent();
                final Iterator<Node> iterator = flowPane.getChildren().iterator();
                Label current = null;
                while (iterator.hasNext()) {
                    final Label next = (Label) iterator.next();
                    if (next.getText().equals(detailString)) {
                        current = next;
                        break;
                    }
                }
                flowPane.getChildren().remove(current);
            }


            mainController.reviewsChange = true;
            mainController.familiarChange = true;
            Platform.runLater(() -> {
                FileUtils.write("resources/word/review.json", mainController.reviews);
                FileUtils.write("resources/word/basic.json", mainController.familiar);
                FileUtils.write("resources/progress.json", mainController.progressMap);
            });

        }
    }

    @FXML
    private void coverAction() {
        if (mediaViewPane.getChildren().size() == 2) {
            initRectangle();
            mediaControll.setLayoutY(mediaView.fitHeightProperty().multiply(0.87).get() - 64);
            mainController.subtitleOpen.set(true);
            config.setProperty("subtitleOpen", String.valueOf(true));
            saveConfig();
        } else {
            mediaViewPane.getChildren().remove(rectangle);
            mediaControll.setLayoutY(mediaView.fitHeightProperty().getValue());
            mainController.subtitleOpen.set(false);
            config.setProperty("subtitleOpen", String.valueOf(false));
            saveConfig();
        }
    }


    private void initRectangle() {

            //设置字幕遮挡器
        rectangle = new AnchorPane();
        rectangle.setPrefHeight(detailVBox.getLayoutBounds().getHeight() - mediaView.fitHeightProperty().get() +
                mediaView.fitHeightProperty().multiply(0.13).get());
        rectangle.setPrefWidth(mediaView.fitWidthProperty().getValue());
        rectangle.setLayoutX(0);
        rectangle.setLayoutY(mediaView.fitHeightProperty().multiply(0.87).get());
        if (mainController.opacity.get()) {
            rectangle.setBackground(new Background(new BackgroundFill(Paint.valueOf("transparent"), CornerRadii.EMPTY, Insets.EMPTY)));
        } else {
            String backgroundColor = "-fx-background-color:" + mainController.subtitleCoverColor.get();
            rectangle.setStyle(backgroundColor);
        }

        VBox vbox = new VBox();

        vbox.setAlignment(Pos.CENTER);
        AnchorPane.setTopAnchor(vbox, 0.0);
        AnchorPane.setRightAnchor(vbox, 0.0);
        AnchorPane.setBottomAnchor(vbox, 0.0);
        AnchorPane.setLeftAnchor(vbox, 0.0);

        font = mainController.font;


        if (mainController.subtitleQuantity.get() == 1) {
            initCurrentSubtitle();
            vbox.getChildren().add(currentSubtitle);
        }

        if (mainController.subtitleQuantity.get() == 3) {
            initThreeSubtitle();
            vbox.getChildren().addAll(previousSubtitle, currentSubtitle, nextSubtitle);
        }

        if (mainController.subtitleQuantity.get() == 5) {
            initPrePreNextNextSubtitle();
            vbox.getChildren().addAll(prePreviousSubtitle, previousSubtitle, currentSubtitle, nextSubtitle, nextNextSubtitle);
        }

        rectangle.getChildren().addAll(vbox);
        DragResizer.makeResizable(rectangle);


        Platform.runLater(() -> {
            mediaViewPane.getChildren().add(rectangle);
        });
    }

    private void initCurrentSubtitle() {
        currentSubtitle = new Text("");
        currentSubtitle.setFill(Paint.valueOf(mainController.subtitleColor.getValue()));
        currentSubtitle.setFont(font);
        currentSubtitle.setTextAlignment(TextAlignment.CENTER);
        currentSubtitle.wrappingWidthProperty().bind(rectangle.prefWidthProperty().subtract(20));
    }

    private void initPreNextSubtitle() {
        previousSubtitle = new Text("");
        previousSubtitle.setFill(Paint.valueOf("#FFFFFF"));
        previousSubtitle.setFont(font);
        previousSubtitle.setTextAlignment(TextAlignment.CENTER);
        previousSubtitle.wrappingWidthProperty().bind(rectangle.prefWidthProperty().subtract(20));

        nextSubtitle = new Text("");
        nextSubtitle.setFill(Paint.valueOf("#FFFFFF"));
        nextSubtitle.setFont(font);
        nextSubtitle.setTextAlignment(TextAlignment.CENTER);
        nextSubtitle.wrappingWidthProperty().bind(rectangle.prefWidthProperty().subtract(20));
    }

    private void initThreeSubtitle() {
        initCurrentSubtitle();
        initPreNextSubtitle();
    }

    private void initPrePreNextNextSubtitle() {
        initCurrentSubtitle();
        initPreNextSubtitle();
        prePreviousSubtitle = new Text("");
        prePreviousSubtitle.setFill(Paint.valueOf("#FFFFFF"));
        prePreviousSubtitle.setFont(font);
        prePreviousSubtitle.setTextAlignment(TextAlignment.CENTER);
        prePreviousSubtitle.wrappingWidthProperty().bind(rectangle.prefWidthProperty().subtract(20));

        nextNextSubtitle = new Text("");
        nextNextSubtitle.setFill(Paint.valueOf("#FFFFFF"));
        nextNextSubtitle.setFont(font);
        nextNextSubtitle.setTextAlignment(TextAlignment.CENTER);
        nextNextSubtitle.wrappingWidthProperty().bind(rectangle.prefWidthProperty().subtract(20));
    }

    @FXML
    private void muteAction(ActionEvent event) {

        if (mediaPlayer != null) {
                if (mediaPlayer.muteProperty().get() == true) {
                    mediaPlayer.setMute(false);
                    volume.setGraphic(notMuteImage);
                } else {
                    mediaPlayer.setMute(true);
                    volume.setGraphic(muteImage);
                }
            }
    }

}
