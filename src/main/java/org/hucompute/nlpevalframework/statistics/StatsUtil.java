package org.hucompute.nlpevalframework.statistics;

import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;

public class StatsUtil {

    public static void posMinMaxHeatMap() throws Exception {
        BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("data/POSMinMaxValues.txt")), Charset.forName("UTF-8")));
        String lLine = null;
        TreeMap<String, TreeMap<String, List<Double>>> lTaggerCorpusValuesMap = new TreeMap<>();
        while ((lLine = lReader.readLine()) != null) {
            String[] lFields = lLine.split("\t", -1);
            if (lFields.length > 1) {
                if (!lTaggerCorpusValuesMap.containsKey(lFields[0])) {
                    lTaggerCorpusValuesMap.put(lFields[0], new TreeMap<>());
                }
                String lCorpus = lFields[2].substring(0, lFields[2].indexOf("_"));
                if (lCorpus.equals("TGermaCorp2")) lCorpus = "TGermaCorp";
                if (!lTaggerCorpusValuesMap.get(lFields[0]).containsKey(lCorpus)) {
                    lTaggerCorpusValuesMap.get(lFields[0]).put(lCorpus, new ArrayList<>());
                }
                lTaggerCorpusValuesMap.get(lFields[0]).get(lCorpus).add(Double.parseDouble(lFields[4].replace(",", ".")));
            }
        }
        lReader.close();
        System.out.print("\\mchori{}");
        for (String lString:lTaggerCorpusValuesMap.keySet()) {
            System.out.print(" & \\mcvert{"+lString+"}");
        }
        System.out.println(" \\\\");
        double lMaxValue = 0;
        for (String lCorpus:new String[]{"TGermaCorp", "Tiger", "Capitularies", "Proiel"}) {
            for (String lTagger:lTaggerCorpusValuesMap.keySet()) {
                double lMin = Double.MAX_VALUE;
                double lMax = 0;
                for (double d:lTaggerCorpusValuesMap.get(lTagger).get(lCorpus)) {
                    if (d < lMin) lMin = d;
                    if (d > lMax) lMax = d;
                }
                double lDelta = (lMax - lMin)*100d;
                if (lDelta > lMaxValue) lMaxValue = lDelta;
            }
        }
        for (String lCorpus:new String[]{"TGermaCorp", "Tiger", "Capitularies", "Proiel"}) {
            System.out.print("\\mchori{"+lCorpus+"}");
            for (String lTagger:lTaggerCorpusValuesMap.keySet()) {
                double lMin = Double.MAX_VALUE;
                double lMax = 0;
                for (double d:lTaggerCorpusValuesMap.get(lTagger).get(lCorpus)) {
                    if (d < lMin) lMin = d;
                    if (d > lMax) lMax = d;
                }
                double lDelta = (lMax - lMin)*100d;
                System.out.print(" & "+lDelta);
            }
            System.out.println(" \\\\");
        }
        System.out.println(lMaxValue);
    }

    public static void getTopNLemmatizationErrorsList(File... pFiles) throws Exception {
        TObjectIntHashMap<String> lMap = new TObjectIntHashMap<>();
        for (File lFile:pFiles) {
            BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(lFile), Charset.forName("UTF-8")));
            String lLine = null;
            while ((lLine = lReader.readLine()) != null) {
                String[] lFields = lLine.split("\t", -1);
                if (lFields.length == 3) {
                    String lForm = lFields[0].substring(0, lFields[0].lastIndexOf("("));
                    String lGold = lFields[0].substring(lFields[0].lastIndexOf("(")+1, lFields[0].length()-1);
                    String lPredicted = lFields[1].substring(lFields[1].lastIndexOf("(")+1, lFields[1].length()-1);
                    lMap.adjustOrPutValue(lForm+"\t"+lGold+"\t"+lPredicted, Integer.parseInt(lFields[2]), Integer.parseInt(lFields[2]));
                }
            }
            lReader.close();
        }
        List<String> lEntries = new ArrayList<>(lMap.keySet());
        lEntries.sort((s1,s2)->Integer.compare(lMap.get(s2), lMap.get(s1)));
        for (int i=0; i<20; i++) {
            System.out.println(lEntries.get(i)+"\t"+lMap.get(lEntries.get(i)));
        }
    }

}
