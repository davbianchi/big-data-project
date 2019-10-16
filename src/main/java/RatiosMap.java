package main.java;

import java.io.BufferedReader;
import java.util.HashMap;

public class RatiosMap {
    private HashMap<String, HashMap<String, Integer>> map = new HashMap<>();

    public void append(String author, HashMap<String, Integer> mappings) {
        map.put(author, mappings);
    }

    public void setValue(String author, String field, int value) {
        map.get(author).put(field, value);
    }
}
