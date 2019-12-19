package org.hucompute.nlpevalframework;

public class StringPairFrequency implements Comparable<StringPairFrequency> {

    protected int frequency;

    protected String gold;

    protected String predicted;

    public StringPairFrequency(int frequency, String gold, String predicted) {
        this.frequency = frequency;
        this.gold = gold;
        this.predicted = predicted;
    }

    public String toString() {
        return frequency+"\t"+ gold +"\t"+ predicted;
    }

    public int getFrequency() {
        return frequency;
    }

    public String getGold() {
        return gold;
    }

    public String getPredicted() {
        return predicted;
    }

    @Override
    public int compareTo(StringPairFrequency o) {
        return Integer.compare(o.getFrequency(), frequency);
    }
}
