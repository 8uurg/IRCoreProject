import index.IndexFactory;
import irproject.ICompletionAlgorithm;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.Test;
import reader.DataReader;
import reader.SearchQuery;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public abstract class BaseTest {
    abstract ICompletionAlgorithm GetAlgorithm() throws IOException;

    private long randomSeed = 123123123;

    Random r = new Random(randomSeed);
    public final static int TESTSIZE = 10000;
    public final boolean ENABLE_PRINT = false;
    public boolean ONLY_NEW = false;
    public final int K = 10;

    @Test
    public void Test() throws IOException {
        OriginalMetric();
    }

    @Test
    void TestOnlyNew() throws IOException {
        ONLY_NEW = true;
        OriginalMetric();
    }

    public void OriginalMetric() throws IOException {
        DataReader reader = new DataReader(GetFile("user-ct-test-collection-10.txt"));

        IndexSearcher searcher = null;
        if (ONLY_NEW) {
            searcher = IndexFactory.ReadIndex("QueryIndex");
        }


        SearchQuery query;
        double MRR = 0;
        double SIMPLE_RELEVANCE = 0.0;
        double DCGRELEVANCE = 0.0;
        double DCGRELEVANCECAPPED = 0.0;
        ICompletionAlgorithm alg = GetAlgorithm();
        NumberFormat formatter = new DecimalFormat("#0.000");

        for (int i = 0; i < TESTSIZE; i++) {
            query = reader.ReadLine();

            if (ONLY_NEW) {
                 TopDocs docs =  searcher.search(new TermQuery(new Term("query",query.Query)), 1);
                 if(docs.scoreDocs.length > 0) {
                     String val = searcher.doc(docs.scoreDocs[0].doc).get("query");
                     if(val.equals(query.Query)) {
                         continue;
                     }
                 }
            }

            final String originalQ = query.Query;
            String q = CutOff(query.Query);
            if(q.equals(query.Query)) {
                continue;
            }
            String[] docs = alg.query(q, 10);
            int index = -1;
            double mrrChange = 0.0;
            if((index = Arrays.asList(docs).indexOf(originalQ)) != -1) {
                if(index <= K) {
                    mrrChange = 1.0/(index+1);
                }
            }

            double simpleRelevanceChange = 0.0;
            double dcgRelevanceChange = 0.0;
            if (docs.length > 0 && docs[0] != null) {
                simpleRelevanceChange = calculateRelevance(originalQ, q, docs[0]);

                for (int j = 0; j < docs.length; j++) {
                    double log = Math.log(j+2) / Math.log(2.0);
                    dcgRelevanceChange += calculateRelevance(originalQ, q, docs[j]);
                    if(j > K) {
                        break;
                    }
                }

                double d = 0;
            }

            MRR += mrrChange;
            SIMPLE_RELEVANCE += simpleRelevanceChange;
            DCGRELEVANCE += dcgRelevanceChange;
            DCGRELEVANCECAPPED += Math.min(dcgRelevanceChange, 1.0);

            Print(padRight(i + ":", 6) + padRight( "MRR: " + formatter.format(mrrChange), 12) + padRight(" RL: " + formatter.format(simpleRelevanceChange), 11) + " RLDCG: " + formatter.format(dcgRelevanceChange));
            if(Math.abs(mrrChange - simpleRelevanceChange) > 0.2) {
                Print("  | Large change! originalQ: '" + originalQ + "' q: '" + q + "' docs[0]: '" + docs[0] + "'");
            }

            if(!ENABLE_PRINT) {
                if (i % 500 == 0) {
                    System.out.println(i);
                }
            }
        }
        System.out.println(padRight(TESTSIZE + ":", 6) + padRight( "MRR: " + formatter.format(MRR / TESTSIZE), 13) + padRight("RL: " + formatter.format(SIMPLE_RELEVANCE / TESTSIZE), 11) + padRight(" RLDCGCCAPPED: " + formatter.format(DCGRELEVANCECAPPED/TESTSIZE), 11)  + " RLDCG: " + formatter.format( DCGRELEVANCE/TESTSIZE));
        Print("");
    }

    private double calculateRelevance(String originalQ, String q, String suggestion) {
        if(suggestion == null) {
            return 0;
        }
        double relevanceChange;
        int similar = 0;
        while (similar < suggestion.length() && similar < originalQ.length()) {
            if(suggestion.charAt(similar) != originalQ.charAt(similar)) {
                break;
            }
            similar++;
        }

        relevanceChange = (Math.max(0, 2 * similar - suggestion.length() - q.length())/Double.sum (originalQ.length() - q.length(), 0));
        return relevanceChange;
    }

    public void Print(String s) {
        if(ENABLE_PRINT) {
            System.out.println(s);
        }
    }

    public static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%" + n + "s", s);
    }


    public String CutOff(String s) {
        //TODO keep first word
        if(s.length() <= 1 ){
            return s;
        }
        return s.substring(0, r.nextInt(s.length()-1) + 1);
    }

    public String[] getTrainingQueries() throws IOException {
        DataReader reader = new DataReader(GetFile("samples.txt"));
        ArrayList<String> queries = new ArrayList<>(4000);
        SearchQuery current;
        while ((current = reader.ReadLine()) != null) {
            queries.add(current.Query);
        }
        String[] a = new String[]{};
        return queries.toArray(a);
    }

    public void resetSeed() {
        r.setSeed(randomSeed);
    }

    private String GetFile(String name) {
        return this.getClass().getClassLoader().getResource(name).getPath();
    }

    protected static void delete(File file)
            throws IOException{

        if(file.isDirectory()){

            //directory is empty, then delete it
            if(file.list().length==0){

                file.delete();
                System.out.println("Directory is deleted : "
                        + file.getAbsolutePath());

            }else{

                //list all the directory contents
                String files[] = file.list();

                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp);

                    //recursive delete
                    delete(fileDelete);
                }

                //check the directory again, if empty then delete it
                if(file.list().length==0){
                    file.delete();
                    System.out.println("Directory is deleted : "
                            + file.getAbsolutePath());
                }
            }

        }else{
            //if file, then delete it
            file.delete();
            //System.out.println("File is deleted : " + file.getAbsolutePath());
        }
    }
}
