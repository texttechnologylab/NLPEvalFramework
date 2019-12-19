package org.hucompute.nlpevalframework;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FLORSExperiment extends Experiment {

    private File modelDir;
    private String trainParameters = null;
    private String testParameters = null;
    private Map<String, String> extendedParameterMap;

    public FLORSExperiment(File pResultBaseDir, Experiment.Category pCategory, CoNLL pTrainingSet, CoNLL pEvaluationSet, String pLanguage, Map<String, String> pExtendedParameterMap) {
        super(pResultBaseDir, Tool.FLORS, pCategory, pTrainingSet, pEvaluationSet, pLanguage);
        extendedParameterMap = pExtendedParameterMap;
    }

    public FLORSExperiment(File pResultBaseDir, Experiment.Category pCategory, CoNLL pTrainingSet, CoNLL pEvaluationSet, String pLanguage, List<Category> pJointCategories, Map<String, String> pExtendedParameterMap) {
        super(pResultBaseDir, Tool.FLORS, pCategory, pTrainingSet, pEvaluationSet, pLanguage, pJointCategories);
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
        modelDir = getModelDirectory();
        File lTrainDir = getTrainDirectory();
        File lTrainingInput = new File(lTrainDir.getAbsolutePath()+"/train.txt");
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainingInput), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    lWriter.println(lEntry.getWordform()+"\t"+getGold(lEntry));
                }
                lWriter.println();
            }
            lWriter.close();
        }
        int lWindow = 5; // 5 is default
        {
            List<String> lParameters = new ArrayList<>();
            lParameters.add("java");
            lParameters.add("-Xmx256g"); // 256
            lParameters.add("-Xms256g"); // 256
            lParameters.add("-XX:-UseGCOverheadLimit");
            lParameters.add("-Dfile.encoding=UTF-8");
            lParameters.add("-jar");
            lParameters.add(new File("bin/flors/train_20150414.jar").getAbsolutePath());
            lParameters.add("-mode");
            lParameters.add("train");
            lParameters.add("-trainFile");
            lParameters.add(lTrainingInput.getAbsolutePath());
            lParameters.add("-pre");
            lParameters.add(modelDir.getAbsolutePath()+"/");
            lParameters.add("-window");
            lParameters.add(Integer.toString(lWindow));
            if (extendedParameterMap.containsKey("FLORSunlabeledData")) {
                lParameters.add("-unlabeledData");
                lParameters.add(extendedParameterMap.get("FLORSunlabeledData"));
            }
            if (extendedParameterMap.containsKey("FLORSbigData")) {
                lParameters.add("-bigData");
                lParameters.add(extendedParameterMap.get("FLORSbigData"));
            }
            String[] lParams = new String[lParameters.size()];
            trainParameters = "";
            for (int i=0; i<lParameters.size(); i++) {
                lParams[i] = lParameters.get(i);
                trainParameters = trainParameters + lParams[i] +" ";
            }
            trainParameters = trainParameters.trim();
            long lStart = System.currentTimeMillis();
            ProcessBuilder lProcessBuilder = new ProcessBuilder(lParams);
            lProcessBuilder.directory(new File("bin/flors"));
            lProcessBuilder.redirectErrorStream(true);
            Process lProcess = lProcessBuilder.start();
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
            String lLine = null;
            while ((lLine = lReader.readLine()) != null) {
                System.out.println(lLine);
            }
            try {
                int lResult = lProcess.waitFor();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            lReader.close();
            trainingTime = System.currentTimeMillis() - lStart;
        }
    }

    @Override
    public void test() throws Exception {
        testParameters = "";
        modelDir = getModelDirectory();
        File lTestInput = new File(getTestDirectory().getAbsolutePath()+"/testinput.txt");
        File lOutput = new File(getTestDirectory().getAbsolutePath()+"/testoutput.txt");
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestInput), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTestSet().getSentences()) {
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    lWriter.println(lEntry.getWordform()+"\t"+lEntry.getGoldCategory(category));
                }
                lWriter.println();
            }
            lWriter.close();
        }

        long lStart = System.currentTimeMillis();
        // The two jars differ on code level but share the same classes. So its more secure to call them separately
        {
            ProcessBuilder lProcessBuilder = new ProcessBuilder("java", "-Xmx32g", "-Dfile.encoding=UTF-8", "-jar", new File("bin/flors/predict_online_1.jar").getAbsolutePath(), "-mode", "predict", "-predictFile", lTestInput.getAbsolutePath(), "-labeled", "1", "-update", "1", "-pre", modelDir.getAbsolutePath()+"/", "-out", lOutput.getAbsolutePath());
            System.out.println("java -Xmx32g -jar "+new File("bin/flors/predict_online_1.jar").getAbsolutePath()+" -mode predict -predictFile "+lTestInput.getAbsolutePath()+" -labeled 1 -update 1 -pre "+modelDir.getAbsolutePath()+"/ -out "+lOutput.getAbsolutePath());
            lProcessBuilder.directory(new File("bin/flors"));
            lProcessBuilder.redirectErrorStream(true);
            Process lProcess = lProcessBuilder.start();
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
            String lLine = null;
            while ((lLine = lReader.readLine()) != null) {
                System.out.println(lLine);
            }
            try {
                int lResult = lProcess.waitFor();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            lReader.close();
        }
        testTime = System.currentTimeMillis() - lStart;

        BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(lOutput), Charset.forName("UTF-8")));
        List<CoNLLSentence> lSentences = new ArrayList<>();
        String lLine = null;
        for (CoNLLSentence lSentence : getTestSet().getSentences()) {
            List<CoNLLEntry> lEntries = new ArrayList<>();
            for (CoNLLEntry lEntry : lSentence.getEntries()) {
                lLine = lReader.readLine();
                String[] lFields = lLine.split("\t", -1);
                String lForm = lFields[0];
                String lPoS = lFields[3];
                CoNLLEntry lNewEntry = new CoNLLEntry(lEntry);
                lNewEntry.setPredictedCategory(category, lPoS);
                lEntries.add(lNewEntry);
            }
            lSentences.add(new CoNLLSentence(lEntries));
            lReader.readLine();
        }
        lReader.close();
        resultSet = new CoNLL(lSentences);
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
    }
}
