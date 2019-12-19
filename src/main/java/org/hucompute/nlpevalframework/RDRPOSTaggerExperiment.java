package org.hucompute.nlpevalframework;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RDRPOSTaggerExperiment extends Experiment {

    private File trainDir = null;
    private String trainParameters = null;
    private String testParameters = null;
    private File optionalInitializationTrainCoNLLFile = null;
    private File optionalInitializationTestCoNLLFile = null;
    private Map<String, String> extendedParameterMap;
    private static String python = System.getProperty("os.name").toLowerCase().contains("linux") ? "python" : "C:/Python27/python.exe";

    public RDRPOSTaggerExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pEvaluationSet, String pLanguage, Map<String, String> pExtendedParameterMap, String pVariant) {
        super(pResultBaseDir, Tool.RDRPOSTagger, pCategory, pTrainingSet, pEvaluationSet, pLanguage, pVariant);
        extendedParameterMap = pExtendedParameterMap;
        if (extendedParameterMap.containsKey("RDRPOSTaggerInitTrainFile")) {
            optionalInitializationTrainCoNLLFile = new File(extendedParameterMap.get("RDRPOSTaggerInitTrainFile"));
        }
        if (extendedParameterMap.containsKey("RDRPOSTaggerInitTestFile")) {
            optionalInitializationTestCoNLLFile = new File(extendedParameterMap.get("RDRPOSTaggerInitTestFile"));
        }
    }

    public RDRPOSTaggerExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pEvaluationSet, String pLanguage, List<Category> pJointCategories, Map<String, String> pExtendedParameterMap, String pVariant) {
        super(pResultBaseDir, Tool.RDRPOSTagger, pCategory, pTrainingSet, pEvaluationSet, pLanguage, pVariant, pJointCategories);
        if (jointCategories != null) Collections.sort(jointCategories);
        extendedParameterMap = pExtendedParameterMap;
        if (extendedParameterMap.containsKey("RDRPOSTaggerInitTrainFile")) {
            optionalInitializationTrainCoNLLFile = new File(extendedParameterMap.get("RDRPOSTaggerInitTrainFile"));
        }
        if (extendedParameterMap.containsKey("RDRPOSTaggerInitTestFile")) {
            optionalInitializationTestCoNLLFile = new File(extendedParameterMap.get("RDRPOSTaggerInitTestFile"));
        }
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

    private String getPredicted(CoNLLEntry pEntry) {
        String lPredicted = null;
        switch (category) {
            case joint: {
                StringBuilder lKey = new StringBuilder();
                for (Category lCategory:jointCategories) {
                    if (lKey.length()>0) lKey.append("|");
                    lKey.append(lCategory.name().equals("case_") ? "case" : lCategory.name());
                    lKey.append("=");
                    lKey.append(pEntry.getPredictedCategory(lCategory));
                }
                lPredicted = lKey.toString();
                break;
            }
            default: {
                lPredicted = pEntry.getPredictedCategory(category);
                break;
            }
        }
        return lPredicted;
    }

    public void train() throws IOException {
        File lTrainingInput = new File(getTrainDirectory().getAbsolutePath()+"/traininput.txt");
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainingInput), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
                boolean lFirst = true;
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    if (!lFirst) {
                        lWriter.print(" ");
                    } else {
                        lFirst = false;
                    }
                    String lForm = lEntry.getWordform().replace(" ", "_").replace("/", "_");
                    String lPOS = getGold(lEntry).replace(" ", "_").replace("/", "_");
                    lForm = lForm.replace("\"", "╬");
                    lForm = lForm.replace("“", "╩");
                    lWriter.print(lForm + "/" + lPOS);
                }
                lWriter.println();
            }
            lWriter.close();
        }
        File lInitFile = null;
        if (optionalInitializationTrainCoNLLFile != null) {
            CoNLL lInitCoNLL = new CoNLL(optionalInitializationTrainCoNLLFile);
            lInitFile = new File(getTrainDirectory().getAbsolutePath()+"/initfile.txt");
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lInitFile), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : lInitCoNLL.getSentences()) {
                boolean lFirst = true;
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    if (!lFirst) {
                        lWriter.print(" ");
                    } else {
                        lFirst = false;
                    }
                    String lForm = lEntry.getWordform().replace(" ", "_").replace("/", "_");
                    String lPOS = getPredicted(lEntry).replace(" ", "_").replace("/", "_");
                    lForm = lForm.replace("\"", "╬");
                    lForm = lForm.replace("“", "╩");
                    lWriter.print(lForm + "/" + lPOS);
                }
                lWriter.println();
            }
            lWriter.close();
        }
        trainDir = getModelDirectory();
        long lStart = System.currentTimeMillis();
        ProcessBuilder lProcessBuilder = null;
        if (lInitFile == null) {
            trainParameters = python+" " + new File("bin/RDRPOSTagger/pSCRDRtagger/RDRPOSTagger.py").getAbsolutePath() + " " + "train" + " " + lTrainingInput.getAbsolutePath();
            lProcessBuilder = new ProcessBuilder(python, new File("bin/RDRPOSTagger/pSCRDRtagger/RDRPOSTagger.py").getAbsolutePath(), "train", lTrainingInput.getAbsolutePath());
        }
        else {
            trainParameters = python+" " + new File("bin/RDRPOSTagger/pSCRDRtagger/ExtRDRPOSTagger.py").getAbsolutePath() + " " + "train" + " " + lTrainingInput.getAbsolutePath()+" "+lInitFile.getAbsolutePath();
            lProcessBuilder = new ProcessBuilder(python, new File("bin/RDRPOSTagger/pSCRDRtagger/ExtRDRPOSTagger.py").getAbsolutePath(), "train", lTrainingInput.getAbsolutePath(), lInitFile.getAbsolutePath());
        }
        System.out.println(trainParameters);
        lProcessBuilder.directory(new File("bin/RDRPOSTagger/pSCRDRtagger"));
        lProcessBuilder.redirectErrorStream(true);
        Process lProcess = lProcessBuilder.start();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
        String lLine = null;
        while ((lLine = lReader.readLine()) != null) {
            System.out.println(lLine);
        }
        try {
            int lResult = lProcess.waitFor();
            if (lResult != 0) {
                throw new IOException("Exit Code is "+lResult);
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (optionalInitializationTrainCoNLLFile == null) {
            {
                File lModelFile = new File(lTrainingInput.getAbsolutePath() + ".RDR");
                if (!lModelFile.exists() || (lModelFile.length() == 0)) {
                    throw new IOException("Model File not existent or empty: " + lModelFile.getAbsolutePath());
                }
            }
            {
                File lModelFile = new File(lTrainingInput.getAbsolutePath() + ".DICT");
                if (!lModelFile.exists() || (lModelFile.length() == 0)) {
                    throw new IOException("Model File not existent or empty: " + lModelFile.getAbsolutePath());
                }
            }
        }
        else {
            {
                File lModelFile = new File(lInitFile.getAbsolutePath() + ".RDR");
                if (!lModelFile.exists() || (lModelFile.length() == 0)) {
                    throw new IOException("Model File not existent or empty: " + lModelFile.getAbsolutePath());
                }
            }
        }
        trainingTime = System.currentTimeMillis() - lStart;
        lReader.close();
    }

    public void test() throws IOException {
        File lModel = optionalInitializationTestCoNLLFile == null ? new File(getTrainDirectory().getAbsolutePath()+"/traininput.txt.RDR") : new File(getTrainDirectory().getAbsolutePath()+"/initfile.txt.RDR");
        File lLexicon = optionalInitializationTestCoNLLFile == null ? new File(getTrainDirectory().getAbsolutePath()+"/traininput.txt.DICT") : new File(getTrainDirectory().getAbsolutePath()+"/initfile.txt.DICT");
        trainDir = getModelDirectory();
        File lTestInput = new File(getTestDirectory().getAbsolutePath()+"/testinput.txt");
        List<CoNLLEntry> lTestEntries = new ArrayList<>();
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestInput), Charset.forName("UTF-8")));
            if (optionalInitializationTestCoNLLFile == null) {
                for (CoNLLSentence lSentence : getTestSet().getSentences()) {
                    boolean lFirst = true;
                    for (CoNLLEntry lEntry : lSentence.getEntries()) {
                        lTestEntries.add(lEntry);
                        if (!lFirst) {
                            lWriter.print(" ");
                        } else {
                            lFirst = false;
                        }
                        lWriter.print(lEntry.getWordform().replace(" ", "_").replace("/", "_").replace("\"", "╬").replace("“", "╩"));
                    }
                    lWriter.println();
                }
            }
            else {
                if (!optionalInitializationTestCoNLLFile.exists()) {
                    System.err.println("File not found: "+optionalInitializationTestCoNLLFile);
                }
                for (CoNLLSentence lSentence : new CoNLL(optionalInitializationTestCoNLLFile).getSentences()) {
                    boolean lFirst = true;
                    for (CoNLLEntry lEntry : lSentence.getEntries()) {
                        lTestEntries.add(lEntry);
                        if (!lFirst) {
                            lWriter.print(" ");
                        } else {
                            lFirst = false;
                        }
                        lWriter.print(lEntry.getWordform().replace(" ", "_").replace("/", "_").replace("\"", "╬").replace("“", "╩")+"/"+getPredicted(lEntry));
                    }
                    lWriter.println();
                }
            }
            lWriter.close();
        }
        long lStart = System.currentTimeMillis();
        ProcessBuilder lProcessBuilder = null;
        testParameters = new File(python).getAbsolutePath()+" -m "+trainDir.getAbsolutePath()+" "+lTestInput.getAbsolutePath();
        if (optionalInitializationTestCoNLLFile == null) {
            lProcessBuilder = new ProcessBuilder(python, new File("bin/RDRPOSTagger/pSCRDRtagger/RDRPOSTagger.py").getAbsolutePath(), "tag", lModel.getAbsolutePath(), lLexicon.getAbsolutePath(), lTestInput.getAbsolutePath());
        }
        else {
            lProcessBuilder = new ProcessBuilder(python, new File("bin/RDRPOSTagger/pSCRDRtagger/ExtRDRPOSTagger.py").getAbsolutePath(), "tag", lModel.getAbsolutePath(), lTestInput.getAbsolutePath());
        }
        lProcessBuilder.directory(new File("bin/RDRPOSTagger/pSCRDRtagger"));
        lProcessBuilder.redirectErrorStream(true);
        Process lProcess = lProcessBuilder.start();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
        String lLine = null;
        while ((lLine = lReader.readLine()) != null) {
            System.out.println(lLine);
        }
        try {
            int lResult = lProcess.waitFor();
            if (lResult != 0) {
                throw new IOException("Exit Code is "+lResult);
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        testTime = System.currentTimeMillis() - lStart;
        lReader.close();

        ArrayList<CoNLLSentence> lSentences = new ArrayList<>();
        int lEntriesCounter = 0;
        lReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(lTestInput.getAbsolutePath()+".TAGGED")), Charset.forName("UTF-8")));
        while ((lLine = lReader.readLine()) != null) {
            lLine = lLine.trim();
            String[] lEntries = lLine.split(" ", -1);
            List<CoNLLEntry> lSentenceEntries = new ArrayList<>();
            for (String lEntry:lEntries) {
                String lForm = lEntry.substring(0, lEntry.indexOf("/"));
                lForm = lForm.replace("╬", "\"");
                lForm = lForm.replace("╩", "“");
                String lPoS = lEntry.substring(lEntry.indexOf("/")+1);
                CoNLLEntry lNewEntry = new CoNLLEntry(lTestEntries.get(lEntriesCounter++));
                lNewEntry.setPredictedCategory(category, lPoS);
                lSentenceEntries.add(lNewEntry);
            }
            lSentences.add(new CoNLLSentence(lSentenceEntries));
        }
        lReader.close();
        resultSet = new CoNLL(lSentences);
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
    }

}
