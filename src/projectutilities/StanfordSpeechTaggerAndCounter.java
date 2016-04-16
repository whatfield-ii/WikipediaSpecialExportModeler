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
package projectutilities;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.util.Map.Entry;
import java.io.IOException;
import java.io.FileWriter;
import java.util.HashMap;
import java.io.File;

/**
 * StandfordSpeechTaggerAndCounter.
 *
 * @author W. Hatfield
 * @author U. Jaimini
 * @author U. Panjala
 */
public class StanfordSpeechTaggerAndCounter {
    
    private static final String MODELFILE =
            "stanford-postagger/english-left3words-distsim.tagger";
    private static final MaxentTagger TAGGER = new MaxentTagger(MODELFILE);
    private static final HashMap<String, Integer> MAP = new HashMap<>();
    
    /**
     * 
     * @param text
     * @param filename 
     */
    public void tagTextAndWriteFile(String text, String filename) {
        try {
            FileWriter fw = new FileWriter(new File(filename));
            fw.write(tagNormalizedString(text));
            fw.close();
        } catch (IOException ex) {
            System.err.println("IOException: " + ex.getMessage());
        }
    }
    
    /**
     * tagNormalizedString - takes a single String argument, that should have
     * been normalized by the WikipediaSpecialExportProcessor, then completes
     * the Part-of-Speech Tagging using Stanford Maximum Entropy Tagger, after
     * which the terms are counted and added to a hash map to keep count.
     * 
     * @param toTag
     * @return 
     */
    public String tagNormalizedString(String toTag) {
        String taggedString = TAGGER.tagString(toTag);
        
        String[] taggedStringArray = taggedString.split(" ");
        for (String toCheck : taggedStringArray) {
            Integer wordCount = MAP.get(toCheck);
            wordCount = (wordCount != null) ? wordCount + 1 : 1;
            MAP.put(toCheck, wordCount);
        }
        
        return taggedString;
    }
    
    /**
     *
     * @param filename
     */
    public void writeReport(String filename) {
        try {
            FileWriter fw = new FileWriter(new File(filename));
            for (Entry<String, Integer> entry : MAP.entrySet()) {
                String toWrite = entry.getKey() + " -> " + entry.getValue();
                fw.write(toWrite + "\n");
            }
            fw.close();
        } catch (IOException ex) {
            System.err.println("IOException: " + ex.getMessage());
        }
    }
    
    /**
     * Clears the collection of processed words and their counts.
     */
    public void resetWordCount() { MAP.clear(); }
    
}
