package APITools;

import MetaData.Episode;
import MetaData.Show;
import Utils.JSONUtils;
import Utils.Logger;
import com.google.gson.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;
/**
 * <h1>KitsuHandler</h1>
 * The Kitsu Handler class will talk to the api of Kitsu. It is used for getting metadata for shows
 * <b>Note:</b> Kitsu api documentation is available here https://kitsu.docs.apiary.io/
 */
public class KitsuHandler{
    String baseURL = "https://kitsu.io/api/edge/anime";
    int pageNum = 0;
    ArrayList<Show> shows = new ArrayList<Show>();

    /**
     * Queries the Kitsu api asking for shows with the input name and then returns a list of Show Objects
     * @param input The input the user wants to search for
     * @param limit The amount of shows the Kitsu api will return on a single page
     * @return shows - An ArrayList list of show Objects
     */
    public ArrayList search(String input, int limit){
        try{
            URL url = new URL(baseURL + "?page[limit]="+limit+"&page[offset]=" +pageNum+ "&filter[text]=" + input);
            Logger.Inst().log("KitsuHandler","search","Got query for " + input);
            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            JsonArray jsonArray = getJSONArray(conn);
            if (jsonArray != null){
                for (int i = 0; i < jsonArray.size(); i++){
                    shows.add(showBuilder(jsonArray.get(i).getAsJsonObject()));
                }
            }

        } catch (IOException e){
            Logger.Inst().log("KitsuHandler","search","Exception in Search Function" + e);
        }
        return shows;
    }
    
    /**
     * Creates a show object from the JSON the Kitsu API returns
     * @param showJSON The json for a single show
     * @return Show - A Show object with a full episode list
     */
    private Show showBuilder(JsonObject showJSON){
        String sourceID = JSONUtils.getJSONString(showJSON.get("id")),synopsis,title_en = null,title_jp,title_romaji,rating,startDate,endDate,ageRating,posterURL,status,episodeCount;
        Show show = null;
        //Get attributes
        JsonObject tmp = showJSON.get("attributes").getAsJsonObject();
        synopsis = JSONUtils.getJSONString(tmp.get("synopsis"));
        rating = JSONUtils.getJSONString(tmp.get("rating"));
        startDate = JSONUtils.getJSONString(tmp.get("startDate"));
        endDate = JSONUtils.getJSONString(tmp.get("endDate"));
        ageRating = JSONUtils.getJSONString(tmp.get("ageRating"));
        status = JSONUtils.getJSONString(tmp.get("status"));
        episodeCount = "0";
        if (tmp.get("episodeCount") != null){
            episodeCount = tmp.get("episodeCount").toString();
        }
        //Get titles
        tmp = tmp.get("titles").getAsJsonObject();
        title_en = JSONUtils.getJSONString(tmp.get("en")).replace("(TV)","");
        title_jp = JSONUtils.getJSONString(tmp.get("ja_jp")).replace("(TV)","");
        title_romaji = JSONUtils.getJSONString(tmp.get("en_jp")).replace("(TV)","");
        //Get Poster
        tmp = showJSON.get("attributes").getAsJsonObject();
        tmp = tmp.get("posterImage").getAsJsonObject();
        posterURL = JSONUtils.getJSONString(tmp.get("large")).split("\\?")[0];
        show = new Show(sourceID,synopsis,title_en,title_jp,title_romaji,rating,startDate,endDate,ageRating,posterURL,status);
        if(!title_en.isEmpty() || !title_jp.isEmpty() || !title_romaji.isEmpty()){
            Logger.Inst().log("KitsuHandler","showBuilder","Found Show: [" + sourceID + "] "+ title_romaji);
            Logger.Inst().log("KitsuHandler","showBuilder","Ep Count: " + episodeCount);
            int pageNum = 0;
            while ((show.episodes.size() < Integer.parseInt(episodeCount))){
                JsonArray epData = getEpData(sourceID, pageNum);
                if (epData != null){
                    for (int i = 0; i < epData.size(); i++){
                        JsonObject data = epData.get(i).getAsJsonObject();
                        String epID = JSONUtils.getJSONString(data.get("id"));
                        data = data.get("attributes").getAsJsonObject();
                        String epSynopsis = JSONUtils.getJSONString(data.get("synopsis"));
                        int epSeasonNum;
                        epSeasonNum = JSONUtils.getJSONInt(data.get("seasonNumber"));
                        int epNum;
                        epNum = JSONUtils.getJSONInt(data.get("number"));
                        data =  data.get("titles").getAsJsonObject();
                        String epEng = JSONUtils.getJSONString(data.get("en_us"));
                        String epRom = JSONUtils.getJSONString(data.get("en_jp"));
                        String epJA = JSONUtils.getJSONString(data.get("ja_jp"));
                        Logger.Inst().log("KitsuHandler","showBuilder","Found EP: " + epEng);
                        show.episodes.add(new Episode(epID,epSeasonNum,epNum,epSynopsis,epEng,epJA,epRom));
                    }
                } else {
                    break;
                }
                pageNum ++;
            }
        }

        return show;
    }
    /**
     * Gets episode data from the Kitsu API and returns a JSONArray
     * @param showID The id of the show on Kitsu
     * @param pageNum The pagenumber of the results
     * @return A JSONArray containg episode data
     */
    private JsonArray getEpData(String showID, int pageNum){
        try{
            URL url = new URL(baseURL + "/"+showID+"/episodes?page[limit]=20&page[offset]="+pageNum * 20);
            Logger.Inst().log("KitsuHandler","getEpData","Getting episodes for " + showID);
            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            return getJSONArray(conn);
        } catch (IOException e){
            Logger.Inst().log("KitsuHandler","episodeListBuilder","Exception in episode get" + e);
        }
        return null;
    }
    /**
     * Converts the response of the http request into a JSONArray
     * @param conn A HttpsURLConnection
     * @return A JSONArray
     */
    private JsonArray getJSONArray(HttpsURLConnection conn){
        String response = new String();
        if (conn != null){
            try{
                if (conn.getResponseCode() == 200) {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    response = bufferedReader.lines().collect(Collectors.joining("\n"));
                    if (response.length() > 0) {
                        JsonObject jsonObj = JsonParser.parseString(response).getAsJsonObject();
                        JsonArray jsonArray = jsonObj.getAsJsonArray("data");
                        Logger.Inst().log("KitsuHandler","getJSON","Array Found - Length: " + jsonArray.size());
                        return jsonArray;
                    }
                }

            } catch (IOException e){
                Logger.Inst().log("KitsuHandler","getItems","Exception in getItems " + e);
                Logger.Inst().log("KitsuHandler","getItems","Kitsu may be offline or you have no internet connection " + e);
            }
        }
        return null;
    }
}
