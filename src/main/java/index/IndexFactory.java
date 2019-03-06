package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.store.*;
import org.apache.lucene.util.BytesRef;
import reader.DataReader;
import reader.SearchQuery;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;


public class IndexFactory {
    public static void CreateIndex(List<DataReader> readers) throws IOException {
        Analyzer analyzer = new KeywordAnalyzer();

        Path indexPath = Files.createDirectory(Paths.get(System.getProperty("java.io.tmpdir") + "QueryIndex"));
        Directory directory = FSDirectory.open(indexPath);
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);

        HashMap<String, Integer> map = new HashMap<>();
        for(DataReader reader : readers) {
            Preprocess(reader, map);
        }

        int amount = 0;

        for (String key : map.keySet()) {
            if (amount < 10) {
                System.out.println(key + " - " + map.get(key));
                amount++;
            }
            Document doc = new Document();
            doc.add(new Field("query", key, TextField.TYPE_STORED));
            doc.add(new NumericDocValuesField("amount", map.get(key)));
            doc.add(new StoredField("amount", map.get(key)));
            iwriter.addDocument(doc);
        }

        iwriter.close();
    }

    public static void Preprocess(DataReader reader, HashMap<String, Integer> map) throws IOException {
        SearchQuery query;
        while ((query = reader.ReadLine()) != null) {
            int output = map.getOrDefault(query.Query, 0);
            map.put(query.Query, output+1);
        }
    }

    public static IndexSearcher ReadIndex() throws IOException {
        Directory directory = FSDirectory.open(Paths.get(System.getProperty("java.io.tmpdir") + "QueryIndex"));
        DirectoryReader ireader = DirectoryReader.open(directory);
        return new IndexSearcher(ireader);
    }

    public static void DeleteIfExists() throws IOException {
        delete(new File(System.getProperty("java.io.tmpdir") + "QueryIndex"));
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
            System.out.println("File is deleted : " + file.getAbsolutePath());
        }
    }
}
