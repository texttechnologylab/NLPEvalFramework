package org.hucompute.nlpevalframework;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class FastTextExperiment extends Experiment {

    protected enum EncodingMode {STANDARD, TOLGA};


    private String trainParameters = null;
    private String testParameters = null;
    protected File modelFile;
    protected int leftNeighbours = 1;
    protected int rightNeighbours = 1;
    protected int dimensions = 100;
    protected double learningRate = 0.05;
    protected int wordNgrams = 1;
    protected int epoch = 5;
    protected int sizeContextWindow = 3;
    protected int threads = 16;
    protected boolean markPosition = true;
    protected EncodingMode encodingMode = EncodingMode.STANDARD;

    public FastTextExperiment(File pResultBaseDir, Experiment.Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage) {
        super(pResultBaseDir, Tool.FastText, pCategory, pTrainingSet, pTestSet, pLanguage);
    }

    public FastTextExperiment(File pResultBaseDir, Experiment.Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, List<Category> pJointCategories) {
        super(pResultBaseDir, Tool.FastText, pCategory, pTrainingSet, pTestSet, pLanguage, pJointCategories);
        if (jointCategories != null) Collections.sort(jointCategories);
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

    protected String getContext(CoNLLEntry[] pEntries, int pCenterIndex) {
        switch (encodingMode) {
            default:
            case STANDARD: {
                StringBuilder lResult = new StringBuilder();
                for (int i=pCenterIndex - leftNeighbours; i<pCenterIndex + rightNeighbours+1; i++) {
                    if (markPosition) {
                        if (i == -1) {
                            lResult.append("¤" + (i - pCenterIndex) + "|<S>¤ ");
                        } else if ((i >= 0) && (i < pEntries.length)) {
                            lResult.append("¤" + (i - pCenterIndex) + "|" + pEntries[i].getWordform() + "¤ ");
                        } else if (i == pEntries.length) {
                            lResult.append("¤" + (i - pCenterIndex) + "|</S>¤ ");
                        }
                    }
                    else {
                        if ((i >= 0) && (i<pEntries.length)) {
                            lResult.append(pEntries[i].getWordform()+" ");
                        }
                    }
                }
                return lResult.toString().trim();
            }
            case TOLGA: {
                StringBuilder lResult = new StringBuilder();
                int lCount = 0;
                for (int i=pCenterIndex - leftNeighbours; i<pCenterIndex + rightNeighbours+1; i++) {
                    if (markPosition) {
                        String lSign = "";
                        switch (i - pCenterIndex) {
                            case -3: {
                                lSign = "☺️";
                                break;
                            }
                            case -2: {
                                lSign = "🙂";
                                break;
                            }
                            case -1: {
                                lSign = "😊";
                                break;
                            }
                            case 0: {
                                lSign = "🌟";
                                break;
                            }
                            case 1: {
                                lSign = "☹️";
                                break;
                            }
                            case 2: {
                                lSign = "🙁";
                                break;
                            }
                            case 3: {
                                lSign = "😠";
                                break;
                            }
                        }
                        if (i == -1) {
                            lResult.append(lSign+"sentenceBegin ");
                        } else if ((i >= 0) && (i < pEntries.length)) {
                            lResult.append(lSign + pEntries[i].getWordform()+" ");
                        } else if (i == pEntries.length) {
                            lResult.append(lSign+"sentenceEnd ");
                        }
                    }
                    else {
                        if ((i >= 0) && (i<pEntries.length)) {
                            lResult.append(pEntries[i].getWordform()+" ");
                        }
                    }
                }
                return lResult.toString().trim();
            }
        }
    }

    @Override
    public void train() throws Exception {
        File lTrainingInput = new File(getTrainDirectory().getAbsolutePath()+"/traininput.txt");
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model");

        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(lTrainingInput.getAbsolutePath())), Charset.forName("UTF-8")));
        for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
            CoNLLEntry[] lEntries = lSentence.getEntries();
            for (int i=0; i<lEntries.length; i++) {
                lWriter.println("__label__"+lEntries[i].getGoldCategory(category)+" , "+getContext(lEntries, i));
            }
        }
        lWriter.close();
        long lStart = System.currentTimeMillis();
        trainParameters = "supervised -input "+lTrainingInput.getAbsolutePath()+" -output "+modelFile.getAbsolutePath()+" -dim "+Integer.toString(dimensions)+" -lr "+Double.toString(learningRate)+" -wordNgrams "+Integer.toString(wordNgrams)+" -epoch "+Integer.toString(epoch)+" -thread "+Integer.toString(threads)+" -ws "+sizeContextWindow;
        ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/FastText/fasttext").getAbsolutePath(), "supervised", "-input", lTrainingInput.getAbsolutePath(), "-output", modelFile.getAbsolutePath(), "-dim", Integer.toString(dimensions), "-lr", Double.toString(learningRate), "-wordNgrams", Integer.toString(wordNgrams), "-epoch", Integer.toString(epoch), "-thread", Integer.toString(threads), "-ws", Integer.toString(sizeContextWindow), "-loss", "ns");
        lProcessBuilder.directory(new File("bin/FastText"));
        lProcessBuilder.redirectErrorStream(true);
        Process lProcess = lProcessBuilder.start();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
        String lLine = null;
        do {
            lLine = lReader.readLine();
        } while (lLine != null);
        try {
            int lResult = lProcess.waitFor();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        lReader.close();
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model.bin");
        trainingTime = System.currentTimeMillis() - lStart;
    }

    @Override
    public void test() throws Exception {
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model.bin");
        File lTestFile = new File(getTestDirectory().getAbsolutePath()+"/testinput.txt");
        File lResultFile = new File(getTestDirectory().getAbsolutePath()+"/testoutput.txt");
        List<CoNLLSentence> lSentences = new ArrayList<>();
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestFile), Charset.forName("UTF-8")));
        for (CoNLLSentence lSentence : getTestSet().getSentences()) {
            CoNLLEntry[] lEntries = lSentence.getEntries();
            for (int i=0; i<lEntries.length; i++) {
                lWriter.println("__label__"+lEntries[i].getGoldCategory(category)+" , "+getContext(lEntries, i));
            }
        }
        lWriter.close();
        long lStart = System.currentTimeMillis();
        testParameters = new File("bin/FastText/fasttext").getAbsolutePath()+" gold "+modelFile.getAbsolutePath()+" "+lTestFile.getAbsolutePath();
        ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/FastText/fasttext").getAbsolutePath(), "predict", modelFile.getAbsolutePath(), lTestFile.getAbsolutePath());
        lProcessBuilder.directory(new File("bin/TnT"));
        lProcessBuilder.redirectErrorStream(true);
        Process lProcess = lProcessBuilder.start();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
        String lLine = null;
        for (CoNLLSentence lSentence : getTestSet().getSentences()) {
            List<CoNLLEntry> lTokens = new ArrayList<>();
            for (int i = 0; i < lSentence.getEntries().length; i++) {
                lLine = lReader.readLine();
                String lTag = lLine.substring(9);
                CoNLLEntry lNewEntry = new CoNLLEntry(lSentence.getEntries()[i]);

                lNewEntry.setPredictedCategory(category, lTag);
                lTokens.add(lNewEntry);
            }
            lSentences.add(new CoNLLSentence(lTokens));
        }
        try {
            int lResult = lProcess.waitFor();
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
