package org.hucompute.nlpevalframework.measures;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.math3.util.CombinatoricsUtils;
import java.util.*;

public class ClusteringPair {

    public enum Measure{ChiSquaredCoefficient, RandIndex, AdjustedRandIndex, Accuracy, FowlkesMallowIndex, MirkinMetric,
        JaccardIndex, PartitionDifference, FMeasure, MeilaHeckermanMeasure, MaximumMatchMeasure, VanDongenMeasure,
        MutualInformation, NormalizedMutualInformationStrehlGhosh, NormalizedMutualInformationFredJain, VariationOfInformation}

    protected String[] listA;
    protected String[] listB;
    protected TreeMap<String, TIntSet> clustersA;
    protected TreeMap<String, TIntSet> clustersB;
    protected List<String> clusterListA;
    protected List<String> clusterListB;
    protected long[] pairs;
    protected long[][] confusionMatrix;

    protected static TObjectIntHashMap<Measure> measureOrderMap;

    protected double accuracy = Double.NaN;
    protected double chiSquaredCoefficient = Double.NaN;
    protected double adjustedRandIndex = Double.NaN;
    protected double fMeasure = Double.NaN;
    protected double meilaHeckermanMeasure = Double.NaN;
    protected double maximumMatchMeasure = Double.NaN;
    protected double vanDongenMeasure = Double.NaN;
    protected double mutualInformation = Double.NaN;
    protected double clusteringEntropyA = Double.NaN;
    protected double clusteringEntropyB = Double.NaN;
    protected double normalizedMutualInformationStrehlGhosh = Double.NaN;
    protected double normalizedMutualInformationFredJain = Double.NaN;
    protected double variationOfInformation = Double.NaN;

    static {
        measureOrderMap = new TObjectIntHashMap<>();
        measureOrderMap.put(Measure.ChiSquaredCoefficient, 1);
        measureOrderMap.put(Measure.RandIndex, -1);
        measureOrderMap.put(Measure.AdjustedRandIndex, 1);
        measureOrderMap.put(Measure.Accuracy, 1);
        measureOrderMap.put(Measure.FowlkesMallowIndex, 1);
        measureOrderMap.put(Measure.MirkinMetric, -1);
        measureOrderMap.put(Measure.JaccardIndex, 1);
        measureOrderMap.put(Measure.PartitionDifference, 1);
        measureOrderMap.put(Measure.FMeasure, 1);
        measureOrderMap.put(Measure.MeilaHeckermanMeasure, 1);
        measureOrderMap.put(Measure.MaximumMatchMeasure, 1);
        measureOrderMap.put(Measure.VanDongenMeasure, -1);
        measureOrderMap.put(Measure.MutualInformation, 1);
        measureOrderMap.put(Measure.NormalizedMutualInformationStrehlGhosh, 1);
        measureOrderMap.put(Measure.NormalizedMutualInformationFredJain, 1);
        measureOrderMap.put(Measure.VariationOfInformation, -1);
    }

    public ClusteringPair(List<String> pListA, List<String> pListB) {
        //
        listA = new String[pListA.size()];
        for (int i=0; i<pListA.size(); i++) {
            listA[i] = pListA.get(i);
        }
        listB = new String[pListB.size()];
        for (int k=0; k<pListB.size(); k++) {
            listB[k] = pListB.get(k);
        }
        clustersA = new TreeMap<>();
        clustersB = new TreeMap<>();
        for (int i=0; i<listA.length; i++) {
            String lLabel = listA[i];
            if (!clustersA.containsKey(lLabel)) clustersA.put(lLabel, new TIntHashSet());
            clustersA.get(lLabel).add(i);
        }
        for (int i=0; i<listB.length; i++) {
            String lLabel = listB[i];
            if (!clustersB.containsKey(lLabel)) clustersB.put(lLabel, new TIntHashSet());
            clustersB.get(lLabel).add(i);
        }
        clusterListA = new ArrayList<>(clustersA.keySet());
        clusterListB = new ArrayList<>(clustersB.keySet());
        Collections.sort(clusterListA);
        Collections.sort(clusterListB);
        //
        pairs = ClusterUtil.getPairs(listA, listB);
        // Compute Confusion Matrix
        confusionMatrix = new long[clusterListA.size()][clusterListB.size()];
        for (int i=0; i<clusterListA.size(); i++) {
            for (int k=0; k<clusterListB.size(); k++) {
                long lIntersect = 0;
                for (int lValue:clustersA.get(clusterListA.get(i)).toArray()) {
                    if (clustersB.get(clusterListB.get(k)).contains(lValue)) {
                        lIntersect++;
                    }
                }
                confusionMatrix[i][k] = lIntersect;
            }
        }
    }

    public double getMeasure(Measure pMeasure) {
        switch (pMeasure) {
            case ChiSquaredCoefficient: return getChiSquaredCoefficient();
            case RandIndex: return getRandIndex();
            case AdjustedRandIndex: return getAdjustedRandIndex();
            case Accuracy: return getAccuracy();
            case FowlkesMallowIndex: return getFowlkesMallowIndex();
            case MirkinMetric: return getMirkinMetric();
            case JaccardIndex: return getJaccardIndex();
            case PartitionDifference: return getPartitionDifference();
            case FMeasure: return getfMeasure();
            case MeilaHeckermanMeasure: return getMeilaHeckermanMeasure();
            case MaximumMatchMeasure: return getMaximumMatchMeasure();
            case VanDongenMeasure: return getVanDongenMeasure();
            case MutualInformation: return getMutualInformation();
            case NormalizedMutualInformationStrehlGhosh: return getNormalizedMutualInformationStrehlGhosh();
            case NormalizedMutualInformationFredJain: return getNormalizedMutualInformationFredJain();
            case VariationOfInformation: return getVariationOfInformation();
            default: {
                return Double.NaN;
            }
        }
    }

    /**
     * [0] = Same Clusters in A and B
     * [1] = Same Cluster in A but different in B
     * [2] = Different Cluster in A but same in B
     * [3] = Different Clusters in A and different clusters in B
     * @return Pairs
     */
    public long[] getPairs() {
        return pairs;
    }

    /**
     * Order: Descending
     * @return
     */
    public double getChiSquaredCoefficient() {
        if (!Double.isNaN(chiSquaredCoefficient)) return chiSquaredCoefficient;
        chiSquaredCoefficient = 0;
        for (int i=0; i<clusterListA.size(); i++) {
            String lClusterA = clusterListA.get(i);
            for (int k=0; k<clusterListB.size(); k++) {
                String lClusterB = clusterListB.get(k);
                double lE = ((long)clustersA.get(lClusterA).size() * (long)clustersB.get(lClusterB).size())/(double)listA.length;
                chiSquaredCoefficient += ((confusionMatrix[i][k] - lE)*(confusionMatrix[i][k] - lE))/lE;
            }
        }
        return chiSquaredCoefficient;
    }

    /**
     * Similarity: Ascending
     * @return
     */
    public double getRandIndex() {
        return (2*(pairs[0]+pairs[3]))/(double)(listA.length*(listA.length-1));
    }

    /**
     * Order: Ascending
     * @return
     */
    public double getAdjustedRandIndex() {
        if (!Double.isNaN(adjustedRandIndex)) return adjustedRandIndex;
        double lZaehler = 0;
        for (int i=0; i<clusterListA.size(); i++) {
            for (int k=0; k<clusterListB.size(); k++) {
                assert confusionMatrix[i][k] <= Integer.MAX_VALUE;
                lZaehler += confusionMatrix[i][k] >= 2 ? CombinatoricsUtils.binomialCoefficient((int)confusionMatrix[i][k], 2) : 0;
            }
        }
        double lt1 = 0;
        for (int i=0; i<clusterListA.size(); i++) {
            lt1 += clustersA.get(clusterListA.get(i)).size() >= 2 ? CombinatoricsUtils.binomialCoefficient(clustersA.get(clusterListA.get(i)).size(), 2) : 0;
        }
        double lt2 = 0;
        for (int k=0; k<clusterListB.size(); k++) {
            lt2 += clustersB.get(clusterListB.get(k)).size() >= 2 ? CombinatoricsUtils.binomialCoefficient(clustersB.get(clusterListB.get(k)).size(), 2) : 0;
        }
        double lt3 = (2*lt1*lt2)/(listA.length*(listA.length-1));
        adjustedRandIndex = (lZaehler-lt3)/((lt1+lt2)/2-lt3);
        return adjustedRandIndex;
    }

    /**
     * Order: Ascending
     * @return
     */
    public double getAccuracy() {
        if (!Double.isNaN(accuracy)) return accuracy;
        long lHits = 0;
        for (int i=0; i<listA.length; i++) {
            if (listA[i].equals(listB[i])) lHits++;
        }
        accuracy = lHits/(double)listA.length;
        return accuracy;
    }

    /**
     * Order: Ascending
     * @return
     */
    public double getFowlkesMallowIndex() {
        return pairs[0]/Math.sqrt((pairs[0]+pairs[1])*(pairs[0]+pairs[2]));
    }

    /**
     * Order: Descending
     * @return
     */
    public double getMirkinMetric() {
        return 2*(pairs[2]+pairs[1]);
    }

    /**
     * Order: Ascending
     * @return
     */
    public double getJaccardIndex() {
        return (double)pairs[0]/(pairs[0]+pairs[1]+pairs[2]);
    }

    /**
     * Order: Descending
     * @return
     */
    public double getPartitionDifference() {
        return pairs[3];
    }

    /**
     * Order: Ascending
     * Not symmetric. a = gold, b = predicted (?)
     * @return
     */
    public double getfMeasure() {
        if (!Double.isNaN(fMeasure)) return fMeasure;
        fMeasure = 0;
        for (int i=0; i<clusterListA.size(); i++) {
            double lFMax = 0;
            double lCi = clustersA.get(clusterListA.get(i)).size();
            for (int k=0; k<clusterListB.size(); k++) {
                double lCk = clustersB.get(clusterListB.get(k)).size();
                //double lRij = confusionMatrix[i][k]/lCi;
                //double lPij = confusionMatrix[i][k]/lCk;
                //double lFAltAlt = (2*lRij*lPij)/(lRij+lPij);
                double lF = 2*confusionMatrix[i][k] / (lCi+lCk);
                //double lFOrig = (2*lCi*lCk)/(lCi+lCk);
                if (lF > lFMax) lFMax = lF;
            }
            fMeasure += lCi*lFMax;
        }
        fMeasure = fMeasure/listA.length;
        return fMeasure;
    }

    /**
     * Order: Ascending
     * Not symmetric
     * According to publication "Comparing Clusterings - An Overview",
     * a is the clustering to be tested and b is the optimal clustering
     * @return
     */
    public double getMeilaHeckermanMeasure() {
        if (!Double.isNaN(meilaHeckermanMeasure)) return meilaHeckermanMeasure;
        meilaHeckermanMeasure = 0;
        for (int i=0; i<clusterListA.size(); i++) {
            double lMax = 0;
            for (int k=0; k<clusterListB.size(); k++) {
                if (confusionMatrix[i][k] > lMax) lMax = confusionMatrix[i][k];
            }
            meilaHeckermanMeasure += lMax;
        }
        meilaHeckermanMeasure = meilaHeckermanMeasure / listA.length;
        return meilaHeckermanMeasure;
    }

    /**
     * Order: Ascending
     * @return
     */
    public double getMaximumMatchMeasure() {
        if (!Double.isNaN(maximumMatchMeasure)) return maximumMatchMeasure;
        TIntSet lAPending = new TIntHashSet();
        TIntSet lBPending = new TIntHashSet();
        for (int i=0; i<clusterListA.size(); i++) {
            lAPending.add(i);
        }
        for (int k=0; k<clusterListB.size(); k++) {
            lBPending.add(k);
        }
        maximumMatchMeasure = 0;
        for (int m=0; m<Math.min(clusterListA.size(), clusterListB.size()); m++) {
            int lBestI = -1;
            int lBestK = -1;
            double lBestValue = -1;
            for (int i:lAPending.toArray()) {
                for (int k:lBPending.toArray()) {
                    if (confusionMatrix[i][k] > lBestValue) {
                        lBestI = i;
                        lBestK = k;
                        lBestValue = confusionMatrix[i][k];
                    }
                }
            }
            lAPending.remove(lBestI);
            lBPending.remove(lBestK);
            maximumMatchMeasure += lBestValue;
        }
        maximumMatchMeasure = maximumMatchMeasure / listA.length;
        return maximumMatchMeasure;
    }

    /**
     * Order: Descending
     * @return
     */
    public double getVanDongenMeasure() {
        if (!Double.isNaN(vanDongenMeasure)) return vanDongenMeasure;
        double lMaxISum = 0;
        for (int i=0; i<clusterListA.size(); i++) {
            double lMax = 0;
            for (int k=0; k<clusterListB.size(); k++) {
                if (confusionMatrix[i][k] > lMax) lMax = confusionMatrix[i][k];
            }
            lMaxISum += lMax;
        }
        double lMaxKSum = 0;
        for (int k=0; k<clusterListB.size(); k++) {
            double lMax = 0;
            for (int i=0; i<clusterListA.size(); i++) {
                if (confusionMatrix[i][k] > lMax) lMax = confusionMatrix[i][k];
            }
            lMaxKSum += lMax;
        }
        vanDongenMeasure = 2*listA.length-lMaxISum-lMaxKSum;
        return vanDongenMeasure;
    }

    public double getClusteringEntropyA() {
        if (!Double.isNaN(clusteringEntropyA)) return clusteringEntropyA;
        clusteringEntropyA = 0;
        for (int i=0; i<clusterListA.size(); i++) {
            double lPi = clustersA.get(clusterListA.get(i)).size()/(double)listA.length;
            clusteringEntropyA += lPi * ClusterUtil.log2(lPi);
        }
        clusteringEntropyA *= -1;
        return clusteringEntropyA;
    }

    public double getClusteringEntropyB() {
        if (!Double.isNaN(clusteringEntropyB)) return clusteringEntropyB;
        clusteringEntropyB = 0;
        for (int k=0; k<clusterListB.size(); k++) {
            double lPk = clustersB.get(clusterListB.get(k)).size()/(double)listB.length;
            clusteringEntropyB += lPk * ClusterUtil.log2(lPk);
        }
        clusteringEntropyB *= -1;
        return clusteringEntropyB;
    }

    /**
     * Order: Descending
     * @return
     */
    public double getMutualInformation() {
        if (!Double.isNaN(mutualInformation)) return mutualInformation;
        mutualInformation = 0;
        for (int i=0; i<clusterListA.size(); i++) {
            for (int k=0; k<clusterListB.size(); k++) {
                double lPik = ClusterUtil.getIntersectionSize(clustersA.get(clusterListA.get(i)), clustersB.get(clusterListB.get(k))) / (double)listA.length;
                double lPi = clustersA.get(clusterListA.get(i)).size()/(double)listA.length;
                double lPk = clustersB.get(clusterListB.get(k)).size()/(double)listB.length;
                mutualInformation += lPik * ClusterUtil.log2(lPik/(lPi*lPk));
            }
        }
        return mutualInformation;
    }

    /**
     * Order: Ascending
     * @return
     */
    public double getNormalizedMutualInformationStrehlGhosh() {
        if (!Double.isNaN(normalizedMutualInformationStrehlGhosh)) return normalizedMutualInformationStrehlGhosh;
        normalizedMutualInformationStrehlGhosh = getMutualInformation() / Math.sqrt(getClusteringEntropyA()*getClusteringEntropyB());
        return normalizedMutualInformationStrehlGhosh;
    }

    /**
     * Order: Ascending
     * @return
     */
    public double getNormalizedMutualInformationFredJain() {
        if (!Double.isNaN(normalizedMutualInformationFredJain)) return normalizedMutualInformationFredJain;
        normalizedMutualInformationStrehlGhosh = 2*getMutualInformation() / (getClusteringEntropyA()+getClusteringEntropyB());
        return normalizedMutualInformationStrehlGhosh;
    }

    /**
     * Order: Descending
     * @return
     */
    public double getVariationOfInformation() {
        if (!Double.isNaN(variationOfInformation)) return variationOfInformation;
        variationOfInformation = getClusteringEntropyA()+getClusteringEntropyB()-2*getMutualInformation();
        return variationOfInformation;
    }

}
