package irproject;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.DenseDataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.ReciprocalRankScorer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LambdaMARTAutocomplete implements ICompletionAlgorithm {

//    Uses learning-to-rank, which I am not familiar with just yet.
    public LambdaMARTAutocomplete(IndexSearcher suffixsearcher, IndexSearcher ngramsearcher) {
        this.suffixsearcher = suffixsearcher;

        scorer = new ReciprocalRankScorer();
        scorer.setK(10);

        if (ngramsearcher == null) {
            usedfeatures = new int[]{1, 2, 3, 4, 5};
        } else {
            this.ngramsearcher = ngramsearcher;
            // Can actually compute and use ngram feature!
            usedfeatures = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        }
    }

    private IndexSearcher suffixsearcher;
    private IndexSearcher ngramsearcher;

    private LambdaMART reranker;

    // Amount of items to retrieve before re-ranking.
    private int nRerank = 100;

    // Note: make sure you actually use all the features you want to use!
    private int[] usedfeatures;

    private MetricScorer scorer;

    public String[] queryit(String query, int n) throws IOException {
        if ( reranker == null ) {
            throw new RuntimeException("Please train or load the reranker before usage.");
        }
        // We do not know the original query in this case. So originalquery is the empty string.
        RankList rankList = this.queryToRankList(query, "", false);

        if (rankList == null) {
            return new String[]{};
        }
        int n2 = rankList.size();
        if (n2 == 0) {
            return new String[]{};
        }
        RankList rankedList = reranker.rank(rankList);

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
        Sort s = new Sort(new SortField("amount", new FieldComparatorSource() {
            @Override
            public FieldComparator<?> newComparator(String s, int i, int i1, boolean b) {
                return new FieldComparator.IntComparator(i, s, i1);
            }
        }, true));
        try {
            TopDocs td = suffixsearcher.search(new PrefixQuery(new Term("query", getEndTerm(query))), n, s);
            return td;
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
            for (int i = 0; i < 6; i++) {
                // We already have 5 features.
                features[i+5] = calculateNGramFeature(fullquery, i+1);
            }
            return features;
        }
    }

    public float calculateNGramFeature(String fullquery, int n) throws IOException {
        StandardTokenizer source = new StandardTokenizer();
        source.setReader(new StringReader(fullquery));
        TokenStream filtered;
        if (n > 2) {
            ShingleFilter shingleFilter = new ShingleFilter(source, n, n);
            shingleFilter.setOutputUnigrams(false);
            filtered = shingleFilter;
        } else {
            filtered = source;
        }

        CharTermAttribute charTermAttribute = filtered.addAttribute(CharTermAttribute.class);

        // Use long, frequency is the number of occurences.
        long result = 0L;
        filtered.reset();
        while(filtered.incrementToken()) {
            String token = charTermAttribute.toString();
            result += ngramsearcher.getIndexReader().getSumTotalTermFreq(token);
        }
        // Cast to float, as features are required to be floats.
        return (float) result;
    }

    public float getRelevance(String query, String document) {
        return query.equals(document)?1.0f:0.0f;
    }

    protected RankList queryToRankList(String query, String originalQuery, boolean returnNullIfNoRelevant) throws IOException {
        TopDocs docs = this.simplequery(query, nRerank);
        ScoreDoc[] scoreDocs = docs.scoreDocs;
        ArrayList<DataPoint> dataPoints = new ArrayList<>();

        if (scoreDocs.length == 0) {
            return null;
        }
        boolean hasRelevantItem = false;
        for (ScoreDoc scoreDoc : scoreDocs) {
            Document document = suffixsearcher.doc(scoreDoc.doc);
            // Build a string for the DenseDataPoint to parse again...
            StringBuilder docDataPoint = new StringBuilder();
            // Is a suffix!
            String docQuery = document.get("query");
            String completedDocQuery = query + docQuery.substring(getEndTerm(query).length());
            // First floating point value is the relevance.
            float relevance = getRelevance(originalQuery, completedDocQuery);
            if (relevance > 0.0f) {
                hasRelevantItem = true;
            }
            docDataPoint.append(relevance);
            docDataPoint.append(' ');
            // Second is an ID.
            docDataPoint.append(':');
            docDataPoint.append(scoreDoc.doc);
            docDataPoint.append(' ');
            // All successive floating point values are features.
            int i = 1;
            float[] features = extractFeatures(document, query);
            for (float feature : features) {
                docDataPoint.append(i);
                docDataPoint.append(':');
                docDataPoint.append(feature);
                docDataPoint.append(' ');
                i += 1;
            }
            DenseDataPoint dp = new DenseDataPoint(docDataPoint.toString());
            dp.setLabel(relevance);
            dataPoints.add(dp);
        }

        // Meh!
        if (!hasRelevantItem && returnNullIfNoRelevant)
            return null;

        return new RankList(dataPoints);
    }

    public void train(String[] queries, String[] originalQueries) throws IOException {
        // Turn the queries into data.
        ArrayList<RankList> samples = new ArrayList<RankList>();
        for (int i = 0; (i < queries.length) && (i < originalQueries.length); i++) {
            String query = queries[i];
            String originalQuery = originalQueries[i];
            RankList rankList = this.queryToRankList(query, originalQuery, true);
            // If no results found. Null is returned.
            if (rankList != null) {
                samples.add(rankList);
            }
        }

        if(samples.size() == 0) {
            throw new RuntimeException("Make sure at least one valid (has items, has at least one relevant item) sample is present.");
        }

        double scorey = this.scorer.score(samples);

        System.out.println("Starting training with " + samples.size() + " samples worth of ranked lists. RR of initial lists is " + scorey + ".");

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
