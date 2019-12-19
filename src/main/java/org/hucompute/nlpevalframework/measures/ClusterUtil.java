package org.hucompute.nlpevalframework.measures;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import org.hucompute.nlpevalframework.CoNLL;
import org.hucompute.nlpevalframework.CoNLLEntry;
import org.hucompute.nlpevalframework.CoNLLSentence;
import org.hucompute.nlpevalframework.Experiment;

import java.util.ArrayList;
import java.util.List;

public class ClusterUtil {

    public static enum GoldPredicted{GOLD, PREDICTED};

    public List<String> getItems(CoNLL pCoNLL, Experiment.Category pCategory, GoldPredicted pGoldPredicted) {
        List<String> lResult = new ArrayList<>();
        for (CoNLLSentence lSentence:pCoNLL.getSentences()) {
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                if (pGoldPredicted.equals(GoldPredicted.GOLD)) {
                    lResult.add(lEntry.getGoldCategory(pCategory));
                }
                else {
                    lResult.add(lEntry.getPredictedCategory(pCategory));
                }
            }
        }
        return lResult;
    }

    /**
     * [0] = Same Clusters in A and B
     * [1] = Same Cluster in A but different in B
     * [2] = Different Cluster in A but same in B
     * [3] = Different Clusters in A and different clusters in B
     * @param pListA
     * @param pListB
     * @return
     */
    public static long[] getPairs(String[] pListA, String[] pListB) {
        long[] lResult = new long[4];
        TObjectIntHashMap<String> lIDMap = new TObjectIntHashMap<>();
        int[] lListA = new int[pListA.length];
        for (int i=0; i<pListA.length; i++) {
            if (!lIDMap.containsKey(pListA[i])) lIDMap.put(pListA[i], lIDMap.size());
            lListA[i] = lIDMap.get(pListA[i]);
        }
        int[] lListB = new int[pListB.length];
        for (int i=0; i<pListB.length; i++) {
            if (!lIDMap.containsKey(pListB[i])) lIDMap.put(pListB[i], lIDMap.size());
            lListB[i] = lIDMap.get(pListB[i]);
        }
        for (int i=0; i<lListA.length-1; i++) {
            int lA1 = lListA[i];
            int lB1 = lListB[i];
            for (int k=i+1; k<lListB.length; k++) {
                int lA2 = lListA[k];
                int lB2 = lListB[k];
                if (lA1 == lA2) {
                    if (lB1 == lB2) {
                        lResult[0]++;
                    }
                    else {
                        lResult[1]++;
                    }
                }
                else {
                    if (lB1 == lB2) {
                        lResult[2]++;
                    }
                    else {
                        lResult[3]++;
                    }
                }
            }
        }
        return lResult;
    }

    public static long[] getPairsOrig(String[] pListA, String[] pListB) {
        long[] lResult = new long[4];
        for (int i=0; i<pListA.length-1; i++) {
            String lA1 = pListA[i];
            String lB1 = pListB[i];
            for (int k=i+1; k<pListB.length; k++) {
                String lA2 = pListA[k];
                String lB2 = pListB[k];
                if (lA1.equals(lA2)) {
                    if (lB1.equals(lB2)) {
                        lResult[0]++;
                    }
                    else {
                        lResult[1]++;
                    }
                }
                else {
                    if (lB1.equals(lB2)) {
                        lResult[2]++;
                    }
                    else {
                        lResult[3]++;
                    }
                }
            }
        }
        return lResult;
    }

    public static long getIntersectionSize(TIntSet a, TIntSet b) {
        long lResult = 0;
        for (int m:a.toArray()) {
            if (b.contains(m)) lResult++;
        }
        return lResult;
    }

    public static double log2(double pValue) {
        return pValue == 0 ? 0 : Math.log10(pValue)/Math.log10(2);
    }

}
