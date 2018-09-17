package trace.controller;


import com.jfoenix.controls.JFXCheckBox;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Font;
import trace.model.Layout;
import trace.subtitle.Caption;
import trace.utils.CustomPaginationSkin;
import trace.utils.FileUtils;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Stream;


public class SelectWordController  {

    @FXML
    private AnchorPane root;
    @FXML
    private ScrollPane scrollPane;

    @FXML
    private FlowPane flowPane;

    @FXML
    private Label select;

    @FXML
    private Label total;


    private TraceController mainController;

    private String mediaPath;

    private Map<String, HashMap<Integer, Caption>> hashMap;
    private ArrayList<String> review;

    private ArrayList<String> know;
    private int count;
    private ArrayList<String> frequency;
    private Queue<String> lists;
    private String mediaInfo;

    public void init(TraceController mainController, Map<String, HashMap<Integer, Caption>> hashMap) {
        this.mainController = mainController;
        this.hashMap = hashMap;

        this.mediaInfo = mainController.mediaPath + "|" + mainController.subtitlePath + "|" + mainController.sync;
        Set<String> words = hashMap.keySet();
        frequency = new ArrayList<>(words);
        // 对所以的单词进行排序
        sort(frequency, hashMap);
        addToExist(frequency, hashMap);
        lists = new LinkedBlockingDeque<>(frequency);
        select.setText(String.valueOf(0));
        total.setText(String.valueOf(frequency.size()));
        makeFlowPane(lists);
        scrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > 0.8) {
                makeFlowPane(lists);
            }


        });
        scrollPane.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Platform.runLater(() -> {
                    double padding = (newValue.doubleValue() - 1055) / 2;
                    flowPane.setPadding(new Insets(24, padding, 0, padding));
                });
            }
        });


    }

    //对 ArrayList frequency 进行排序
    private void sort(List<String> list, Map<String, HashMap<Integer, Caption>> hashMap) {
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String t, String t1) {
                return hashMap.get(t1).size() - hashMap.get(t).size();
            }
        });
    }

    /**
     * 将字幕加入已有的单词
     */

    private void addToExist(ArrayList<String> lists, Map<String, HashMap<Integer, Caption>> hashMap) {

        ArrayList<String> temp = new ArrayList<>();
        for (String word : lists) {

            final Stream<String> stringStream = mainController.familiar.keySet().stream()
                    .filter(str -> str.equalsIgnoreCase(word));
            final Object[] toArray = stringStream.toArray();

            if (toArray.length > 0) {
                final String oneStr = (String) toArray[0];
                TreeMap<String, TreeMap<Integer, Caption>> medias = mainController.familiar.get(oneStr);
                final Iterator<TreeMap<Integer, Caption>> iterator = medias.values().iterator();
                int count = 0;
                while (iterator.hasNext()) {
                    count = count + iterator.next().size();
                }
                if (count < 10) {
                    final Iterator<Map.Entry<Integer, Caption>> itr = hashMap.get(word).entrySet().iterator();
                    while (itr.hasNext()) {
                        final Map.Entry<Integer, Caption> next = itr.next();
                        TreeMap<Integer, Caption> map = new TreeMap<>();
                        map.put(next.getKey(), next.getValue());
                        medias.put(mediaInfo, map);
                        mainController.familiar.put(oneStr, medias);
                        count++;
                        if (count == 10) break;
                    }
                }
                temp.add(word);
            }

            final Stream<String> reviewStream = mainController.reviews.keySet().stream()
                    .filter(str -> str.equalsIgnoreCase(word));
            final Object[] toStr = reviewStream.toArray();
            if (toStr.length > 0) {
                final String oneStr = (String) toStr[0];
                TreeMap<String, TreeMap<Integer, Caption>> medias = mainController.reviews.get(oneStr);
                final Iterator<TreeMap<Integer, Caption>> iterator = medias.values().iterator();
                int count = 0;
                while (iterator.hasNext()) {
                    count = count + iterator.next().size();
                }

                if (count < 10) {

                    final Iterator<Map.Entry<Integer, Caption>> itr = hashMap.get(word).entrySet().iterator();
                    while (itr.hasNext()) {
                        final Map.Entry<Integer, Caption> next = itr.next();
                        TreeMap<Integer, Caption> map = new TreeMap<>();
                        map.put(next.getKey(), next.getValue());
                        medias.put(mediaInfo, map);
                        mainController.reviews.put(oneStr, medias);
                        count++;
                        if (count == 10) break;
                    }
                }


                temp.add(word);

            }

        }
        lists.removeAll(temp);
    }

    public void makeFlowPane(Queue<String> lists) {
        final int size = lists.size() - 1;
        if (!lists.isEmpty()) {
            for (int i = 0; i < 200; i++) {
                if (i > size) break;
                String str = lists.remove();
                JFXCheckBox checkBox = new JFXCheckBox(str);
                checkBox.getStyleClass().add("select");
                checkBox.setFont(Font.font(16));
                checkBox.setPrefSize(140, 30);
                checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.booleanValue() == true) {
                        count++;
                        select.setText(String.valueOf(count));
                    } else {
                        count--;
                        select.setText(String.valueOf(count));
                    }
                });

                flowPane.getChildren().add(checkBox);

            }
        }


    }



    @FXML
    void affirmAction(ActionEvent event) {
        Platform.runLater(() -> {
            know = new ArrayList<>();
            review = new ArrayList<>();
            ObservableList<Node> children = flowPane.getChildren();
            Iterator<Node> itr = children.iterator();
            while(itr.hasNext()){
                JFXCheckBox checkBox = (JFXCheckBox)itr.next();
                String word = checkBox.getText();
//                long intervalMillis = 12 * 60 * 60 * 1000;
                //添加到我的词库
                if (checkBox.isSelected() == true) {
                    final Stream<String> stringStream = mainController.familiar.keySet().stream()
                            .filter(str -> str.equalsIgnoreCase(word));// 处理大小写的
                    final Object[] toArray = stringStream.toArray();

                    if (toArray.length > 0) {
                        final String oneStr = (String) toArray[0];
                        TreeMap<String, TreeMap<Integer, Caption>> medias = mainController.familiar.get(oneStr);
                        medias.put(mediaInfo, new TreeMap<>(hashMap.get(word)));
                        mainController.familiar.put(oneStr, medias);

                    } else {
                        TreeMap<String, TreeMap<Integer, Caption>> newMedias = new TreeMap<>();
                        newMedias.put(mediaInfo, new TreeMap<>(hashMap.get(word)));
                        mainController.familiar.put(word, newMedias);
                        mainController.progressMap.put(word, "0|" + System.currentTimeMillis());
                    }
                } else {
                    final Stream<String> stringStream = mainController.reviews.keySet().stream()
                            .filter(str -> str.equalsIgnoreCase(word));
                    final Object[] toArray = stringStream.toArray();
                    if (toArray.length > 0) {
                        final String oneStr = (String) toArray[0];
                        TreeMap<String, TreeMap<Integer, Caption>> medias = mainController.reviews.get(oneStr);
                        medias.put(mediaInfo, new TreeMap<>(hashMap.get(word)));
                        mainController.reviews.put(oneStr, medias);
                        mainController.progressMap.put(oneStr, "0|" + System.currentTimeMillis());

                    } else {
                        TreeMap<String, TreeMap<Integer, Caption>> newMedias = new TreeMap<>();
                        newMedias.put(mediaInfo, new TreeMap<>(hashMap.get(word)));
                        mainController.reviews.put(word, newMedias);
                        mainController.progressMap.put(word, "0|" + System.currentTimeMillis());
                    }
                }

                while (!lists.isEmpty()) {
                    final String poll = lists.poll();
                    final Stream<String> stringStream = mainController.reviews.keySet().stream()
                            .filter(str -> str.equalsIgnoreCase(poll));
                    final Object[] toArray = stringStream.toArray();
                    if (toArray.length > 0) {
                        final String oneStr = (String) toArray[0];
                        TreeMap<String, TreeMap<Integer, Caption>> medias = mainController.reviews.get(oneStr);
                        medias.put(mediaInfo, new TreeMap<>(hashMap.get(poll)));
                        mainController.reviews.put(oneStr, medias);
                        mainController.progressMap.put(oneStr, "0|" + System.currentTimeMillis());
                    } else {
                        TreeMap<String, TreeMap<Integer, Caption>> newMedias = new TreeMap<>();
                        newMedias.put(mediaInfo, new TreeMap<>(hashMap.get(poll)));
                        mainController.reviews.put(poll, newMedias);
                        mainController.progressMap.put(poll, "0|" + System.currentTimeMillis());
                    }
                }
            }
            mainController.familiarSize.set(String.valueOf(mainController.familiar.size()));
//            mainController.reviewSize.set(String.valueOf(mainController.reviews.size()));
            mainController.reviewSize.set(String.valueOf(mainController.reviewsShuffleList.size()));


            mainController.setCurrentLayoutVisiable(true);
            mainController.updateShuffList();
            mainController.reviewsChange = true;
            mainController.familiarChange = true;
            if (mainController.currentLayout.equals(Layout.reviewsLayout)) {
                CustomPaginationSkin skin = (CustomPaginationSkin) mainController.getReviewsPagination().getSkin();
                skin.resetIndexes(true);
            } else if (mainController.currentLayout.equals(Layout.familiarLayout)) {
                CustomPaginationSkin skin = (CustomPaginationSkin) mainController.getFamiliarPagination().getSkin();
                skin.resetIndexes(true);
            } else {
                mainController.getRootPane().getChildren().remove(1);
                mainController.VideoListView();
            }

            mainController.getStackPane().getChildren().remove(1);
            FileUtils.write("resources/word/basic.json", mainController.familiar);
            FileUtils.write("resources/word/review.json", mainController.reviews);
            FileUtils.write("resources/progress.json", mainController.progressMap);
        });
    }

    @FXML
    void cancelAction(ActionEvent event) {
        mainController.getStackPane().getChildren().remove(1);
        mainController.NOTBASICView();
    }

}
