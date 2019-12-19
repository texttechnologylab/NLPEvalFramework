package org.hucompute.nlpevalframework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class IOUtil {

    public static void deleteRecursively(File pFile) {
        if (pFile.isDirectory()) {
            for (File lSub:pFile.listFiles()) {
                deleteRecursively(lSub);
            }
        }
        pFile.delete();
    }

    public static void copyFile(File pSource, File pTarget) throws IOException {
        byte[] lBuffer = new byte[65536];
        int lRead = 0;
        FileInputStream lInput = new FileInputStream(pSource);
        FileOutputStream lOutput = new FileOutputStream(pTarget);
        while ((lRead = lInput.read(lBuffer)) > 0) {
            lOutput.write(lBuffer, 0, lRead);
        }
        lOutput.flush();
        lOutput.close();
        lInput.close();
    }

}
