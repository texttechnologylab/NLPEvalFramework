package org.hucompute.nlpevalframework.statistics;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class BipartiteMismatchGraph {

    public static String createBipartiteMismatchGraph(List<File> pInputFiles, int pTopN) throws IOException {
        TObjectLongHashMap<String> lGoldMap = new TObjectLongHashMap<>();
        TObjectLongHashMap<String> lPredictedMap = new TObjectLongHashMap<>();
        Map<String, TObjectLongHashMap<String>> lCountMap = new HashMap<>();
        long lGoldSum = 0;
        long lPredictedSum = 0;
        for (File lFile:pInputFiles) {
            BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(lFile), Charset.forName("UTF-8")));
            String lLine = null;
            while ((lLine = lReader.readLine()) != null) {
                String[] lFields = lLine.split("\t", -1);
                if (lFields.length == 3) {
                    lGoldSum += Integer.parseInt(lFields[2]);
                    lPredictedSum += Integer.parseInt(lFields[2]);
                    lGoldMap.adjustOrPutValue(lFields[0], Integer.parseInt(lFields[2]), Integer.parseInt(lFields[2]));
                    lPredictedMap.adjustOrPutValue(lFields[1], Integer.parseInt(lFields[2]), Integer.parseInt(lFields[2]));
                    if (!lCountMap.containsKey(lFields[0])) lCountMap.put(lFields[0], new TObjectLongHashMap<>());
                    lCountMap.get(lFields[0]).adjustOrPutValue(lFields[1], Integer.parseInt(lFields[2]), Integer.parseInt(lFields[2]));
                }
            }
            lReader.close();
        }
        List<String> lGold = new ArrayList<>(lGoldMap.keySet());
        lGold.sort((s1,s2)->Long.compare(lGoldMap.get(s2), lGoldMap.get(s1)));
        while (lGold.size() > pTopN) {
            String s = lGold.remove(lGold.size()-1);
            lGoldMap.remove(s);
        }
        //Collections.sort(lGold);
        List<String> lPredicted = new ArrayList<>(lPredictedMap.keySet());
        lPredicted.sort((s1,s2)->Long.compare(lPredictedMap.get(s2), lPredictedMap.get(s1)));
        while (lPredicted.size() > pTopN) {
            String s = lPredicted.remove(lPredicted.size()-1);
            lPredictedMap.remove(s);
        }
        //Collections.sort(lPredicted);
        Map<String, String> lGoldIDMap = new HashMap<>();
        for (int i=0; i<lGold.size(); i++) {
            lGoldIDMap.put(lGold.get(i), "g"+i);
        }
        Map<String, String> lPredictedIDMap = new HashMap<>();
        for (int i=0; i<lPredicted.size(); i++) {
            lPredictedIDMap.put(lPredicted.get(i), "p"+i);
        }
        long lMinEdge = Long.MAX_VALUE;
        long lMaxEdge = Long.MIN_VALUE;
        for (Map.Entry<String, TObjectLongHashMap<String>> lEntry:lCountMap.entrySet()) {
            for (long lValue:lEntry.getValue().values()) {
                lMinEdge = Math.min(lMinEdge, lValue);
                lMaxEdge = Math.max(lMaxEdge, lValue);
            }
        }
        StringBuilder lResult = new StringBuilder();
        lResult.append("\\newcommand*{\\Scaling}{0.2}\n" +
                "\\begin{tikzpicture}[Vertex/.style={circle, line width=1.0, fill=white, minimum size=\\Scaling*4.0cm},\n" +
                "Seed/.style={circle, line width=1.0, fill=SeminarRot, minimum size=\\Scaling*2cm},\n" +
                "every label/.style={rectangle, align=center, minimum width=0.7cm, inner sep=1, font=\\scriptsize\\ttfamily},\n" +
                "SeedText/.style={rectangle, fill=SeminarRot, align=center, minimum width=1cm, font=\\ttfamily, text=white},\n" +
                "% every to/.style={bend left},\n" +
                "x=\\Scaling,y=\\Scaling,\n" +
                ">=latex]\n" +
                "\\pgftransformyscale{-1}\n");
        for (int i=0; i<lGold.size(); i++) {
            String lPercentage = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.US)).format((lGoldMap.get(lGold.get(i))*100d)/lGoldSum);
            lResult.append("\\node ("+lGoldIDMap.get(lGold.get(i))+") [at={("+(120*i)+",200)},  Vertex, fill=GU-Helles-Gruen, label={center:"+lGold.get(i)+"\\\\"+lPercentage+"\\%}]{};\n");
        }
        for (int i=0; i<lPredicted.size(); i++) {
            String lPercentage = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.US)).format((lPredictedMap.get(lPredicted.get(i))*100d)/lPredictedSum);
            lResult.append("\\node ("+lPredictedIDMap.get(lPredicted.get(i))+") [at={("+(120*i)+",600)},  Vertex, fill=GU-Sonnengelb, label={center:"+lPredicted.get(i)+"\\\\"+lPercentage+"\\%}]{};\n");
        }
        lResult.append("\\begin{scope}[on background layer]\n");
        int lMaxEdgeWidth = 10;
        for (Map.Entry<String, TObjectLongHashMap<String>> lEntry:lCountMap.entrySet()) {
            String lSource = lEntry.getKey();
            if (lGoldMap.containsKey(lSource)) {
                for (String lTarget : lEntry.getValue().keySet()) {
                    if (lPredictedMap.containsKey(lTarget)) {
                        double lEdgeWidth = (lEntry.getValue().get(lTarget)) * (double) lMaxEdgeWidth / (lMaxEdge);
                        if (lEdgeWidth >= 0.1) {
                            lResult.append("\\path [->,line width=\\Scaling*" + new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US)).format(lEdgeWidth) + ",draw,](" + lGoldIDMap.get(lSource) + ") to (" + lPredictedIDMap.get(lTarget) + ");\n");
                        }
                    }
                }
            }
        }
        lResult.append("\\end{scope}\n" +
                "\\end{tikzpicture}");
        return lResult.toString();
    }

}
