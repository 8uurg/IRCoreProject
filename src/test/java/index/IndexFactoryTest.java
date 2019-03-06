package index;

import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reader.DataReader;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class IndexFactoryTest {

    Document document;
    DataReader reader;

    @BeforeEach
    void Setup() throws IOException {
        reader = new DataReader(GetFile("user-ct-test-collection-01.txt"));
        IndexFactory.DeleteIfExists();
    }

    @Test
    void Test() throws IOException {
        IndexFactory.CreateIndex(Collections.singletonList(reader));
        Main.main(new String[]{});
        double d = 0.0;
    }

    @Test
    void TestAll() throws IOException {
        IndexFactory.CreateIndex(Arrays.asList(reader,
                new DataReader(GetFile("user-ct-test-collection-02.txt")),
                new DataReader(GetFile("user-ct-test-collection-03.txt")),
                new DataReader(GetFile("user-ct-test-collection-04.txt")),
                new DataReader(GetFile("user-ct-test-collection-05.txt")),
                new DataReader(GetFile("user-ct-test-collection-06.txt")),
                new DataReader(GetFile("user-ct-test-collection-07.txt")),
                new DataReader(GetFile("user-ct-test-collection-08.txt")),
                new DataReader(GetFile("user-ct-test-collection-09.txt")),
                new DataReader(GetFile("user-ct-test-collection-10.txt"))));
    }

    private String GetFile(String name) {
        return this.getClass().getClassLoader().getResource(name).getPath();
    }
}