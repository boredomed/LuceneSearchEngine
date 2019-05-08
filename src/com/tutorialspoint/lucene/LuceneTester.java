/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tutorialspoint.lucene;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;
import com.tutorialspoint.lucene.args;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Ahmad
 */

public class LuceneTester {

    String indexDir = "E:\\lms\\semester 8\\ir\\Assignment#1_Package\\Assignment#1_Package\\index";
    String dataDir = "E:\\lms\\semester 8\\ir\\Assignment#1_Package\\Assignment#1_Package\\Documents";
    Indexer indexer;
    Searcher searcher;
    
    Map<Double, String> unsortMap_tfidf = new HashMap<Double, String>();
    Map<Double, String> unsortMap_normal_tfidf = new HashMap<Double, String>();
    Map<Double, String> unsortMap_bm25 = new HashMap<Double, String>();
    
    Map<Double, String> treeMap_tfidf = new TreeMap<Double, String>(
                new Comparator<Double>() {

                    @Override
                    public int compare(Double o1, Double o2) {
                        return o2.compareTo(o1);
                    }
                });
    
    Map<Double, String> treeMap_normal_tfidf = new TreeMap<Double, String>(
                new Comparator<Double>() {

                    @Override
                    public int compare(Double o1, Double o2) {
                        return o2.compareTo(o1);
                    }
                });
        
    Map<Double, String> treeMap_bm25 = new TreeMap<Double, String>(
                new Comparator<Double>() {

                    @Override
                    public int compare(Double o1, Double o2) {
                        return o2.compareTo(o1);
                    }
                });

    public static void main(String[] args) {
        LuceneTester tester;
        try {
            tester = new LuceneTester();
            //tester.createIndex();
            //Using our own build functions for sorting
            tester.sortUsing_tfidf_ntfidf_bm25("data");
            tester.sortUsing_tfidf_ntfidf_bm25("information retrieval");
            tester.sortUsing_tfidf_ntfidf_bm25("computer network changing");
            //tester.sortUsingRelevance("data restaurants");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    //Finding TFIDF
    static double tfidf(double tf, double df, double TotalFiles) {
        if (tf != 0) {
            double termWeight = tf * (Math.log(TotalFiles / df));
            return termWeight;
        } else {
            //System.out.println("Term Frequency is 0");
        }
        return 0;
    }

    //Finding Normalized TFIDF
    static double ntfidf(double tf, double df, double TotalFilesNumber) {
        double termWeight;
        if (tf != 0) {
            termWeight = (1 + Math.log(tf)) * (Math.log(TotalFilesNumber / df));
            return termWeight;
        } else {
            //System.out.println("Term Frequency is 0");
        }
        return 0;
    }

    //Calculating weights of bm25
    private double bm25(double tf, double df, double TotalFilesNumber, double docLength) {
        double tw;
        double k = 2.0;
        double b = 1.75;
        double averageLengthDoc = indexer.getterAverageDocLength();
        averageLengthDoc = averageLengthDoc / TotalFilesNumber;

        if (averageLengthDoc != 0 && tf != 0) {
            tw = Math.log((docLength - df + 0.5) / (df + 0.5)) * ((tf * (k + 1)) / (tf + k * (1 - b + b * (docLength / averageLengthDoc))));
            return tw;
        } else {
            //System.out.println("Tw is 0");
        }
        return 0;

    }
    
    public static <K, V> void printMap(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            System.out.println("Weight : " + entry.getKey() 
				+ " Document : " + entry.getValue());
        }
    }
    private void sortUsing_tfidf_ntfidf_bm25(String searchQuery)
            throws IOException, ParseException {
        indexer = new Indexer(indexDir);
        //This is the total number of files indexed
        int numIndexed = indexer.createIndex(dataDir, new TextFileFilter());

        //Parsing the query
        QueryParser qp = new QueryParser(Version.LUCENE_36, LuceneConstants.CONTENTS, new StandardAnalyzer(Version.LUCENE_36));
        Query q = qp.parse(searchQuery);

        //Accesing the indexed directory
        Directory indexDirectory = FSDirectory.open(new File(indexDir));
        org.apache.lucene.search.Searcher searcher = new IndexSearcher(indexDirectory);

        //Finding total number of doc that matched
        ScoreDoc[] hits = searcher.search(q, null, numIndexed).scoreDocs;
        System.out.println(hits.length + " total results..");
        int hitLength = hits.length;

        for (int i = 0; i < hitLength; i++) {
            Document d = searcher.doc(hits[i].doc);
            String LengthOfDoc = d.get("doclength");
            Long documentLength = Long.parseLong(LengthOfDoc);
            //System.out.println(i + " " + d.get("filename"));
         
            //Variables for storing the final Weight
            double dataTFDF[];
            double tfidf_Weight = 0.0;
            double nTFIDF_Weight = 0.0;
            double bm25_Weight = 0.0;

            Explanation explanation = searcher.explain(q, hits[i].doc);
            StringTokenizer st = new StringTokenizer(searchQuery);

            while (st.hasMoreElements()) {

                dataTFDF = extractTermStatistics(explanation.toString(), st.nextElement().toString());
                tfidf_Weight = tfidf_Weight + tfidf(dataTFDF[0], dataTFDF[1], numIndexed);
                nTFIDF_Weight = nTFIDF_Weight + ntfidf(dataTFDF[0], dataTFDF[1], numIndexed);
                bm25_Weight = bm25_Weight + bm25(dataTFDF[0], dataTFDF[1], numIndexed, documentLength);

            }

            unsortMap_tfidf.put(tfidf_Weight , d.get("filename"));
            unsortMap_normal_tfidf.put(nTFIDF_Weight , d.get("filename"));
            unsortMap_bm25.put(bm25_Weight , d.get("filename"));


        }
        

        
        System.out.println("Ranking due TFIDF");
        treeMap_tfidf.putAll(unsortMap_tfidf);
        printMap(treeMap_tfidf);
        System.out.println("\n\n");
        
        System.out.println("Ranking due Normalized TFIDF");
        treeMap_normal_tfidf.putAll(unsortMap_normal_tfidf);
        printMap(treeMap_normal_tfidf);
        System.out.println("\n\n");
        
        System.out.println("Ranking due BM25");
        treeMap_bm25.putAll(unsortMap_bm25);
        printMap(treeMap_bm25);
        System.out.println("\n\n");
        
            
        /*
    QueryParser qp = new QueryParser(Version.LUCENE_36, LuceneConstants.CONTENTS, new StandardAnalyzer(Version.LUCENE_36));  
    Query q = qp.parse(searchQuery);
    searcher.setDefaultFieldSortScoring(true, false);
    //do the search
    TopDocs hits = searcher.search(q,Sort.RELEVANCE);
    
      
      long endTime = System.currentTimeMillis();
      
      System.out.println(hits.totalHits + " documents found. Time :" + (endTime - startTime) + "ms");
      for(ScoreDoc scoreDoc : hits.scoreDocs) {
         Document doc = searcher.getDocument(scoreDoc);
         System.out.print("Score: " + scoreDoc.score + " ");
         System.out.println("File: "+ doc.get(LuceneConstants.FILE_PATH));
      }
      searcher.close();*/
    }

      private void sortUsingRelevance(String searchQuery)
      throws IOException, ParseException {
      searcher = new Searcher(indexDir);
      long startTime = System.currentTimeMillis();
      
      //Method of Two term or three term
      //For it use this q in Searcher
      //Exracting Unique words from the actual string
      TopDocs hits = null;
    //Method of Single Term Query
    QueryParser qp = new QueryParser(Version.LUCENE_36, LuceneConstants.CONTENTS, new StandardAnalyzer(Version.LUCENE_36));  
    Query q = qp.parse(searchQuery);
    searcher.setDefaultFieldSortScoring(true, false);
    //do the search
    hits = searcher.search(q,Sort.RELEVANCE);
     
      long endTime = System.currentTimeMillis();
      
      System.out.println(hits.totalHits + " documents found. Time :" + (endTime - startTime) + "ms");
      for(ScoreDoc scoreDoc : hits.scoreDocs) {
         Document doc = searcher.getDocument(scoreDoc);
         //System.out.print("Score: " + scoreDoc.score + " ");
         //System.out.println("File: "+ doc.get(LuceneConstants.FILE_PATH));
      }
      searcher.close();
   }

    static double[] extractTermStatistics(String explanation, String queryTerm) {
        int position1 = explanation.toString().indexOf(queryTerm + ")=");
//                   
        int start1 = -1;
        String tfScore = "";
        String dfScore = "";
        double tf;
        double df;
        if (position1 <= 0) {
            position1 = explanation.toString().indexOf("(docFreq=");
            tfScore = "0";
            dfScore = "";

            //System.out.println("pri/n"+explanation.toString());
            for (int z = position1 + 9; z < position1 + 11; z++) {
                if(explanation.toString().charAt(z) != ','){
                dfScore += explanation.toString().charAt(z);
                }
            }
            tf = 0.0;

        } else {

            int startIDF = 0;
            for (int z = position1; z < explanation.toString().length(); z++) {
                if (startIDF == 2 && explanation.toString().charAt(z) != ',') {
                    dfScore += explanation.toString().charAt(z);
                }

                if (startIDF == 2 && explanation.toString().charAt(z) == ',') {
                    break;
                }
                if (start1 == 0 && explanation.toString().charAt(z) != ')') {
                    tfScore += explanation.toString().charAt(z);
                }
                if (start1 == 2 && explanation.toString().charAt(z) == '=') {
                    startIDF++;
                }

                if (start1 == 0 && explanation.toString().charAt(z) == ')') {
                    start1 = 2;
                }

                if (explanation.toString().charAt(z) == '=' && start1 == -1) {
                    start1 = 0;
                }
            }

        }
        //System.out.println("\nTerm = (" + queryTerm + ")" + "tf=" + tfScore + " df=" + dfScore);
        
        tf = Double.parseDouble(tfScore);
        df = Double.parseDouble(dfScore);

        double data[] = {tf, df};
        return data;
    }
}
