package reader;

public class SearchQuery {
    public int ID;
    public String Query;
    public String DateTime;
    public int ClickedUrlRank;
    public String ClickedUrl;

    public SearchQuery() {
        ClickedUrlRank = -1;
    }
}
