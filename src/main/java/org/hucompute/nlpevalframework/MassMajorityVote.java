package org.hucompute.nlpevalframework;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class MassMajorityVote {

    private int minTaggerCount;
    private int maxTaggerCount;
    private List<Experiment.Tool> tools;
    private List<File> trainFiles;
    private List<File> testFiles;
    private File resultBaseDir;

    public MassMajorityVote(File pResultBaseDir, int pMinTaggerCounter, int pMaxTaggerCount, List<Experiment.Tool> pTools, List<File> pTrainFiles, List<File> pTestFiles) {
        resultBaseDir = pResultBaseDir;
        minTaggerCount = pMinTaggerCounter;
        maxTaggerCount = pMaxTaggerCount;
        tools = pTools;
        trainFiles = pTrainFiles;
        testFiles = pTestFiles;
    }

    public void run() throws IOException {
        int lEvalCount = 0;
        Map<File, List<String>> lPredictedMap = new HashMap<>();
        Map<File, List<String>> lGoldMap = new HashMap<>();
        for (int lSliceCount = 0; lSliceCount < testFiles.size(); lSliceCount++) {
            CoNLL lCoNLL = new CoNLL(testFiles.get(lSliceCount));
            List<String> lGold = new ArrayList<>();
            for (CoNLLSentence lSentence:lCoNLL.getSentences()) {
                for (CoNLLEntry lEntry:lSentence.getEntries()) {
                    lGold.add(lEntry.getGoldCategory(Experiment.Category.pos));
                }
            }
            lGoldMap.put(testFiles.get(lSliceCount), lGold);
        }
        Experiment.Tool[] lBestTaggers = null;
        double lBestAccuracy = 0;
        for (int lTaggerCount = minTaggerCount; lTaggerCount <=maxTaggerCount; lTaggerCount++) {
            int[] lPermutation = new int[lTaggerCount];
            for (int i=0; i<lPermutation.length; i++) lPermutation[i] = i;
            boolean lAllDone = true;
            do {
                Experiment.Tool[] lTaggers = new Experiment.Tool[lPermutation.length];
                for (int i=0; i<lTaggers.length; i++) lTaggers[i] = tools.get(lPermutation[i]);
                if (lBestTaggers == null) lBestTaggers = lTaggers;
                double lAccuracy = 0;
                for (int lSliceCount = 0; lSliceCount < trainFiles.size(); lSliceCount++) {
                    List<List<String>> lPredictedLists = new ArrayList<>();
                    for (int i=0; i<lTaggers.length; i++) {
                        File lFile = new File((resultBaseDir.getAbsolutePath()+"/"+trainFiles.get(lSliceCount).getName()+"/"+testFiles.get(lSliceCount).getName()+"/"+lTaggers[i].name()+"/pos"+"/"+trainFiles.get(lSliceCount).getName()+"_"+testFiles.get(lSliceCount).getName()+"_"+lTaggers[i].name()+"_pos.conll").replace("\\", "/"));
                        if (!lPredictedMap.containsKey(lFile)) {
                            CoNLL lCoNLL = new CoNLL(lFile);
                            List<String> lPredicted = new ArrayList<>();
                            for (CoNLLSentence lSentence:lCoNLL.getSentences()) {
                                for (CoNLLEntry lEntry:lSentence.getEntries()) {
                                    lPredicted.add(lEntry.getPredictedCategory(Experiment.Category.pos));
                                }
                            }
                            lPredictedMap.put(lFile, lPredicted);
                        }
                        lPredictedLists.add(lPredictedMap.get(lFile));
                    }
                    List<String> lGoldList = lGoldMap.get(testFiles.get(lSliceCount));
                    // All Data in lPredictedLists and lGoldList - Evaluate
                    int lHits = 0;
                    for (int i=0; i<lGoldList.size(); i++) {
                        TObjectIntHashMap<String> lTagFreq = new TObjectIntHashMap<>();
                        TObjectIntHashMap<String> lScoreMap = new TObjectIntHashMap<>();
                        for (int k=0; k<lPredictedLists.size(); k++) {
                            String lPredicted = lPredictedLists.get(k).get(i);
                            lTagFreq.adjustOrPutValue(lPredicted, 1, 1);
                            lScoreMap.adjustOrPutValue(lPredicted, k, k);
                        }
                        String lPredicted = lTagFreq.keySet().iterator().next();
                        if (lTagFreq.size() > 1) {
                            List<String> lList = new ArrayList<>(lTagFreq.keySet());
                            lList.sort(new Comparator<String>() {
                                @Override
                                public int compare(String o1, String o2) {
                                    int lRes = Integer.compare(lTagFreq.get(o2), lTagFreq.get(o1));
                                    if (lRes != 0) return lRes;
                                    lRes = Integer.compare(lScoreMap.get(o1), lScoreMap.get(o2));
                                    return lRes;
                                }
                            });
                            lPredicted = lList.get(0);
                        }
                        if (lPredicted.equals(lGoldList.get(i))) lHits++;
                    }
                    double lThisAccuracy = lHits/(double)lGoldList.size();
                    lAccuracy += lThisAccuracy;
                }
                lAccuracy /= trainFiles.size();
                if (lAccuracy > lBestAccuracy) {
                    lBestAccuracy = lAccuracy;
                    lBestTaggers = lTaggers;
                }
                // Eval Taggers
                lEvalCount++;
                System.out.print("Latest: "+lEvalCount);
                for (Experiment.Tool lTool:lTaggers) {
                    System.out.print("\t"+lTool.name());
                }
                System.out.print("\t"+lAccuracy);
                System.out.print("\tBest: ");
                for (Experiment.Tool lTool:lBestTaggers) {
                    System.out.print("\t"+lTool.name());
                }
                System.out.println("\t"+lBestAccuracy);
                // Done: Eval Taggers

                lAllDone = true;
                for (int i=0; i<lPermutation.length; i++) {
                    if (lPermutation[i] != tools.size()-1-i) {
                        lAllDone = false;
                        break;
                    }
                }
                if (lAllDone) break;
                boolean lAllOk = true;
                do {
                    lAllOk = true;
                    int lIndex = lPermutation.length-1;
                    lPermutation[lIndex]++;
                    while (lPermutation[lIndex] >= tools.size()) {
                        lPermutation[lIndex-1]++;
                        for (int i=lIndex; i<lPermutation.length; i++) lPermutation[i] = 0;
                        lIndex--;
                    }
                    TIntSet lSet = new TIntHashSet();
                    for (int i=0; i<lPermutation.length; i++) {
                        if (lSet.contains(lPermutation[i])) {
                            lAllOk = false;
                            break;
                        }
                        else {
                            lSet.add(lPermutation[i]);
                        }
                    }
                } while (!lAllOk);
            } while (!lAllDone);
        }
        System.out.print("Eval: "+lEvalCount);
        for (Experiment.Tool lTool:lBestTaggers) {
            System.out.print("\t"+lTool.name());
        }
        System.out.println("\t"+lBestAccuracy);
    }

}
