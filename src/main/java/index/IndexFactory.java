package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import reader.DataReader;
import reader.SearchQuery;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class IndexFactory {
    public static void CreateIndex(List<DataReader> readers) throws IOException {
        Analyzer analyzer = new KeywordAnalyzer();

        Path indexPath = Files.createDirectory(Paths.get(System.getProperty("java.io.tmpdir") + "QueryIndex"));
        Path prefixPath = Files.createDirectory(Paths.get(System.getProperty("java.io.tmpdir") + "PrefixIndex"));
        Path ngramPath = Files.createDirectory(Paths.get(System.getProperty("java.io.tmpdir") + "NgramIndex"));
        Directory indexDirectory = FSDirectory.open(indexPath);
        Directory prefixDirectory = FSDirectory.open(prefixPath);
        Directory ngramDirectory = FSDirectory.open(ngramPath);
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriterConfig config2 = new IndexWriterConfig(analyzer);
        IndexWriterConfig config3 = new IndexWriterConfig(analyzer);
        IndexWriter iwriter = new IndexWriter(indexDirectory, config);
        IndexWriter pwriter = new IndexWriter(prefixDirectory, config2);
        IndexWriter nwriter = new IndexWriter(ngramDirectory, config3);

        HashMap<String, Integer> map = new HashMap<>();
        HashMap<String, Integer> prefixMap = new HashMap<>();
        HashMap<String, Integer> ngramMap = new HashMap<>();
        for(DataReader reader : readers) {
            Preprocess(reader, map);
        }

        //Remove - from indeces
        map.remove("-");

        int amount = 0;

        for (String key : map.keySet()) {
            Integer oc = map.get(key);
            if (amount < 10) {
                System.out.println(key + " - " + oc);
                amount++;
            }
            AddToIndex(iwriter, key, oc);
            NgramPreprocess(nwriter, key, oc);
            SuffixPreprocess(key, oc, prefixMap);
        }

        for(String key : prefixMap.keySet()) {
            AddToIndex(pwriter, key, prefixMap.get(key));
        }

        iwriter.close();
        pwriter.close();
        nwriter.close();
    }

    public static void NgramPreprocess(IndexWriter nwriter, String key, int val) throws IOException {
        StandardTokenizer source = new StandardTokenizer();
        source.setReader(new StringReader(key));
        ShingleFilter shingleFilter = new ShingleFilter(source, 2, 6);

        CharTermAttribute charTermAttribute = shingleFilter.addAttribute(CharTermAttribute.class);

        // Use long, frequency is the number of occurences.
        long result = 0L;
        shingleFilter.reset();
        while(shingleFilter.incrementToken()) {
            String token = charTermAttribute.toString();
            for (int i = 0; i < val; i++) {
                AddToIndex(nwriter, token, 1);
            }
        }
    }

    public  static void SuffixPreprocess(String key, int value, HashMap<String, Integer> prefixMap) {         List<String> list2 = Arrays.asList(key.split(" "));
        Collections.reverse(list2);
        ArrayList<String> list = new ArrayList<String>(list2);
        String soFar = "";
        while(list.size() > 0) {
            soFar = list.remove(0) + " " + soFar;
            soFar = soFar.trim();
            int val = prefixMap.getOrDefault(soFar, 0);
            prefixMap.put(soFar, val + value);
        }
    }

    private static void AddToIndex(IndexWriter iwriter, String key, int value) throws IOException {
        Document doc = new Document();
        doc.add(new Field("query", key, TextField.TYPE_STORED));
        doc.add(new NumericDocValuesField("amount", value));
        doc.add(new StoredField("amount", value));
        iwriter.addDocument(doc);
    }

    public static void Preprocess(DataReader reader, HashMap<String, Integer> map) throws IOException {
        SearchQuery query;
        while ((query = reader.ReadLine()) != null) {
            int output = map.getOrDefault(query.Query, 0);
            map.put(query.Query, output+1);
        }
    }

    public static IndexSearcher ReadIndex(String name) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(System.getProperty("java.io.tmpdir") + name));
        DirectoryReader ireader = DirectoryReader.open(directory);
        return new IndexSearcher(ireader);
    }

    public static void DeleteIfExists() throws IOException {
        delete(new File(System.getProperty("java.io.tmpdir") + "QueryIndex"));
        delete(new File(System.getProperty("java.io.tmpdir") + "PrefixIndex"));
        delete(new File(System.getProperty("java.io.tmpdir") + "NgramIndex"));
    }

    private static void delete(File file)
            throws IOException{

        if(file.isDirectory()){

            //directory is empty, then delete it
            if(file.list().length==0){

                file.delete();
                System.out.println("Directory is deleted : "
                        + file.getAbsolutePath());

            }else{

                //list all the directory contents
                String files[] = file.list();

                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp);

                    //recursive delete
                    delete(fileDelete);
                }

                //check the directory again, if empty then delete it
                if(file.list().length==0){
                    file.delete();
                    System.out.println("Directory is deleted : "
                            + file.getAbsolutePath());
                }
            }

        }else{
            //if file, then delete it
            file.delete();
            //System.out.println("File is deleted : " + file.getAbsolutePath());
        }
    }
}
