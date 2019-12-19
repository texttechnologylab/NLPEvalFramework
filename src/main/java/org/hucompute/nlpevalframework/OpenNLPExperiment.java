package org.hucompute.nlpevalframework;

import opennlp.tools.cmdline.CLI;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenNLPExperiment extends Experiment {

    protected File modelFile;
    private String trainParameters = null;
    private String testParameters = null;

    public OpenNLPExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage) {
        super(pResultBaseDir, Tool.OpenNLP, pCategory, pTrainingSet, pTestSet, pLanguage);
    }

    public OpenNLPExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, List<Category> pJointCategories) {
        super(pResultBaseDir, Tool.OpenNLP, pCategory, pTrainingSet, pTestSet, pLanguage, pJointCategories);
        if (jointCategories != null) Collections.sort(jointCategories);
    }

    @Override
    public void trainJackKnifing(int pFolds) throws Exception {
        throw new Exception("Not Implemented");
    }

    public String getLanguage() {
        return language;
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
        File lTrainInputFile = new File(getTrainDirectory().getAbsolutePath()+"/traininput.txt");
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainInputFile), Charset.forName("UTF-8")));
        for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
            boolean lFirst = true;
            for (CoNLLEntry lEntry : lSentence.getEntries()) {
                if (!lFirst) {
                    lWriter.print(" ");
                } else {
                    lFirst = false;
                }
                lWriter.print(lEntry.getWordform().replace(" ", "~").replace("_", "~") + "_" + getGold(lEntry).replace(" ", "~").replace("_", "~"));
            }
            lWriter.println();
        }
        lWriter.close();
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model.txt");
        trainParameters = "POSTaggerTrainer -type maxent -model "+modelFile.getAbsolutePath()+" -lang "+language+" -data "+lTrainInputFile.getAbsolutePath()+" -encoding UTF-8";
        long lStart = System.currentTimeMillis();
        CLI.main(new String[]{"POSTaggerTrainer", "-type", "maxent", "-model", modelFile.getAbsolutePath(), "-lang", language, "-data", lTrainInputFile.getAbsolutePath(), "-encoding", "UTF-8"});
        trainingTime = System.currentTimeMillis() - lStart;
    }

    @Override
    public void test() throws Exception {
        long lStart = System.currentTimeMillis();
        POSModel lPOSModel = new POSModel(modelFile);
        POSTaggerME lPOSTaggerME = new POSTaggerME(lPOSModel);

        List<CoNLLSentence> lResultSentences = new ArrayList<>();

        for (CoNLLSentence lSentence : getTestSet().getSentences()) {
            String[] lInputArray = new String[lSentence.getEntries().length];
            for (int i=0; i<lInputArray.length; i++) {
                lInputArray[i] = lSentence.getEntries()[i].getWordform().replace("_","~");
            }
            String[] lOutputArray = lPOSTaggerME.tag(lInputArray);
            List<CoNLLEntry> lEntries = new ArrayList<>();
            for (int i=0; i<lOutputArray.length; i++) {
                String lForm = lInputArray[i];
                String lPoS = lOutputArray[i].replace("~","_");
                CoNLLEntry lNewEntry = new CoNLLEntry(lSentence.getEntries()[i]);
                lNewEntry.setPredictedCategory(category, lPoS);
                lEntries.add(lNewEntry);
            }
            CoNLLSentence lResultSentence = new CoNLLSentence(lEntries);
            lResultSentences.add(lResultSentence);
        }
        testTime = System.currentTimeMillis() - lStart;
        resultSet = new CoNLL(lResultSentences);
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
    }

}
