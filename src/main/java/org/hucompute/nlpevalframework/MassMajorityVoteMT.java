package org.hucompute.nlpevalframework;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class MassMajorityVoteMT {

    private int minTaggerCount;
    private int maxTaggerCount;
    private List<Experiment.Tool> tools;
    private List<File> trainFiles;
    private List<File> testFiles;
    private int maxThreads = Runtime.getRuntime().availableProcessors();
    private Set<MajorityVoteThread> threads;
    private double bestAccuracy = 0;
    private Experiment.Tool[] bestTaggers = null;
    private double latestAccuracy = 0;
    private Experiment.Tool[] latestTaggers = null;
    private Map<File, List<String>> predictedMap = new HashMap<>();
    private Map<File, List<String>> goldMap = new HashMap<>();
    private TIntObjectHashMap<String> levelBestTaggerMap;
    private TIntDoubleHashMap levelBestAccuracyMap;
    private PrintWriter writer;
    private File resultBaseDir;

    public MassMajorityVoteMT(File pResultBaseDir, int pMinTaggerCounter, int pMaxTaggerCount, List<Experiment.Tool> pTools, List<File> pTrainFiles, List<File> pTestFiles) {
        resultBaseDir = pResultBaseDir;
        minTaggerCount = pMinTaggerCounter;
        maxTaggerCount = pMaxTaggerCount;
        tools = pTools;
        trainFiles = pTrainFiles;
        testFiles = pTestFiles;
    }

    public void run() throws IOException {
        writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File("data/MajorityVoteEval_Tiger_ID.txt")), Charset.forName("UTF-8")));
        threads = new HashSet<>();
        int lEvalCount = 0;
        levelBestTaggerMap = new TIntObjectHashMap<>();
        levelBestAccuracyMap = new TIntDoubleHashMap();
        for (int lSliceCount = 0; lSliceCount < testFiles.size(); lSliceCount++) {
            CoNLL lCoNLL = new CoNLL(testFiles.get(lSliceCount));
            List<String> lGold = new ArrayList<>();
            for (CoNLLSentence lSentence:lCoNLL.getSentences()) {
                for (CoNLLEntry lEntry:lSentence.getEntries()) {
                    lGold.add(lEntry.getGoldCategory(Experiment.Category.pos));
                }
            }
            goldMap.put(testFiles.get(lSliceCount), lGold);
        }
        for (int lTaggerCount = minTaggerCount; lTaggerCount <=maxTaggerCount; lTaggerCount++) {
            int[] lPermutation = new int[lTaggerCount];
            for (int i=0; i<lPermutation.length; i++) lPermutation[i] = i;
            boolean lAllDone = true;
            do {
                Experiment.Tool[] lTaggers = new Experiment.Tool[lPermutation.length];
                for (int i=0; i<lTaggers.length; i++) lTaggers[i] = tools.get(lPermutation[i]);
                if (bestTaggers == null) {
                    bestTaggers = lTaggers;
                    latestTaggers = lTaggers;
                    latestAccuracy = 0;
                }


                synchronized (this) {
                    while (threads.size() > maxThreads) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    MajorityVoteThread lMajorityVoteThread = new MajorityVoteThread(this, lTaggers);
                    threads.add(lMajorityVoteThread);
                    lMajorityVoteThread.start();
                }



                lEvalCount++;
                System.out.print("Latest: "+lEvalCount);
                for (Experiment.Tool lTool:latestTaggers) {
                    System.out.print("\t"+lTool.name());
                }
                System.out.print("\t"+latestAccuracy);
                System.out.print("\tBest: ");
                for (Experiment.Tool lTool: bestTaggers) {
                    System.out.print("\t"+lTool.name());
                }
                System.out.println("\t"+ bestAccuracy);
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
        synchronized (this) {
            while (threads.size() > 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.print("Eval: "+lEvalCount);
        for (Experiment.Tool lTool: bestTaggers) {
            System.out.print("\t"+lTool.name());
        }
        System.out.println("\t"+ bestAccuracy);
        for (int i=minTaggerCount; i<=maxTaggerCount; i++) {
            System.out.println("Layer-Best: "+i+", "+levelBestTaggerMap.get(i)+", "+levelBestAccuracyMap.get(i));
        }
        writer.flush();
        writer.close();
    }

    protected void reportResults(MajorityVoteThread pMajorityVoteThread) {
        synchronized (this) {
            writer.print(pMajorityVoteThread.accuracy);
            writer.print("\t"+pMajorityVoteThread.taggers.length);
            for (Experiment.Tool lTool:pMajorityVoteThread.taggers) {
                writer.print("\t"+lTool.name());
            }
            writer.println();
            int lSize = pMajorityVoteThread.getTaggers().length;
            if (!levelBestTaggerMap.containsKey(lSize) || (pMajorityVoteThread.getAccuracy() > levelBestAccuracyMap.get(lSize))) {
                StringBuilder lBuilder = new StringBuilder();
                for (Experiment.Tool lTool:pMajorityVoteThread.taggers) {
                    lBuilder.append(lTool.name()+" ");
                }
                levelBestTaggerMap.put(lSize, lBuilder.toString().trim());
                levelBestAccuracyMap.put(lSize, pMajorityVoteThread.getAccuracy());
            }
            if (pMajorityVoteThread.getAccuracy() > bestAccuracy) {
                bestAccuracy = pMajorityVoteThread.getAccuracy();
                bestTaggers = pMajorityVoteThread.getTaggers();
            }
            latestTaggers = pMajorityVoteThread.getTaggers();
            latestAccuracy = pMajorityVoteThread.getAccuracy();
            threads.remove(pMajorityVoteThread);
        }
    }

    protected List<String> getPredicted(File pFile) {
        synchronized (this) {
            try {
                if (!predictedMap.containsKey(pFile)) {
                    CoNLL lCoNLL = new CoNLL(pFile);
                    List<String> lPredicted = new ArrayList<>();
                    for (CoNLLSentence lSentence : lCoNLL.getSentences()) {
                        for (CoNLLEntry lEntry : lSentence.getEntries()) {
                            lPredicted.add(lEntry.getPredictedCategory(Experiment.Category.pos));
                        }
                    }
                    predictedMap.put(pFile, lPredicted);
                }
                return predictedMap.get(pFile);
            }
            catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                return null;
            }
        }
    }

    private class MajorityVoteThread extends Thread {

        private MassMajorityVoteMT massMajorityVoteMT;
        private Experiment.Tool[] taggers;
        private double accuracy;

        public MajorityVoteThread(MassMajorityVoteMT pMassMajorityVoteMT, Experiment.Tool[] pTaggers) {
            massMajorityVoteMT = pMassMajorityVoteMT;
            taggers = pTaggers;
        }

        public Experiment.Tool[] getTaggers() {
            return taggers;
        }

        public double getAccuracy() {
            return accuracy;
        }

        public void run() {
            accuracy = 0;
            for (int lSliceCount = 0; lSliceCount < trainFiles.size(); lSliceCount++) {
                List<List<String>> lPredictedLists = new ArrayList<>();
                for (int i=0; i<taggers.length; i++) {
                    File lFile = new File((resultBaseDir.getAbsolutePath()+"/"+trainFiles.get(lSliceCount).getName()+"/"+testFiles.get(lSliceCount).getName()+"/"+taggers[i].name()+"/pos"+"/"+trainFiles.get(lSliceCount).getName()+"_"+testFiles.get(lSliceCount).getName()+"_"+taggers[i].name()+"_pos.conll").replace("\\", "/"));
                    lPredictedLists.add(getPredicted(lFile));
                }
                List<String> lGoldList = goldMap.get(testFiles.get(lSliceCount));
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
                accuracy += lThisAccuracy;
            }
            accuracy /= trainFiles.size();
            reportResults(this);
            synchronized (massMajorityVoteMT) {
                massMajorityVoteMT.notifyAll();
            }
        }
    }

}
