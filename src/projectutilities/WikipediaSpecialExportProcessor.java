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

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Transformer;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import java.util.ArrayList;
import java.io.IOException;
import java.io.File;

/**
 * WikipediaSpecialExportProcessor - a collection of some file processing tools.
 * 
 * Developed to work with wikipedias special export files that can be retrieved
 * from the https://en.wikipedia.org/wiki/Special:Export web page. This suite
 * makes heavy use of the Document Object Model (DOM) for parsing the export and
 * also for writing the processed data to disk.
 * 
 * @author W. Hatfield
 * @author U. Jaimini
 * @author U. Panjala
 */
public class WikipediaSpecialExportProcessor {
    /**
     * The (simple) Wikipedia Page Data Structure
     */
    private class WikiPage {
        public ArrayList<String> categories;
        public ArrayList<String> anchors;
        public ArrayList<String> texts;
        public String title;
    }
    
    /**
     * 
     * @param filename
     * @param depth
     * @return 
     */
    public ArrayList<String> getTextsFromProcessedExport(String filename, int depth) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(filename);
            NodeList nodes = document.getElementsByTagName("page");
            ArrayList<String> texts = new ArrayList<>();
            
            for (int i = 0; i < nodes.getLength(); i++) {
                Element page = (Element) nodes.item(i);
                //String s = page.getElementsByTagName("text").item(0).getTextContent();
                NodeList paragraphs = page.getElementsByTagName("text");
                for (int j = 0; j < paragraphs.getLength() && j < depth; j++) {
                    texts.add(paragraphs.item(j).getTextContent());
                }
            }
            
            return texts;
            
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            System.err.println("ERR @ getTextsFromProcessedExport: " + ex.toString());
        }
        return null;
    }
    
    /**
     * 
     * @param export
     * @param xml 
     */
    public void convertSpecialExport(String export, String xml) {
        Document document = importSpecialExport(export);
        saveDocumentAsXML(document, xml);
    }
    
    /**
     * 
     * @param filename
     * @return 
     */
    private Document importSpecialExport(String filename) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(filename);
            //
            NodeList nodes = document.getElementsByTagName("page");
            ArrayList<WikiPage> wikis = processPageNodeList(nodes);
            //
            Document processed = makeDocumentFromWikis(wikis);
            return processed;
            //
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            System.err.println("ERR @ importSpecialExport: " + ex.getMessage());
        }
        return null;
    }
    
    /**
     * 
     * @param nodelist
     * @return 
     */
    private ArrayList<WikiPage> processPageNodeList(NodeList nodelist) {
        
        ArrayList<WikiPage> wikis = new ArrayList<>(nodelist.getLength());
        for (int i = 0; i < nodelist.getLength(); i++) {
            //
            Element page = (Element) nodelist.item(i);
            //
            String title = page.getElementsByTagName("title").item(0).getTextContent().trim();
            char[] chars = page.getElementsByTagName("text").item(0).getTextContent().toCharArray();
            //
            ArrayList<String> categories = parseTextByType(chars, "categories");
            ArrayList<String> anchors = parseTextByType(chars, "anchors");
            //
            ArrayList<String> texts = getDifferentParagraphs(chars);
            //
            WikiPage wikipage = new WikiPage();
            wikipage.categories = categories;
            wikipage.anchors = anchors;
            wikipage.title = title;
            wikipage.texts = texts;
            //
            wikis.add(wikipage);
        }
        return wikis;
    }

    private ArrayList<String> getDifferentParagraphs(char[] symbols) {

        ArrayList<String> texts = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int braceCount = 0;
        int equalCount = 0;
        char current;

        for (int i = 0; i < symbols.length - 1; i++) {
            current = symbols[i];
            if (current == '{') braceCount++;
            if (current == '}') braceCount--;
            if (braceCount > 0) continue;
            if (current == '=') equalCount++;
            if (equalCount == 4) {
                String norml = normalizeWikiPageTextForPOSTagging(sb.toString().toCharArray());
                texts.add(norml);
                sb = new StringBuilder();
                equalCount = 0;
            }
            boolean letter = Character.isAlphabetic(current);
            boolean white = Character.isWhitespace(current);
            boolean digit = Character.isDigit(current);
            if (letter || digit) {
                sb.append(current);
            } else if (white) {
                sb.append(' ');
            }
        }
        return texts;
    }
    
    private String normalizeWikiPageTextForPOSTagging(char[] symbols) {

        StringBuilder sb = new StringBuilder();
        int braceCount = 0;
        char current, next;

        for (int i = 0; i < symbols.length - 1; i++) {
            next = symbols[i + 1];  // hence length - 1
            current = symbols[i];
            if (current == '=' && next == '=') break;
            if (current == '{') braceCount++;
            if (current == '}') braceCount--;
            if (braceCount > 0) continue;
            boolean letter = Character.isAlphabetic(current);
            boolean white = Character.isWhitespace(current);
            boolean digit = Character.isDigit(current);
            if (letter || digit) {
                sb.append(current);
            } else if (white) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    /**
     * 
     * @param symbols
     * @param type
     * @return 
     */
    private ArrayList<String> parseTextByType(char[] symbols, String type) {

        ArrayList<String> list = new ArrayList<>();
        StringBuilder sb = null;
        boolean reading = false;
        int braceCount = 0;
        char current, next;

        for (int i = 0; i < symbols.length - 1; i++) {
            next = symbols[i + 1];  // hence length - 1
            current = symbols[i];
            if (current == '{' && !reading) braceCount++;
            if (current == '}' && !reading) braceCount--;
            if (braceCount > 0) continue;
            if (current == '[' && next == '[') {
                sb = new StringBuilder();
                reading = true;
                i++; // step over second brace
            } else if (current == ']' && next == ']') {
                String parsedTerm = parseTermByType(sb.toString(), type);
                if (!parsedTerm.isEmpty()) list.add(parsedTerm);
                reading = false;
                i++; // step over second brace
            } else if (reading) {
                sb.append(current);
            }
        }
        return list;
    }

    /**
     * 
     * @param term
     * @param type
     * @return 
     */
    private String parseTermByType(String term, String type) {

        String prefix = "Category:";
        switch (type) {
            case "categories":
                if (term.startsWith("Category:")) {
                    return term.substring("Category:".length());
                }
                break;
            case "anchors":
                if (term.startsWith(prefix)) return "";
                int bar = term.indexOf('|');
                if (bar > 0) {
                    return term.substring(0, bar);
                } else {
                    return term.substring(0);
            }
        }
        return "";
    }
    
    /**
     * 
     * @param wikilist
     * @return 
     */
    private Document makeDocumentFromWikis(ArrayList<WikiPage> wikilist) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            //
            Element root = document.createElement("ProcessedSpecialExportData");
            document.appendChild(root);
            //
            for (WikiPage wikipage : wikilist) {
                Element page = document.createElement("page");
                root.appendChild(page);
                //
                Element title = document.createElement("title");
                title.appendChild(document.createTextNode(wikipage.title));
                page.appendChild(title);
                //
                for (String p : wikipage.texts) {
                    Element text = document.createElement("text");
                    text.appendChild(document.createTextNode(p));
                    page.appendChild(text);
                }
                //
                Element categories = document.createElement("categories");
                String cat_Text = stringifyList(wikipage.categories);
                categories.appendChild(document.createTextNode(cat_Text));
                page.appendChild(categories);
                //
                Element anchors = document.createElement("anchors");
                String anc_Text = stringifyList(wikipage.anchors);
                anchors.appendChild(document.createTextNode(anc_Text));
                page.appendChild(anchors);
                //
            }
            return document;
        } catch (ParserConfigurationException ex) {
            System.err.println("ParserConfigurationException: " + ex.getMessage());
        }
        return null;
    }
    
    /**
     * 
     * @param list
     * @return 
     */
    private String stringifyList(ArrayList<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(' ');
            sb.append(s);
        }
        return sb.toString();
    }
    
    /**
     * 
     * @param document
     * @param filename 
     */
    private void saveDocumentAsXML(Document document, String filename) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            DOMSource source = new DOMSource(document);
            File xmlFile = new File(filename);
            StreamResult result = new StreamResult(xmlFile);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException ex) {
            System.err.println("TransformerConfigurationException: " + ex.getMessage());
        } catch (TransformerException ex) {
            System.err.println("TransformerException: " + ex.getMessage());
        }
    }
}