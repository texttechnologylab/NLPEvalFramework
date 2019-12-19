package org.hucompute.nlpevalframework;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.util.*;

public class MajorityVoteExperiment extends Experiment {

    private File trainDir = null;
    private String trainParameters = null;
    private String testParameters = null;
    private List<Tool> majorityVoteTools = null;

    public MajorityVoteExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pEvaluationSet, String pLanguage, List<Tool> pMajorityVoteTools, String pVariant) {
        super(pResultBaseDir, Tool.MajorityVote, pCategory, pTrainingSet, pEvaluationSet, pLanguage, pVariant);
        majorityVoteTools = pMajorityVoteTools;
    }

    public MajorityVoteExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pEvaluationSet, String pLanguage, List<Category> pJointCategories, List<Tool> pMajorityVoteTools, String pVariant) {
        super(pResultBaseDir, Tool.MajorityVote, pCategory, pTrainingSet, pEvaluationSet, pLanguage, pVariant, pJointCategories);
        if (jointCategories != null) Collections.sort(jointCategories);
        majorityVoteTools = pMajorityVoteTools;
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

    public void train() throws IOException {
    }

    public void test() throws IOException {
        long lStart = System.currentTimeMillis();
        List<Map<Tool, String>> lEntries = new ArrayList<>();
        boolean lFirst = true;
        CoNLL lResult = null;
        for (Tool lTool:majorityVoteTools) {
            File lToolResultFile = new File((resultBaseDir.getAbsolutePath()+"/"+trainingSet.getFile().getName()+"/"+testSet.getFile().getName()+"/"+ lTool.name()+"/"+category.name()+"/"+trainingSet.getFile().getName()+"_"+testSet.getFile().getName()+"_"+ lTool.name()+"_"+category.name()+".conll").replace("\\", "/"));
            if (!lToolResultFile.exists()) throw new IOException("Missing file for "+lTool+": "+lToolResultFile.getAbsolutePath());
            CoNLL lCoNLL = new CoNLL(lToolResultFile);
            if (lFirst) {
                lResult = new CoNLL(lCoNLL);
            }
            int i=0;
            for (CoNLLSentence lSentence:lCoNLL.getSentences()) {
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    if (lFirst) {
                        Map<Tool, String> lMap = new HashMap<>();
                        lMap.put(lTool, lEntry.getPredictedCategory(category));
                        lEntries.add(lMap);
                    }
                    else {
                        lEntries.get(i).put(lTool, lEntry.getPredictedCategory(category));
                    }
                    i++;
                }
            }
            lFirst = false;
        }
        int i = 0;
        TObjectIntHashMap<Tool> lToolRankMap = new TObjectIntHashMap<>();
        for (int k=0; k<majorityVoteTools.size(); k++) {
            lToolRankMap.put(majorityVoteTools.get(k), majorityVoteTools.size()-k);
        }
        for (CoNLLSentence lSentence:lResult.getSentences()) {
            for (CoNLLEntry lEntry : lSentence.getEntries()) {
                Map<Tool, String> lMap = lEntries.get(i);
                Set<String> lValues = new HashSet<>(lMap.values());
                if (lValues.size() == 0) {
                    lEntry.setPredictedCategory(category, "_");
                }
                else if (lValues.size() == 1) {
                    lEntry.setPredictedCategory(category, lValues.iterator().next());
                }
                else {
                    TObjectIntHashMap<String> lFreqMap = new TObjectIntHashMap<>();
                    for (Map.Entry<Tool, String> lValue:lMap.entrySet()) {
                        lFreqMap.adjustOrPutValue(lValue.getValue(), 1, 1);
                    }
                    List<String> lKeys = new ArrayList<>(lFreqMap.keySet());
                    lKeys.sort(new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            int lResult = Integer.compare(lFreqMap.get(o2), lFreqMap.get(o1));
                            if (lResult == 0) {
                                int lSum1 = 0;
                                int lSum2 = 0;
                                for (Map.Entry<Tool, String> lEntry:lMap.entrySet()) {
                                    if (lEntry.getValue().equals(o1)) {
                                        lSum1 += lToolRankMap.get(lEntry.getKey());
                                    }
                                    if (lEntry.getValue().equals(o2)) {
                                        lSum2 += lToolRankMap.get(lEntry.getKey());
                                    }
                                }
                                lResult = Integer.compare(lSum2, lSum1);
                            }
                            return lResult;
                        }
                    });
                    lEntry.setPredictedCategory(category, lKeys.get(0));
                }
                i++;
            }
        }
        testTime = System.currentTimeMillis() - lStart;
        resultSet = lResult;
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
    }

}
