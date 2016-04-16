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

import java.io.FileNotFoundException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Map.Entry;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author W. Hatfield
 * @author U. Jaimini
 * @author U. Panjala
 */
public class TermCountProbabilityModel implements Serializable {
    
    /**
     * CountProbabilityPair - structure for multiple primitive data type access.
     */
    private class CountProbabilityPair implements Serializable {
        public double prob; // probability of the term in the dataset
        public int count;   // count of the term in the dataset
        @Override
        public String toString() {
            return "{C: " + count + " && P: " + prob + "}";
        }
    }
    
    /**
     * Primary data structure for storing terms, counts, and probabilities.
     */
    private final HashMap<String, CountProbabilityPair> MODEL;
    
    /**
     * Convenient counter for storing the total number of map entries.
     */
    private int totalTermCount;
    
    /**
     * Default Constructor - initializes a new empty hashmap and counter.
     */
    public TermCountProbabilityModel() {
        MODEL = new HashMap<>();
        totalTermCount = 0;
    }
    
    /**
     * Serialized Model Constructor - generates a new TermCountProbabilityModel
     * that is constructed from the data stored in the serialized model file.
     * 
     * If any errors are encountered during reading the default values are used.
     * 
     * @param serializedModelFileName serialized model to construct from
     */
    public TermCountProbabilityModel(String serializedModelFileName) {
        TermCountProbabilityModel tcpm = null;
        try {
            FileInputStream fis = new FileInputStream(serializedModelFileName);
            ObjectInputStream objReader = new ObjectInputStream(fis);
            tcpm = (TermCountProbabilityModel) objReader.readObject();
        } catch (ClassNotFoundException ex) {
            System.err.println("ClassNotFoundException: " + ex.getMessage());
        }catch (FileNotFoundException ex) {
            System.err.println("FileNotFoundException: " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println("IOException: " + ex.getMessage());
        }
        if (tcpm != null) {
            this.totalTermCount = tcpm.totalTermCount;
            this.MODEL = tcpm.MODEL;
        } else {
            MODEL = new HashMap<>();
            totalTermCount = 0;
        }
    }
    
    /**
     * Compares this object to the TermCountProbabilityModel object that was
     * passed as the only argument ot the function, returning a double that
     * represents the probability (similarity) of the passed model to this one.
     * 
     * @param tcpm
     * @return 
     */
    public double getClassificationProbability(TermCountProbabilityModel tcpm) {
        double probability = 1.0;
        for (Entry<String, CountProbabilityPair> entry : tcpm.MODEL.entrySet()) {
            if (this.MODEL.containsKey(entry.getKey())) {
                double thisTermProb = this.MODEL.get(entry.getKey()).prob;
                double tcpmTermProb = entry.getValue().prob;
                double termProbability = thisTermProb * tcpmTermProb;
                probability = probability * termProbability;
            } else {
                double multiplier = 1.0 / (MODEL.size() + totalTermCount);
                probability = probability * multiplier;
            }
        }
        return probability;
    }
    
    /**
     * Writes the current state of the calling object out to disk, and uses the
     * String argument as the PATH TO and NAME OF 'this' serialized object.
     * 
     * @param outputFileName 
     */
    public void serializeTermCountProbabilityModel(String outputFileName) {
        TermCountProbabilityModel tcpm = new TermCountProbabilityModel();
        tcpm.totalTermCount = this.totalTermCount;
        tcpm.MODEL.putAll(this.MODEL);
        try {
            FileOutputStream fos = new FileOutputStream(outputFileName);
            ObjectOutputStream objWriter = new ObjectOutputStream(fos);
            objWriter.writeObject(tcpm);    // write the object
            objWriter.flush();              // flush the buffer
            objWriter.close();              // close the writer
            return;                         // exit the function
        } catch (FileNotFoundException ex) {
            System.err.println("FileNotFoundException: " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println("IOException: " + ex.getMessage());
            System.err.println(ex.toString());
        }
        System.err.println("ERR @ serializeTermCountProbabilityModel !!!");
    }
    
    /**
     * Either adds a new term to the model with an initial count of 1 and an
     * initial probability of -1 (calculated when all pushes complete), OR if
     * the term is already present in the model it's counter is incremented.
     * 
     * @param term the string to add to the model or increment the counter of.
     */
    public void pushTerm(String term) {
        if (MODEL.containsKey(term)) {
            MODEL.get(term).count++;
        } else {
            CountProbabilityPair cpp = new CountProbabilityPair();
            cpp.count = 1;
            cpp.prob = -1;
            MODEL.put(term, cpp);
        }
        totalTermCount++;
    }
    
    /**
     * Iterates through all elements in the model (HashMap) and computes the
     * probability of the term (key) in the element by dividing the individual
     * term count over the total term count, then stores the result in model.
     */
    public void computeTheTermProbabilites() {
        for (Entry<String, CountProbabilityPair> entry : MODEL.entrySet()) {
            double termCount = (double) entry.getValue().count;
            double probability = termCount / totalTermCount;
            entry.getValue().prob = probability;
        }
    }
    
    /**
     * Gets the set of all words (keys) in this model.
     * 
     * @return all keys in this model
     */
    public Set<String> getVocabulary() {
        HashSet<String> vocabulary = new HashSet<>();
        vocabulary.addAll(MODEL.keySet());
        return vocabulary;
    }
    
    /**
     * Returns the probability of the the term in the data set, if the function
     * computeTheTermProbabilites() has not been called the default value -1 is
     * returned, and if the term is not present in the data set 0 is returned.
     * 
     * @param term
     * @return 
     */
    public double getTermProbability(String term) {
        if (MODEL.containsKey(term)) {
            double prob = MODEL.get(term).prob;
            return prob;
        } else {
            return 0;
        }
    }
    
    /**
     * Returns the count of the term in the data set, zero if it is not present.
     * 
     * @param term
     * @return 
     */
    public int getTermCount(String term) {
        if (MODEL.containsKey(term)) {
            int termCount = MODEL.get(term).count;
            return termCount;
        } else {
            return 0;
        }
    }
    
    /**
     * Returns the size of the HashMap structure used as the model.
     * 
     * @return 
     */
    public int getModelSize() {
        int size = MODEL.size();
        return size;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, CountProbabilityPair> entry : MODEL.entrySet()) {
            sb.append(entry.getKey());
            sb.append(" => ");
            sb.append(entry.getValue().toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
