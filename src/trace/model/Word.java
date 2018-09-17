package trace.model;

import trace.subtitle.Caption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class Word {
    private String word;
    private ArrayList<String> media;
    private HashMap<Integer, Caption> captions;

    private TreeMap<String, TreeMap<String, TreeMap<Integer, Caption>>> c;

    private TreeMap<String, TreeMap<Integer, Caption>> t;


    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }


}
