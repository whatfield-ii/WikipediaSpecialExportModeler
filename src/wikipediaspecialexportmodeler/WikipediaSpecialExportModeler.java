/* Copyright (c) 2016 William Hatfield, Utkarshani Jaimini, Uday Sagar Panjala.
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. <-- LICENSE.md -->
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc. 51 Franklin
 * Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package wikipediaspecialexportmodeler;
import projectutilities.*;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.io.IOException;
import java.io.FileReader;
import java.io.File;

/**
 * 
 * @author W. Hatfield
 * @author U. Jaimini
 * @author U. Panjala
 */
public class WikipediaSpecialExportModeler {
    /**************************************************************************/
    // The Top Level Data Directories
    private static final String SPECIAL_EXPORTS = "files/special_exports/";
    private static final String REFINED_XML = "files/refined_xml/";
    private static final String TAGGED_TEXT = "files/tagged_text/";
    private static final String MODEL_FILES = "files/model_files/";
    // The Tagged Text Output Directories
    private static final String TAGGED_OBJECTS = TAGGED_TEXT + "objects/";
    private static final String TAGGED_WOMEN = TAGGED_TEXT + "women/";
    private static final String TAGGED_MEN = TAGGED_TEXT + "men/";
    //
    private static final String MODEL_OF_OBJECTS = MODEL_FILES + "objects.mdl";
    private static final String MODEL_OF_WOMEN = MODEL_FILES + "women.mdl";
    private static final String MODEL_OF_MEN = MODEL_FILES + "men.mdl";
    /**************************************************************************/
    private static WikipediaSpecialExportProcessor WSEP;    // export processor
    private static StanfordSpeechTaggerAndCounter SSTC;     // speech tagger
    private static final int NUMBER_OF_PARAGRAPHS_FOR_TRAINING = 1;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        WSEP = new WikipediaSpecialExportProcessor();
        SSTC = new StanfordSpeechTaggerAndCounter();
        //
        System.out.println("processing export files ...");
        initWikipediaSpecialExportProcessor();
        System.out.println("tagging refined xml");
        parseAndTagProcessedExportTexts();
        //
        System.out.println("training the object model");
        trainModelsFromTaggedText(TAGGED_OBJECTS, MODEL_OF_OBJECTS);
        System.out.println("training the women model");
        trainModelsFromTaggedText(TAGGED_WOMEN, MODEL_OF_WOMEN);
        System.out.println("training the men model");
        trainModelsFromTaggedText(TAGGED_MEN, MODEL_OF_MEN);
        //
        System.out.println("Training Complete - Have A Good Day!");
        //
        
    }
    
    private static void initWikipediaSpecialExportProcessor() {
        String[] exportFiles = new File(SPECIAL_EXPORTS).list();
        if (exportFiles == null || exportFiles.length == 0) {
            // the directory is empty of does not exist, this is a fatal error
            System.err.println("ERR @ initWikipediaSpecialExportProcessor");
            System.err.println("ERR: Directory Empty @ " + SPECIAL_EXPORTS);
            System.err.println("FATAL ERROR: Exiting Program !");
            System.exit(1);
        } else {
            for (String fileName : exportFiles) {
                String exportType = determineFilesType(fileName);
                if (exportType.equals("objects") 
                 || exportType.equals("women")
                 || exportType.equals("men")) {
                    String xmlFileName = REFINED_XML + exportType + ".xml";
                    String exportInput = SPECIAL_EXPORTS + fileName;
                    System.err.println("Processing Export File @ " + exportInput);
                    WSEP.convertSpecialExport(exportInput, xmlFileName);
                    System.err.println("Processing Complete -> " + xmlFileName);
                } else {
                    System.err.print("ERR: Export File Not Processed: ");
                    System.err.println(SPECIAL_EXPORTS + fileName);
                }
            }
        }
    }
    
    private static String determineFilesType(String fileName) {
        String objects = "objects";
        String women = "women";
        String men = "men";
        if (fileName.toLowerCase().contains(objects)) return objects;
        if (fileName.toLowerCase().contains(women)) return women;
        if (fileName.toLowerCase().contains(men)) return men;
        return null;
    }
    
    private static void parseAndTagProcessedExportTexts() {
        String[] processedExports = new File(REFINED_XML).list();
        if (processedExports == null || processedExports.length == 0) {
            // the directory is empty of does not exist, this is a fatal error
            System.err.println("ERR @ parseAndTagProcessedExportTexts");
            System.err.println("ERR: Directory Empty @ " + REFINED_XML);
            System.err.println("FATAL ERROR: Exiting Program !");
            System.exit(2);
        } else {
            for (String processed : processedExports) {
                processed = REFINED_XML + processed; // prepend file path
                System.err.println("Parsing and Tagging @ " + processed);
                ArrayList<String> texts 
                        = WSEP.getTextsFromProcessedExport(processed,
                                NUMBER_OF_PARAGRAPHS_FOR_TRAINING);
                String processedType = determineFilesType(processed);
                switch (processedType) {
                    case "objects": { 
                        tagAndSaveTexts(texts, TAGGED_OBJECTS);
                        break;
                    }
                    case "women" : {
                        tagAndSaveTexts(texts, TAGGED_WOMEN);
                        break;
                    }
                    case "men": {
                        tagAndSaveTexts(texts, TAGGED_MEN);
                        break;
                    }
                    default: {
                        System.err.print("ERR: XML File Not Parsed: ");
                        System.err.println(processed);
                    }
                }
            }
        }
    }
    
    private static void tagAndSaveTexts(ArrayList<String> texts, String dir) {
        for (int i = 0; i < texts.size(); i++) {
            String fileName = Integer.toString(i); // all filenames same length
            while (fileName.length() < 7) fileName = '0' + fileName;
            fileName = dir + fileName;
            SSTC.tagTextAndWriteFile(texts.get(i), fileName);
        }
    }
    
    private static void trainModelsFromTaggedText(String dir, String modelName) {
        String[] tagged_text_files = new File(dir).list();
        if (tagged_text_files == null || tagged_text_files.length == 0) {
            // the directory is empty of does not exist, this is a fatal error
            System.err.println("ERR @ trainModelsFromTaggedText");
            System.err.println("ERR: Directory Empty @ " + dir);
            System.err.println("FATAL ERROR: Exiting Program !");
            System.exit(3);
        } else {
            TermCountProbabilityModel tcpm = new TermCountProbabilityModel();
            // iterate through all of the files in the directory
            for(String text_file_name : tagged_text_files) {
                try {
                    text_file_name = dir + text_file_name;
                    FileReader fr = new FileReader(text_file_name);
                    StringBuilder sb = new StringBuilder();
                    // read in all characters from the current file
                    while (fr.ready()) sb.append((char)fr.read());
                    String[] tokens = sb.toString().split(" ");
                    // add all proper and possessive pronouns to the model
                    for (String token : tokens) {
                        if (token.endsWith("_PRP$") || token.endsWith("_PRP")) {
                            tcpm.pushTerm(token);
                        }
                    }
                } catch (FileNotFoundException ex) {
                    System.err.println("FileNotFoundException: " + ex.getMessage());
                } catch (IOException ex) {
                    System.err.println("IOException: " + ex.getMessage());
                }
            }
            // compute the probabilities of the model and save the model
            tcpm.computeTheTermProbabilites(10);
            System.out.println(tcpm.toString());
            tcpm.serializeTermCountProbabilityModel(modelName);
        }
    }
}
