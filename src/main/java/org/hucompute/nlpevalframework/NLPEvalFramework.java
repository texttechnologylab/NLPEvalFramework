package org.hucompute.nlpevalframework;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class NLPEvalFramework {

    public static void printSyntax() {
        System.out.println("NLPEvalFramework experiment -t <tool> -c <categories> -tr <training> -te <gold> -l <language> [-rb <resultbasedir>] [-jc <categories>] [-lem <lemmatizer>] [-mv <mv-tool>-<mv-tool>...] [-var variant]");
        System.out.println("NLPEvalFramework evaluation -t <tool> -c <categories> -tr <training> -te <gold> -l <language> [-rb <resultbasedir>] [-lem <lemmatizer>]");
        System.out.println("-jc cat1,cat2,... for joint and pipeline");
        System.out.println("Taggers:");
        for (Experiment.Tool lTool : Experiment.Tool.values()) {
            System.out.println("\t"+ lTool.name());
        }
        System.out.println("Categories:");
        for (Experiment.Category lCategory:Experiment.Category.values()) {
            System.out.println("\t"+lCategory.name());
        }
        System.exit(0);
    }

    public static String msToTime(long pMS) {
        if (pMS < 1000) {
            return pMS+"ms";
        }
        else if (pMS < 60000) {
            return (long)(Math.round(pMS/1000d))+"s";
        }
        else if (pMS < 60000l*60l) {
            long lMinutes = (long)Math.floor(pMS / 60000d);
            long lSeconds = Math.round((pMS-(lMinutes*60000))/1000d);
            return lMinutes+"m"+lSeconds+"s";
        }
        else {
            long lHours = (long)Math.floor(pMS / 3600000d);
            long lRestMS = pMS-(lHours*3600000);
            long lMinutes = (long)Math.floor(lRestMS / 60000d);
            long lSeconds = Math.round((lRestMS-(lMinutes*60000))/1000d);
            return lHours+"h"+lMinutes+"m"+lSeconds+"s";
        }
    }

    public static void main(String[] args) throws Exception {
        if ((args.length == 0)) {
            printSyntax();
        }
        boolean lForceTraining = false;
        boolean lOOV = false;
        String lMode = args[0];
        Experiment.Tool lTool = null;
        Experiment.Tool lLemmatizer = null;
        List<Experiment.Category> lCategories = new ArrayList<>();
        List<Experiment.Category> lJCCategories = new ArrayList<>();
        List<Experiment.Tool> lMajorityVoteTools = new ArrayList<>();
        File lResultBaseDir = new File("results");
        File lTrainFile = null;
        File lTestFile = null;
        boolean lVerbose = false;
        String lLanguage = null;
        String lVariant = null;
        Map<String, String> lExtendedParameterMap = new HashMap<>();
        for (int i=0; i<args.length; i++) {
            if (args[i].equals("-t")) {
                lTool = Experiment.Tool.valueOf(args[i+1]);
                i++;
            }
            else if (args[i].equals("-rb")) {
                lResultBaseDir = new File(args[i+1]);
                i++;
            }
            else if (args[i].equals("-oov")) {
                lOOV = true;
            }
            else if (args[i].equals("-f")) {
                lForceTraining = true;
            }
            else if (args[i].equals("-v")) {
                lVerbose = true;
            }
            else if (args[i].equals("-var")) {
                lVariant = args[i+1];
                i++;
            }
            else if (args[i].equals("-lem")) {
                lLemmatizer = Experiment.Tool.valueOf(args[i+1]);
                i++;
            }
            else if (args[i].equals("-c")) {
                for (String lCategory:args[i+1].split(",",-1)) {
                    if (lCategory.length()>0) {
                        lCategories.add(Experiment.Category.valueOf(lCategory));
                    }
                }
                i++;
            }
            else if (args[i].equals("-tr")) {
                lTrainFile = new File(args[i+1]);
                i++;
            }
            else if (args[i].equals("-te")) {
                lTestFile = new File(args[i+1]);
                i++;
            }
            else if (args[i].equals("-l")) {
                lLanguage = args[i+1];
                i++;
            }
            else if (args[i].equals("-jc")) {
                for (String lCategory:args[i+1].split(",",-1)) {
                    if (lCategory.length()>0) {
                        lJCCategories.add(Experiment.Category.valueOf(lCategory));
                    }
                }
                i++;
            }
            else if (args[i].startsWith("-X")) {
                String lParam = args[i].substring(2);
                lExtendedParameterMap.put(lParam, args[i+1]);
                i++;
            }
            else if (args[i].equals("-mv")) {
                for (String lString:args[i+1].split("-", -1)) {
                    if (lString.length() > 0) {
                        lMajorityVoteTools.add(Experiment.Tool.valueOf(lString));
                    }
                }
                i++;
            }
        }

        if (lMode.equals("experiment") || lMode.equals("trainjackknifing")) {
            CoNLL lTrainCoNLL = new CoNLL(lTrainFile);
            CoNLL lTestCoNLL = new CoNLL(lTestFile);
            new File("results").mkdirs();
            for (Experiment.Category lCategory : lCategories) {
                System.out.println("Computing " + lTool.name() + " " + lCategory.name() + " " + lTrainFile.getName() + " " + lTestFile.getName()+" "+(lVariant != null ? lVariant : ""));
                if (lCategory.name().equals(Experiment.Category.pipeline.name())) {
                    PipelineExperiment lPipelineExperiment = new PipelineExperiment(lResultBaseDir, lTool, lJCCategories, lTrainCoNLL, lTestCoNLL, lLanguage, lExtendedParameterMap, lMajorityVoteTools, lVariant, lOOV, lForceTraining);
                    if (lMode.equals("experiment")) {
                        lPipelineExperiment.execute();
                    }
                    else {
                        lPipelineExperiment.trainJackKnifing(10);
                    }
                } else {
                    if (Evaluation.exists(lResultBaseDir, lTool, lCategory, lTrainCoNLL, lTestCoNLL, lVariant) && lMode.equals("experiment") && !lForceTraining) continue;
                    Experiment lExperiment = null;
                    switch (lTool) {
                        case Lapos: {
                            lExperiment = new LaposExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories);
                            break;
                        }
                        case Mate: {
                            lExperiment = new MateExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories);
                            break;
                        }
                        case OpenNLP: {
                            lExperiment = new OpenNLPExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories);
                            break;
                        }
                        case Stanford: {
                            lExperiment = new StanfordExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories);
                            break;
                        }
                        case TreeTagger: {
                            lExperiment = new TreeTaggerExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories);
                            break;
                        }
                        case RDRPOSTagger: {
                            lExperiment = new RDRPOSTaggerExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories, lExtendedParameterMap, lVariant);
                            break;
                        }
                        case TnT: {
                            lExperiment = new TnTExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories);
                            break;
                        }
                        case FLORS: {
                            lExperiment = new FLORSExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories, lExtendedParameterMap);
                            break;
                        }
                        case NonLexNN: {
                            lExperiment = new NonLexNNExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage);
                            break;
                        }
                        case MarMoT: {
                            lExperiment = new MarMoTExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories, lExtendedParameterMap, lVariant);
                            break;
                        }
                        case MarMoTLATPOS: {
                            lExperiment = new MarMoTLATPOSExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories, lExtendedParameterMap, lVariant);
                            break;
                        }
                        case FastText: {
                            lExperiment = new FastTextExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories);
                            break;
                        }
                        case BLSTMRNN: {
                            lExperiment = new BLSTMRNNExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories, lExtendedParameterMap, lVariant);
                            break;
                        }
                        case MajorityVote: {
                            lExperiment = new MajorityVoteExperiment(lResultBaseDir, lCategory, lTrainCoNLL, lTestCoNLL, lLanguage, lJCCategories, lMajorityVoteTools, lVariant);
                            break;
                        }
                    }
                    if (lMode.equals("experiment")) {
                        lExperiment.execute();
                    }
                    else {
                        lExperiment.trainJackKnifing(10);
                    }
                }
            }
            if (lLemmatizer != null) {
                if (lMode.equals("experiment")) {
                    switch (lLemmatizer) {
                        case LemmaGenPlain: {
                            LemmaGenPlainExperiment lLemmaGenPlainExperiment = new LemmaGenPlainExperiment(lResultBaseDir, new Evaluation(lResultBaseDir, lTool, lCategories.get(0), lTrainCoNLL, lTestCoNLL, null, lVariant, lOOV), lLanguage);
                            if (!lLemmaGenPlainExperiment.isResultConLLFileExistent())
                                lLemmaGenPlainExperiment.execute();
                            break;
                        }
                        case MarMoTLAT: {
                            MarMoTLATExperiment lMarMoTLATExperiment = new MarMoTLATExperiment(lResultBaseDir, new Evaluation(lResultBaseDir, lTool, lCategories.get(0), lTrainCoNLL, lTestCoNLL, null, lVariant, lOOV), lLanguage);
                            if (!lMarMoTLATExperiment.isResultConLLFileExistent()) lMarMoTLATExperiment.execute();
                            break;
                        }
                    }
                }
            }
        }
        else if (lMode.equals("evaluation")) {
            for (Experiment.Category lCategory : lCategories) {
                Evaluation lEvaluation = new Evaluation(lResultBaseDir, lTool, lCategory, new CoNLL(lTrainFile), new CoNLL(lTestFile), lLemmatizer, lVariant, lOOV);
                lEvaluation.evaluate();
                System.out.println((lLemmatizer == null ? lTool.name() : lLemmatizer.name()) + "\t" + (lLemmatizer != null ? "lemma" : lCategory.name()) + "\t" + lTrainFile.getName() + "\t" + lTestFile.getName() + "\t" + Double.toString(lEvaluation.getAccuracy()).replace(".",",")+"\t"+(lEvaluation.getVariant() != null ? lEvaluation.getVariant() : ""));
                if (lVerbose) {
                    for (StringPairFrequency lPair : lEvaluation.getMismatchingPairFrequencies()) {
                        System.out.println(lPair.getFrequency() + "\t" + lPair.getGold() + "\t" + lPair.getPredicted());
                    }
                }
            }
        }
    }

}
