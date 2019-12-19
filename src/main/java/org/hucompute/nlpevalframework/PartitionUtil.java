package org.hucompute.nlpevalframework;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class PartitionUtil {

    public static void splitHalf(File pSource) throws IOException {
        String lAbsolute = pSource.getAbsolutePath();
        lAbsolute = lAbsolute.substring(0, lAbsolute.lastIndexOf("."));
        File pTarget1 = new File(lAbsolute+".1.conll");
        File pTarget2 = new File(lAbsolute+".2.conll");
        List<String> lSentences = new ArrayList<>();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(pSource), Charset.forName("UTF-8")));
        String lLine = null;
        StringBuilder lCurrent = new StringBuilder();
        while ((lLine = lReader.readLine()) != null) {
            lLine = lLine.trim();
            if (lLine.length() > 0) {
                lCurrent.append(lLine+"\n");
            }
            else {
                if (lCurrent.length() > 0) {
                    lSentences.add(lCurrent.toString());
                }
                lCurrent = new StringBuilder();
            }
        }
        if (lCurrent.length() > 0) {
            lSentences.add(lCurrent.toString());
        }
        PrintWriter lWriter1 = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pTarget1), Charset.forName("UTF-8")));
        PrintWriter lWriter2 = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pTarget2), Charset.forName("UTF-8")));
        for (int i=0; i<lSentences.size(); i++) {
            if (i < lSentences.size()/2d) {
                lWriter1.println(lSentences.get(i));
            }
            else {
                lWriter2.println(lSentences.get(i));
            }
        }
        lWriter1.close();
        lWriter2.close();
        lReader.close();
    }

    public static void split(File pSource, boolean pShuffleSentenceOrder, File[] pTargets, double... pFractions) throws IOException {
        List<String> lSentences = new ArrayList<>();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(pSource), Charset.forName("UTF-8")));
        String lLine = null;
        StringBuilder lCurrent = new StringBuilder();
        while ((lLine = lReader.readLine()) != null) {
            lLine = lLine.trim();
            if (lLine.length() > 0) {
                lCurrent.append(lLine+"\n");
            }
            else {
                if (lCurrent.length() > 0) {
                    lSentences.add(lCurrent.toString());
                }
                lCurrent = new StringBuilder();
            }
        }
        if (lCurrent.length() > 0) {
            lSentences.add(lCurrent.toString());
        }
        if (pShuffleSentenceOrder) {
            List<String> lList = new ArrayList<>();
            while (lSentences.size() > 0) {
                lList.add(lSentences.remove((int)Math.floor(Math.random()*lSentences.size())));
            }
            lSentences = lList;
        }
        int lIndex = 0;
        for (int i=0; i<pFractions.length; i++) {
            int lStartIndex = lIndex;
            PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pTargets[i]), Charset.forName("UTF-8")));
            while ((((lIndex-lStartIndex)/(double)lSentences.size()) < pFractions[i]) || i == pFractions.length-1) {
                if (lIndex >= lSentences.size()) break;
                lWriter.println(lSentences.get(lIndex));
                lIndex++;
            }
            lWriter.close();
            System.out.println(i+"\t"+(lIndex-lStartIndex)/(double)lSentences.size());
        }
    }

}
