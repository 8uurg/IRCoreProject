package irproject;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

public class SuffixSearcher extends IndexSearcher {
    public SuffixSearcher(IndexReader r) {
        super(r);
    }
}
