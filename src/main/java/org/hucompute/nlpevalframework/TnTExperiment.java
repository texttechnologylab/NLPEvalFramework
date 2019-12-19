package org.hucompute.nlpevalframework;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class TnTExperiment extends Experiment {

    private String trainParameters = null;
    private String testParameters = null;
    protected File modelFile;

    public TnTExperiment(File pResultBaseDir, Experiment.Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage) {
        super(pResultBaseDir, Tool.TnT, pCategory, pTrainingSet, pTestSet, pLanguage);
    }

    public TnTExperiment(File pResultBaseDir, Experiment.Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, List<Category> pJointCategories) {
        super(pResultBaseDir, Tool.TnT, pCategory, pTrainingSet, pTestSet, pLanguage, pJointCategories);
        if (jointCategories != null) Collections.sort(jointCategories);
    }

    @Override
    public String getTrainParameters() {
        return trainParameters;
    }

    @Override
    public String getTestParameters() {
        return testParameters;
    }

    private String getGold(CoNLLEntry pEntry) {
        String lGoldPos = null;
        switch (category) {
            case joint: {
                StringBuilder lKey = new StringBuilder();
                for (Category lCategory:jointCategories) {
                    if (lKey.length()>0) lKey.append("|");
                    lKey.append(lCategory.name().equals("case_") ? "case" : lCategory.name());
                    lKey.append("=");
                    lKey.append(pEntry.getGoldCategory(lCategory));
                }
                lGoldPos = lKey.toString();
                break;
            }
            default: {
                lGoldPos = pEntry.getGoldCategory(category);
                break;
            }
        }
        return lGoldPos;
    }

    @Override
    public void trainJackKnifing(int pFolds) throws Exception {
        Map<Integer, List<CoNLLSentence>> lTrainFolds = new HashMap<>();
        Map<Integer, List<CoNLLSentence>> lTestFolds = new HashMap<>();
        Map<CoNLLSentence, CoNLLSentence> lResultSentenceMap = new HashMap<>();
        {
            int lCounter = 0;
            for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
                if (!lTestFolds.containsKey(lCounter)) lTestFolds.put(lCounter, new ArrayList<>());
                lTestFolds.get(lCounter).add(lSentence);
                for (int i=0; i<pFolds; i++) {
                    if (i != lCounter) {
                        if (!lTrainFolds.containsKey(i)) lTrainFolds.put(i, new ArrayList<>());
                        lTrainFolds.get(i).add(lSentence);
                    }
                }
                lCounter++;
                if (lCounter == pFolds) lCounter = 0;
            }
        }
        File lDir = getTrainJackKnifingDirectory();
        lDir.mkdirs();
        for (int m=0; m<pFolds; m++) {
            File lModelFile = new File(lDir.getAbsolutePath()+"/model.txt");
            TObjectIntHashMap<String> lTokenFrequencyMap = new TObjectIntHashMap<>();
            Map<String, TObjectIntHashMap<String>> lTokenTagFrequencyMap = new HashMap<>();
            TObjectIntHashMap<String> lUnigramMap = new TObjectIntHashMap<>();
            Map<String, TObjectIntHashMap<String>> lBigramMap = new HashMap<>();
            Map<String, Map<String, TObjectIntHashMap<String>>> lTrigramMap = new HashMap<>();

            for (CoNLLSentence lSentence : lTrainFolds.get(m)) {
                CoNLLEntry[] lEntries = lSentence.getEntries();
                for (int i=0; i<lEntries.length; i++) {
                    CoNLLEntry lEntry = lEntries[i];
                    lTokenFrequencyMap.adjustOrPutValue(lEntry.getWordform(), 1, 1);
                    if (!lTokenTagFrequencyMap.containsKey(lEntry.getWordform())) {
                        lTokenTagFrequencyMap.put(lEntry.getWordform(), new TObjectIntHashMap<>());
                    }
                    String lGoldPos = getGold(lEntry);
                    lUnigramMap.adjustOrPutValue(lGoldPos, 1, 1);
                    if (i<lEntries.length-1) {
                        if (!lBigramMap.containsKey(lGoldPos)) {
                            lBigramMap.put(lGoldPos, new TObjectIntHashMap<String>());
                        }
                        String lGoldPos1 = getGold(lEntries[i + 1]);
                        lBigramMap.get(lGoldPos).adjustOrPutValue(lGoldPos1, 1, 1);
                        if (i<lEntries.length-2) {
                            if (!lTrigramMap.containsKey(lGoldPos)) {
                                lTrigramMap.put(lGoldPos, new HashMap<>());
                            }
                            if (!lTrigramMap.get(lGoldPos).containsKey(lGoldPos1)) {
                                lTrigramMap.get(lGoldPos).put(lGoldPos1, new TObjectIntHashMap<>());
                            }
                            String lGoldPos2 = getGold(lEntries[i + 2]);
                            lTrigramMap.get(lGoldPos).get(lGoldPos1).adjustOrPutValue(lGoldPos2, 1, 1);
                        }
                    }
                    lTokenTagFrequencyMap.get(lEntry.getWordform()).adjustOrPutValue(lGoldPos, 1, 1);
                }
            }
            {
                PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(lModelFile.getAbsolutePath()+".lex")), Charset.forName("UTF-8")));
                List<String> lEntries = new ArrayList<>(lTokenFrequencyMap.keySet());
                Collections.sort(lEntries);
                for (String lKey:lEntries) {
                    lWriter.print(lKey+"\t"+lTokenFrequencyMap.get(lKey));
                    if (lTokenTagFrequencyMap.containsKey(lKey)) {
                        List<String> lSubEntries = new ArrayList<>(lTokenTagFrequencyMap.get(lKey).keySet());
                        Collections.sort(lSubEntries);
                        for (String lTag:lSubEntries) {
                            lWriter.print("\t"+lTag+"\t"+lTokenTagFrequencyMap.get(lKey).get(lTag));
                        }
                    }
                    lWriter.println();
                }
                lWriter.close();
            }
            {
                PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(lModelFile.getAbsolutePath()+".123")), Charset.forName("UTF-8")));
                List<String> lUnigramList = new ArrayList<>(lUnigramMap.keySet());
                Collections.sort(lUnigramList, (s1,s2)->Integer.compare(lUnigramMap.get(s2), lUnigramMap.get(s1)));
                for (String lTag:lUnigramList) {
                    lWriter.println(lTag+"\t"+lUnigramMap.get(lTag));
                    if (lBigramMap.containsKey(lTag)) {
                        List<String> lBigramList = new ArrayList<>(lBigramMap.get(lTag).keySet());
                        Collections.sort(lBigramList, (s1,s2)->Integer.compare(lBigramMap.get(lTag).get(s2), lBigramMap.get(lTag).get(s1)));
                        for (String lSubTag:lBigramList) {
                            lWriter.println("\t"+lSubTag+"\t"+lBigramMap.get(lTag).get(lSubTag));
                            if (lTrigramMap.containsKey(lTag) && lTrigramMap.get(lTag).containsKey(lSubTag)) {
                                List<String> lTrigramList = new ArrayList<>(lTrigramMap.get(lTag).get(lSubTag).keySet());
                                Collections.sort(lTrigramList, (s1,s2)->Integer.compare(lTrigramMap.get(lTag).get(lSubTag).get(s2), lTrigramMap.get(lTag).get(lSubTag).get(s1)));
                                for (String lSubSubTag:lTrigramList) {
                                    lWriter.println("\t\t"+lSubSubTag+"\t"+lTrigramMap.get(lTag).get(lSubTag).get(lSubSubTag));
                                }
                            }
                        }
                    }
                }
                lWriter.close();
            }
            // Test
            File lTestFile = new File(lDir.getAbsolutePath()+"/testinput.txt");
            File lResultFile = new File(lDir.getAbsolutePath()+"/testoutput.txt");
            List<CoNLLSentence> lSentences = new ArrayList<>();
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestFile), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : lTestFolds.get(m)) {
                for (int i = 0; i < lSentence.getEntries().length; i++) {
                    lWriter.println(lSentence.getEntries()[i].getWordform());
                }
            }
            lWriter.close();
            long lStart = System.currentTimeMillis();
            ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/TnT/tnt").getAbsolutePath(), lModelFile.getAbsolutePath(), lTestFile.getAbsolutePath());
            lProcessBuilder.directory(new File("bin/TnT"));
            lProcessBuilder.redirectErrorStream(true);
            Process lProcess = lProcessBuilder.start();
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
            String lLine = null;
            do {
                lLine = lReader.readLine();
            } while (!lLine.startsWith("Setup:"));
            for (CoNLLSentence lSentence : lTestFolds.get(m)) {
                List<CoNLLEntry> lTokens = new ArrayList<>();
                for (int i = 0; i < lSentence.getEntries().length; i++) {
                    lLine = lReader.readLine();
                    if (lLine.startsWith("Tagging ")) lLine = lLine.substring(8);
                    String lForm = lLine.substring(0, lLine.indexOf("\t"));
                    String lTag = lLine.substring(lLine.lastIndexOf("\t")+1);
                    CoNLLEntry lNewEntry = new CoNLLEntry(lSentence.getEntries()[i]);

                    lNewEntry.setPredictedCategory(category, lTag);
                    lTokens.add(lNewEntry);
                }
                CoNLLSentence lRes = new CoNLLSentence(lTokens);
                lResultSentenceMap.put(lSentence, lRes);
                lSentences.add(lRes);
            }
            try {
                int lResult = lProcess.waitFor();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            lReader.close();
        }
        List<CoNLLSentence> lSentences = new ArrayList<>();
        for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
            lSentences.add(lResultSentenceMap.get(lSentence));
        }
        CoNLL lResult = new CoNLL(lSentences);
        lResult.setFile(new File(lDir.getAbsolutePath()+"/TrainJackKnifing.txt"));
        lResult.write();
    }

    @Override
    public void train() throws Exception {
        long lStart = System.currentTimeMillis();
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model.txt");

        TObjectIntHashMap<String> lTokenFrequencyMap = new TObjectIntHashMap<>();
        Map<String, TObjectIntHashMap<String>> lTokenTagFrequencyMap = new HashMap<>();
        TObjectIntHashMap<String> lUnigramMap = new TObjectIntHashMap<>();
        Map<String, TObjectIntHashMap<String>> lBigramMap = new HashMap<>();
        Map<String, Map<String, TObjectIntHashMap<String>>> lTrigramMap = new HashMap<>();

        for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
            CoNLLEntry[] lEntries = lSentence.getEntries();
            for (int i=0; i<lEntries.length; i++) {
                CoNLLEntry lEntry = lEntries[i];
                lTokenFrequencyMap.adjustOrPutValue(lEntry.getWordform(), 1, 1);
                if (!lTokenTagFrequencyMap.containsKey(lEntry.getWordform())) {
                    lTokenTagFrequencyMap.put(lEntry.getWordform(), new TObjectIntHashMap<>());
                }
                String lGoldPos = getGold(lEntry);
                lUnigramMap.adjustOrPutValue(lGoldPos, 1, 1);
                if (i<lEntries.length-1) {
                    if (!lBigramMap.containsKey(lGoldPos)) {
                        lBigramMap.put(lGoldPos, new TObjectIntHashMap<String>());
                    }
                    String lGoldPos1 = getGold(lEntries[i + 1]);
                    lBigramMap.get(lGoldPos).adjustOrPutValue(lGoldPos1, 1, 1);
                    if (i<lEntries.length-2) {
                        if (!lTrigramMap.containsKey(lGoldPos)) {
                            lTrigramMap.put(lGoldPos, new HashMap<>());
                        }
                        if (!lTrigramMap.get(lGoldPos).containsKey(lGoldPos1)) {
                            lTrigramMap.get(lGoldPos).put(lGoldPos1, new TObjectIntHashMap<>());
                        }
                        String lGoldPos2 = getGold(lEntries[i + 2]);
                        lTrigramMap.get(lGoldPos).get(lGoldPos1).adjustOrPutValue(lGoldPos2, 1, 1);
                    }
                }
                lTokenTagFrequencyMap.get(lEntry.getWordform()).adjustOrPutValue(lGoldPos, 1, 1);
            }
        }
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(modelFile.getAbsolutePath()+".lex")), Charset.forName("UTF-8")));
            List<String> lEntries = new ArrayList<>(lTokenFrequencyMap.keySet());
            Collections.sort(lEntries);
            for (String lKey:lEntries) {
                lWriter.print(lKey+"\t"+lTokenFrequencyMap.get(lKey));
                if (lTokenTagFrequencyMap.containsKey(lKey)) {
                    List<String> lSubEntries = new ArrayList<>(lTokenTagFrequencyMap.get(lKey).keySet());
                    Collections.sort(lSubEntries);
                    for (String lTag:lSubEntries) {
                        lWriter.print("\t"+lTag+"\t"+lTokenTagFrequencyMap.get(lKey).get(lTag));
                    }
                }
                lWriter.println();
            }
            lWriter.close();
        }
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(modelFile.getAbsolutePath()+".123")), Charset.forName("UTF-8")));
            List<String> lUnigramList = new ArrayList<>(lUnigramMap.keySet());
            Collections.sort(lUnigramList, (s1,s2)->Integer.compare(lUnigramMap.get(s2), lUnigramMap.get(s1)));
            for (String lTag:lUnigramList) {
                lWriter.println(lTag+"\t"+lUnigramMap.get(lTag));
                if (lBigramMap.containsKey(lTag)) {
                    List<String> lBigramList = new ArrayList<>(lBigramMap.get(lTag).keySet());
                    Collections.sort(lBigramList, (s1,s2)->Integer.compare(lBigramMap.get(lTag).get(s2), lBigramMap.get(lTag).get(s1)));
                    for (String lSubTag:lBigramList) {
                        lWriter.println("\t"+lSubTag+"\t"+lBigramMap.get(lTag).get(lSubTag));
                        if (lTrigramMap.containsKey(lTag) && lTrigramMap.get(lTag).containsKey(lSubTag)) {
                            List<String> lTrigramList = new ArrayList<>(lTrigramMap.get(lTag).get(lSubTag).keySet());
                            Collections.sort(lTrigramList, (s1,s2)->Integer.compare(lTrigramMap.get(lTag).get(lSubTag).get(s2), lTrigramMap.get(lTag).get(lSubTag).get(s1)));
                            for (String lSubSubTag:lTrigramList) {
                                lWriter.println("\t\t"+lSubSubTag+"\t"+lTrigramMap.get(lTag).get(lSubTag).get(lSubSubTag));
                            }
                        }
                    }
                }
            }
            lWriter.close();
        }
        trainingTime = System.currentTimeMillis() - lStart;
    }

    @Override
    public void test() throws Exception {
        File lTestFile = new File(getTestDirectory().getAbsolutePath()+"/testinput.txt");
        File lResultFile = new File(getTestDirectory().getAbsolutePath()+"/testoutput.txt");
        List<CoNLLSentence> lSentences = new ArrayList<>();
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestFile), Charset.forName("UTF-8")));
        for (CoNLLSentence lSentence : getTestSet().getSentences()) {
            for (int i = 0; i < lSentence.getEntries().length; i++) {
                lWriter.println(lSentence.getEntries()[i].getWordform());
            }
        }
        lWriter.close();
        long lStart = System.currentTimeMillis();
        testParameters = new File("bin/TnT/tnt").getAbsolutePath()+" "+modelFile.getAbsolutePath()+" "+lTestFile.getAbsolutePath();
        ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/TnT/tnt").getAbsolutePath(), modelFile.getAbsolutePath(), lTestFile.getAbsolutePath());
        lProcessBuilder.directory(new File("bin/TnT"));
        lProcessBuilder.redirectErrorStream(true);
        Process lProcess = lProcessBuilder.start();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
        String lLine = null;
        do {
            lLine = lReader.readLine();
        } while (!lLine.startsWith("Setup:"));
        for (CoNLLSentence lSentence : getTestSet().getSentences()) {
            List<CoNLLEntry> lTokens = new ArrayList<>();
            for (int i = 0; i < lSentence.getEntries().length; i++) {
                lLine = lReader.readLine();
                if (lLine.startsWith("Tagging ")) lLine = lLine.substring(8);
                String lForm = lLine.substring(0, lLine.indexOf("\t"));
                String lTag = lLine.substring(lLine.lastIndexOf("\t")+1);
                CoNLLEntry lNewEntry = new CoNLLEntry(lSentence.getEntries()[i]);

                lNewEntry.setPredictedCategory(category, lTag);
                lTokens.add(lNewEntry);
            }
            lSentences.add(new CoNLLSentence(lTokens));
        }
        try {
            int lResult = lProcess.waitFor();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        testTime = System.currentTimeMillis() - lStart;
        lReader.close();
        resultSet = new CoNLL(lSentences);
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
    }
}
