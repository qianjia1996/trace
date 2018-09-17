package trace.controller;

import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
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
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;

public class VerifySubtitleController {


    @FXML
    private MediaView mediaView;

    @FXML
    private VBox rootPane;
    @FXML
    private Label subtitle;

    TraceController mainController;

    private List<File> files;
    private String mediaPath;
    private String subtitlePath;
    private MediaPlayer mediaPlayer;
    private Duration duration;
    private TreeMap<Integer, Caption> captions;



    public void init(List<File> files, TraceController mainController) {

        mediaView.setFitWidth(rootPane.getPrefWidth());
        mediaView.setFitHeight(mediaView.getFitWidth() * 0.6);

        subtitle.setPrefWidth(mediaView.getFitWidth());
        subtitle.setPrefHeight(mediaView.getFitHeight() * 0.067);
        subtitle.setLayoutX(0);
        subtitle.setLayoutY(mediaView.getFitHeight() * 0.65);



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
        mediaView.setMediaPlayer(mediaPlayer);
        duration = media.getDuration();
        mediaPlayer.setOnMarker(new EventHandler<MediaMarkerEvent>() {
            @Override
            public void handle(MediaMarkerEvent event) {
                Platform.runLater(() -> {
                    final String key = event.getMarker().getKey();
                    subtitle.setText(key.substring(11));
                });
            }
        });

        final String charset = FileUtils.charsetDetector(subtitlePath);
        try (InputStream is = new FileInputStream(subtitlePath)) {

            FormatSRT fsrt = new FormatSRT();
            TimedTextObject tto = fsrt.parseFile(subtitlePath, is, Charset.forName(charset));
            captions = tto.captions;

            //播放字幕
            ObservableMap<String, Duration> makers = mediaPlayer.getMedia().getMarkers();
            final Set<Map.Entry<Integer, Caption>> entries = captions.entrySet();
            final Iterator<Map.Entry<Integer, Caption>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                final Map.Entry<Integer, Caption> next = iterator.next();
                final String content = next.getValue().content.replaceFirst(
                        "((^\\{\\\\an\\d\\})((┌|┐|└|┘)?)(\\{.*\\})?((m|M)? .*)?)|^m .*", "");
                makers.put(next.getValue().start.toString() + content, Duration.millis(next.getKey()));
                makers.put(next.getValue().end.toString(), Duration.millis(next.getValue().end.getMseconds()));
            }

        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        mediaPlayer.setAutoPlay(true);
    }


    @FXML
    void YESAction(ActionEvent event) {
        if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) mediaPlayer.pause();
        mainController.mediaPath = mediaPath;
        mainController.subtitlePath = subtitlePath;
        mainController.sync = 0;
        mainController.precessFiles();
        mainController.getStackPane().getChildren().remove(1);
    }

    @FXML
    void NOAction(ActionEvent event) {
        if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) mediaPlayer.pause();
        mainController.mediaPath = mediaPath;
        mainController.subtitlePath = subtitlePath;
        AdjustSubtitle();
        mainController.getStackPane().getChildren().remove(1);


    }

    private void AdjustSubtitle() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("/trace/view/AdjustSubtitle.fxml"));
        Pane adjustPane = null;
        try {
            adjustPane = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        adjustPane.prefWidthProperty().bind(rootPane.prefWidthProperty());
        adjustPane.prefWidthProperty().bind(rootPane.prefWidthProperty());
        AdjustSubtitleController adjustSubtitleController = loader.getController();
        adjustSubtitleController.init(files, mainController);
        mainController.getStackPane().getChildren().add(adjustPane);
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
}
