package org.hucompute.nlpevalframework;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PipelineExperiment extends Experiment {

    private List<Category> pipelineCategories;
    private Map<String, String> extendedParameterMap;
    private List<Tool> majorityVoteTools;
    private boolean outOfVocabularyOnly;
    private boolean forceTraining;

    public PipelineExperiment(File pResultBaseDir, Tool pTool, List<Category> pPipelineCategories, CoNLL pTrainingSet, CoNLL pEvaluationSet, String pLanguage, Map<String, String> pExtendedParameterMap, List<Tool> pMajorityVoteTools, String pVariant, boolean pOutOfVocabularyOnly, boolean pForceTraining) {
        super(pResultBaseDir, pTool, Category.pipeline, pTrainingSet, pEvaluationSet, pLanguage, pVariant);
        extendedParameterMap = pExtendedParameterMap;
        pipelineCategories = pPipelineCategories;
        majorityVoteTools = pMajorityVoteTools;
        outOfVocabularyOnly = pOutOfVocabularyOnly;
        forceTraining = pForceTraining;
    }

    @Override
    public void trainJackKnifing(int pFolds) throws Exception {
        throw new Exception("Not Implemented");
    }

    @Override
    public void train() throws Exception {
        for (Category lCategory:pipelineCategories) {
            if (!Evaluation.exists(resultBaseDir, tool, lCategory, trainingSet, testSet, variant) || forceTraining) {
                System.out.println("Computing Missing Case: "+tool.name() + " " + lCategory.name() + " " + getTrainingSet().getFile().getName() + " " + getTestSet().getFile().getName());
                switch (tool) {
                    case Lapos: {
                        new LaposExperiment(resultBaseDir, lCategory, trainingSet, testSet, language).execute();
                        break;
                    }
                    case Mate: {
                        new MateExperiment(resultBaseDir, lCategory, trainingSet, testSet, language).execute();
                        break;
                    }
                    case OpenNLP: {
                        new OpenNLPExperiment(resultBaseDir, lCategory, trainingSet, testSet, language).execute();
                        break;
                    }
                    case Stanford: {
                        new StanfordExperiment(resultBaseDir, lCategory, trainingSet, testSet, language).execute();
                        break;
                    }
                    case TreeTagger: {
                        new TreeTaggerExperiment(resultBaseDir, lCategory, trainingSet, testSet, language).execute();
                        break;
                    }
                    case RDRPOSTagger: {
                        new RDRPOSTaggerExperiment(resultBaseDir, lCategory, trainingSet, testSet, language, extendedParameterMap, variant).execute();
                        break;
                    }
                    case TnT: {
                        new TnTExperiment(resultBaseDir, lCategory, trainingSet, testSet, language).execute();
                        break;
                    }
                    case FLORS: {
                        new FLORSExperiment(resultBaseDir, lCategory, trainingSet, testSet, language, extendedParameterMap).execute();
                        break;
                    }
                    case NonLexNN: {
                        new NonLexNNExperiment(resultBaseDir, lCategory, trainingSet, testSet, language).execute();
                        break;
                    }
                    case MarMoT: {
                        new MarMoTExperiment(resultBaseDir, lCategory, trainingSet, testSet, language, extendedParameterMap, variant).execute();
                        break;
                    }
                    case BLSTMRNN: {
                        new BLSTMRNNExperiment(resultBaseDir, lCategory, trainingSet, testSet, language, extendedParameterMap, variant).execute();
                        break;
                    }
                    case FastText: {
                        new FastTextExperiment(resultBaseDir, lCategory, trainingSet, testSet, language).execute();
                        break;
                    }
                    case MajorityVote: {
                        new MajorityVoteExperiment(resultBaseDir, lCategory, trainingSet, testSet, language, majorityVoteTools, variant).execute();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void test() throws Exception {
        // Take TestSet as base and add predicted data from the specific cases
        resultSet = new CoNLL(getTestSet());
        resultSet.setFile(getResultConLLFile());
        // Paranoid reset- should not be necessary
        for (int i=0; i<resultSet.getSentences().size(); i++) {
            for (int k = 0; k < resultSet.getSentences().get(i).getEntries().length; k++) {
                resultSet.getSentences().get(i).getEntries()[k].setPredictedLemma("_");
                resultSet.getSentences().get(i).getEntries()[k].setPredictedCategory(Category.pos, "_");
                resultSet.getSentences().get(i).getEntries()[k].setPredictedMorphology("_");
            }
        }

        for (Category lCategory : pipelineCategories) {
            if (!Evaluation.exists(resultBaseDir, tool, lCategory, trainingSet, testSet, variant)) {
                throw new IOException("Missing Evaluation: "+tool.name()+", "+lCategory+", "+variant);
            }
            // Load Results of that specific case
            Evaluation lEvaluation = new Evaluation(resultBaseDir, tool, lCategory, trainingSet, testSet, null, variant, outOfVocabularyOnly);
            for (int i=0; i<resultSet.getSentences().size(); i++) {
                for (int k=0; k<resultSet.getSentences().get(i).getEntries().length; k++) {
                    switch (lCategory) {
                        case lemma: {
                            resultSet.getSentences().get(i).getEntries()[k].setPredictedLemma(lEvaluation.getResultSet().getSentences().get(i).getEntries()[k].getPredictedLemma());
                            break;
                        }
                        case pos: {
                            resultSet.getSentences().get(i).getEntries()[k].setPredictedCategory(Category.pos, lEvaluation.getResultSet().getSentences().get(i).getEntries()[k].getPredictedCategory(Category.pos));
                            break;
                        }
                        case joint: {
                            break;
                        }
                        case pipeline: {
                            break;
                        }
                        default: {
                            String lValue = lEvaluation.getResultSet().getSentences().get(i).getEntries()[k].getPredictedCategory(lCategory);
                            if (!lValue.equals("_") && !lValue.equals("")) {
                                if (lValue.contains("|")) {
                                    throw new Exception("Predicted Value in Pipeline contains a |: "+lValue);
                                }
                                if (lValue.contains("=")) {
                                    throw new Exception("Predicted Value in Pipeline contains a =: "+lValue);
                                }
                                String lExistingValue = resultSet.getSentences().get(i).getEntries()[k].getPredictedMorphology();
                                StringBuilder lMorphology = new StringBuilder();
                                if (!lExistingValue.equals("_") && !lExistingValue.equals("")) {
                                    lMorphology.append(lExistingValue);
                                }
                                if (lMorphology.length() > 0) lMorphology.append("|");
                                if (lCategory.name().equals(Category.case_.name())) {
                                    if (lMorphology.indexOf("case=") > -1) {
                                        throw new Exception("Duplicate Field");
                                    }
                                    lMorphology.append("case=");
                                } else {
                                    if (lMorphology.indexOf(lCategory.name()+"=") > -1) {
                                        throw new Exception("Duplicate Field");
                                    }
                                    lMorphology.append(lCategory.name() + "=");
                                }
                                lMorphology.append(lValue);
                                resultSet.getSentences().get(i).getEntries()[k].setPredictedMorphology(lMorphology.toString());
                            }
                            break;
                        }
                    }
                }
            }
        }
        resultSet.write();
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
