package MetaData;

import Utils.JSONUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TorrentFile {
    public int torrentDBID = -1, episodeDBID = -1,DBID = -1;
    public String fileName;
    public TorrentFile(String fileName){
        this.fileName = fileName;
    }
    public TorrentFile(String fileName, int torrentDBID, int episodeDBID){
        this.fileName = fileName;
        this.torrentDBID = torrentDBID;
        this.episodeDBID = episodeDBID;
    }
    public String toJSON(){
        Gson gson = new Gson();
        String tmpFileName = gson.toJson(fileName);
        String json =
                "{" +
                        "\"fileName\":" + tmpFileName + "," +
                        "\"torrentDBID\":" + torrentDBID + "," +
                        "\"DBID\":" + DBID + "," +
                        "\"episodeDBID\": " + episodeDBID  + "}";
        return json;
    }
    public TorrentFile(JsonObject torrentFileObj){
        this.fileName = JSONUtils.getJSONString(torrentFileObj.get("fileName"));
        this.torrentDBID = JSONUtils.getJSONInt(torrentFileObj.get("torrentDBID"));
        this.DBID = JSONUtils.getJSONInt(torrentFileObj.get("DBID"));
        this.episodeDBID = JSONUtils.getJSONInt(torrentFileObj.get("episodeDBID"));
    }
}
