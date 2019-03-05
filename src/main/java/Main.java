import index.IndexFactory;
import irproject.MostPopularCompletion;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.*;
import org.apache.lucene.util.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException, ParseException {
        IndexSearcher s =  IndexFactory.ReadIndex();
        MostPopularCompletion completion = new MostPopularCompletion();
        completion.searcher = s;
        TopDocs docs = completion.query("flights", 10);
        for(ScoreDoc hitDoc : docs.scoreDocs) {
            Document doc = s.doc(hitDoc.doc);
            System.out.println(doc.get("query"));
            //assertEquals("This is the text to be indexed.", hitDoc.get("fieldname"));
        }
    }
}
