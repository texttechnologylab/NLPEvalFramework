package org.hucompute.nlpevalframework;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class CoNLL {

    protected List<CoNLLSentence> sentences;

    private File file;

    /**
     * Create a Clone
     * @param pCoNLL
     */
    public CoNLL(CoNLL pCoNLL) throws IOException {
        file = pCoNLL.file;
        sentences = new ArrayList<>();
        for (CoNLLSentence lSentence:pCoNLL.getSentences()) {
            CoNLLEntry[] lEntries = new CoNLLEntry[lSentence.getEntries().length];
            int i=0;
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                lEntries[i] = new CoNLLEntry(lEntry);
                i++;
            }
            sentences.add(new CoNLLSentence(lEntries));
        }
    }

    public CoNLL(List<CoNLLSentence> pSentences) {
        sentences = pSentences;
    }

    public CoNLL(File pFile) throws IOException {
        file = pFile;
        sentences = new ArrayList<>();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(pFile), Charset.forName("UTF-8")));
        String lLine = null;
        String lPrev = "";
        List<CoNLLEntry> lCurrentSentence = null;
        while ((lLine = lReader.readLine()) != null) {
            lLine = lLine.trim();
            if (lLine.length()>0) {
                if (lPrev.length() == 0) {
                    // Start new Sentence
                    lCurrentSentence = new ArrayList<CoNLLEntry>();
                }
                lCurrentSentence.add(new CoNLLEntry(lLine.split("\t", -1)));
            }
            else {
                if (lCurrentSentence.size() > 0) {
                    sentences.add(new CoNLLSentence(lCurrentSentence));
                    lCurrentSentence = new ArrayList<CoNLLEntry>();
                }
            }
            lPrev = lLine;
        }
        if (lCurrentSentence.size() > 0) {
            sentences.add(new CoNLLSentence(lCurrentSentence));
            lCurrentSentence = new ArrayList<CoNLLEntry>();
        }
        lReader.close();
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void write() throws IOException {
        Writer lWriter = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"));
        write(lWriter);
        lWriter.close();
    }

    public void write(File pFile) throws IOException {
        file = pFile;
        Writer lWriter = new OutputStreamWriter(new FileOutputStream(pFile), Charset.forName("UTF-8"));
        write(lWriter);
        lWriter.close();
    }

    public void write(Writer pWriter) throws IOException {
        PrintWriter lWriter = new PrintWriter(pWriter);
        for (CoNLLSentence lCoNLLSentence:sentences) {
            for (CoNLLEntry lEntry:lCoNLLSentence.getEntries()) {
                for (int i=0; i<lEntry.getFields().length; i++) {
                    if (i>0) lWriter.print("\t");
                    lWriter.print(lEntry.getFields()[i]);
                }
                if (lEntry.getFields().length < 13) lWriter.print("\t_");
                lWriter.println();
            }
            lWriter.println();
        }
        lWriter.close();
    }

    public List<CoNLLSentence> getSentences() {
        return sentences;
    }

    public int getTokenCount() {
        int lResult = 0;
        for (CoNLLSentence lSentence:sentences) {
            lResult += lSentence.entries.length;
        }
        return lResult;
    }

    public String getStatistics() {
        StringBuilder lResult = new StringBuilder();
        int lSentenceCounter = 0;
        int lTokenCounter = 0;
        Set<String> lLemmas = new HashSet<>();
        Map<String, TObjectIntHashMap<String>> lAttributeValueFreqMap = new HashMap<>();
        for (Experiment.Category lCategory: Experiment.Category.values()) {
            if (lCategory.name().equals("lemma")) continue;
            if (lCategory.name().equals("joint")) continue;
            if (lCategory.name().equals("pipeline")) continue;
            lAttributeValueFreqMap.put(lCategory.name(), new TObjectIntHashMap<>());
        }
        for (CoNLLSentence lSentence:getSentences()) {
            lSentenceCounter++;
            for (CoNLLEntry lEntry:lSentence.getEntries()) {
                lTokenCounter++;
                String lKey = lEntry.getGoldLemma()+"\t"+lEntry.getGoldCategory(Experiment.Category.pos);
                lLemmas.add(lKey);
                for (Experiment.Category lCategory: Experiment.Category.values()) {
                    if (lCategory.name().equals("lemma")) continue;
                    if (lCategory.name().equals("joint")) continue;
                    if (lCategory.name().equals("pipeline")) continue;
                    String lValue = lEntry.getGoldCategory(lCategory);
                    if ((lValue != null) && (lValue.length() > 0) && (!lValue.equals("_"))) {
                        lAttributeValueFreqMap.get(lCategory.name()).adjustOrPutValue(lValue, 1, 1);
                    }
                }
            }
        }
        lResult.append("Sentences\t"+lSentenceCounter+"\n");
        lResult.append("Tokens\t"+lTokenCounter+"\n");
        lResult.append("Lemmas\t"+lLemmas.size()+"\n");
        for (Map.Entry<String, TObjectIntHashMap<String>> lEntry:lAttributeValueFreqMap.entrySet()) {
            lResult.append(lEntry.getKey()+"\n");
            for (String lKey:lEntry.getValue().keySet()) {
                lResult.append("\t"+lKey+"\t"+lEntry.getValue().get(lKey)+"\n");
            }
        }
        return lResult.toString();
    }

}
