package irproject;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;

import java.io.IOException;

public class MostPopularCompletion {

    public MostPopularCompletion(DirectoryReader reader) {
        this.searcher = new IndexSearcher(reader);
    }

    private IndexSearcher searcher;

    public TopDocs query(String query, int n) throws IOException {
        return searcher.search(new PrefixQuery(new Term("query", query)), n);
    }

}
