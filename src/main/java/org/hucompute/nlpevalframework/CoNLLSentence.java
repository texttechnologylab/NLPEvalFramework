package org.hucompute.nlpevalframework;

import java.io.IOException;
import java.util.List;

public class CoNLLSentence {

    protected CoNLLEntry[] entries;

    public CoNLLSentence(CoNLLEntry[] pEntries) throws IOException {
        entries = pEntries;
        if (!isValid()) throw new IOException("Invalid ID");
    }

    public CoNLLSentence(List<CoNLLEntry> pEntries) throws IOException {
        entries = new CoNLLEntry[pEntries.size()];
        for (int i=0; i<pEntries.size(); i++) {
            entries[i] = pEntries.get(i);
        }
        if (!isValid()) throw new IOException("Invalid ID");
    }

    public CoNLLEntry[] getEntries() {
        return entries;
    }

    public boolean isValid() {
        String lPrefix = "";
        if (entries.length > 0) {
            if (entries[0].getID().contains("_")) {
                lPrefix = entries[0].getID();
                if (lPrefix.contains("_")) lPrefix = lPrefix.substring(0, lPrefix.indexOf("_") + 1);
            }
        }
        for (CoNLLEntry lCoNLLEntry:entries) {
            if (!lCoNLLEntry.getID().startsWith(lPrefix)) {
                return false;
            }
        }
        return true;
    }

}
