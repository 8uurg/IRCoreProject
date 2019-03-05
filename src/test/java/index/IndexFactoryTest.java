package index;

import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reader.DataReader;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IndexFactoryTest {

    Document document;
    DataReader reader;

    @BeforeEach
    void Setup() throws IOException {
        reader = new DataReader("C:\\Users\\martijn\\Coding\\IRCoreProject\\src\\main\\resources\\user-ct-test-collection-01.txt");
    }

    @Test
    void Test() throws IOException {
        IndexFactory.CreateIndex(reader);
        double d = 0.0;
    }
}