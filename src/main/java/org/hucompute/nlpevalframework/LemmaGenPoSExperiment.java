package org.hucompute.nlpevalframework;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class LemmaGenPoSExperiment extends Experiment {

    private Map<String, String> posMap;

    private Map<String, File> trainPosMap;

    private Map<String, File> modelFileMap;

    private Map<String, File> testPosMap;

    private Map<String, File> predictPosMap;

    private int tokens;

    private int correctTokens;

    private TObjectIntHashMap<String> mismatchMap;

    private boolean trainPoSSeparately;

    private boolean useGoldPosInTest;

    public LemmaGenPoSExperiment(File pResultBaseDir, CoNLL pTrain, CoNLL pTest, String pLanguage, boolean pUseGoldPoSInTest, boolean pTrainPoSSeparately) throws IOException {
        super(pResultBaseDir, Tool.LemmaGenPoS, Category.lemma, pTrain, new CoNLL(pTest), pLanguage);
        trainPoSSeparately = pTrainPoSSeparately;
        useGoldPosInTest = pUseGoldPoSInTest;
        posMap = new HashMap<>();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("data/PoSTree.txt")), Charset.forName("UTF-8")));
        String lLine = null;
        while ((lLine = lReader.readLine()) != null) {
            lLine = lLine.replaceAll("[ ]{1,}", "\t");
            lLine = lLine.replaceAll("[\t]{2,}", "\t").trim();
            if (lLine.length()>0) {
                String[] lFields = lLine.split("\t", -1);
                for (int i = 0; i < lFields.length; i++) {
                    posMap.put(lFields[i], lFields[0]);
                }
            }
        }
    }

    @Override
    public void trainJackKnifing(int pFolds) throws Exception {
        throw new Exception("Not Implemented");
    }

    @Override
    public String getTrainParameters() {
        return null;
    }

    @Override
    public String getTestParameters() {
        return null;
    }

    public void train() throws IOException {
        File lDir = getTrainDirectory();
        trainPosMap = new HashMap<>();
        modelFileMap = new HashMap<>();
        Map<String, List<String>> lMap = new HashMap<>();
        for (CoNLLSentence lSentence:getTrainingSet().getSentences()) {
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                String lBasePos = trainPoSSeparately ? posMap.get(lEntry.getGoldCategory(Experiment.Category.pos)) : "BASE";
                String lKey = lEntry.getWordform()+"\t"+lEntry.getGoldLemma()+"\t*";
                if (!lMap.containsKey(lBasePos)) lMap.put(lBasePos, new ArrayList<>());
                lMap.get(lBasePos).add(lKey);
            }
        }
        for (Map.Entry<String, List<String>> lEntry:lMap.entrySet()) {
            File lTrainFile = new File(lDir.getAbsolutePath()+"/Train_"+lEntry.getKey()+".txt");
            trainPosMap.put(lEntry.getKey(), lTrainFile);
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainFile), Charset.forName("UTF-8")));
            for (String lString:lEntry.getValue()) {
                lWriter.println(lString);
            }
            lWriter.close();
            File lPreModelFile = new File(lDir.getAbsolutePath()+"/PreModel_"+lEntry.getKey()+".txt");
            File lModelFile = new File(lDir.getAbsolutePath()+"/Model_"+lEntry.getKey()+".txt");
            modelFileMap.put(lEntry.getKey(), lModelFile);
            {
                ProcessBuilder lProcessBuilder = new ProcessBuilder("wine", new File("bin/LemmaGenPoSExperiment/lemLearn.exe").getAbsolutePath(), "-o", lPreModelFile.getAbsolutePath(), lTrainFile.getAbsolutePath());
                lProcessBuilder.directory(new File("bin/LemmaGenPoSExperiment"));
                lProcessBuilder.redirectErrorStream(true);
                Process lProcess = lProcessBuilder.start();
                BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
                String lLine = null;
                while ((lLine = lReader.readLine()) != null) {
                    System.out.println(lLine);
                }
                try {
                    int lResult = lProcess.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lReader.close();
            }
            {
                ProcessBuilder lProcessBuilder = new ProcessBuilder("wine", new File("bin/LemmaGenPoSExperiment/lemBuild.exe").getAbsolutePath(), "-o", lModelFile.getAbsolutePath(), lPreModelFile.getAbsolutePath());
                lProcessBuilder.directory(new File("bin/LemmaGenPoSExperiment"));
                lProcessBuilder.redirectErrorStream(true);
                Process lProcess = lProcessBuilder.start();
                BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
                String lLine = null;
                while ((lLine = lReader.readLine()) != null) {
                    System.out.println(lLine);
                }
                try {
                    int lResult = lProcess.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lReader.close();
            }
        }
    }

    public void test() throws IOException {
        File lDir = getTestDirectory();
        testPosMap = new HashMap<>();
        predictPosMap = new HashMap<>();
        Map<String, Set<String>> lInputMap = new HashMap<>();
        Map<String, Map<String, String>> lOutputMap = new HashMap<>();
        for (CoNLLSentence lSentence:getTestSet().getSentences()) {
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                String lBasePos = trainPoSSeparately ? posMap.get(useGoldPosInTest ? lEntry.getGoldCategory(Experiment.Category.pos) : lEntry.getPredictedCategory(Experiment.Category.pos)) : "BASE";
                String lKey = lEntry.getWordform();
                if (!lInputMap.containsKey(lBasePos)) lInputMap.put(lBasePos, new HashSet<>());
                lInputMap.get(lBasePos).add(lKey);
            }
        }
        for (Map.Entry<String, Set<String>> lEntry:lInputMap.entrySet()) {
            File lTestFile = new File(lDir.getAbsolutePath()+"/Test_"+lEntry.getKey()+".txt");
            testPosMap.put(lEntry.getKey(), lTestFile);
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestFile), Charset.forName("UTF-8")));
            for (String lString:lEntry.getValue()) {
                lWriter.println(lString);
            }
            lWriter.close();
            File lPredictFile = new File(lDir.getAbsolutePath()+"/Predict_"+lEntry.getKey()+".txt");
            predictPosMap.put(lEntry.getKey(), lPredictFile);
            if (modelFileMap.containsKey(lEntry.getKey())) {
                {
                    ProcessBuilder lProcessBuilder = new ProcessBuilder("wine", new File("bin/LemmaGenPoSExperiment/lemmatize.exe").getAbsolutePath(), "-f", "wpl", "-l", modelFileMap.get(lEntry.getKey()).getAbsolutePath(), lTestFile.getAbsolutePath(), lPredictFile.getAbsolutePath());
                    lProcessBuilder.directory(new File("bin/LemmaGenPoSExperiment"));
                    lProcessBuilder.redirectErrorStream(true);
                    Process lProcess = lProcessBuilder.start();
                    BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
                    String lLine = null;
                    while ((lLine = lReader.readLine()) != null) {
                        System.out.println(lLine);
                    }
                    try {
                        int lResult = lProcess.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    lReader.close();
                }
                BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(lPredictFile), Charset.forName("UTF-8")));
                String lLine = null;
                Map<String, String> lMap = new HashMap<>();
                lOutputMap.put(lEntry.getKey(), lMap);
                while ((lLine = lReader.readLine()) != null) {
                    String[] lFields = lLine.split("\t", -1);
                    if (lFields.length>1) {
                        lMap.put(lFields[0], lFields[1]);
                    }
                }
                lReader.close();
            }
        }
        tokens = 0;
        correctTokens = 0;
        mismatchMap = new TObjectIntHashMap<>();
        for (CoNLLSentence lSentence:getTestSet().getSentences()) {
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                tokens++;
                String lLemma = null;
                String lBasePos = trainPoSSeparately ? posMap.get(useGoldPosInTest ? lEntry.getGoldCategory(Experiment.Category.pos) : lEntry.getPredictedCategory(Experiment.Category.pos)) : "BASE";
                if (lOutputMap.containsKey(lBasePos)) {
                    lLemma = lOutputMap.get(lBasePos).get(lEntry.getWordform());
                }
                if (lLemma == null) {
                    lLemma = lEntry.getWordform();
                    if (!(useGoldPosInTest ? lEntry.getGoldCategory(Experiment.Category.pos) : lEntry.getPredictedCategory(Experiment.Category.pos)).startsWith("N")) {
                        lLemma = lLemma.toLowerCase();
                    }
                }
                lEntry.setPredictedLemma(lLemma);
                if (lEntry.getPredictedLemma().equals(lEntry.getGoldLemma())) {
                    correctTokens++;
                }
                else {
                    mismatchMap.adjustOrPutValue(lEntry.getGoldLemma() + "\t" + lEntry.getPredictedLemma(), 1, 1);
                }
            }
        }
        resultSet = testSet;
        resultSet.setFile(getResultConLLFile());
    }

}
