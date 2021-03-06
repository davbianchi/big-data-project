package main.java.analysis.frequencies;


public class CommonWord implements Comparable {
    private String word;
    private float value;

    public CommonWord(String word, int value) {
        this.value = (float) value;
        this.word = word;
    }

    public CommonWord (String word, float value) {
        this.value = value;
        this.word = word;
    }

    public String getWord() {
        return word;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    @Override
    public int compareTo(Object o) {
        CommonWord com = (CommonWord) o;
        if (this.value < com.value) {
            return 1;
        } else if (this.value > com.value) {
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return this.word  + "=" + this.value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CommonWord && this.word.equals(((CommonWord) obj).word);
    }
}
