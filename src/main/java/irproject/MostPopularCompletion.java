package irproject;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import java.io.IOException;

public class MostPopularCompletion implements ICompletionAlgorithm {

    public MostPopularCompletion(IndexSearcher searcher) {
        this.searcher = searcher;
    }

    private IndexSearcher searcher;

    public String[] query(String query, int n) {
        try {
            TopDocs docs = fullQuery(query, n);
            String[] res = new String[n];
            for (int i = 0; i < docs.scoreDocs.length; i++) {
                res[i] = searcher.doc(docs.scoreDocs[i].doc).getField("query").stringValue();
            }
            return res;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public TopDocs fullQuery(String query, int n) throws IOException {
        Sort s = new Sort(new SortField("amount", new FieldComparatorSource() {
            @Override
            public FieldComparator<?> newComparator(String s, int i, int i1, boolean b) {
                return new FieldComparator.IntComparator(i, s, i1);
            }
        }, true));
        return searcher.search(new PrefixQuery(new Term("query", query)), n, s);
    }
}
