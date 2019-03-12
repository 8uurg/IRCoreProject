package index;

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
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String search = null;
        System.out.println("What are you looking for?");
        while (!(search = scanner.nextLine()).equals("q")) {
            searchFor(search);
        }
    }

    public static void searchFor(String input) throws IOException {
        IndexSearcher s =  IndexFactory.ReadIndex("PrefixIndex");
        MostPopularCompletion completion = new MostPopularCompletion(s);
        TopDocs docs = completion.fullQuery(input, 10);
        for(ScoreDoc hitDoc : docs.scoreDocs) {
            Document doc = s.doc(hitDoc.doc);
            System.out.print(doc.get("query"));
            System.out.println(" - " + doc.get("amount"));
        }
    }
}
