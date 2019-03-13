package irproject;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.DenseDataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.ReciprocalRankScorer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LambdaMARTAutocomplete implements ICompletionAlgorithm {

//    Uses learning-to-rank, which I am not familiar with just yet.
    public LambdaMARTAutocomplete(IndexSearcher suffixsearcher, IndexSearcher ngramsearcher) {
        this.suffixsearcher = suffixsearcher;
        if (ngramsearcher == null) {
            usedfeatures = new int[]{0, 1, 2, 3, 4};
        } else {
            this.ngramsearcher = ngramsearcher;
            // Can actually compute and use ngram feature!
            usedfeatures = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        }
    }

    private IndexSearcher suffixsearcher;
    private IndexSearcher ngramsearcher;

    private LambdaMART reranker;

    // Amount of items to retrieve before re-ranking.
    private int nRerank = 100;

    // Note: make sure you actually use all the features you want to use!
    private int[] usedfeatures;
    private MetricScorer scorer = new ReciprocalRankScorer();

    public String[] queryit(String query, int n) throws IOException {
        if ( reranker == null ) {
            throw new RuntimeException("Please train or load the reranker before usage.");
        }
        // We do not know the original query in this case. So originalquery is the empty string.
        RankList rankList = this.queryToRankList(query, "");
        RankList rankedList = reranker.rank(rankList);
        int n2 = rankedList.size();
        int an = Math.min(n, n2);
        String[] result = new String[an];
        for (int i = 0; i < an; i++) {
            result[i] = rankedList.get(i).getID();
        }
        return result;
    }

    private final String regex = "[^\\s]+\\s?$";
    private final Pattern pattern = Pattern.compile(regex);

    private String getEndTerm(String query) {
        Matcher m = pattern.matcher(query);
        boolean foundEndTerm = m.find();
        if (foundEndTerm) {
            return m.group();
        } else {
            return "";
        }
    }

    // Query without reranking.
    public TopDocs simplequery(String query, int n) {
        try {
            // TODO: Sort?
            return suffixsearcher.search(new PrefixQuery(new Term("query", getEndTerm(query))), n);
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

    public float[] extractFeatures(Document document, String query) throws IOException {
        // Amount of occurences feature
        float occurences = document.getField("amount").numericValue().floatValue();
        // Ends with space
        String docQuery = document.getField("query").stringValue();
        String endTerm = getEndTerm(query);
        float prefix_length = query.length();
        float suffix_length = docQuery.length();
        float total_length = prefix_length + suffix_length - endTerm.length();
        float endsinspace = query.endsWith(" ")?1.0f:0.0f;
        if (ngramsearcher == null) {
            return new float[]{occurences, prefix_length, suffix_length, total_length, endsinspace};
        } else {
            float[] features = new float[]{occurences, prefix_length, suffix_length, total_length, endsinspace, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
            String fullquery = query.subSequence(0, query.length() - endTerm.length()).toString();
            for (int i = 2; i < 6; i++) {
                // We already have 5 features.
                features[i+5] = calculateNGramFeature(fullquery, i+1);
            }
            return features;
        }
    }

    public float calculateNGramFeature(String fullquery, int n) throws IOException {
        StandardTokenizer source = new StandardTokenizer();
        source.setReader(new StringReader(fullquery));
        ShingleFilter shingleFilter = new ShingleFilter(source, n, n);
        shingleFilter.setOutputUnigrams(false);

        CharTermAttribute charTermAttribute = shingleFilter.addAttribute(CharTermAttribute.class);
        shingleFilter.setOutputUnigrams(false);

        // Use long, frequency is the number of occurences.
        long result = 0L;
        shingleFilter.reset();
        while(shingleFilter.incrementToken()) {
            String token = charTermAttribute.toString();
            result += ngramsearcher.getIndexReader().getSumTotalTermFreq(token);
        }
        // Cast to float, as features are required to be floats.
        return (float) result;
    }

    public float getRelevance(String query, String document) {
        return query.equals(document)?1.0f:0.0f;
    }

    protected RankList queryToRankList(String query, String originalQuery) throws IOException {
        TopDocs docs = this.simplequery(query, nRerank);
        ScoreDoc[] scoreDocs = docs.scoreDocs;
        ArrayList<DataPoint> dataPoints = new ArrayList<>();
        for (ScoreDoc scoreDoc : scoreDocs) {
            Document document = suffixsearcher.doc(scoreDoc.doc);
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

    public void train(String[] queries, String[] originalQueries) throws IOException {
        // Turn the queries into data.
        ArrayList<RankList> samples = new ArrayList<RankList>();
        for (int i = 0; (i < queries.length) && (i < originalQueries.length); i++) {
            String query = queries[i];
            String originalQuery = originalQueries[i];
            RankList rankList = this.queryToRankList(query, originalQuery);
            samples.add(rankList);
        }

        reranker = new LambdaMART(samples, this.usedfeatures, this.scorer);
        reranker.init();
        reranker.learn();
    }

    @Override
    public String[] query(String query, int n) {
        try {
            return queryit(query, n);
        } catch (IOException e) {
            return new String[]{};
        }
    }
}
