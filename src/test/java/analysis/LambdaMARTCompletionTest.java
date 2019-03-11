package analysis;

import irproject.ICompletionAlgorithm;
import irproject.LambdaMARTAutocomplete;
import org.apache.lucene.search.IndexSearcher;

public class LambdaMARTCompletionTest extends BaseTest {
    @Override
    ICompletionAlgorithm GetAlgorithm(IndexSearcher searcher) {
        return new LambdaMARTAutocomplete(searcher, null);
    }
}
