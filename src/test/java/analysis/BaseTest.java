package analysis;

import index.IndexFactory;
import irproject.ICompletionAlgorithm;
import irproject.MostPopularCompletion;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.Test;
import reader.DataReader;
import reader.SearchQuery;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public abstract class BaseTest {
    abstract ICompletionAlgorithm GetAlgorithm(IndexSearcher searcher);

    Random r = new Random(123123123);

    @Test
    public void Test() throws IOException {
        OriginalMetric();
    }

    public void OriginalMetric() throws IOException {
        DataReader reader = new DataReader(GetFile("user-ct-test-collection-10.txt"));

        SearchQuery query;
        int total = 0;
        int correct = 0;
        IndexSearcher s = IndexFactory.ReadIndex();
        ICompletionAlgorithm alg = GetAlgorithm(s);

        for (int i = 0; i < 2000; i++) {
            query = reader.ReadLine();
            final String q = query.Query;
            String[] docs = alg.query(CutOff(query.Query), 10);
            if(Arrays.asList(docs).contains(q)) {
                System.out.print("1");
                correct++;
            } else {
                System.out.print("0");
            }
            total++;

            if(total%100 == 0) {
                System.out.println();
            }

        }
        System.out.println();
        System.out.println("Correctness Original Metric: " + correct + "/" +total);
    }

    public String CutOff(String s) {
        if(s.length() <= 1 ){
            return s;
        }
        return s.substring(0, r.nextInt(s.length()-1) + 1);
    }

    private String GetFile(String name) {
        return this.getClass().getClassLoader().getResource(name).getPath();
    }
}
