package org.hucompute.nlpevalframework;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public abstract class Experiment {

    public static enum Tool {Lapos, Mate, OpenNLP, Stanford, TreeTagger, TnT, FLORS, NonLexNN, MarMoT, FastText, LemmaGenPlain, LemmaGenPoS, MarMoTLAT, MarMoTLATPOS, MajorityVote, BLSTMRNN, RDRPOSTagger, LemmaTag};

    public static enum Category{pos, case_, number, person, gender, degree, tense, mood, voice, pipeline, joint, lemma, lemma_pos, joint_lemma};

    protected Tool tool;

    protected Category category;

    protected CoNLL trainingSet;

    protected CoNLL testSet;

    protected CoNLL resultSet;

    protected long trainingTime = 0;

    protected long testTime = 0;

    protected List<StringPairFrequency> mismatchingPairFrequencies = null;

    protected String language;

    protected List<Category> jointCategories;

    protected String variant;

    protected File resultBaseDir = null;

    public Experiment(File pResultBaseDir, Tool pTool, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage) {
        this(pResultBaseDir, pTool, pCategory, pTrainingSet, pTestSet, pLanguage, "");
    }

    public Experiment(File pResultBaseDir, Tool pTool, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, String pVariant) {
        resultBaseDir = pResultBaseDir;
        tool = pTool;
        category = pCategory;
        trainingSet = pTrainingSet;
        testSet = pTestSet;
        resultSet = null;
        language = pLanguage;
        variant = pVariant;
    }

    public Experiment(File pResultBaseDir, Tool pTool, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, List<Category> pJointCategories) {
        this(pResultBaseDir, pTool, pCategory, pTrainingSet, pTestSet, pLanguage, "", pJointCategories);
    }

    public Experiment(File pResultBaseDir, Tool pTool, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, String pVariant, List<Category> pJointCategories) {
        resultBaseDir = pResultBaseDir;
        tool = pTool;
        category = pCategory;
        trainingSet = pTrainingSet;
        testSet = pTestSet;
        resultSet = null;
        language = pLanguage;
        variant = pVariant;
        jointCategories = pJointCategories;
    }

    public String getLanguage() {
        return language;
    }

    public List<Category> getJointCategories() {
        return jointCategories;
    }

    public File getResultConLLFile() {
        return new File(getDirectory().getAbsolutePath()+"/"+trainingSet.getFile().getName()+"_"+testSet.getFile().getName()+"_"+ tool.name()+"_"+category.name()+".conll");
    }

    public boolean isResultConLLFileExistent() {
        return getResultConLLFile().exists();
    }

    public File getDirectory() {
        File lResult;
        if ((variant == null) || (variant.length() == 0)) {
            lResult = new File((resultBaseDir.getAbsolutePath()+"/" + trainingSet.getFile().getName() + "/" + testSet.getFile().getName() + "/" + tool.name() + "/" + category.name()).replace("\\", "/"));
        }
        else {
            lResult = new File((resultBaseDir.getAbsolutePath()+"/" + trainingSet.getFile().getName() + "/" + testSet.getFile().getName() + "/" + tool.name() + "/" + category.name()+"/"+variant).replace("\\", "/"));
        }
        if (!lResult.exists()) lResult.mkdirs();
        return lResult;
    }

    public File getModelDirectory() {
        File lResult = new File((getDirectory().getAbsolutePath()+"/model").replace("\\", "/"));
        if (!lResult.exists()) lResult.mkdirs();
        return lResult;
    }

    public File getTrainJackKnifingDirectory() {
        File lResult = new File((getDirectory().getAbsolutePath()+"/trainJackKnifing").replace("\\", "/"));
        if (!lResult.exists()) lResult.mkdirs();
        return lResult;
    }

    public File getTrainDirectory() {
        File lResult = new File((getDirectory().getAbsolutePath()+"/train").replace("\\", "/"));
        if (!lResult.exists()) lResult.mkdirs();
        return lResult;
    }

    public File getTestDirectory() {
        File lResult = new File((getDirectory().getAbsolutePath()+"/gold").replace("\\", "/"));
        if (!lResult.exists()) lResult.mkdirs();
        return lResult;
    }

    public abstract void train() throws Exception;

    public abstract void test() throws Exception;

    public abstract void trainJackKnifing(int pFolds) throws Exception;

    public void execute() throws Exception {
        train();
        test();
        validate();
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File((getDirectory().getAbsolutePath()+"/statistics.txt").replace("\\", "/"))), Charset.forName("UTF-8")));
        lWriter.println("Training\t"+trainingTime);
        lWriter.println("Testing\t"+testTime);
        lWriter.close();
    }

    public abstract String getTrainParameters();

    public abstract String getTestParameters();

    public long getTrainingTime() {
        return trainingTime;
    }

    public long getTestTime() {
        return testTime;
    }

    public Tool getTool() {
        return tool;
    }

    public CoNLL getTrainingSet() {
        return trainingSet;
    }

    public CoNLL getTestSet() {
        return testSet;
    }

    public CoNLL getResultSet() {
        return resultSet;
    }

    public Category getCategory() {
        return category;
    }

    public int getTestSetSentenceCount() {
        return testSet.getSentences().size();
    }

    public int getTrainingSetSentenceCount() {
        return trainingSet.getSentences().size();
    }

    public int getTestSetTokenCount() {
        int lResult = 0;
        for (CoNLLSentence lSentence:testSet.getSentences()) {
            lResult += lSentence.getEntries().length;
        }
        return lResult;
    }

    public int getTrainingSetTokenCount() {
        int lResult = 0;
        for (CoNLLSentence lSentence:trainingSet.getSentences()) {
            lResult += lSentence.getEntries().length;
        }
        return lResult;
    }

    public List<StringPairFrequency> getMismatchingPairFrequencies() {
        return mismatchingPairFrequencies;
    }

    public void validate() throws IOException {
        TObjectIntHashMap<String> mismatchingPairFrequencyMap = new TObjectIntHashMap<>();
        if (testSet.getSentences().size() != resultSet.getSentences().size()) {
            throw new IOException("testSet has size "+testSet.getSentences().size()+", but resultSet has size "+resultSet.getSentences().size());
        }
        for (int i=0; i<testSet.getSentences().size(); i++) {
            CoNLLSentence lTestSentence = testSet.getSentences().get(i);
            CoNLLSentence lResultSentence = resultSet.getSentences().get(i);
            if (lTestSentence.getEntries().length != lResultSentence.getEntries().length) {
                throw new IOException("Test Sentence has length "+lTestSentence.getEntries().length+", but Result Sentence has size "+lResultSentence.getEntries().length);
            }
            for (int k=0; k<lTestSentence.getEntries().length; k++) {
                /*if (!lTestSentence.getEntries()[k].getWordform().equals(lResultSentence.getEntries()[k].getWordform())) {
                    throw new IOException("Wordform mismatch: "+lTestSentence.getEntries()[k].getWordform()+" vs "+lResultSentence.getEntries()[k].getWordform());
                }*/
            }
        }
    }

}
