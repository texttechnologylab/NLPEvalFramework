package org.hucompute.nlpevalframework;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LaposExperiment extends Experiment {

    public static boolean COMMAPATCH = true; // Fix problem that , is tagged as $(

    private File trainDir = null;
    private String trainParameters = null;
    private String testParameters = null;

    public LaposExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pEvaluationSet, String pLanguage) {
        super(pResultBaseDir, Tool.Lapos, pCategory, pTrainingSet, pEvaluationSet, pLanguage);
    }

    public LaposExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pEvaluationSet, String pLanguage, List<Category> pJointCategories) {
        super(pResultBaseDir, Tool.Lapos, pCategory, pTrainingSet, pEvaluationSet, pLanguage, pJointCategories);
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

    @Override
    public void trainJackKnifing(int pFolds) throws Exception {
        throw new Exception("Not Implemented");
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
                    lWriter.print(lForm + "/" + lPOS);
                }
                lWriter.println();
            }
            lWriter.close();
        }
        trainDir = getModelDirectory();
        long lStart = System.currentTimeMillis();
        trainParameters = new File("bin/Lapos/lapos-learn").getAbsolutePath()+" -m "+trainDir.getAbsolutePath()+" "+lTrainingInput.getAbsolutePath();
        ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/Lapos/lapos-learn").getAbsolutePath(), "-m", trainDir.getAbsolutePath(), lTrainingInput.getAbsolutePath());
        System.out.println(new File("bin/Lapos/lapos-learn").getAbsolutePath()+" -m "+trainDir.getAbsolutePath()+" "+lTrainingInput.getAbsolutePath());
        lProcessBuilder.directory(new File("bin/Lapos"));
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
        File lModelFile = new File(trainDir.getAbsolutePath()+"/model.la");
        if (!lModelFile.exists() || (lModelFile.length()==0)) {
            throw new IOException("Model File not existent or empty: "+lModelFile.getAbsolutePath());
        }
        trainingTime = System.currentTimeMillis() - lStart;
        lReader.close();
    }

    public void test() throws IOException {
        trainDir = getModelDirectory();
        File lTestInput = new File(getTestDirectory().getAbsolutePath()+"/testinput.txt");
        List<CoNLLEntry> lTestEntries = new ArrayList<>();
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestInput), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTestSet().getSentences()) {
                boolean lFirst = true;
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    lTestEntries.add(lEntry);
                    if (!lFirst) {
                        lWriter.print(" ");
                    } else {
                        lFirst = false;
                    }
                    lWriter.print(lEntry.getWordform().replace(" ", "_").replace("/", "_"));
                }
                lWriter.println();
            }
            lWriter.close();
        }
        long lStart = System.currentTimeMillis();
        testParameters = new File("bin/Lapos/lapos").getAbsolutePath()+" -m "+trainDir.getAbsolutePath()+" "+lTestInput.getAbsolutePath();
        ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/Lapos/lapos").getAbsolutePath(), "-m", trainDir.getAbsolutePath(), lTestInput.getAbsolutePath());
        lProcessBuilder.directory(new File("bin/Lapos"));
        lProcessBuilder.redirectErrorStream(true);
        Process lProcess = lProcessBuilder.start();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
        String lLine = null;
        boolean lDataEnabled = false;

        ArrayList<CoNLLSentence> lSentences = new ArrayList<>();
        int lEntriesCounter = 0;
        while ((lLine = lReader.readLine()) != null) {
            if (lDataEnabled) {
                lLine = lLine.trim();
                String[] lEntries = lLine.split(" ", -1);
                List<CoNLLEntry> lSentenceEntries = new ArrayList<>();
                for (String lEntry:lEntries) {
                    String lForm = lEntry.substring(0, lEntry.indexOf("/"));
                    String lPoS = lEntry.substring(lEntry.indexOf("/")+1);
                    if (COMMAPATCH && lForm.equals(",")) {
                        lPoS = "$,";
                    }
                    CoNLLEntry lNewEntry = new CoNLLEntry(lTestEntries.get(lEntriesCounter++));
                    lNewEntry.setPredictedCategory(category, lPoS);
                    lSentenceEntries.add(lNewEntry);
                }
                lSentences.add(new CoNLLSentence(lSentenceEntries));
            }
            if (!lDataEnabled && lLine.equals("done")) {
                lDataEnabled = true;
            }
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
        resultSet = new CoNLL(lSentences);
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
    }

}
