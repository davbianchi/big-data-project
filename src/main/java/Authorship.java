package main.java;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Authorship extends Configured implements Tool {
    private static final List<String> IT_CONJUNCTIONS = new ArrayList<>(Arrays.asList("e", "né", "o", "inoltre", "ma",
            "però", "dunque", "anzi", "che"));

    private static final List<String> EN_CONJUNCTIONS = new ArrayList<>(Arrays.asList("and", "or", "not"));

    private static final List<String> IT_ARTICLES = new ArrayList<>(Arrays.asList("il", "lo", "la", "i",
            "gli", "le", "un", "una", "uno"));

    private static final List<String> EN_ARTICLES = new ArrayList<>(Arrays.asList("the", "a", "an"));

    private static final List<String> IT_PREPOSITIONS = new ArrayList<>(Arrays.asList("di", "a", "da", "in", "con", "su", "per", "tra", "fra", "d'"));

    private static final List<String> EN_PREPOSITIONS = new ArrayList<>(Arrays.asList("of", "to", "from", "in", "with", "on", "for", "between"));

    static final String INPUT_PATH = "/user/root/authorship/input";
    static final String OUTPUT_PATH = "/user/root/authorship/output";
    static final String UNKNOWNS_INPUT_PATH = "/user/root/authorship/input/unknowns";


    @Override
    public int run(String[] args) throws Exception {
        Job job = Job.getInstance(this.getConf(), "authorship");
        job.setJarByClass(this.getClass());
        TextInputFormat.setInputPaths(job, new Path(INPUT_PATH));
        TextInputFormat.setInputPaths(job, new Path(UNKNOWNS_INPUT_PATH));
        TextOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH));

        // job setup
        for (String s : Main.buildPaths(this))
            FileInputFormat.addInputPath(job, new Path(s));

        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(String.class);
        job.setOutputValueClass(Integer.class);

        return job.waitForCompletion(true) ? 0 : 1;
    }


    public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
        private static final Pattern WORD_BOUNDARY = Pattern.compile("\\s*\\b\\s *");
        //private static final Pattern APHOSTROPHE_BOUNDARY = Pattern.compile("\\bl'");
        private static final Pattern END_PERIOD = Pattern.compile("[a-z][.!?]");
        private static final Pattern EN_LANG = Pattern.compile("(\\bthe\\b|\\bof\\b|\\band\\b)");

        @Override
        public void map(LongWritable offset, Text lineText, Context context) throws IOException, InterruptedException {
            String filePathString = ((FileSplit) context.getInputSplit()).getPath().getName();
            Matcher m = EN_LANG.matcher(lineText.toString());
            for (String word : WORD_BOUNDARY.split(lineText.toString())) {
                if (!word.isEmpty()) {
                    if (m.find()) {
                        if (Authorship.EN_ARTICLES.contains(word)) {
                            context.write(new Text(filePathString + "*article"), new IntWritable(1));
                        }

                        if (Authorship.EN_CONJUNCTIONS.contains(word)) {
                            context.write(new Text(filePathString + "*conjunction"), new IntWritable(1));
                        }

                        if (Authorship.EN_PREPOSITIONS.contains(word)) {
                            context.write(new Text(filePathString + "*preposition"), new IntWritable(1));
                        }
                    } else {
                        if (Authorship.IT_ARTICLES.contains(word) || word.startsWith("l'") || word.startsWith("un'") || word.startsWith("gl'")) {
                            context.write(new Text(filePathString + "*article"), new IntWritable(1));
                        }

                        if (Authorship.IT_CONJUNCTIONS.contains(word)) {
                            context.write(new Text(filePathString + "*conjunction"), new IntWritable(1));
                        }

                        if (Authorship.IT_PREPOSITIONS.contains(word)) {
                            context.write(new Text(filePathString + "*preposition"), new IntWritable(1));
                        }
                    }

                    context.write(new Text(filePathString + "*nwords"), new IntWritable(1));
                }
            }

            Matcher matcher = END_PERIOD.matcher(lineText.toString());
            while (matcher.find()) {
                context.write(new Text(filePathString + "*periods"), new IntWritable(1));
            }

        }
    }


    public static class Reduce extends Reducer<Text, IntWritable, String, Integer> {
        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable count : values) {
                sum += count.get();
            }

            context.write(key.toString().split("\\*")[0] + key.toString().split("\\*")[1], sum);

        }
    }
}


