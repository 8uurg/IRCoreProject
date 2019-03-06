package irproject;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.DenseDataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.ReciprocalRankScorer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;


import java.io.IOException;
import java.util.ArrayList;

public class LambdaMARTAutocomplete implements ICompletionAlgorithm {

//    Uses learning-to-rank, which I am not familiar with just yet.
    public LambdaMARTAutocomplete(IndexSearcher searcher) {
        this.searcher = searcher;
    }

    private IndexSearcher searcher;
    private LambdaMART reranker;

    // Note: make sure you actually use all the features you want to use!
    private int[] usedfeatures = new int[]{0, 1, 2, 3, 4};
    private MetricScorer scorer = new ReciprocalRankScorer();

    public String[] query(String query, int n, int n_before_reranking) throws IOException {
        if ( reranker == null ) {
            throw new RuntimeException("Please train or load the reranker before usage.");
        }
        // We do not know the original query in this case. So originalquery is the empty string.
        RankList rankList = this.queryToRankList(query, "", n_before_reranking);
        RankList rankedList = reranker.rank(rankList);
        int n2 = rankedList.size();
        int an = Math.min(n, n2);
        String[] result = new String[an];
        for (int i = 0; i < an; i++) {
            result[i] = rankedList.get(i).getID();
        }
        return result;
    }

    // Query without reranking.
    public TopDocs simplequery(String query, int n) {
        try {
            return searcher.search(new PrefixQuery(new Term("query", query)), n);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load the LambdaMART model from a string.
     * @param modelString The model to load.
     */
    public void loadReranker(String modelString) {
        reranker = new LambdaMART();
        reranker.loadFromString(modelString);
    }

    /**
     * Write the rerankers model to a string.
     */
    public String rerankerString() {
        return reranker.model();
    }

    public float[] extractFeatures(Document document, String query) {
        // Amount of occurences feature
        float occurences = document.getField("amount").numericValue().floatValue();
        // Ends with space
        String docQuery = document.getField("query").stringValue();
        float prefix_length = query.length();
        float total_length = docQuery.length();
        float suffix_length = total_length - prefix_length;
        float endsinspace = query.endsWith(" ")?1.0f:0.0f;
        return new float[]{occurences, prefix_length, suffix_length, total_length, endsinspace};
    }

    public float getRelevance(String query, String document) {
        return query.equals(document)?1.0f:0.0f;
    }

    protected RankList queryToRankList(String query, String originalQuery, int n) throws IOException {
        TopDocs docs = this.simplequery(query, n);
        ScoreDoc[] scoreDocs = docs.scoreDocs;
        ArrayList<DataPoint> dataPoints = new ArrayList<>();
        for (ScoreDoc scoreDoc : scoreDocs) {
            Document document = searcher.doc(scoreDoc.doc);
            // Build a string for the DenseDataPoint to parse again...
            StringBuilder docDataPoint = new StringBuilder();
            String docQuery = document.get("query");
            // First floating point value is the relevance.
            docDataPoint.append(getRelevance(originalQuery, docQuery));
            docDataPoint.append(' ');
            // Second is an ID.
            docDataPoint.append(scoreDoc.doc);
            docDataPoint.append(' ');
            // All successive floating point values are features.
            for (float feature : extractFeatures(document, query)) {
                docDataPoint.append(feature);
                docDataPoint.append(' ');
            }
            dataPoints.add(new DenseDataPoint(docDataPoint.toString()));
        }

        return new RankList(dataPoints);
    }

    public void train(String[] queries, String[] originalQueries, int n) throws IOException {
        // Turn the queries into data.
        ArrayList<RankList> samples = new ArrayList<RankList>();
        for (int i = 0; (i < queries.length) && (i < originalQueries.length); i++) {
            String query = queries[i];
            String originalQuery = originalQueries[i];
            RankList rankList = this.queryToRankList(query, originalQuery, n);
            samples.add(rankList);
        }

        reranker = new LambdaMART(samples, this.usedfeatures, this.scorer);
        reranker.init();
        reranker.learn();
    }

    @Override
    public String[] query(String query, int n) {
        try {
            return query(query, n, n);
        } catch (IOException e) {
            return new String[]{};
        }
    }
}
