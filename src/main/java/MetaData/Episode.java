package MetaData;

import Utils.JSONUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * <h1>Episode</h1>
 * A datatype which stores Episode information
 */
public class Episode {
    public String sourceID,synopsis,title_en,title_jp,title_romaji;
    public int DBID = -1, seasonNumber = -1, number = -1;
    public Episode(String sourceID, int seasonNumber, int number, String synopsis, String title_en, String title_jp, String title_romaji){
        this.sourceID = sourceID;
        this.seasonNumber = seasonNumber;
        this.number = number;
        this.synopsis = synopsis;
        this.title_en = title_en;
        this.title_jp = title_jp;
        this.title_romaji = title_romaji;
    }
    public Episode(){}
    public Episode(JsonObject episodeObj){
        this.sourceID = JSONUtils.getJSONString(episodeObj.get("sourceID"));
        this.DBID = JSONUtils.getJSONInt(episodeObj.get("DBID"));
        this.synopsis = JSONUtils.getJSONString(episodeObj.get("synopsis"));
        this.seasonNumber = JSONUtils.getJSONInt(episodeObj.get("seasonNumber")) ;
        this.number = JSONUtils.getJSONInt(episodeObj.get("number")) ;
        this.title_en = JSONUtils.getJSONString(episodeObj.get("title_en")) ;
        this.title_jp = JSONUtils.getJSONString(episodeObj.get("title_jp")) ;
        this.title_romaji =  JSONUtils.getJSONString(episodeObj.get("title_romaji")) ;
    }
    public String toJSON(){
        Gson gson = new Gson();
        String tmpSynopsis = gson.toJson(synopsis);
        String json =
                "{" +
                        "\"sourceID\":\"" + sourceID + "\"," +
                        "\"seasonNumber\":" + seasonNumber + "," +
                        "\"DBID\":" + DBID + "," +
                        "\"number\": " + number  + "," +
                        "\"synopsis\": " + tmpSynopsis  + "," +
                        "\"title_en\": " + gson.toJson(title_en) + "," +
                        "\"title_jp\": " + gson.toJson(title_jp) + "," +
                        "\"title_romaji\": " + gson.toJson(title_romaji) + "}";
        return json;

    }
}
