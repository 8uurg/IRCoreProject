package irproject;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.document.TextField;

import java.awt.*;
import java.io.IOException;

public class MostPopularCompletion {

    public MostPopularCompletion(DirectoryReader reader) {
        this.searcher = new IndexSearcher(reader);
    }

    private IndexSearcher searcher;

    public TopDocs query(String query, int n) throws IOException {
        Sort s = new Sort(new SortField("amount", new FieldComparatorSource() {
            @Override
            public FieldComparator<?> newComparator(String s, int i, int i1, boolean b) {
                return new FieldComparator.IntComparator(i, s, i1);
            }
        }, true));

        return searcher.search(new PrefixQuery(new Term("query", query)), n, s);
    }

}
