package org.hucompute.nlpevalframework;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BLSTMRNNExperiment extends Experiment {

    private String trainParameters = null;
    private String testParameters = null;
    private File modelFile;
    private Map<String, String> extendedParameterMap;

    public BLSTMRNNExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, Map<String, String> pExtendedParameterMap, String pVariant) {
        super(pResultBaseDir, Tool.BLSTMRNN, pCategory, pTrainingSet, pTestSet, pLanguage, pVariant);
        extendedParameterMap = pExtendedParameterMap;
    }

    public BLSTMRNNExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, List<Category> pJointCategories, Map<String, String> pExtendedParameterMap, String pVariant) {
        super(pResultBaseDir, Tool.BLSTMRNN, pCategory, pTrainingSet, pTestSet, pLanguage, pVariant, pJointCategories);
        if (jointCategories != null) Collections.sort(jointCategories);
        extendedParameterMap = pExtendedParameterMap;
    }

    @Override
    public void trainJackKnifing(int pFolds) throws Exception {
        throw new Exception("Not Implemented");
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
        {
            CoNLL lTrainCoNLL = new CoNLL(getTrainingSet());
            for (CoNLLSentence lSentence : lTrainCoNLL.getSentences()) {
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    lEntry.setGoldCategory(Category.pos, getGold(lEntry));
                }
            }
            lTrainCoNLL.write(lTrainingInput);
        }

        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(getModelDirectory().getAbsolutePath().replace("\\", "/")+"/embeddingsfile.txt")), Charset.forName("UTF-8")));
        lWriter.println(extendedParameterMap.get("BLSTMRNNembeddings"));
        lWriter.close();

        File lTestInput = new File(getTestDirectory().getAbsolutePath()+"/testinput.txt");
        File lTestSetFile = getTestSet().getFile();
        getTestSet().write(lTestInput);
        getTestSet().setFile(lTestSetFile);
        File lOutput = new File(getTestDirectory().getAbsolutePath()+"/testoutput.txt");

        List<String> lParameters = new ArrayList<>();
        lParameters.add("java");
        lParameters.add("-Xmx32g");
        lParameters.add("-Dfile.encoding=UTF-8");
        lParameters.add("-jar");
        lParameters.add(new File("bin/BLSTMRNN/POS-Complete.jar").getAbsolutePath());
        lParameters.add(lTrainingInput.getAbsolutePath());
        lParameters.add(extendedParameterMap.get("BLSTMRNNembeddings"));
        lParameters.add("14"); // Epochs. Recommended: 14
        lParameters.add("0.1"); // Learning Rate
        lParameters.add(lTestInput.getAbsolutePath());
        lParameters.add(getModelDirectory().getAbsolutePath().replace("\\", "/")+"/");
        lParameters.add(lOutput.getAbsolutePath());
        String[] lParams = new String[lParameters.size()];
        trainParameters = "";
        for (int i=0; i<lParameters.size(); i++) {
            lParams[i] = lParameters.get(i);
            trainParameters = trainParameters + lParams[i] +" ";
        }
        trainParameters = trainParameters.trim();
        long lStart = System.currentTimeMillis();
        ProcessBuilder lProcessBuilder = new ProcessBuilder(lParameters);
        //lProcessBuilder.directory(new File("bin/BLSTMRNN"));
        lProcessBuilder.redirectErrorStream(true);
        Process lProcess = lProcessBuilder.start();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
        String lLine = null;
        do {
            lLine = lReader.readLine();
            System.out.println(lLine);
        } while (lLine != null);
        try {
            int lResult = lProcess.waitFor();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        lReader.close();
        trainingTime = System.currentTimeMillis() - lStart;
    }

    @Override
    public void test() throws Exception {
        File lTestInput = new File(getTestDirectory().getAbsolutePath()+"/testinput.txt");
        File lOutput = new File(getTestDirectory().getAbsolutePath()+"/testoutput.txt");
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model.txt");
        File lTestSetFile = getTestSet().getFile();
        getTestSet().write(lTestInput);
        getTestSet().setFile(lTestSetFile);
        // Fetch Embedding File
        File lEmbeddingFile = null;
        {
            BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(getModelDirectory().getAbsolutePath().replace("\\", "/")+"/embeddingsfile.txt")), Charset.forName("UTF-8")));
            lEmbeddingFile = new File(lReader.readLine().trim());
            lReader.close();
            if (!lEmbeddingFile.exists()) throw new Exception("Embedding-File not found: "+lEmbeddingFile.getAbsolutePath()+" for model: "+getModelDirectory().getAbsolutePath());
        }
        //
        List<String> lParameters = new ArrayList<>();
        lParameters.add("java");
        lParameters.add("-Xmx32g");
        lParameters.add("-Dfile.encoding=UTF-8");
        lParameters.add("-jar");
        lParameters.add(new File("bin/BLSTMRNN/POS-eval.jar").getAbsolutePath());
        lParameters.add(lTestInput.getAbsolutePath());
        lParameters.add(lEmbeddingFile.getAbsolutePath());
        lParameters.add(getModelDirectory().getAbsolutePath().replace("\\","/")+"/posTags.txt");
        lParameters.add(getModelDirectory().getAbsolutePath().replace("\\","/")+"/unknownWords.vec");
        lParameters.add(getModelDirectory().getAbsolutePath().replace("\\","/")+"/modelBLSTM.model");
        lParameters.add(lOutput.getAbsolutePath());
        String[] lParams = new String[lParameters.size()];
        trainParameters = "";
        for (int i=0; i<lParameters.size(); i++) {
            lParams[i] = lParameters.get(i);
            trainParameters = trainParameters + lParams[i] +" ";
        }
        trainParameters = trainParameters.trim();
        long lStart = System.currentTimeMillis();
        ProcessBuilder lProcessBuilder = new ProcessBuilder(lParameters);
        //lProcessBuilder.directory(new File("bin/BLSTMRNN"));
        lProcessBuilder.redirectErrorStream(true);
        Process lProcess = lProcessBuilder.start();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
        String lLine = null;
        do {
            lLine = lReader.readLine();
            System.out.println(lLine);
        } while (lLine != null);
        try {
            int lResult = lProcess.waitFor();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        lReader.close();
        //
        testTime = System.currentTimeMillis()-lStart;
        resultSet = new CoNLL(lOutput);
        CoNLL lGold = new CoNLL(getTestSet());
        int lSentenceCounter = 0;
        for (CoNLLSentence lSentence:resultSet.getSentences()) {
            CoNLLSentence lGoldSentence = lGold.getSentences().get(lSentenceCounter);
            int lEntryCounter = 0;
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                //lEntry.getFields()[5] = lEntry.getFields()[4]; // The Results of BLSTMRNN mix up predicted and gold...
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
