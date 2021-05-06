package MetaData;

import Utils.JSONUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * <h1>Episode</h1>
 * A datatype which stores Show information
 */
public class Show {
    public String sourceID,synopsis,title_en,title_jp,title_romaji,rating,startDate,endDate,ageRating,posterURL,status;
    public int DBID = -1;
    public TorrentMeta torrentMeta;
    public ArrayList<Episode> episodes = new ArrayList<Episode>();
    public Show(){}
    public Show(String sourceID, String synopsis, String title_en,String title_jp,String title_romaji,String rating, String startDate,String endDate,String ageRating,String posterURL,String status){
        this.sourceID = sourceID;
        this.synopsis = synopsis;
        this.title_en = title_en;
        this.title_jp = title_jp;
        this.title_romaji = title_romaji;
        this.rating = rating;
        this.startDate = startDate;
        this.endDate = endDate;
        this.ageRating = ageRating;
        this.posterURL = posterURL;
        this.status = status;
    }
    public Show(JsonObject showObj){
        this.sourceID = JSONUtils.getJSONString(showObj.get("sourceID"));
        this.DBID = JSONUtils.getJSONInt(showObj.get("DBID"));
        this.synopsis = JSONUtils.getJSONString(showObj.get("synopsis"));
        this.title_en = JSONUtils.getJSONString(showObj.get("title_en")) ;
        this.title_jp = JSONUtils.getJSONString(showObj.get("title_jp"));
        this.title_romaji = JSONUtils.getJSONString(showObj.get("title_romaji"));
        this.rating =  JSONUtils.getJSONString(showObj.get("rating"));
        this.startDate =  JSONUtils.getJSONString(showObj.get("startDate"));
        this.endDate = JSONUtils.getJSONString(showObj.get("endDate"));
        this.ageRating = JSONUtils.getJSONString(showObj.get("ageRating")) ;
        this.posterURL = JSONUtils.getJSONString(showObj.get("posterURL")) ;
        this.status = JSONUtils.getJSONString(showObj.get("status")) ;
        JsonArray episodeObjs = showObj.get("episodes").getAsJsonArray();
        for (int i = 0; i < episodeObjs.size(); i++){
            episodes.add(new Episode(episodeObjs.get(i).getAsJsonObject()));
        }
        if(showObj.has("torrentMeta")){
            JsonObject torrentMetaObj = showObj.get("torrentMeta").getAsJsonObject();
            if (torrentMetaObj != null){
                this.torrentMeta = new TorrentMeta(torrentMetaObj);
            }
        }
    }
    public String toJSON(){
        Gson gson = new Gson();
        String tmpSynopsis = gson.toJson(synopsis);
        String json =
                "{" +
                        "\"sourceID\":\"" + sourceID + "\"," +
                        "\"DBID\":" + DBID + "," +
                        "\"title_en\": " + gson.toJson(title_en) + "," +
                        "\"title_jp\": " + gson.toJson(title_jp) + "," +
                        "\"title_romaji\": " + gson.toJson(title_romaji) + "," +
                        "\"synopsis\": " + tmpSynopsis  + "," +
                        "\"rating\": \"" + rating + "\"," +
                        "\"startDate\": \"" + startDate + "\"," +
                        "\"endDate\": \"" + endDate + "\"," +
                        "\"ageRating\": \"" + ageRating + "\"," +
                        "\"posterURL\": \"" + posterURL + "\"," +
                        "\"status\": \"" + status + "\"," +
                        "\"episodes\":[";
        for (int i = 0; i < episodes.size(); i++){
            json += episodes.get(i).toJSON();
            if (i != episodes.size() -1){
                json+= ",";
            }
        }
        String metajson = "";
        json += "]";
        if (torrentMeta != null){
            metajson = torrentMeta.toJSON();
            json += "," +
                    "\"torrentMeta\":" + metajson;
        }
        json += "}";
        return json;
    }

}
