package org.hucompute.nlpevalframework;

import java.util.*;

public class CoNLLEntry {

    protected String[] fields;

    public CoNLLEntry(String[] pFields) {
        fields = pFields;
    }

    public CoNLLEntry(CoNLLEntry pCoNLLEntry) {
        fields = new String[pCoNLLEntry.fields.length];
        for (int i=0; i<pCoNLLEntry.fields.length; i++) {
            fields[i] = pCoNLLEntry.fields[i];
        }
    }

    public String getID() {
        return fields[0];
    }

    public String getWordform() {
        return fields[1];
    }

    public void setGoldLemma(String pLemma) {
        fields[2] = pLemma;
    }

    public void setPredictedLemma(String pLemma) {
        fields[3] = pLemma;
    }

    public String getGoldLemma() {
        return fields[2];
    }

    public String getPredictedLemma() {
        return fields[3];
    }

    public String getGoldMorphology() {
        return fields[6];
    }

    public String getPredictedMorphology() {
        return fields[7];
    }

    public void setGoldMorphology(String pString) {
        fields[6] = pString;
    }

    public void setPredictedMorphology(String pString) {
        fields[7] = pString;
    }

    public String getGoldCategory(Experiment.Category pCategory) {
        switch (pCategory) {
            case pos: {
                return fields[4];
            }
            case case_:
            case number:
            case person:
            case gender:
            case degree:
            case tense:
            case voice:
            case mood: {
                for (String lTuple:fields[6].split("\\|")) {
                    if (lTuple.contains("=")) {
                        String lKey = lTuple.substring(0, lTuple.indexOf("=")).trim().toLowerCase();
                        String lValue = lTuple.substring(lTuple.indexOf("=")+1).trim();
                        if (lKey.equals("case")) lKey = "case_";
                        if (lKey.equals(pCategory.name())) {
                            return lValue;
                        }
                    }
                }
                return "_";
            }
            case lemma: {
                return fields[2];
            }
        }
        return "_";
    }

    public String getPredictedCategory(Experiment.Category pCategory) {
        switch (pCategory) {
            case pos: {
                return fields[5];
            }
            case case_:
            case number:
            case person:
            case gender:
            case degree:
            case tense:
            case voice:
            case mood: {
                for (String lTuple:fields[7].split("\\|")) {
                    if (lTuple.contains("=")) {
                        String lKey = lTuple.substring(0, lTuple.indexOf("=")).trim().toLowerCase();
                        String lValue = lTuple.substring(lTuple.indexOf("=")+1).trim();
                        if (lKey.equals("case")) lKey = "case_";
                        if (lKey.equals(pCategory.name())) {
                            return lValue;
                        }
                    }
                }
                return "_";
            }
            case lemma: {
                return fields[3];
            }
        }
        return "_";
    }

    public void setPredictedCategory(Experiment.Category pCategory, String pValue) {
        switch (pCategory) {
            case pos: {
                fields[5] = pValue;
                break;
            }
            case case_:
            case number:
            case person:
            case gender:
            case degree:
            case tense:
            case voice:
            case mood: {
                List<String> lKeys = new ArrayList<>();
                Map<String, String> lMap = new HashMap<>();
                for (String lTuple:fields[7].split("\\|")) {
                    if (lTuple.contains("=")) {
                        String lKey = lTuple.substring(0, lTuple.indexOf("=")).trim().toLowerCase();
                        String lValue = lTuple.substring(lTuple.indexOf("=")+1).trim();
                        if (lKey.equals("case")) lKey = "case_";
                        if ((lValue == null) || (lValue.length() == 0)) lValue = "_";
                        if (!lValue.equals("_")) {
                            lKeys.add(lKey);
                            lMap.put(lKey, lValue);
                        }
                    }
                }
                if ((pValue == null) || (pValue.length() == 0) || pValue.equals("_")) {
                    if (lMap.containsKey(pCategory.name())) {
                        lKeys.remove(pCategory.name());
                        lMap.remove(pCategory.name());
                    }
                }
                else {
                    if (!lMap.containsKey(pCategory.name())) {
                        lKeys.add(pCategory.name());
                        Collections.sort(lKeys);
                    }
                    lMap.put(pCategory.name(), pValue);
                }
                if (lKeys.size() == 0) {
                    fields[7] = "_";
                }
                else {
                    StringBuilder lBuilder = new StringBuilder();
                    for (int i=0; i<lKeys.size(); i++) {
                        if (i>0) lBuilder.append("|");
                        String lKey = lKeys.get(i);
                        if (lKey.equals("case_")) lKey = "case";
                        lBuilder.append(lKey+"="+lMap.get(lKeys.get(i)));
                    }
                    fields[7] = lBuilder.toString();
                }
                break;
            }
            case lemma: {
                fields[3] = pValue;
                break;
            }
            case joint: {
                List<String> lKeys = new ArrayList<>();
                Map<String, String> lMap = new HashMap<>();
                for (String lTuple:pValue.split("\\|")) {
                    if (lTuple.contains("=")) {
                        String lKey = lTuple.substring(0, lTuple.indexOf("=")).trim().toLowerCase();
                        String lValue = lTuple.substring(lTuple.indexOf("=")+1).trim();
                        if (lKey.equals("case")) lKey = "case_";
                        if ((lValue == null) || (lValue.length() == 0)) lValue = "_";
                        if (!lValue.equals("_")) {
                            lKeys.add(lKey);
                            lMap.put(lKey, lValue);
                        }
                    }
                }
                if ((pValue == null) || (pValue.length() == 0) || pValue.equals("_")) {
                    if (lMap.containsKey(pCategory.name())) {
                        lKeys.remove(pCategory.name());
                        lMap.remove(pCategory.name());
                    }
                }
                else {
                    if (!lMap.containsKey(pCategory.name())) {
                        lKeys.add(pCategory.name());
                        Collections.sort(lKeys);
                    }
                    lMap.put(pCategory.name(), pValue);
                }
                if (lMap.containsKey("pos")) {
                    fields[5] = lMap.get("pos");
                    lMap.remove("pos");
                    lKeys.remove("pos");
                }
                if (lKeys.size() == 0) {
                    fields[7] = "_";
                }
                else {
                    StringBuilder lBuilder = new StringBuilder();
                    for (int i=0; i<lKeys.size(); i++) {
                        if (i>0) lBuilder.append("|");
                        String lKey = lKeys.get(i);
                        if (lKey.equals("case_")) lKey = "case";
                        lBuilder.append(lKey+"="+lMap.get(lKeys.get(i)));
                    }
                    fields[7] = lBuilder.toString();
                }
                break;
            }
        }
    }

    public void setGoldCategory(Experiment.Category pCategory, String pValue) {
        switch (pCategory) {
            case pos: {
                fields[4] = pValue;
                break;
            }
            case case_:
            case number:
            case person:
            case gender:
            case degree:
            case tense:
            case voice:
            case mood: {
                List<String> lKeys = new ArrayList<>();
                Map<String, String> lMap = new HashMap<>();
                for (String lTuple:fields[6].split("\\|")) {
                    if (lTuple.contains("=")) {
                        String lKey = lTuple.substring(0, lTuple.indexOf("=")).trim().toLowerCase();
                        String lValue = lTuple.substring(lTuple.indexOf("=")+1).trim();
                        if (lKey.equals("case")) lKey = "case_";
                        if ((lValue == null) || (lValue.length() == 0)) lValue = "_";
                        if (!lValue.equals("_")) {
                            lKeys.add(lKey);
                            lMap.put(lKey, lValue);
                        }
                    }
                }
                if ((pValue == null) || (pValue.length() == 0) || pValue.equals("_")) {
                    if (lMap.containsKey(pCategory.name())) {
                        lKeys.remove(pCategory.name());
                        lMap.remove(pCategory.name());
                    }
                }
                else {
                    if (!lMap.containsKey(pCategory.name())) {
                        lKeys.add(pCategory.name());
                        Collections.sort(lKeys);
                    }
                    lMap.put(pCategory.name(), pValue);
                }
                if (lKeys.size() == 0) {
                    fields[6] = "_";
                }
                else {
                    StringBuilder lBuilder = new StringBuilder();
                    for (int i=0; i<lKeys.size(); i++) {
                        if (i>0) lBuilder.append("|");
                        String lKey = lKeys.get(i);
                        if (lKey.equals("case_")) lKey = "case";
                        lBuilder.append(lKey+"="+lMap.get(lKeys.get(i)));
                    }
                    fields[6] = lBuilder.toString();
                }
                break;
            }
            case lemma: {
                fields[2] = pValue;
                break;
            }
            case joint: {
                List<String> lKeys = new ArrayList<>();
                Map<String, String> lMap = new HashMap<>();
                for (String lTuple:pValue.split("\\|")) {
                    if (lTuple.contains("=")) {
                        String lKey = lTuple.substring(0, lTuple.indexOf("=")).trim().toLowerCase();
                        String lValue = lTuple.substring(lTuple.indexOf("=")+1).trim();
                        if (lKey.equals("case")) lKey = "case_";
                        if ((lValue == null) || (lValue.length() == 0)) lValue = "_";
                        if (!lValue.equals("_")) {
                            lKeys.add(lKey);
                            lMap.put(lKey, lValue);
                        }
                    }
                }
                if ((pValue == null) || (pValue.length() == 0) || pValue.equals("_")) {
                    if (lMap.containsKey(pCategory.name())) {
                        lKeys.remove(pCategory.name());
                        lMap.remove(pCategory.name());
                    }
                }
                else {
                    if (!lMap.containsKey(pCategory.name())) {
                        lKeys.add(pCategory.name());
                        Collections.sort(lKeys);
                    }
                    lMap.put(pCategory.name(), pValue);
                }
                if (lMap.containsKey("pos")) {
                    fields[4] = lMap.get("pos");
                    lMap.remove("pos");
                    lKeys.remove("pos");
                }
                if (lKeys.size() == 0) {
                    fields[6] = "_";
                }
                else {
                    StringBuilder lBuilder = new StringBuilder();
                    for (int i=0; i<lKeys.size(); i++) {
                        if (i>0) lBuilder.append("|");
                        String lKey = lKeys.get(i);
                        if (lKey.equals("case_")) lKey = "case";
                        lBuilder.append(lKey+"="+lMap.get(lKeys.get(i)));
                    }
                    fields[6] = lBuilder.toString();
                }
                break;
            }
        }
    }

    public void setID(String pID) {
        fields[0] = pID;
    }

    public String[] getFields() {
        return fields;
    }
}
