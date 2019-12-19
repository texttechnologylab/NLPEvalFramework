package org.hucompute.nlpevalframework;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class TreeTaggerExperiment extends Experiment {

    private String trainParameters = null;
    private String testParameters = null;
    protected File modelFile;

    public TreeTaggerExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage) {
        super(pResultBaseDir, Tool.TreeTagger, pCategory, pTrainingSet, pTestSet, pLanguage);
    }

    public TreeTaggerExperiment(File pResultBaseDir, Category pCategory, CoNLL pTrainingSet, CoNLL pTestSet, String pLanguage, List<Category> pJointCategories) {
        super(pResultBaseDir, Tool.TreeTagger, pCategory, pTrainingSet, pTestSet, pLanguage, pJointCategories);
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

    @Override
    public void train() throws Exception {
        Map<String, Set<String>> lLexionMap = new HashMap<>();
        Set<String> lTagSet = new HashSet<>();
        // Write Training Data
        File lTrainingInput = new File(getTrainDirectory().getAbsolutePath()+"/traininput.txt");
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainingInput), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
                for (int i=0; i<lSentence.getEntries().length; i++) {
                    String lGold = getGold(lSentence.getEntries()[i]);
                    lWriter.println(lSentence.getEntries()[i].getWordform()+"\t"+lGold);
                    if (!lLexionMap.containsKey(lSentence.getEntries()[i].getWordform())) {
                        lLexionMap.put(lSentence.getEntries()[i].getWordform(), new HashSet<>());
                    }
                    lLexionMap.get(lSentence.getEntries()[i].getWordform()).add(lGold+" -"); // - is dummy lemma
                    lTagSet.add(lGold);
                }
            }
            lWriter.close();
        }
        // Write Lexicon
        File lLexiconFile = new File(getTrainDirectory().getAbsolutePath()+"/lexicon.txt");
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lLexiconFile), Charset.forName("UTF-8")));
            for (Map.Entry<String, Set<String>> lEntry:lLexionMap.entrySet()) {
                lWriter.print(lEntry.getKey());
                for (String lString:lEntry.getValue()) {
                    lWriter.print("\t"+lString);
                }
                lWriter.println();
            }
            //if (!category.name().equals("pos") && !category.name().equals("joint")) {
                lWriter.println("ł\t$. -");
            //}
            lWriter.close();
        }
        // Write Open Class File
        File lOpenClassFile = new File(getTrainDirectory().getAbsolutePath()+"/openclasses.txt");
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lOpenClassFile), Charset.forName("UTF-8")));
            for (String lString:lTagSet) {
                if (lString.startsWith("ADJ") || lString.startsWith("ADV") || lString.startsWith("NN") || lString.startsWith("NE") || lString.startsWith("V") || lString.equals("N")) {
                    lWriter.println(lString);
                }
                else if (lString.startsWith("pos=ADJ") || lString.startsWith("pos=ADV") || lString.startsWith("pos=NN") || lString.startsWith("pos=NE") || lString.startsWith("pos=V") || lString.startsWith("pos=N")) {
                    lWriter.println(lString);
                }
            }
            lWriter.close();
        }
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model.txt");

        trainParameters = new File("bin/TreeTagger/train-tree-tool").getAbsolutePath()+" "+lLexiconFile.getAbsolutePath()+" "+lOpenClassFile.getAbsolutePath()+" "+lTrainingInput.getAbsolutePath()+" "+modelFile.getAbsolutePath()+" -st $.";
        long lStart = System.currentTimeMillis();
        //ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/TreeTagger/train-tree-tool").getAbsolutePath(), lLexiconFile.getAbsolutePath(), lOpenClassFile.getAbsolutePath(), lTrainingInput.getAbsolutePath(), modelFile.getAbsolutePath(), "-st", "$.");
        ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/TreeTagger/train-tree-tagger").getAbsolutePath(), "-st", "$.", lLexiconFile.getAbsolutePath(), lOpenClassFile.getAbsolutePath(), lTrainingInput.getAbsolutePath(), modelFile.getAbsolutePath());
        lProcessBuilder.directory(new File("bin/TreeTagger"));
        lProcessBuilder.redirectErrorStream(true);
        Process lProcess = lProcessBuilder.start();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
        String lLine = null;
        while ((lLine = lReader.readLine()) != null) {
            System.out.println(lLine);
        }
        try {
            int lResult = lProcess.waitFor();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        trainingTime = System.currentTimeMillis() - lStart;
        lReader.close();
    }

    @Override
    public void test() throws Exception {
        File lTestFile = new File(getTestDirectory().getAbsolutePath()+"/testinput.txt");
        File lResultFile = new File(getTestDirectory().getAbsolutePath()+"/testoutput.txt");
        List<CoNLLSentence> lSentences = new ArrayList<>();
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestFile), Charset.forName("UTF-8")));
        for (CoNLLSentence lSentence : getTestSet().getSentences()) {
            for (int i = 0; i < lSentence.getEntries().length; i++) {
                lWriter.println(lSentence.getEntries()[i].getWordform());
            }
        }
        lWriter.close();
        testParameters = new File("bin/TreeTagger/tree-tool").getAbsolutePath()+" "+modelFile.getAbsolutePath()+" "+lTestFile.getAbsolutePath()+" "+lResultFile.getAbsolutePath();
        long lStart = System.currentTimeMillis();
        ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/TreeTagger/tree-tagger").getAbsolutePath(), modelFile.getAbsolutePath(), lTestFile.getAbsolutePath(), lResultFile.getAbsolutePath());
        lProcessBuilder.directory(new File("bin/TreeTagger"));
        lProcessBuilder.redirectErrorStream(true);
        Process lProcess = lProcessBuilder.start();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
        String lLine = null;
        while ((lLine = lReader.readLine()) != null) {
            System.out.println(lLine);
        }
        try {
            int lResult = lProcess.waitFor();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        testTime = System.currentTimeMillis() - lStart;
        lReader.close();
        lReader = new BufferedReader(new InputStreamReader(new FileInputStream(lResultFile), Charset.forName("UTF-8")));
        for (CoNLLSentence lSentence : getTestSet().getSentences()) {
            List<CoNLLEntry> lTokens = new ArrayList<>();
            for (int i = 0; i < lSentence.getEntries().length; i++) {
                lLine = lReader.readLine();
                CoNLLEntry lNewEntry = new CoNLLEntry(lSentence.getEntries()[i]);
                lNewEntry.setPredictedCategory(category, lLine);
                lTokens.add(lNewEntry);
            }
            lSentences.add(new CoNLLSentence(lTokens));
        }
        resultSet = new CoNLL(lSentences);
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
    }
}
