package org.hucompute.nlpevalframework;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class MateExperiment extends Experiment {

    private String trainParameters = null;
    private String testParameters = null;
    protected File modelFile;

    public MateExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage) {
        super(pResultBaseDir, Tool.Mate, pCategory, pTrainingSet, pTestSet, pLanguage);
    }

    public MateExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, List<Category> pJointCategories) {
        super(pResultBaseDir, Tool.Mate, pCategory, pTrainingSet, pTestSet, pLanguage, pJointCategories);
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

    @Override
    public void train() throws Exception {
        File lTrainInputFile = new File((getTrainDirectory().getAbsolutePath()+"/traininput.txt").replace("\\", "/"));
        CoNLL lTrain = new CoNLL(getTrainingSet());
        for (CoNLLSentence lSentence:lTrain.getSentences()) {
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                lEntry.setGoldCategory(Category.pos, getGold(lEntry));
                lEntry.setPredictedLemma("_");
                lEntry.setGoldLemma("_");
            }
        }
        lTrain.write(lTrainInputFile);
        modelFile = new File((getModelDirectory().getAbsolutePath()+"/model.txt").replace("\\", "/"));
        trainParameters = "-train "+lTrainInputFile.getAbsolutePath()+" -model "+modelFile.getAbsolutePath();
        long lStart = System.currentTimeMillis();
        is2.tag.Tagger.main(new String[]{"-train", lTrainInputFile.getAbsolutePath(), "-model", modelFile.getAbsolutePath()});
        trainingTime = System.currentTimeMillis() - lStart;
    }

    @Override
    public void test() throws Exception {
        modelFile = new File((getModelDirectory().getAbsolutePath()+"/model.txt").replace("\\", "/"));
        CoNLL lTrain = new CoNLL(getTestSet());
        for (CoNLLSentence lSentence:lTrain.getSentences()) {
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                lEntry.setPredictedLemma("_");
                lEntry.setGoldLemma("_");
            }
        }

        File lTestInputFile = new File((getTestDirectory().getAbsolutePath()+"/testinput.txt").replace("\\", "/"));
        lTrain.write(lTestInputFile);
        File lOutputFile = new File((getTestDirectory().getAbsolutePath()+"/testoutput.txt").replace("\\", "/"));

        testParameters = "-model "+modelFile.getAbsolutePath()+" -test "+lTestInputFile.getAbsolutePath()+" -out "+lOutputFile.getAbsolutePath();
        long lStart = System.currentTimeMillis();
        is2.tag.Tagger.main(new String[]{"-model", modelFile.getAbsolutePath(), "-test", lTestInputFile.getAbsolutePath(), "-out", lOutputFile.getAbsolutePath()});
        testTime = System.currentTimeMillis() - lStart;

        resultSet = new CoNLL(lOutputFile);
        for (CoNLLSentence lSentence:resultSet.getSentences()) {
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                lEntry.setPredictedCategory(category, lEntry.getPredictedCategory(Category.pos));
            }
        }
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
    }
}
