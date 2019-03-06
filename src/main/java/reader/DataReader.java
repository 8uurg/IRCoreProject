package reader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class DataReader {

    BufferedReader br = null;
    FileReader fr = null;
    String name;

    public DataReader(String filename) throws IOException {
        name = filename;
        fr = new FileReader(filename);
        br = new BufferedReader(fr);
        br.readLine();
    }

    public SearchQuery ReadLine() throws IOException {
        String currentLine = br.readLine();

        if(currentLine == null) {
            try {

                if (br != null)
                    br.close();

                if (fr != null)
                    fr.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }
            return null;
        }

        return Parse(currentLine);
    }


    public static SearchQuery Parse(String input) {
        String[] parts = input.split("\t");
        SearchQuery query = new SearchQuery();
        try {
            query.ID = Integer.parseInt(parts[0]);
        } catch (NumberFormatException ignored) {}

        query.Query = parts[1];
        query.DateTime = parts[2];

        if(parts.length > 3) {
            try {
                query.ClickedUrlRank = Integer.parseInt(parts[3]);
            } catch (NumberFormatException ignored) {}
            query.ClickedUrl = parts[4];
        }
        return query;
    }


}
