package org.hucompute.nlpevalframework;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LemmaTagExperiment extends Experiment {

    private String trainParameters = null;
    private String testParameters = null;
    private File modelFile;
    private Map<String, String> extendedParameterMap;

    public LemmaTagExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, Map<String, String> pExtendedParameterMap, String pVariant) {
        super(pResultBaseDir, Tool.MarMoT, pCategory, pTrainingSet, pTestSet, pLanguage, pVariant);
        extendedParameterMap = pExtendedParameterMap;
    }

    public LemmaTagExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, List<Category> pJointCategories, Map<String, String> pExtendedParameterMap, String pVariant) {
        super(pResultBaseDir, Tool.MarMoT, pCategory, pTrainingSet, pTestSet, pLanguage, pVariant, pJointCategories);
        if (jointCategories != null) Collections.sort(jointCategories);
        extendedParameterMap = pExtendedParameterMap;
    }

    @Override
    public void train() throws Exception {

    }

    @Override
    public void test() throws Exception {

    }

    @Override
    public void trainJackKnifing(int pFolds) throws Exception {

    }

    @Override
    public String getTrainParameters() {
        return null;
    }

    @Override
    public String getTestParameters() {
        return null;
    }
}
