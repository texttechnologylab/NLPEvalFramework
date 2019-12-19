package org.hucompute.nlpevalframework;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ResultEntry {

    private Experiment.Tool tool;
    private Experiment.Category category;
    private String language;
    private File trainFile;
    private File testFile;
    private String trainParameters;
    private String testParameters;
    private long trainingTime;
    private long testTime;
    private double correctTokenRatio;
    private List<StringPairFrequency> mismatches = new ArrayList<>();

    public ResultEntry(File pFile) throws IOException {
        BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(pFile), Charset.forName("UTF-8")));
        String lLine = null;
        while ((lLine = lReader.readLine()) != null) {
            String[] lFields = lLine.split("\t", -1);
            if (lFields[0].equals("Tool") || lFields[0].equals("Tagger")) {
                tool = Experiment.Tool.valueOf(lFields[1]);
            }
            else if (lFields[0].equals("Category")) {
                category = Experiment.Category.valueOf(lFields[1]);
            }
            else if (lFields[0].equals("Language")) {
                language = lFields[1];
            }
            else if (lFields[0].equals("Train")) {
                trainFile = new File(lFields[1]);
            }
            else if (lFields[0].equals("Test")) {
                testFile = new File(lFields[1]);
            }
            else if (lFields[0].equals("TrainParameters:")) {
                StringBuilder lBuilder = new StringBuilder();
                while (!(lLine = lReader.readLine()).equals("TestParameters:")) {
                    lBuilder.append(lLine+"\n");
                }
                trainParameters = lBuilder.toString().trim();
                lBuilder = new StringBuilder();
                while (!(lLine = lReader.readLine()).startsWith("Training Time")) {
                    lBuilder.append(lLine+"\n");
                }
                testParameters = lBuilder.toString().trim();
                lFields = lLine.split("\t", -1);
            }
            if (lFields[0].equals("Training Time[ms]")) {
                trainingTime = Long.parseLong(lFields[1]);
            }
            else if (lFields[0].equals("Test Time[ms]")) {
                testTime = Long.parseLong(lFields[1]);
            }
            else if (lFields[0].equals("CorrectTokenRation")) {
                correctTokenRatio = Double.parseDouble(lFields[1]);
            }
            else if (lFields[0].equals("MatchingPairFrequencies:")) {
                while ((lLine = lReader.readLine()) != null) {
                    if (lLine.length()>0) {
                        lFields = lLine.split("\t", -1);
                        mismatches.add(new StringPairFrequency(Integer.parseInt(lFields[0]), lFields[1], lFields[2]));
                    }
                }
                break;
            }
        }
        lReader.close();
    }

    public Experiment.Tool getTool() {
        return tool;
    }

    public Experiment.Category getCategory() {
        return category;
    }

    public String getLanguage() {
        return language;
    }

    public File getTrainFile() {
        return trainFile;
    }

    public File getTestFile() {
        return testFile;
    }

    public String getTrainParameters() {
        return trainParameters;
    }

    public String getTestParameters() {
        return testParameters;
    }

    public long getTrainingTime() {
        return trainingTime;
    }

    public long getTestTime() {
        return testTime;
    }

    public double getCorrectTokenRatio() {
        return correctTokenRatio;
    }

    public List<StringPairFrequency> getMismatches() {
        return mismatches;
    }
}
