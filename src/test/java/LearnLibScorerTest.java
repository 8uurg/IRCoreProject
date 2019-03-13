import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.DenseDataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.metric.ReciprocalRankScorer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class LearnLibScorerTest {
    @Test
    public void testyTest() {
        ReciprocalRankScorer scorer = new ReciprocalRankScorer();

        DenseDataPoint a = new DenseDataPoint("1.0 :hi 1:0.0 2:1.0");
        DenseDataPoint b = new DenseDataPoint("0.0 :hi 1:0.0 2:1.0");

        RankList l1 =  new RankList(Arrays.asList(new DenseDataPoint[]{a, b}));
        RankList l2 =  new RankList(Arrays.asList(new DenseDataPoint[]{b, a}));
        System.out.println(l1.get(0).getLabel());
        System.out.println(scorer.score(l1));
        System.out.println(scorer.score(l2));
    }
}
