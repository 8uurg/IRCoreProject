package analysis;

import irproject.ICompletionAlgorithm;
import irproject.LambdaMARTAutocomplete;
import org.apache.lucene.search.IndexSearcher;
import org.junit.jupiter.api.Test;

import java.io.*;

public class LambdaMARTCompletionTestNoNGram extends BaseTest {
    private String modelStoragePath = System.getProperty("java.io.tmpdir") + "LambdaMART/LambdaMARTNoNGram.txt";

    @Override
    ICompletionAlgorithm GetAlgorithm(IndexSearcher searcher) {
        LambdaMARTAutocomplete autocomplete = new LambdaMARTAutocomplete(searcher, null);
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
        // TODO: Actually pass the indices!
        LambdaMARTAutocomplete lambdaMART = new LambdaMARTAutocomplete(null ,null);
        // TODO: Actually add some queries here!
        String[] originalqueries = new String[]{};
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