/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trace.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * FXML Controller class
 *
 * @author tangs
 */
public class SidePaneController {
    @FXML
    private Label familiarLabel;

    @FXML
    private Label reviewsLabel;

    TraceController mainController;

    public void setMainController(TraceController mainController) {
        this.mainController = mainController;
        familiarLabel.textProperty().bind(mainController.familiarSize);
        reviewsLabel.textProperty().bind(mainController.reviewSize);
    }


    @FXML
    private void BASICView(ActionEvent event) {
        if (mainController.getRootPane().getChildren().size() == 6) {
            mainController.getRootPane().getChildren().remove(1);
        }
        mainController.BASICView();
    }

    @FXML
    private void NOTBASICView(ActionEvent event) {
        if (mainController.getRootPane().getChildren().size() == 6) {
            mainController.getRootPane().getChildren().remove(1);
        }
        mainController.NOTBASICView();
    }

    @FXML
    void VideoListView(ActionEvent event) {
        if (mainController.getRootPane().getChildren().size() < 6) {
            mainController.VideoListView();
        }
    }

}
