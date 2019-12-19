package org.hucompute.nlpevalframework;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class LemmaGenPlainExperiment extends Experiment {

    private Evaluation evaluation;

    public static Set<Character> chars = new HashSet<>();

    public LemmaGenPlainExperiment(File pResultBaseDir, Evaluation pEvaluationBase, String pLanguage) throws IOException {
        super(pResultBaseDir, Tool.LemmaGenPlain, Category.lemma, pEvaluationBase.getTrainingSet(), pEvaluationBase.getTestSet(), pLanguage);
        evaluation = pEvaluationBase;
    }

    @Override
    public void trainJackKnifing(int pFolds) throws Exception {
        throw new Exception("Not Implemented");
    }

    public File getDirectory() {
        File lResult;
        if ((variant == null) || (variant.length() == 0)) {
            lResult = new File((resultBaseDir.getAbsolutePath()+"/" + trainingSet.getFile().getName() + "/" + testSet.getFile().getName() + "/" + evaluation.getTool().name() + "/" + evaluation.getCategory().name()+"/"+tool.name()+"/lemma").replace("\\", "/"));
        }
        else {
            lResult = new File((resultBaseDir.getAbsolutePath()+"/" + trainingSet.getFile().getName() + "/" + testSet.getFile().getName() + "/" + evaluation.getTool().name() + "/" + evaluation.getCategory().name()+"/"+evaluation.getVariant()+"/"+tool.name()+"/lemma").replace("\\", "/"));
        }
        if (!lResult.exists()) lResult.mkdirs();
        return lResult;
    }

    @Override
    public String getTrainParameters() {
        return null;
    }

    @Override
    public String getTestParameters() {
        return null;
    }

    public String cleanString(String pString) {
        StringBuilder lResult = new StringBuilder();
        for (char c:pString.toCharArray()) {
            switch (c) {
                case '"':break;
                case '>':
                case ',':
                case '-':
                case '.':
                case '`':
                case '\'':
                case '/':
                case ':':
                case ';':
                case '(':
                case ')':
                case '?':
                case '&':
                case '!':
                case '%':
                case '+':
                case '=':
                case '#':
                case '§':
                case '_':
                case '*':
                case '@':
                default: {
                    lResult.append(c);
                }
            }
            /*if (Character.isLetterOrDigit(c)) {
                lResult.append(c);
            }
            else {
                if (!chars.contains(c)) {
                    chars.add(c);
                    System.out.println("'"+c+"'");
                }
            }*/
        }
        return lResult.toString();
    }

    public void train() throws IOException {
        File lDir = getTrainDirectory();
        File lTrainFile = new File(lDir.getAbsolutePath()+"/Train.txt");
        File lPreModelFile = new File(lDir.getAbsolutePath()+"/PreModel.txt");
        lPreModelFile.delete();
        File lModelFile = new File(lDir.getAbsolutePath()+"/Model.txt");
        lModelFile.delete();
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainFile), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    String lForm = cleanString(lEntry.getWordform());
                    String lLemma = cleanString(lEntry.getGoldLemma());
                    if ((lLemma.length()>0) && (lForm.length()>0)) {
                        lWriter.println(lForm+"\t"+lLemma);
                    }
                }
            }
            lWriter.close();
        }
        long lStart = System.currentTimeMillis();
        {
            ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/LemmaGen/lemLearn").getAbsolutePath(), "-o", lPreModelFile.getAbsolutePath(), lTrainFile.getAbsolutePath());
            lProcessBuilder.directory(new File("bin/LemmaGen"));
            lProcessBuilder.redirectErrorStream(true);
            Process lProcess = lProcessBuilder.start();
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
            String lLine = null;
            while ((lLine = lReader.readLine()) != null) {
                System.out.println(lLine);
            }
            try {
                int lResult = lProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lReader.close();
        }
        {
            ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/LemmaGen/lemBuild").getAbsolutePath(), "-o", lModelFile.getAbsolutePath(), lPreModelFile.getAbsolutePath());
            lProcessBuilder.directory(new File("bin/LemmaGen"));
            lProcessBuilder.redirectErrorStream(true);
            Process lProcess = lProcessBuilder.start();
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
            String lLine = null;
            while ((lLine = lReader.readLine()) != null) {
                System.out.println(lLine);
            }
            try {
                int lResult = lProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lReader.close();
        }
        trainingTime = System.currentTimeMillis()-lStart;
    }

    public void test() throws IOException {
        File lDir = getTestDirectory();
        Set<String> lInputSet = new HashSet<>();
        Map<String, String> lOutputMap = new HashMap<>();
        for (CoNLLSentence lSentence:getTestSet().getSentences()) {
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                String lKey = lEntry.getWordform();
                lInputSet.add(lKey);
            }
        }
        File lTestFile = new File(lDir.getAbsolutePath()+"/Test.txt");
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestFile), Charset.forName("UTF-8")));
        for (String lString:lInputSet) {
            lWriter.println(lString);
        }
        lWriter.close();
        File lPredictFile = new File(lDir.getAbsolutePath()+"/Predict.txt");
        lPredictFile.delete();
        long lStart = System.currentTimeMillis();
        {
            ProcessBuilder lProcessBuilder = new ProcessBuilder(new File("bin/LemmaGen/lemmatize").getAbsolutePath(), "-f", "wpl", "-l", new File(getTrainDirectory().getAbsolutePath()+"/Model.txt").getAbsolutePath(), lTestFile.getAbsolutePath(), lPredictFile.getAbsolutePath());
            lProcessBuilder.directory(new File("bin/LemmaGen"));
            lProcessBuilder.redirectErrorStream(true);
            Process lProcess = lProcessBuilder.start();
            BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream(), Charset.forName("UTF-8")));
            String lLine = null;
            while ((lLine = lReader.readLine()) != null) {
                System.out.println(lLine);
            }
            try {
                int lResult = lProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lReader.close();
        }
        testTime = System.currentTimeMillis() - lStart;
        BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(lPredictFile), Charset.forName("UTF-8")));
        String lLine = null;
        while ((lLine = lReader.readLine()) != null) {
            String[] lFields = lLine.split("\t", -1);
            if (lFields.length>1) {
                lOutputMap.put(lFields[0], lFields[1]);
            }
        }
        lReader.close();

        for (CoNLLSentence lSentence:getTestSet().getSentences()) {
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                String lLemma = lOutputMap.get(lEntry.getWordform());
                if (lLemma == null) {
                    lLemma = lEntry.getWordform();
                    if (lEntry.getPredictedCategory(Experiment.Category.pos).startsWith("N")) {
                        lLemma = lLemma.toLowerCase();
                    }
                }
                lEntry.setPredictedLemma(lLemma);
            }
        }
        resultSet = testSet;
        File lFile = resultSet.getFile();
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
        resultSet.setFile(lFile);
    }

}
