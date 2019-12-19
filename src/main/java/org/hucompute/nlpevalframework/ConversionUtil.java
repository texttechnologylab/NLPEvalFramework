package org.hucompute.nlpevalframework;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ConversionUtil {

    public static void convertConll2LemmaTag(File pSource, File pTarget, String pTag) throws IOException {
        pTag = pTag.toLowerCase();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(pSource), Charset.forName("UTF-8")));
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pTarget), Charset.forName("UTF-8")));
        String lLine = null;
        while ((lLine = lReader.readLine()) != null) {
            String[] lFields = lLine.split("\t", -1);
            if (lFields.length > 1) {
                if (pTag.equals("pos")) {
                    lWriter.println(lFields[1] + "\t" + lFields[2] + "\t" + lFields[4]);
                }
                else if (pTag.equals("joint")) {
                    TreeMap<String, String> lValues = new TreeMap<>();
                    lValues.put("pos", lFields[4]);
                    String[] lSubFields = lFields[6].split("\\|", -1);
                    for (String lSubField:lSubFields) {
                        if (lSubField.contains("=")) {
                            String lAttribute = lSubField.substring(0, lSubField.indexOf("="));
                            String lValue = lSubField.substring(lSubField.indexOf("=")+1);
                            lValues.put(lAttribute, lValue);
                        }
                    }
                    StringBuilder lBuilder = new StringBuilder();
                    for (String lKey:lValues.keySet()) {
                        if (lBuilder.length() > 0) lBuilder.append("|");
                        lBuilder.append(lKey+"="+lValues.get(lKey));
                    }
                    if (lBuilder.length() == 0) lBuilder.append("_");
                    lWriter.println(lFields[1] + "\t" + lFields[2] + "\t" + lBuilder.toString());
                }
                else {
                    String lTagValue = null;
                    String[] lSubFields = lFields[6].split("\\|", -1);
                    for (String lSubField:lSubFields) {
                        if (lSubField.contains("=")) {
                            String lAttribute = lSubField.substring(0, lSubField.indexOf("="));
                            String lValue = lSubField.substring(lSubField.indexOf("=")+1);
                            if (pTag.equals(lAttribute.toLowerCase())) {
                                lTagValue = lValue;
                                break;
                            }
                        }
                    }
                    if ((lTagValue == null) || (lTagValue.length() == 0)) lTagValue = "_";
                    lWriter.println(lFields[1] + "\t" + lFields[2] + "\t" + lTagValue);
                }
            }
            else {
                lWriter.println();
            }
        }
        lWriter.close();
        lReader.close();
    }

}
