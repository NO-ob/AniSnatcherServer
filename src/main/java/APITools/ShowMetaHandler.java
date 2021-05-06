package APITools;

import javax.net.ssl.HttpsURLConnection;
import java.util.ArrayList;

abstract class ShowMetaHandler {
    abstract public void search(String input, int limit);
    abstract protected ArrayList getShows (HttpsURLConnection conn);
}
