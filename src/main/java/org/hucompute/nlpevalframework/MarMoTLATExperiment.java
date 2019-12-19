package org.hucompute.nlpevalframework;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class MarMoTLATExperiment extends Experiment {

    private Evaluation evaluation;

    public static Set<Character> chars = new HashSet<>();

    private String trainParameters = null;
    private String testParameters = null;
    private File modelFile;

    protected Map<String, String> cacheMap = new HashMap<>();

    public MarMoTLATExperiment(File pResultBaseDir, Evaluation pEvaluationBase, String pLanguage) throws IOException {
        super(pResultBaseDir, Tool.MarMoTLAT, Category.lemma, pEvaluationBase.getTrainingSet(), pEvaluationBase.getTestSet(), pLanguage);
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

    public static String getLongestCommonSubstring(String pA, String pB) {
        int lStart = 0;
        int lMax = 0;
        for (int i=0; i<pA.length(); i++) {
            for (int j=0; j<pB.length(); j++) {
                int x = 0;
                while (pA.charAt(i+x) == pB.charAt(j+x)) {
                    x++;
                    if (((i+x) >= pA.length()) || ((j+x) >= pB.length())) break;
                }
                if (x > lMax) {
                    lMax = x;
                    lStart = i;
                }
            }
        }
        return pA.substring(lStart, (lStart+lMax));
    }

    public String getTuple(String pWord, String pLemma) {
        String lResult = cacheMap.get(pWord+"\t"+pLemma);
        if (lResult == null) {
            String lCommonSubString = getLongestCommonSubstring(pWord, pLemma);
            if (lCommonSubString.length() == 0) {
                return "0||" + pWord.length() + "|" + pLemma;
            }
            int lIndexWord = pWord.indexOf(lCommonSubString);
            int lIndexLemma = pLemma.indexOf(lCommonSubString);
            int lWordSuffixLength = (pWord.length() - lIndexWord - lCommonSubString.length());
            int lLemmaSuffixLength = (pLemma.length() - lIndexLemma - lCommonSubString.length());
            lResult = lIndexWord + "|" + (lIndexLemma == 0 ? "" : pLemma.substring(0, lIndexLemma)) + "|" + lWordSuffixLength + "|" + (lLemmaSuffixLength == 0 ? "" : pLemma.substring(pLemma.length() - lLemmaSuffixLength));
            cacheMap.put(pWord+"\t"+pLemma, lResult);
        }
        return lResult;
    }

    public String applyTuple(String pTuple, String pWord) {
        String lResult = cacheMap.get(pTuple+"\t"+pWord);
        if (lResult == null) {
            try {
                String[] lFields = pTuple.split("\\|", -1);
                if (lFields.length != 4) {
                    lResult = pWord;
                } else {
                    StringBuilder lBuilder = new StringBuilder(pWord);
                    lBuilder.delete(0, Integer.parseInt(lFields[0]));
                    lBuilder.delete(lBuilder.length() - Integer.parseInt(lFields[2]), lBuilder.length());
                    lBuilder.insert(0, lFields[1]);
                    lBuilder.append(lFields[3]);
                    lResult = lBuilder.toString();
                }
            }
            catch (Exception e) {
                lResult = pWord;
            }
            if (lResult.length() == 0) lResult = pWord;
            cacheMap.put(pTuple + "\t" + pWord, lResult);
        }
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

    @Override
    public void train() throws Exception {
        File lTrainingInput = new File(getTrainDirectory().getAbsolutePath()+"/traininput.txt");
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model.txt");
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainingInput), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTrainingSet().getSentences()) {
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    lWriter.println(lEntry.getID() + "\t" + lEntry.getWordform() + "\t" + getTuple(lEntry.getWordform(), lEntry.getGoldLemma()));
                }
                lWriter.println();
            }
            lWriter.close();
        }
        List<String> lParameters = new ArrayList<>();
        lParameters.add("--train-file");
        lParameters.add("form-index=1,tag-index=2," + lTrainingInput.getAbsolutePath());
        lParameters.add("--tag-morph");
        lParameters.add("false");
        lParameters.add("--model-file");
        lParameters.add(modelFile.getAbsolutePath());
        String[] lParams = new String[lParameters.size()];
        trainParameters = "";
        for (int i=0; i<lParameters.size(); i++) {
            lParams[i] = lParameters.get(i);
            trainParameters = trainParameters + lParams[i] +" ";
        }
        trainParameters = trainParameters.trim();
        long lStart = System.currentTimeMillis();
        marmot.morph.cmd.Trainer.main(lParams);
        trainingTime = System.currentTimeMillis() - lStart;
    }

    public void test() throws IOException {
        File lTestInput = new File(getTestDirectory().getAbsolutePath()+"/testinput.txt");
        File lOutput = new File(getTestDirectory().getAbsolutePath()+"/testoutput.txt");
        modelFile = new File(getModelDirectory().getAbsolutePath()+"/model.txt");
        {
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTestInput), Charset.forName("UTF-8")));
            for (CoNLLSentence lSentence : getTestSet().getSentences()) {
                for (CoNLLEntry lEntry : lSentence.getEntries()) {
                    lWriter.println(lEntry.getWordform());
                }
                lWriter.println();
            }
            lWriter.close();
        }
        testParameters = "--model-file "+modelFile.getAbsolutePath()+" --test-file form-index=0,"+lTestInput.getAbsolutePath()+" --pred-file "+lOutput.getAbsolutePath();
        long lStart = System.currentTimeMillis();
        marmot.morph.cmd.Annotator.main(new String[]{"--model-file", modelFile.getAbsolutePath(), "--test-file", "form-index=0,"+lTestInput.getAbsolutePath(), "--pred-file", lOutput.getAbsolutePath()});
        testTime = System.currentTimeMillis() - lStart;
        resultSet = new CoNLL(lOutput);
        CoNLL lGold = new CoNLL(getTestSet());
        int lSentenceCounter = 0;
        for (CoNLLSentence lSentence:resultSet.getSentences()) {
            CoNLLSentence lGoldSentence = lGold.getSentences().get(lSentenceCounter);
            int lEntryCounter = 0;
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                lEntry.setGoldLemma(lGoldSentence.getEntries()[lEntryCounter].getGoldLemma());
                lEntry.setGoldCategory(Category.pos, lGoldSentence.getEntries()[lEntryCounter].getGoldCategory(Category.pos));
                lEntry.setGoldMorphology(lGoldSentence.getEntries()[lEntryCounter].getGoldMorphology());
                switch (category) {
                    case joint: {
                        String[] lFields = lEntry.getPredictedCategory(Category.pos).split("\\|", -1);
                        if (lFields.length != jointCategories.size()) {
                            throw new IOException("Expected "+jointCategories.size()+" but found "+lFields.length);
                        }
                        String lPredictedPoS = null;
                        StringBuilder lPredictedMorph = new StringBuilder();
                        for (String lString:lFields) {
                            String lKey = lString.substring(0, lString.indexOf("="));
                            if (lKey.equals("case_")) lKey = "case";
                            String lValue = lString.substring(lString.indexOf("=")+1);
                            if (lKey.equals("pos")) {
                                lPredictedPoS = lValue;
                            }
                            else {
                                if (lPredictedMorph.length()>0) lPredictedMorph.append("|");
                                lPredictedMorph.append(lKey+"|"+lValue);
                            }
                        }
                        lEntry.setPredictedCategory(Category.pos, lPredictedPoS);
                        lEntry.setPredictedMorphology(lPredictedMorph.toString());
                        break;
                    }
                    default: {
                        lEntry.setPredictedCategory(category, applyTuple(lEntry.getPredictedCategory(Category.pos), lEntry.getWordform()));
                        break;
                    }
                }
                lEntryCounter++;
            }
            lSentenceCounter++;
        }
        resultSet.setFile(getResultConLLFile());
        resultSet.write();
    }

}
