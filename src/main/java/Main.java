package main.java;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.util.ToolRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {
    public static void main(String[] args) throws Exception {
        Authorship authorship = new Authorship();
        ToolRunner.run(authorship, args);
        FileSystem fs = FileSystem.get(authorship.getConf());
        FreqMap freqMap = new FreqMap();

        freqMap.fromFile(fs, new Path(Authorship.OUTPUT_PATH + "/part-r-00000"));
        freqMap.toFile(fs, new Path(Authorship.OUTPUT_PATH + "/known-frequencies.txt"));


         SimilarityAnalysis similarityAnalysis = new SimilarityAnalysis(freqMap);

    }

    static LinkedList<String> buildPaths(Authorship authorship) throws IOException {
        FileSystem fs = FileSystem.get(authorship.getConf());
        RemoteIterator<LocatedFileStatus> remoteIterator = fs.listFiles(new Path(Authorship.INPUT_PATH), false);
        LinkedList<String> names = new LinkedList<>();
        while (remoteIterator.hasNext()) {
            names.add(remoteIterator.next().getPath().toString());
        }

        return names;
    }


}
