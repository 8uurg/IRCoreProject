package irproject;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import java.io.IOException;

public class MostPopularCompletion {

    public IndexSearcher searcher;

    public TopDocs query(String query, int n) throws IOException {
        return searcher.search(new PrefixQuery(new Term("query", query)), n);
    }

}
