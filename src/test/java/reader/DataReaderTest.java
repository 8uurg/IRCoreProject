package reader;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DataReaderTest {

    @Test
    void TestParse() {
        SearchQuery query = DataReader.Parse("479\tcar sponsor decals\t2006-03-03 23:28:59\t\t");
        assertEquals(479, query.ID);
        assertEquals("car sponsor decals", query.Query);
        assertEquals(-1, query.ClickedUrlRank);
    }

    @Test
    void TestParseWithoutResult() {
        SearchQuery query = DataReader.Parse("479\tcar window sponsor decals\t2006-03-03 23:27:17\t3\thttp://www.streetglo.net");
        assertEquals(479, query.ID);
        assertEquals("car window sponsor decals", query.Query);
        assertEquals("http://www.streetglo.net", query.ClickedUrl);
        assertEquals(3, query.ClickedUrlRank);
    }

    @Test
    void TestRead() throws IOException {
        DataReader reader = new DataReader("C:\\Users\\martijn\\Coding\\IRCoreProject\\src\\main\\resources\\user-ct-test-collection-01.txt");
        SearchQuery q = reader.ReadLine();
        int i = q.ClickedUrlRank;
    }
}