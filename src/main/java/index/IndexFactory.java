package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.*;
import reader.DataReader;
import reader.SearchQuery;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class IndexFactory {
    public static void CreateIndex(DataReader reader) throws IOException {
        Analyzer analyzer = new KeywordAnalyzer();
        Path indexPath = Files.createTempDirectory("QueryIndex");
        Directory directory = FSDirectory.open(indexPath);
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);
        Document doc = new Document();

        SearchQuery query;
        while ((query = reader.ReadLine()) != null) {
            doc.add(new Field("query", query.Query, TextField.TYPE_STORED));
        }

        iwriter.addDocument(doc);
        iwriter.close();
    }

    public static IndexSearcher ReadIndex() throws IOException {

        Path indexPath = new File("C:\\Users\\martijn\\AppData\\Local\\Temp\\QueryIndex4857859413033495429").toPath();
        Directory directory = FSDirectory.open(indexPath);
        DirectoryReader ireader = DirectoryReader.open(directory);
        return new IndexSearcher(ireader);
    }
}
