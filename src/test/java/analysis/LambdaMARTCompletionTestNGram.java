package analysis;

import index.IndexFactory;
import irproject.ICompletionAlgorithm;
import irproject.LambdaMARTAutocomplete;
import org.apache.lucene.search.IndexSearcher;
import org.junit.jupiter.api.Test;

import java.io.*;

public class LambdaMARTCompletionTestNGram extends BaseTest {
    private String modelStoragePath = System.getProperty("java.io.tmpdir") + "LambdaMART/LambdaMARTNoNGram.txt";

    @Override
    ICompletionAlgorithm GetAlgorithm() throws IOException {

        IndexSearcher searcher = IndexFactory.ReadIndex("PrefixIndex");
        IndexSearcher ngramsearcher = IndexFactory.ReadIndex("NgramIndex");
        LambdaMARTAutocomplete autocomplete = new LambdaMARTAutocomplete(searcher, ngramsearcher);
        // Load the model
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(modelStoragePath)));
            StringBuilder lines = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.append(line);
                lines.append('\n');
            }
            autocomplete.loadReranker(lines.toString());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Train a model first before trying doing anything else!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return autocomplete;
    }

    @Test
    public void train() throws IOException {
        LambdaMARTAutocomplete lambdaMART = (LambdaMARTAutocomplete) this.GetAlgorithm();
        String[] originalqueries = this.getTrainingQueries();
        String[] queries = new String[originalqueries.length];
        // Actually generate the cut off queries.
        resetSeed();
        for (int i = 0; i < originalqueries.length; i++) {
            queries[i] =  this.CutOff(originalqueries[i]);
        }
        resetSeed();
        // Actually call training method.
        lambdaMART.train(queries, originalqueries);
        // Dump result of training to file.
        File file = new File(modelStoragePath);
        FileWriter writer = new FileWriter(file);
        writer.write(lambdaMART.rerankerString());
        writer.close();
    }
}