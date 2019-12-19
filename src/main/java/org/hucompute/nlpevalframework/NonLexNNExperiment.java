package org.hucompute.nlpevalframework;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NonLexNNExperiment extends Experiment {

    private String trainParameters = null;
    private String testParameters = null;
    private File trainDir;

    public NonLexNNExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage) {
        super(pResultBaseDir, Tool.NonLexNN, pCategory, pTrainingSet, pTestSet, pLanguage);
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

    @Override
    public void train() throws Exception {
        trainDir = getTrainDirectory();
        File lTrainWordsFile = new File(trainDir.getAbsolutePath()+"/train.wrd");
        File lTrainPoSFile = new File(trainDir.getAbsolutePath()+"/train.pos");
        File lTrainWordsVocFile = new File(trainDir.getAbsolutePath()+"/train.wrd.voc");
        File lTrainPoSVocFile = new File(trainDir.getAbsolutePath()+"/train.pos.voc");
        TObjectIntHashMap<String> lTrainWordsVocMap = new TObjectIntHashMap<>();
        TObjectIntHashMap<String> lTrainPoSVocMap = new TObjectIntHashMap<>();
        {
            PrintWriter lWordsWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainWordsFile), Charset.forName("UTF-8")));
            PrintWriter lPoSWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainPoSFile), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
                boolean lFirst = true;
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    lTrainWordsVocMap.adjustOrPutValue(lEntry.getWordform(), 1, 1);
                    if (!lFirst) {
                        lWordsWriter.print(" ");
                        lPoSWriter.print(" ");
                    } else {
                        lFirst = false;
                    }
                    lWordsWriter.print(lEntry.getWordform());
                    lPoSWriter.print(lEntry.getGoldCategory(category));
                    lTrainPoSVocMap.adjustOrPutValue(lEntry.getGoldCategory(category), 1, 1);
                }
                lWordsWriter.println();
                lPoSWriter.println();
            }
            lWordsWriter.close();
            lPoSWriter.close();
        }
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainWordsVocFile), Charset.forName("UTF-8")));
            List<String> lList = new ArrayList<>(lTrainWordsVocMap.keySet());
            Collections.sort(lList, (s1, s2) -> Integer.compare(lTrainWordsVocMap.get(s2), lTrainWordsVocMap.get(s1)));
            for (String lKey:lList) {
                lWriter.println(lKey+"\t"+lTrainWordsVocMap.get(lKey));
            }
            lWriter.close();
        }
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainPoSVocFile), Charset.forName("UTF-8")));
            List<String> lList = new ArrayList<>(lTrainPoSVocMap.keySet());
            Collections.sort(lList, (s1,s2)->Integer.compare(lTrainPoSVocMap.get(s2), lTrainPoSVocMap.get(s1)));
            for (String lKey:lList) {
                lWriter.println(lKey+"\t"+lTrainPoSVocMap.get(lKey));
            }
            lWriter.close();
        }
    }

    @Override
    public void test() throws Exception {
        File lTestWordsFile = new File(trainDir.getAbsolutePath()+"/gold.wrd");
        File lTestPoSFile = new File(trainDir.getAbsolutePath()+"/gold.pos");
        File lTestWordsVocFile = new File(trainDir.getAbsolutePath()+"/gold.wrd.voc");
        File lTestPoSVocFile = new File(trainDir.getAbsolutePath()+"/gold.pos.voc");
        TObjectIntHashMap<String> lTestWordsVocMap = new TObjectIntHashMap<>();
        TObjectIntHashMap<String> lTestPoSVocMap = new TObjectIntHashMap<>();
        {
            PrintWriter lWordsWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestWordsFile), Charset.forName("UTF-8")));
            PrintWriter lPoSWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestPoSFile), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTestSet().getSentences()) {
                boolean lFirst = true;
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    lTestWordsVocMap.adjustOrPutValue(lEntry.getWordform(), 1, 1);
                    if (!lFirst) {
                        lWordsWriter.print(" ");
                        lPoSWriter.print(" ");
                    } else {
                        lFirst = false;
                    }
                    lWordsWriter.print(lEntry.getWordform());
                    lPoSWriter.print(lEntry.getGoldCategory(category));
                    lTestPoSVocMap.adjustOrPutValue(lEntry.getGoldCategory(category), 1, 1);
                }
                lWordsWriter.println();
                lPoSWriter.println();
            }
            lWordsWriter.close();
            lPoSWriter.close();
        }
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestWordsVocFile), Charset.forName("UTF-8")));
            List<String> lList = new ArrayList<>(lTestWordsVocMap.keySet());
            Collections.sort(lList, (s1, s2) -> Integer.compare(lTestWordsVocMap.get(s2), lTestWordsVocMap.get(s1)));
            for (String lKey:lList) {
                lWriter.println(lKey+"\t"+lTestWordsVocMap.get(lKey));
            }
            lWriter.close();
        }
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestPoSVocFile), Charset.forName("UTF-8")));
            List<String> lList = new ArrayList<>(lTestPoSVocMap.keySet());
            Collections.sort(lList, (s1, s2) -> Integer.compare(lTestPoSVocMap.get(s2), lTestPoSVocMap.get(s1)));
            for (String lKey:lList) {
                lWriter.println(lKey+"\t"+lTestPoSVocMap.get(lKey));
            }
            lWriter.close();
        }
    }
}
