package analysis;

import index.IndexFactory;
import irproject.ICompletionAlgorithm;
import irproject.MostPopularCompletion;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;

public class MostPopularCompletionTest extends BaseTest {
    @Override
    ICompletionAlgorithm GetAlgorithm(IndexSearcher searcher) {
        return new MostPopularCompletion(searcher);
    }
}
