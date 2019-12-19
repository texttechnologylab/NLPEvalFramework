package org.hucompute.nlpevalframework;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class StanfordExperiment extends Experiment {

    private String trainParameters = null;
    private String testParameters = null;

    protected File modelFile;

    public StanfordExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage) {
        super(pResultBaseDir, Tool.Stanford, pCategory, pTrainingSet, pTestSet, pLanguage);
    }

    public StanfordExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, List<Category> pJointCategories) {
        super(pResultBaseDir, Tool.Stanford, pCategory, pTrainingSet, pTestSet, pLanguage, pJointCategories);
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
        Properties lProperties = new Properties();
        lProperties.load(StanfordExperiment.class.getResourceAsStream("/org/hucompute/nlpevalframework/stanford/" + language + ".pos.properties"));
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model.txt");
        File lTrainFile = new File(getTrainDirectory().getAbsolutePath()+"/traininput.txt");
        lProperties.setProperty("model", modelFile.getAbsolutePath());
        lProperties.setProperty("trainFile", "format=TEXT," + lTrainFile.getAbsolutePath());
        File lPropertiesFile = new File(getTrainDirectory().getAbsolutePath()+"/trainproperties.txt");
        lProperties.store(new OutputStreamWriter(new FileOutputStream(lPropertiesFile), Charset.forName("UTF-8")), "");
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainFile), Charset.forName("UTF-8")));
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
        StringBuilder lBuilder = new StringBuilder();
        for (String lString:lProperties.stringPropertyNames()) {
            lBuilder.append(lString+" = "+lProperties.get(lString)+"\n");
        }
        trainParameters = lBuilder.toString();
        long lStart = System.currentTimeMillis();
        edu.stanford.nlp.tagger.maxent.MaxentTagger.main(new String[]{"-props", lPropertiesFile.getAbsolutePath()});
        trainingTime = System.currentTimeMillis() - lStart;
    }

    @Override
    public void test() throws Exception {
        long lStart = System.currentTimeMillis();
        MaxentTagger lMaxentTagger = new MaxentTagger(modelFile.getAbsolutePath());
        List<CoNLLSentence> lResultSentences = new ArrayList<>();
        for (CoNLLSentence lSentence : getTestSet().getSentences()) {
            String[] lWords = new String[lSentence.getEntries().length];
            for (int i=0; i<lWords.length; i++) {
                lWords[i] = lSentence.getEntries()[i].getWordform();
            }
            List<TaggedWord> lTagged = lMaxentTagger.tagSentence(Sentence.toWordList(lWords));
            List<CoNLLEntry> lTokens = new ArrayList<>();
            int i=0;
            for (TaggedWord lWord:lTagged) {
                CoNLLEntry lNewEntry = new CoNLLEntry(lSentence.getEntries()[i]);
                lNewEntry.setPredictedCategory(category, lWord.tag().replace("~","_"));
                lTokens.add(lNewEntry);
                i++;
            }
            lResultSentences.add(new CoNLLSentence(lTokens));
        }
        testTime = System.currentTimeMillis() - lStart;
        resultSet = new CoNLL(lResultSentences);
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
    }
}
