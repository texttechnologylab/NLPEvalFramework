package org.hucompute.nlpevalframework;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class MarMoTExperiment extends Experiment {

    private String trainParameters = null;
    private String testParameters = null;
    private File modelFile;
    private Map<String, String> extendedParameterMap;

    public MarMoTExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, Map<String, String> pExtendedParameterMap, String pVariant) {
        super(pResultBaseDir, Tool.MarMoT, pCategory, pTrainingSet, pTestSet, pLanguage, pVariant);
        extendedParameterMap = pExtendedParameterMap;
    }

    public MarMoTExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, List<Category> pJointCategories, Map<String, String> pExtendedParameterMap, String pVariant) {
        super(pResultBaseDir, Tool.MarMoT, pCategory, pTrainingSet, pTestSet, pLanguage, pVariant, pJointCategories);
        if (jointCategories != null) Collections.sort(jointCategories);
        extendedParameterMap = pExtendedParameterMap;
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
            System.out.println("Jackknifing-Fold: "+(m+1)+"/"+pFolds);
            File lTrainingInput = new File(lDir.getAbsolutePath()+"/traininput.txt");
            File lModelFile = new File(lDir.getAbsolutePath()+"/model.txt");
            {
                PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainingInput), Charset.forName("UTF-8")));
                for (CoNLLSentence lSentence : lTrainFolds.get(m)) {
                    for (CoNLLEntry lEntry : lSentence.getEntries()) {
                        lWriter.println(lEntry.getID() + "\t" + lEntry.getWordform() + "\t" + getGold(lEntry));
                    }
                    lWriter.println();
                }
                lWriter.close();
            }
            List<String> lParameters = new ArrayList<>();
            lParameters.add("--train-file");
            lParameters.add("form-index=1,tag-index=2," + lTrainingInput.getAbsolutePath());
            lParameters.add("--tag-morph");
            lParameters.add("false");
            lParameters.add("--model-file");
            lParameters.add(lModelFile.getAbsolutePath());
            if (extendedParameterMap.containsKey("MarMoTtype-embeddings")) {
                lParameters.add("--type-embeddings");
                lParameters.add("dense=true,"+extendedParameterMap.get("MarMoTtype-embeddings"));
            }
            if (extendedParameterMap.containsKey("MarMoTtype-dict")) {
                lParameters.add("--type-dict");
                lParameters.add(extendedParameterMap.get("MarMoTtype-dict")+",indexes=[1]");
            }
            String[] lParams = new String[lParameters.size()];
            trainParameters = "";
            for (int i=0; i<lParameters.size(); i++) {
                lParams[i] = lParameters.get(i);
                trainParameters = trainParameters + lParams[i] +" ";
            }
            trainParameters = trainParameters.trim();
            marmot.morph.cmd.Trainer.main(lParams);

            // Test
            File lTestFile = new File(lDir.getAbsolutePath()+"/testinput.txt");
            File lResultFile = new File(lDir.getAbsolutePath()+"/testoutput.txt");
            {
                PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestFile), Charset.forName("UTF-8")));
                for (CoNLLSentence lSentence : lTestFolds.get(m)) {
                    for (CoNLLEntry lEntry : lSentence.getEntries()) {
                        lWriter.println(lEntry.getWordform());
                    }
                    lWriter.println();
                }
                lWriter.close();
            }
            testParameters = "--model-file "+lModelFile.getAbsolutePath()+" --test-file form-index=0,"+lTestFile.getAbsolutePath()+" --pred-file "+lResultFile.getAbsolutePath();
            marmot.morph.cmd.Annotator.main(new String[]{"--model-file", lModelFile.getAbsolutePath(), "--test-file", "form-index=0,"+lTestFile.getAbsolutePath(), "--pred-file", lResultFile.getAbsolutePath()});
            CoNLL lResultSet = new CoNLL(lResultFile);
            int lSentenceCounter = 0;
            for (CoNLLSentence lSentence:lResultSet.getSentences()) {
                lResultSentenceMap.put(lTestFolds.get(m).get(lSentenceCounter), lSentence);
                CoNLLSentence lGoldSentence = lTestFolds.get(m).get(lSentenceCounter);
                int lEntryCounter = 0;
                for (CoNLLEntry lEntry:lSentence.getEntries()) {
                    lEntry.setGoldLemma(lGoldSentence.getEntries()[lEntryCounter].getGoldLemma());
                    lEntry.setGoldCategory(Category.pos, lGoldSentence.getEntries()[lEntryCounter].getGoldCategory(Category.pos));
                    lEntry.setGoldMorphology(lGoldSentence.getEntries()[lEntryCounter].getGoldMorphology());
                    lEntry.setPredictedCategory(category, lEntry.getPredictedCategory(Category.pos));
                    lEntryCounter++;
                }
                lSentenceCounter++;
            }
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
    public void train() throws Exception {
        File lTrainingInput = new File(getTrainDirectory().getAbsolutePath()+"/traininput.txt");
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model.txt");
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainingInput), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    lWriter.println(lEntry.getID() + "\t" + lEntry.getWordform() + "\t" + getGold(lEntry));
                }
                lWriter.println();
            }
            lWriter.close();
        }
        List<String> lParameters = new ArrayList<>();
        lParameters.add("--train-file");
        lParameters.add("form-index=1,tag-index=2," + lTrainingInput.getAbsolutePath());
        lParameters.add("--tag-morph");
        lParameters.add("false");
        lParameters.add("--model-file");
        lParameters.add(modelFile.getAbsolutePath());
        if (extendedParameterMap.containsKey("MarMoTtype-embeddings")) {
            lParameters.add("--type-embeddings");
            lParameters.add("dense=true,"+extendedParameterMap.get("MarMoTtype-embeddings"));
        }
        if (extendedParameterMap.containsKey("MarMoTtype-dict")) {
            lParameters.add("--type-dict");
            lParameters.add(extendedParameterMap.get("MarMoTtype-dict")+",indexes=[1]");
        }
        String[] lParams = new String[lParameters.size()];
        trainParameters = "";
        for (int i=0; i<lParameters.size(); i++) {
            lParams[i] = lParameters.get(i);
            trainParameters = trainParameters + lParams[i] +" ";
        }
        trainParameters = trainParameters.trim();
        long lStart = System.currentTimeMillis();
        marmot.morph.cmd.Trainer.main(lParams);
        trainingTime = System.currentTimeMillis() - lStart;
    }

    @Override
    public void test() throws Exception {
        File lTestInput = new File(getTestDirectory().getAbsolutePath()+"/testinput.txt");
        File lOutput = new File(getTestDirectory().getAbsolutePath()+"/testoutput.txt");
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model.txt");
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestInput), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTestSet().getSentences()) {
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    lWriter.println(lEntry.getWordform());
                }
                lWriter.println();
            }
            lWriter.close();
        }
        testParameters = "--model-file "+modelFile.getAbsolutePath()+" --test-file form-index=0,"+lTestInput.getAbsolutePath()+" --pred-file "+lOutput.getAbsolutePath();
        long lStart = System.currentTimeMillis();
        marmot.morph.cmd.Annotator.main(new String[]{"--model-file", modelFile.getAbsolutePath(), "--test-file", "form-index=0,"+lTestInput.getAbsolutePath(), "--pred-file", lOutput.getAbsolutePath()});
        testTime = System.currentTimeMillis()-lStart;
        resultSet = new CoNLL(lOutput);
        CoNLL lGold = new CoNLL(getTestSet());
        int lSentenceCounter = 0;
        for (CoNLLSentence lSentence:resultSet.getSentences()) {
            CoNLLSentence lGoldSentence = lGold.getSentences().get(lSentenceCounter);
            int lEntryCounter = 0;
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                lEntry.setGoldLemma(lGoldSentence.getEntries()[lEntryCounter].getGoldLemma());
                lEntry.setGoldCategory(Category.pos, lGoldSentence.getEntries()[lEntryCounter].getGoldCategory(Category.pos));
                lEntry.setGoldMorphology(lGoldSentence.getEntries()[lEntryCounter].getGoldMorphology());
                lEntry.setPredictedCategory(category, lEntry.getPredictedCategory(Category.pos));
                lEntryCounter++;
            }
            lSentenceCounter++;
        }
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
    }
}
