import index.IndexFactory;
import irproject.ICompletionAlgorithm;
import irproject.LambdaMARTAutocomplete;
import org.apache.lucene.search.IndexSearcher;
import org.junit.jupiter.api.Test;

import java.io.*;

public class LambdaMARTCompletionTestNGram extends BaseTest {

    public static int CHEATNUMBER = 2000;

    @Override
    ICompletionAlgorithm GetAlgorithm() throws IOException {

        IndexSearcher searcher = IndexFactory.ReadIndex("SuffixIndex");
        IndexSearcher ngramsearcher = IndexFactory.ReadIndex("NgramIndex");
        LambdaMARTAutocomplete autocomplete = new LambdaMARTAutocomplete(searcher, ngramsearcher);
        // Load the model
        try {
            System.out.println("Testing with " + CHEATNUMBER);
            BufferedReader reader = new BufferedReader(new FileReader(new File(System.getProperty("java.io.tmpdir") + "LambdaMART/LambdaMARTNGram" + CHEATNUMBER + ".txt")));
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
    void test100() throws IOException {
        CHEATNUMBER = 100;
        this.Test();
    }


    @Test
    void test500() throws IOException {
        CHEATNUMBER = 500;
        this.Test();
    }

    @Test
    void test1000() throws IOException {
        CHEATNUMBER = 1000;
        this.Test();
    }

    @Test
    void test2000() throws IOException {
        CHEATNUMBER = 2000;
        this.Test();
    }

    @Test
    void train100() throws IOException {
        train(100);
    }

    @Test
    void train500() throws IOException {
        train(500);
    }

    @Test
    void train1000() throws IOException {
        train(1000);
    }

    @Test
    void train2000() throws IOException {
        train(2000);
    }


    public void train(int amount) throws IOException {
        IndexSearcher searcher = IndexFactory.ReadIndex("SuffixIndex");
        IndexSearcher ngramsearcher = IndexFactory.ReadIndex("NgramIndex");
        LambdaMARTAutocomplete lambdaMART = new LambdaMARTAutocomplete(searcher, ngramsearcher);

        String[] originalqueries = this.getTrainingQueries();
        String[] queries = new String[amount];

        // Actually generate the cut off queries.
        resetSeed();
        for (int i = 0; i < amount; i++) {
            queries[i] =  this.CutOff(originalqueries[i]);
        }
        resetSeed();
        // Actually call training method.
        lambdaMART.train(queries, originalqueries);
        // Dump result of training to file.
        //delete(new File(System.getProperty("java.io.tmpdir") + "LambdaMART"));
        //exiPath ngramPath = Files.createDirectory(Paths.get(System.getProperty("java.io.tmpdir") + "LambdaMART"));
        File file = new File(System.getProperty("java.io.tmpdir") + "LambdaMART/LambdaMARTNGram" + amount + ".txt");
        FileWriter writer = new FileWriter(file);
        writer.write(lambdaMART.rerankerString());
        writer.close();
    }
}
