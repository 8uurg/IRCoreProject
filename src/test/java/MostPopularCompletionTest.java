import index.IndexFactory;
import irproject.ICompletionAlgorithm;
import irproject.MostPopularCompletion;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;

public class MostPopularCompletionTest extends BaseTest {
    @Override
    ICompletionAlgorithm GetAlgorithm() throws IOException {

        IndexSearcher searcher = IndexFactory.ReadIndex("QueryIndex");
        return new MostPopularCompletion(searcher);
    }
}
