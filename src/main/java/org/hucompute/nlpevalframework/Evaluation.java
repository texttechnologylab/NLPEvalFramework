package org.hucompute.nlpevalframework;

import com.sun.istack.internal.Nullable;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import static org.hucompute.nlpevalframework.Experiment.Category.joint_lemma;
import static org.hucompute.nlpevalframework.Experiment.Category.lemma_pos;

public class Evaluation {

    private int tokenCount;
    private int matchTokenCount;
    private List<StringPairFrequency> mismatchingPairFrequencies;
    private List<StringPairFrequency> mismatchingPairFormFrequencies;
    private CoNLL testSet;
    private CoNLL trainingSet;
    private CoNLL resultSet;
    private Experiment.Category category;
    private Experiment.Tool tool;
    private Experiment.Tool lemmatizer;
    private Map<String, TObjectIntHashMap<String>> matchMap;
    private String variant;
    private File resultsBaseDir;
    private boolean outOfVocabularyOnly = false;

    public Evaluation(File pResultsBaseDir, Experiment.Tool pTool, Experiment.Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, @Nullable Experiment.Tool pLemmatizerTool, String pVariant, boolean pOutOfVocabularyOnly) throws IOException {
        resultsBaseDir = pResultsBaseDir;
        tool = pTool;
        category = pCategory;
        trainingSet = pTrainingSet;
        testSet = pTestSet;
        lemmatizer = pLemmatizerTool;
        variant = pVariant;
        outOfVocabularyOnly = pOutOfVocabularyOnly;
        if (!getResultConLLFile().exists()) {
            throw new IOException("Experiment-Directory does not exist: "+getResultConLLFile().getAbsolutePath());
        }
        resultSet = new CoNLL(getResultConLLFile());
    }

    public String getVariant() {
        return variant;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public double getAccuracy() {
        return matchTokenCount/(double)tokenCount;
    }

    public int getMatchTokenCount() {
        return matchTokenCount;
    }

    public List<StringPairFrequency> getMismatchingPairFrequencies() {
        return mismatchingPairFrequencies;
    }

    public CoNLL getTestSet() {
        return testSet;
    }

    public CoNLL getTrainingSet() {
        return trainingSet;
    }

    public CoNLL getResultSet() {
        return resultSet;
    }

    public Experiment.Category getCategory() {
        return category;
    }

    public Experiment.Tool getTool() {
        return tool;
    }

    public static boolean exists(File pResultsBaseDir, Experiment.Tool pTool, Experiment.Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pVariant) {
        File lDirectory = null;
        if ((pVariant == null) || (pVariant.length() == 0)) {
            lDirectory = new File((pResultsBaseDir.getAbsolutePath()+File.separator+pTrainingSet.getFile().getName()+File.separator+pTestSet.getFile().getName()+File.separator+ pTool.name()+File.separator+pCategory.name()).replace("\\", "/"));
        }
        else {
            lDirectory = new File((pResultsBaseDir.getAbsolutePath()+File.separator+pTrainingSet.getFile().getName()+File.separator+pTestSet.getFile().getName()+File.separator+ pTool.name()+File.separator+pCategory.name()+File.separator+pVariant).replace("\\", "/"));
        }
        if (!lDirectory.exists()) return false;
        return new File((lDirectory.getAbsolutePath()+"/"+pTrainingSet.getFile().getName()+"_"+pTestSet.getFile().getName()+"_"+ pTool.name()+"_"+pCategory.name()+".conll").replace("\\", "/")).exists();
    }

    public static boolean exists(File pResultsBaseDir, Experiment.Tool pTool, Experiment.Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, Experiment.Tool pLemmatizer, String pVariant) {
        File lDirectory;
        if ((pVariant == null) || (pVariant.length() == 0)) {
            lDirectory = new File((pResultsBaseDir.getAbsolutePath()+File.separator+pTrainingSet.getFile().getName()+"/"+pTestSet.getFile().getName()+"/"+ pTool.name()+"/"+pCategory.name()+"/"+pLemmatizer.name()).replace("\\", "/"));
        }
        else {
            lDirectory = new File((pResultsBaseDir.getAbsolutePath()+File.separator+pTrainingSet.getFile().getName()+"/"+pTestSet.getFile().getName()+"/"+ pTool.name()+"/"+pCategory.name()+"/"+pVariant+"/"+pLemmatizer.name()).replace("\\", "/"));
        }
        if (!lDirectory.exists()) return false;
        return new File(lDirectory.getAbsolutePath()+"/"+lDirectory.getParentFile().getParentFile().getParentFile().getParentFile().getName()
                +"_"+lDirectory.getParentFile().getParentFile().getParentFile().getName()
                +"_"+lDirectory.getParentFile().getParentFile().getName()
                +"_"+lDirectory.getParentFile().getName()
                +"_"+lDirectory.getName()+".conll").exists();
    }

    public File getDirectory() {
        File lResult = null;
        if (lemmatizer == null) {
            if ((variant == null) || (variant.length() == 0)) {
                if (category.equals(joint_lemma)) {
                    lResult = new File((resultsBaseDir.getAbsolutePath()+File.separator+trainingSet.getFile().getName() + "/" + testSet.getFile().getName() + "/" + tool.name() + "/" + Experiment.Category.joint.name()).replace("\\", "/"));
                }
                else if (category.equals(lemma_pos)) {
                    lResult = new File((resultsBaseDir.getAbsolutePath()+File.separator+trainingSet.getFile().getName() + "/" + testSet.getFile().getName() + "/" + tool.name() + "/" + Experiment.Category.pos.name()).replace("\\", "/"));
                }
                else {
                    lResult = new File((resultsBaseDir.getAbsolutePath()+File.separator+trainingSet.getFile().getName() + "/" + testSet.getFile().getName() + "/" + tool.name() + "/" + category.name()).replace("\\", "/"));
                }
            }
            else {
                lResult = new File((resultsBaseDir.getAbsolutePath()+File.separator+trainingSet.getFile().getName() + "/" + testSet.getFile().getName() + "/" + tool.name() + "/" + category.name()+"/"+variant).replace("\\", "/"));
            }
        }
        else {
            if ((variant == null) || (variant.length() == 0)) {
                lResult = new File((resultsBaseDir.getAbsolutePath()+File.separator+trainingSet.getFile().getName() + "/" + testSet.getFile().getName() + "/" + tool.name() + "/" + category.name() + "/" + lemmatizer.name()+"/lemma").replace("\\", "/"));
            }
            else {
                lResult = new File((resultsBaseDir.getAbsolutePath()+File.separator+trainingSet.getFile().getName() + "/" + testSet.getFile().getName() + "/" + tool.name() + "/" + category.name() + "/" + variant+"/"+lemmatizer.name()+"/lemma").replace("\\", "/"));
            }
        }
        return lResult;
    }

    public File getResultConLLFile() {
        if (lemmatizer == null) {
            if (category.equals(Experiment.Category.joint_lemma)) {
                return new File((getDirectory().getAbsolutePath() + "/" + trainingSet.getFile().getName() + "_" + testSet.getFile().getName() + "_" + tool.name() + "_" + Experiment.Category.joint.name() + ".conll").replace("\\", "/"));
            }
            else if (category.equals(Experiment.Category.lemma_pos)) {
                return new File((getDirectory().getAbsolutePath() + "/" + trainingSet.getFile().getName() + "_" + testSet.getFile().getName() + "_" + tool.name() + "_" + Experiment.Category.pos.name() + ".conll").replace("\\", "/"));
            }
            else {
                return new File((getDirectory().getAbsolutePath() + "/" + trainingSet.getFile().getName() + "_" + testSet.getFile().getName() + "_" + tool.name() + "_" + category.name() + ".conll").replace("\\", "/"));
            }
        }
        else {
            File lDir = getDirectory();
            if ((variant == null) || (variant.length() == 0)) {
                return new File((getDirectory().getAbsolutePath() + "/" + trainingSet.getFile().getName() + "_" + testSet.getFile().getName() + "_" + lemmatizer.name() + "_lemma.conll").replace("\\", "/"));
            }
            else {
                return new File((getDirectory().getAbsolutePath() + "/" + trainingSet.getFile().getName() + "_" + testSet.getFile().getName() + "_" + lemmatizer.name() + "_lemma.conll").replace("\\", "/"));
            }
        }
    }

    public void evaluate() throws IOException {
        Experiment.Category lCategory = category;
        if (lemmatizer != null) {
            category = Experiment.Category.lemma;
        }
        tokenCount = 0;
        matchTokenCount = 0;
        mismatchingPairFrequencies = new ArrayList<>();
        mismatchingPairFormFrequencies = new ArrayList<>();
        matchMap = new HashMap<>();
        TObjectIntHashMap<String> mismatchingPairFrequencyMap = new TObjectIntHashMap<>();
        TObjectIntHashMap<String> mismatchingPairFrequencyFormMap = new TObjectIntHashMap<>();
        Set<String> lTrainingWordforms = new HashSet<>();
        for (CoNLLSentence lSentence:trainingSet.getSentences()) {
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                lTrainingWordforms.add(lEntry.getWordform());
            }
        }
        for (int i=0; i<resultSet.getSentences().size(); i++) {
            CoNLLSentence lResultSentence = resultSet.getSentences().get(i);
            for (int k=0; k<lResultSentence.getEntries().length; k++) {
                if (!outOfVocabularyOnly || !lTrainingWordforms.contains(lResultSentence.getEntries()[k].getWordform())) {
                    tokenCount++;
                    /*if (!lTestSentence.getEntries()[k].getWordform().equals(lResultSentence.getEntries()[k].getWordform())) {
                        throw new IOException("Wordform mismatch: "+lTestSentence.getEntries()[k].getWordform()+" vs "+lResultSentence.getEntries()[k].getWordform());
                    }*/
                    switch (category) {
                        case pipeline:
                        case joint: {
                            String lGold = lResultSentence.getEntries()[k].getGoldMorphology();
                            if (lGold.length() == 0) lGold = "_";
                            String lPredicted = lResultSentence.getEntries()[k].getPredictedMorphology();
                            if (lPredicted.length() == 0) lPredicted = "_";
                            // The ordering of the MorphologyString may be different...
                            String[] lGoldFields = lGold.equals("_") ? new String[0] : lGold.split("\\|", -1);
                            String[] lPredictedFields = lPredicted.equals("_") ? new String[0] : lPredicted.split("\\|", -1);
                            Arrays.sort(lGoldFields);
                            Arrays.sort(lPredictedFields);
                            boolean lIsEqual = lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.pos).equals(lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.pos));
                            StringBuilder lGoldNormalized = new StringBuilder("pos=" + lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.pos));
                            StringBuilder lPredictedNormalized = new StringBuilder("pos=" + lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.pos));
                            if (lIsEqual) {
                                Map<String, String> lGoldMap = new HashMap<>();
                                Map<String, String> lPredictedMap = new HashMap<>();
                                for (String lString : lGoldFields) {
                                    String lKey = lString.substring(0, lString.indexOf("=")).toLowerCase();
                                    if (lKey.equals("joint")) continue;
                                    String lValue = lString.substring(lString.indexOf("=") + 1);
                                    if (lValue.length() == 0) lValue = "_";
                                    if (!lValue.equals("_")) {
                                        lGoldMap.put(lKey, lValue);
                                    }
                                }
                                for (String lString : lPredictedFields) {
                                    String lKey = lString.substring(0, lString.indexOf("=")).toLowerCase();
                                    if (lKey.equals("joint")) continue;
                                    String lValue = lString.substring(lString.indexOf("=") + 1);
                                    if (lValue.length() == 0) lValue = "_";
                                    if (!lValue.equals("_")) {
                                        lPredictedMap.put(lKey, lValue);
                                    }
                                }
                                {
                                    List<String> lKeys = new ArrayList<>(lGoldMap.keySet());
                                    Collections.sort(lKeys);
                                    for (String lKey : lKeys) {
                                        String lValue = lGoldMap.get(lKey);
                                        if ((lValue.length() > 0) && (!lValue.equals("_"))) {
                                            lGoldNormalized.append("\t" + lKey + "=" + lValue);
                                        }
                                    }
                                }
                                {
                                    List<String> lKeys = new ArrayList<>(lPredictedMap.keySet());
                                    Collections.sort(lKeys);
                                    for (String lKey : lKeys) {
                                        String lValue = lPredictedMap.get(lKey);
                                        if ((lValue.length() > 0) && (!lValue.equals("_"))) {
                                            lPredictedNormalized.append("\t" + lKey + "=" + lValue);
                                        }
                                    }
                                }
                                if (lGoldMap.size() == lPredictedMap.size()) {
                                    for (Map.Entry<String, String> lEntry : lGoldMap.entrySet()) {
                                        if (!lPredictedMap.containsKey(lEntry.getKey())) {
                                            lIsEqual = false;
                                            break;
                                        }
                                        if (!lPredictedMap.get(lEntry.getKey()).equals(lEntry.getValue())) {
                                            lIsEqual = false;
                                            break;
                                        }
                                    }
                                } else {
                                    lIsEqual = false;
                                }
                            }
                            if (!matchMap.containsKey(lGoldNormalized.toString()))
                                matchMap.put(lGoldNormalized.toString(), new TObjectIntHashMap<>());
                            matchMap.get(lGoldNormalized.toString()).adjustOrPutValue(lPredictedNormalized.toString(), 1, 1);
                            if (lIsEqual) {
                                matchTokenCount++;
                            } else {
                                mismatchingPairFrequencyMap.adjustOrPutValue("pos=" + lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.pos) + "|" + lResultSentence.getEntries()[k].getGoldMorphology() + "\tpos=" + lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.pos) + "|" + lResultSentence.getEntries()[k].getPredictedMorphology(), 1, 1);
                                mismatchingPairFrequencyFormMap.adjustOrPutValue(lResultSentence.getEntries()[k].getWordform() + "(pos=" + lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.pos) + "|" + lResultSentence.getEntries()[k].getGoldMorphology() + ")\t" + lResultSentence.getEntries()[k].getWordform() + "(pos=" + lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.pos) + "|" + lResultSentence.getEntries()[k].getPredictedMorphology() + ")", 1, 1);
                            }
                            break;
                        }
                        case joint_lemma: {
                            String lGold = lResultSentence.getEntries()[k].getGoldMorphology();
                            if (lGold.length() == 0) lGold = "_";
                            String lPredicted = lResultSentence.getEntries()[k].getPredictedMorphology();
                            if (lPredicted.length() == 0) lPredicted = "_";
                            // The ordering of the MorphologyString may be different...
                            String[] lGoldFields = lGold.equals("_") ? new String[0] : lGold.split("\\|", -1);
                            String[] lPredictedFields = lPredicted.equals("_") ? new String[0] : lPredicted.split("\\|", -1);
                            Arrays.sort(lGoldFields);
                            Arrays.sort(lPredictedFields);
                            boolean lIsEqual = lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.pos).equals(lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.pos));
                            StringBuilder lGoldNormalized = new StringBuilder("pos=" + lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.pos));
                            StringBuilder lPredictedNormalized = new StringBuilder("pos=" + lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.pos));
                            if (lIsEqual) {
                                Map<String, String> lGoldMap = new HashMap<>();
                                Map<String, String> lPredictedMap = new HashMap<>();
                                for (String lString : lGoldFields) {
                                    String lKey = lString.substring(0, lString.indexOf("=")).toLowerCase();
                                    if (lKey.equals("joint")) continue;
                                    String lValue = lString.substring(lString.indexOf("=") + 1);
                                    if (lValue.length() == 0) lValue = "_";
                                    if (!lValue.equals("_")) {
                                        lGoldMap.put(lKey, lValue);
                                    }
                                }
                                for (String lString : lPredictedFields) {
                                    String lKey = lString.substring(0, lString.indexOf("=")).toLowerCase();
                                    if (lKey.equals("joint")) continue;
                                    String lValue = lString.substring(lString.indexOf("=") + 1);
                                    if (lValue.length() == 0) lValue = "_";
                                    if (!lValue.equals("_")) {
                                        lPredictedMap.put(lKey, lValue);
                                    }
                                }
                                {
                                    List<String> lKeys = new ArrayList<>(lGoldMap.keySet());
                                    Collections.sort(lKeys);
                                    for (String lKey : lKeys) {
                                        String lValue = lGoldMap.get(lKey);
                                        if ((lValue.length() > 0) && (!lValue.equals("_"))) {
                                            lGoldNormalized.append("\t" + lKey + "=" + lValue);
                                        }
                                    }
                                }
                                {
                                    List<String> lKeys = new ArrayList<>(lPredictedMap.keySet());
                                    Collections.sort(lKeys);
                                    for (String lKey : lKeys) {
                                        String lValue = lPredictedMap.get(lKey);
                                        if ((lValue.length() > 0) && (!lValue.equals("_"))) {
                                            lPredictedNormalized.append("\t" + lKey + "=" + lValue);
                                        }
                                    }
                                }
                                if (lGoldMap.size() == lPredictedMap.size()) {
                                    for (Map.Entry<String, String> lEntry : lGoldMap.entrySet()) {
                                        if (!lPredictedMap.containsKey(lEntry.getKey())) {
                                            lIsEqual = false;
                                            break;
                                        }
                                        if (!lPredictedMap.get(lEntry.getKey()).equals(lEntry.getValue())) {
                                            lIsEqual = false;
                                            break;
                                        }
                                    }
                                } else {
                                    lIsEqual = false;
                                }
                            }
                            lGoldNormalized.append("_"+lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.lemma));
                            lPredictedNormalized.append("_"+lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.lemma));
                            if (lIsEqual) {
                                if (!lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.lemma).equals(lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.lemma))) {
                                    lIsEqual = false;
                                }
                            }
                            if (!matchMap.containsKey(lGoldNormalized.toString()))
                                matchMap.put(lGoldNormalized.toString(), new TObjectIntHashMap<>());
                            matchMap.get(lGoldNormalized.toString()).adjustOrPutValue(lPredictedNormalized.toString(), 1, 1);
                            if (lIsEqual) {
                                matchTokenCount++;
                            } else {
                                mismatchingPairFrequencyMap.adjustOrPutValue("pos=" + lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.pos) + "|" + lResultSentence.getEntries()[k].getGoldMorphology() + "\tpos=" + lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.pos) + "|" + lResultSentence.getEntries()[k].getPredictedMorphology(), 1, 1);
                                mismatchingPairFrequencyFormMap.adjustOrPutValue(lResultSentence.getEntries()[k].getWordform() + "(pos=" + lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.pos) + "|" + lResultSentence.getEntries()[k].getGoldMorphology() + ")\t" + lResultSentence.getEntries()[k].getWordform() + "(pos=" + lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.pos) + "|" + lResultSentence.getEntries()[k].getPredictedMorphology() + ")", 1, 1);
                            }
                            break;
                        }
                        case lemma_pos: {
                            String lGold = lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.lemma)+"_"+lResultSentence.getEntries()[k].getGoldCategory(Experiment.Category.pos);
                            String lPredicted = lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.lemma)+"_"+lResultSentence.getEntries()[k].getPredictedCategory(Experiment.Category.pos);
                            if (!matchMap.containsKey(lGold)) matchMap.put(lGold, new TObjectIntHashMap<>());
                            matchMap.get(lGold).adjustOrPutValue(lPredicted, 1, 1);
                            if (lGold.equals(lPredicted)) {
                                matchTokenCount++;
                            } else {
                                mismatchingPairFrequencyMap.adjustOrPutValue(lGold + "\t" + lPredicted, 1, 1);
                                mismatchingPairFrequencyFormMap.adjustOrPutValue(lResultSentence.getEntries()[k].getWordform() + "(" + lGold + ")\t" + lResultSentence.getEntries()[k].getWordform() + "(" + lPredicted + ")", 1, 1);
                            }
                            break;
                        }
                        default: {
                            String lGold = lResultSentence.getEntries()[k].getGoldCategory(category);
                            String lPredicted = lResultSentence.getEntries()[k].getPredictedCategory(category);
                            if (!matchMap.containsKey(lGold)) matchMap.put(lGold, new TObjectIntHashMap<>());
                            matchMap.get(lGold).adjustOrPutValue(lPredicted, 1, 1);
                            if (lGold.equals(lPredicted)) {
                                matchTokenCount++;
                            } else {
                                mismatchingPairFrequencyMap.adjustOrPutValue(lResultSentence.getEntries()[k].getGoldCategory(category) + "\t" + lResultSentence.getEntries()[k].getPredictedCategory(category), 1, 1);
                                mismatchingPairFrequencyFormMap.adjustOrPutValue(lResultSentence.getEntries()[k].getWordform() + "(" + lResultSentence.getEntries()[k].getGoldCategory(category) + ")\t" + lResultSentence.getEntries()[k].getWordform() + "(" + lResultSentence.getEntries()[k].getPredictedCategory(category) + ")", 1, 1);
                            }
                            break;
                        }
                    }
                }
            }
        }
        category = lCategory;
        for (String lKey:mismatchingPairFrequencyMap.keySet()) {
            mismatchingPairFrequencies.add(new StringPairFrequency(mismatchingPairFrequencyMap.get(lKey), lKey.substring(0, lKey.indexOf("\t")), lKey.substring(lKey.indexOf("\t")+1)));
        }
        for (String lKey:mismatchingPairFrequencyFormMap.keySet()) {
            mismatchingPairFormFrequencies.add(new StringPairFrequency(mismatchingPairFrequencyFormMap.get(lKey), lKey.substring(0, lKey.indexOf("\t")), lKey.substring(lKey.indexOf("\t")+1)));
        }
        Collections.sort(mismatchingPairFrequencies);
        Collections.sort(mismatchingPairFormFrequencies);
        if (!category.equals(lemma_pos) && !category.equals(joint_lemma)) {
            if (lemmatizer == null) writeMismatchMatrix();
            writeMismatchList();
            writeMismatchFormList();
        }
    }

    private void writeMismatchMatrix() throws IOException {
        File lFile = new File((getDirectory().getAbsolutePath()+"/MatchMatrix"+(outOfVocabularyOnly?"_oov":"")+".txt").replace("\\", "/"));
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lFile), Charset.forName("UTF-8")));
        List<String> lGoldKeys = new ArrayList<>(matchMap.keySet());
        Set<String> lPredirectedSet = new HashSet<>();
        for (TObjectIntHashMap<String> lMap:matchMap.values()) {
            for (String lPredicted:lMap.keySet()) {
                lPredirectedSet.add(lPredicted);
            }
        }
        List<String> lPredictedKeys = new ArrayList<>(lPredirectedSet);
        Collections.sort(lGoldKeys);
        Collections.sort(lPredictedKeys);
        for (String lPredicted:lPredictedKeys) {
            lWriter.print("\t"+lPredicted.replace("\t", "|"));
        }
        lWriter.println();
        for (String lGold:lGoldKeys) {
            lWriter.print(lGold.replace("\t", "|"));
            for (String lPredicted:lPredictedKeys) {
                if (matchMap.get(lGold).containsKey(lPredicted)) {
                    lWriter.print("\t"+matchMap.get(lGold).get(lPredicted));
                }
                else {
                    lWriter.print("\t0");
                }
            }
            lWriter.println();
        }
        lWriter.close();
    }

    private void writeMismatchList() throws IOException {
        File lFile = new File((getDirectory().getAbsolutePath()+"/MismatchList"+(outOfVocabularyOnly?"_oov":"")+".txt").replace("\\", "/"));
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lFile), Charset.forName("UTF-8")));
        for (StringPairFrequency lStringPairFrequency:mismatchingPairFrequencies) {
            lWriter.println(lStringPairFrequency.getGold()+"\t"+lStringPairFrequency.getPredicted()+"\t"+lStringPairFrequency.getFrequency());
        }
        lWriter.close();
    }

    private void writeMismatchFormList() throws IOException {
        File lFile = new File((getDirectory().getAbsolutePath()+"/MismatchFormList"+(outOfVocabularyOnly?"_oov":"")+".txt").replace("\\", "/"));
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lFile), Charset.forName("UTF-8")));
        for (StringPairFrequency lStringPairFrequency:mismatchingPairFormFrequencies) {
            lWriter.println(lStringPairFrequency.getGold()+"\t"+lStringPairFrequency.getPredicted()+"\t"+lStringPairFrequency.getFrequency());
        }
        lWriter.close();
    }

}
