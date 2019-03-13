import ciir.umass.edu.learning.tree.LambdaMART;
import index.IndexFactory;
import irproject.ICompletionAlgorithm;
import irproject.LambdaMARTAutocomplete;
import org.apache.lucene.search.IndexSearcher;
import org.junit.jupiter.api.Test;

import java.io.*;

public class LambdaMARTCompletionTestNoNGram extends BaseTest {
    public static int CHEATNUMBER = 100;

    public String GetModelStoragePath() {
        return System.getProperty("java.io.tmpdir") + "LambdaMART/LambdaMARTNoNGram" +CHEATNUMBER + ".txt";
    }

    @Override
    ICompletionAlgorithm GetAlgorithm() throws IOException {
        LambdaMART.verbose = false;
        IndexSearcher searcher = IndexFactory.ReadIndex("SuffixIndex");
        LambdaMARTAutocomplete autocomplete = new LambdaMARTAutocomplete(searcher, null);
        // Load the model
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(GetModelStoragePath())));
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
        IndexSearcher searcher = IndexFactory.ReadIndex("SuffixIndex");
        LambdaMARTAutocomplete lambdaMART = new LambdaMARTAutocomplete(searcher, null);
        String[] originalqueries = this.getTrainingQueries();
        String[] queries = new String[CHEATNUMBER];
        // Actually generate the cut off queries.
        resetSeed();
        for (int i = 0; i < CHEATNUMBER; i++) {
            queries[i] =  this.CutOff(originalqueries[i]);
        }
        resetSeed();
        // Actually call training method.
        lambdaMART.train(queries, originalqueries);
        // Dump result of training to file.
        File file = new File(GetModelStoragePath());
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        FileWriter writer = new FileWriter(file);
        writer.write(lambdaMART.rerankerString());
        writer.close();
    }

    @Test
    void test100() throws IOException {
        CHEATNUMBER = 100;
        train();
        this.Test();
    }


    @Test
    void test500() throws IOException {
        CHEATNUMBER = 500;
        train();
        this.Test();
    }

    @Test
    void test1000() throws IOException {
        CHEATNUMBER = 1000;
        train();
        this.Test();
    }

    @Test
    void test2000() throws IOException {
        CHEATNUMBER = 2000;
        train();
        this.Test();
    }
}
