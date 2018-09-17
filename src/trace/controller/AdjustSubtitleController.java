package trace.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSlider;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaMarkerEvent;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import trace.subtitle.Caption;
import trace.subtitle.FormatSRT;
import trace.subtitle.TimedTextObject;
import trace.utils.FileUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;

public class AdjustSubtitleController implements Initializable {


    @FXML
    private VBox rootPane;
    @FXML
    private Pane pane;
    @FXML
    private MediaView mediaView;
    @FXML
    private JFXButton input;
    @FXML
    private JFXButton play;
    @FXML
    private JFXButton affirm;
    @FXML
    private JFXButton cancel;
    @FXML
    private Label subtitle;
    @FXML
    private Label error;

    @FXML
    private JFXButton sync;
    @FXML
    private JFXButton rewind;
    @FXML
    private JFXButton fastForward;
    @FXML
    private JFXButton reset;
    @FXML
    private TextField field;
    @FXML
    private JFXSlider slider;


    TraceController mainController;

    // 广告时间
    private int syncTime = 0;
    private List<File> files;
    private String mediaPath;
    private String subtitlePath;
    private MediaPlayer mediaPlayer;
    private Duration duration;
    private TreeMap<Integer, Caption> captions;
    private Duration adjust;
    private Duration rewindAdjust = new Duration(100);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        field.setText("30000");
        slider.setValue(0);
        setAction();
    }


    private void setAction() {
        rewind.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                syncTime = syncTime + 100;
                field.setText(String.valueOf(syncTime));
                syncMakers(syncTime);
            }
        });

        fastForward.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                syncTime = syncTime - 100;
                field.setText(String.valueOf(syncTime));
                syncMakers(syncTime);
            }
        });


        sync.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                error.setVisible(false);
                try {
                    syncTime = Integer.parseInt(field.getText());
                    mediaPlayer.seek(Duration.millis(syncTime));
                    syncMakers(syncTime);
                } catch (NumberFormatException e) {
                    field.setText("");
                    error.setVisible(true);
                }
            }
        });


        reset.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                mediaPlayer.seek(Duration.ZERO);
                field.setText(String.valueOf(0));
                syncTime = 0;
                syncMakers(syncTime);
            }
        });
        play.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PAUSED)) {
                    mediaPlayer.play();
                    play.setText("暂停");
                } else {
                    mediaPlayer.pause();
                    play.setText("播放");
                }
            }
        });

        affirm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) mediaPlayer.pause();
                mainController.getStackPane().getChildren().remove(1);
                mainController.sync = syncTime;
                mainController.precessFiles();
            }
        });


        cancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) mediaPlayer.pause();
                mainController.getStackPane().getChildren().remove(1);
            }
        });

        slider.valueProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                if (slider.isValueChanging()) {
                    Platform.runLater(() -> {
                        if (duration != null) {
                            mediaPlayer.seek(duration.multiply(slider.getValue() / 100.0));
                        }

                    });

                }
            }
        });

        slider.setValueFactory(slider
                        -> Bindings.createStringBinding(
                () -> (displayCurrentTime(mediaPlayer.getCurrentTime())),
                slider.valueProperty()
                )
        );


    }

    public void init(List<File> files, TraceController mainController) {


        this.mainController = mainController;
        this.files = files;

        final String path1 = files.get(0).toPath().toString(); // 这个 Path 是双反斜线，
        final String path2 = files.get(1).toPath().toString(); // 这个 Path 是双反斜线，
        if (path1.endsWith(".mp4")) {
            mediaPath = Paths.get(path1).toUri().toString();
            subtitlePath = path2;
        } else {
            mediaPath = Paths.get(path2).toUri().toString();
            subtitlePath = path1;
        }


        Media media = new Media(mediaPath);
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> {
            duration = mediaPlayer.getMedia().getDuration();

            mediaView.setMediaPlayer(mediaPlayer);
            mediaView.setFitWidth(900);
            mediaView.setFitHeight(575);

            slider.setPrefWidth(900);
            slider.setLayoutY(520);
            subtitle.setPrefWidth(mediaView.getFitWidth());
            subtitle.setPrefHeight(mediaView.getFitHeight() * 0.067);
            subtitle.setLayoutX(0);
            subtitle.setLayoutY(mediaView.getFitHeight() * 0.65);
        });
        mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {
            @Override
            public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
                Platform.runLater(() -> {
                    if (!slider.isDisabled() && duration.greaterThan(Duration.ZERO) && !slider.isValueChanging()) {
                        double toMillis = newValue.divide(duration.toMillis()).toMillis();
                        double value = toMillis * 100.0;
                        slider.setValue(value);
                    }
                });
            }
        });


        final String charset = FileUtils.charsetDetector(subtitlePath);

        try (InputStream is = new FileInputStream(subtitlePath)) {

            FormatSRT fsrt = new FormatSRT();
            TimedTextObject tto = fsrt.parseFile(subtitlePath, is, Charset.forName(charset));
            captions = tto.captions;
            syncMakers(syncTime);


            mediaPlayer.setOnMarker(new EventHandler<MediaMarkerEvent>() {
                @Override
                public void handle(MediaMarkerEvent event) {
                    Platform.runLater(() -> {
                        final String key = event.getMarker().getKey();
                        subtitle.setText(key.substring(11));
                    });
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        mediaPlayer.setAutoPlay(true);
    }

    private String displayCurrentTime(Duration currentTime) {
        int intElapsed = (int) Math.floor(currentTime.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        if (elapsedHours > 0) {
            intElapsed -= elapsedHours * 60 * 60;
        }
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 - elapsedMinutes * 60;

        if (elapsedHours > 0) {
            return String.format(" %d:%02d:%02d ",
                    elapsedHours, elapsedMinutes, elapsedSeconds);
        } else {
            return String.format(" %02d:%02d ",
                    elapsedMinutes, elapsedSeconds);
        }
    }

    private String formatTime(Duration elapsed, Duration duration) {
        int intElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        if (elapsedHours > 0) {
            intElapsed -= elapsedHours * 60 * 60;
        }
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) Math.floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration - durationHours * 60 * 60 - durationMinutes * 60;

            if (durationHours > 0) {
                return String.format("%d:%02d:%02d / %d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds,
                        durationHours, durationMinutes, durationSeconds);
            } else {
                return String.format("%02d:%02d / %02d:%02d",
                        elapsedMinutes, elapsedSeconds,
                        durationMinutes, durationSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d",
                        elapsedMinutes, elapsedSeconds);
            }
        }
    }

    public void syncMakers(int sync) {

        ObservableMap<String, Duration> makers = mediaPlayer.getMedia().getMarkers();
        final Set<Map.Entry<Integer, Caption>> entries = captions.entrySet();
        final Iterator<Map.Entry<Integer, Caption>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Integer, Caption> next = iterator.next();
            final String content = next.getValue().content.replaceFirst(
                    "((^\\{\\\\an\\d\\})((┌|┐|└|┘)?)(\\{.*\\})?((m|M)? .*)?)|^m .*", "");
            makers.put(next.getValue().start.toString() + content, Duration.millis(next.getKey() + sync));
            makers.put(next.getValue().end.toString(), Duration.millis(next.getValue().end.getMseconds() + sync));
        }

    }
}
