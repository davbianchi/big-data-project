package main.java.hadoop;

import main.java.Main;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main hadoop job. The workflow proceeds as follows:
 * <ul>
 *     <li>Job setup including input and output paths.</li>
 *     <li>Mapper starts: counts the words identifying them by using the previously declared lists. The mapper outputs
 *     a string formatted as [filepath][*][speechpart] and an IntWritable instance with a One.</li>
 *     <li> Reducer starts: counts the ones for every speech part in every file and generates an output file with
 *     the results.</li>
 * </ul>
 */
public class Authorship extends Configured implements Tool {
    // speech parts used in wordcount-like job
    private static final List<String> CONJUNCTIONS = new ArrayList<>(Arrays.asList("and", "or", "not", "but", "yet", "though", "when", "how", "so", "if", "very", "then", "some", "no"));
    private static final List<String> ARTICLES = new ArrayList<>(Arrays.asList("the", "a", "an", "one"));
    private static final List<String> PREPOSITIONS = new ArrayList<>(Arrays.asList("of", "to", "from", "in", "with", "on", "for", "between", "at", "by", "like", "but", "as", "out", "about","there", "up", "down", "now", "after", "little", "more", "over"));
    private static final List<String> PRONOUNS = new ArrayList<>(Arrays.asList("i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "his", "us", "them", "my", "your", "its", "our", "their", "that", "this", "these", "which", "what", "where", "who", "all", "any", "other", "into", "upon", "such", "himself"));
    private static final List<String> VERBS = new ArrayList<>(Arrays.asList("is", "are", "be", "been", "was", "were", "have", "has", "had", "do", "don't", "say", "said", "says", "would", "could", "make", "made", "should", "did", "didn't", "go", "came", "will", "see", "went"));

    public static final String INPUT_PATH = "/user/root/authorship/input";
    public static final String OUTPUT_PATH = "/user/root/authorship/output";
    private static final String UNKNOWNS_INPUT_PATH = "/user/root/authorship/input/unknowns";


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
        job.setCombinerClass(Reduce.class);
        job.setReducerClass(Reduce.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        return job.waitForCompletion(true) ? 0 : 1;
    }


    public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
        private static final Pattern WORD_BOUNDARY = Pattern.compile("\\s*\\b\\s*");
        private static final Pattern END_PERIOD = Pattern.compile("[a-z][.!?]");
        private static final Pattern MARKS_COMMAS = Pattern.compile("[,!?]");
        private static final IntWritable ONE = new IntWritable(1);
        private static final Pattern COMMONS = Pattern.compile("\\b\\w[a-zA-Z']*\\b");
        private Text text = new Text();

        @Override
        public void map(LongWritable offset, Text lineText, Context context) throws IOException, InterruptedException {
            String filePathString = ((FileSplit) context.getInputSplit()).getPath().getName();


            for (String word : WORD_BOUNDARY.split(lineText.toString())) {
                String refWord = word.toLowerCase();
                if (!word.isEmpty()) {
                    if (Authorship.ARTICLES.contains(refWord)) {
                        text.set(filePathString + "*articles");
                        context.write(text, ONE);
                    }

                    if (Authorship.CONJUNCTIONS.contains(refWord)) {
                        text.set(filePathString + "*conjunctions");
                        context.write(text, ONE);
                    }

                    if (Authorship.PREPOSITIONS.contains(refWord)) {
                        text.set(filePathString + "*prepositions");
                        context.write(text, ONE);
                    }

                    if (Authorship.PRONOUNS.contains(refWord)) {
                        text.set(filePathString + "*pronouns");
                        context.write(text, ONE);
                    }

                    if (Authorship.VERBS.contains(refWord)) {
                        text.set(filePathString + "*verbs");
                        context.write(text, ONE);
                    }

                    text.set(filePathString + "*nwords");
                    context.write(text, ONE);

                }
            }

            Matcher commonWordsMatcher = COMMONS.matcher(lineText.toString());
            while (commonWordsMatcher.find()) {
                String w = commonWordsMatcher.group().toLowerCase();
                if (!Authorship.ARTICLES.contains(w) && !Authorship.PREPOSITIONS.contains(w) &&
                        !Authorship.CONJUNCTIONS.contains(w) && !Authorship.PRONOUNS.contains(w) &&
                        !Authorship.VERBS.contains(w)) {
                    text.set(filePathString + "*commons:" + w);
                    context.write(text, ONE);
                }
            }

            String refLineText = lineText.toString();

            // period number count
            Matcher matcher = END_PERIOD.matcher(refLineText);
            while (matcher.find()) {
                text.set(filePathString + "*periods");
                context.write(text, ONE);
            }

            // commas number count
            Matcher commas = MARKS_COMMAS.matcher(refLineText);
            while (commas.find()) {
                text.set(filePathString + "*commas");
                context.write(text, ONE);
            }

        }
    }


    public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {
        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable count : values) {
                sum += count.get();
            }

            context.write(key, new IntWritable(sum));

        }
    }
}


