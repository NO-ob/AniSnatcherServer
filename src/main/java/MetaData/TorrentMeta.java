package MetaData;

import Utils.JSONUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
/**
 * <h1>Episode</h1>
 * A datatype which stores Torrent information
 */
public class TorrentMeta {
    public String releaseName = "",torrentURL = "",pageURL = "",releaseGroup = "",resolution = "",sourceType = "",torrentDirectory,status;
    public ArrayList<TorrentFile> fileList = new ArrayList<>();
    public int snatchCount;
    public int seedCount;
    public int DBID = -1;
    public int showID = -1;
    public String clientID;
    public double percentDone = 0;
    public String toString(){
        return "\nRelease Name: " + releaseName + "\n" +
                "Release Group: " + releaseGroup + "\n" +
                "Resolution: " + resolution + "\n" +
                "Source Type: " + sourceType + "\n" +
                "Page URL: " + pageURL + "\n" +
                "Torrent URL: " + torrentURL + "\n" +
                "Total Snatches: " + snatchCount + "\n" +
                "Seeders: " + seedCount + "\n"+
                "Directory: " + torrentDirectory + "\n" +
                "File Count: " + fileList.size() + "\n";

    }

    public int getSnatchCount() {
        return snatchCount;
    }
    public TorrentMeta(){}
    public String toJSON(){
        Gson gson = new Gson();
        String tmpName = gson.toJson(releaseName);
        String json =
                "{" +
                        "\"releaseName\":" + tmpName + "," +
                        "\"releaseGroup\":\"" + releaseGroup + "\"," +
                        "\"torrentURL\":\"" + torrentURL + "\"," +
                        "\"pageURL\": \"" + pageURL + "\"," +
                        "\"sourceType\": \"" + sourceType + "\"," +
                        "\"resolution\": \"" + resolution + "\"," +
                        "\"torrentDirectory\": \"" + torrentDirectory  + "\"," +
                        "\"status\": \"" + status + "\"," +
                        "\"snatchCount\": \"" + snatchCount + "\"," +
                        "\"seedCount\": \"" + seedCount + "\"," +
                        "\"DBID\": \"" + DBID + "\"," +
                        "\"showID\": \"" + showID + "\"," +
                        "\"clientID\": \"" + clientID + "\"," +
                        "\"percentDone\": \"" + percentDone + "\"," +
                        "\"fileList\":[";

        if (!fileList.isEmpty()){
            for (int i = 0; i < fileList.size(); i++){
                json += fileList.get(i).toJSON();
                if (i != fileList.size() -1){
                    json+= ",";
                }
            }
        }
        json += "]}";
        return json;
    }
    public TorrentMeta(JsonObject torrentMetaObj){
        this.DBID = JSONUtils.getJSONInt(torrentMetaObj.get("DBID"));
        this.releaseName = JSONUtils.getJSONString(torrentMetaObj.get("releaseName"));
        this.torrentURL = JSONUtils.getJSONString(torrentMetaObj.get("releaseGroup"));
        this.torrentURL = JSONUtils.getJSONString(torrentMetaObj.get("torrentURL"));
        this.pageURL = JSONUtils.getJSONString(torrentMetaObj.get("pageURL"));
        this.sourceType = JSONUtils.getJSONString(torrentMetaObj.get("sourceType"));
        this.resolution = JSONUtils.getJSONString(torrentMetaObj.get("resolution"));
        this.torrentDirectory = JSONUtils.getJSONString(torrentMetaObj.get("torrentDirectory"));
        this.status = JSONUtils.getJSONString(torrentMetaObj.get("status"));
        this.snatchCount = JSONUtils.getJSONInt(torrentMetaObj.get("snatchCount"));
        this.seedCount = JSONUtils.getJSONInt(torrentMetaObj.get("seedCount"));
        this.showID = JSONUtils.getJSONInt(torrentMetaObj.get("showID"));
        this.clientID = JSONUtils.getJSONString(torrentMetaObj.get("clientID"));
        this.percentDone = JSONUtils.getJSONDouble(torrentMetaObj.get("percentDone"));
        JsonArray torrentFileObjs = torrentMetaObj.get("fileList").getAsJsonArray();
        for (int i = 0; i < torrentFileObjs.size(); i++){
            fileList.add(new TorrentFile(torrentFileObjs.get(i).getAsJsonObject()));
        }
    }
}

