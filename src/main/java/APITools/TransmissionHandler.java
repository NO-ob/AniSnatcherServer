package APITools;

import MetaData.TorrentMeta;
import Utils.JSONUtils;
import Utils.Logger;
import Utils.SettingsHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.simple.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;
/**
 * <h1>TransmissionHandler</h1>
 * The Transmission handler class will talk to the RPC api of Transmission. It is used for adding/managing torrents
 * <b>Note:</b> Tranmsission RPC spec is available here https://github.com/transmission/transmission/blob/master/extras/rpc-spec.txt
 */
public class TransmissionHandler {
    enum PostMethod {
        ADD,
        SETMUTATORS,
        GETINFO,
        REMOVE,
    }
    String urlStr = "http://"+ SettingsHandler.Inst().transmissionIP +":"+SettingsHandler.Inst().transmissionPort+"/transmission/rpc";
    String sessionID = "";
    public TransmissionHandler(){
        setSessionID();
    }
    /**
     * Sends an empty request to transmission so it can get the sessionID which is required for making api queries.
     * The sessionID is then stored in the sessionID variable
     */
    private void setSessionID (){
        try{
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            if (conn.getResponseCode() == 409){
                sessionID = conn.getHeaderField("X-Transmission-Session-Id");
                Logger.Inst().log("TransmissionHandler","setSessionID","Got Session ID: "+ sessionID);
            }
            if (sessionID.isEmpty()){
                Logger.Inst().log("TransmissionHandler","setSessionID","Failed to get transmission session ID");
            }
        } catch (IOException e) {
            Logger.Inst().log("TransmissionHandler","setSessionID","Transmission may be offline or the port and ip are wrong");
        }
    }

    /**
     * Calls sendPostReq to add a torrent to transmission
     * @param torrentURL the url of the torrent file e.g. https://site.com/uwu.torrent
     * @param torrentName the name of the torrent
     * @return String the transmission torrent ID.
     */
    public String addTorrent (String torrentURL, String torrentName){
        String torrentID = null;
        ArrayList methodArgs = new ArrayList();
        methodArgs.add(torrentURL);
        String response = sendPostReq(PostMethod.ADD,methodArgs);
        if (response != null){
            Logger.Inst().log("TransmissionHandler","addTorrent","Torrent added: " + torrentName);
            torrentID = response.substring(response.indexOf("id") + 4,response.indexOf(",",response.indexOf("id")));
            setTorrentMutators(torrentID);
            //getTorrentInfo(torrentID);
        } else {
            Logger.Inst().log("TransmissionHandler","addTorrent","Failed to add torrent: " + torrentName);
        }
        return torrentID;
    }

    /**
     * Calls send ost request to delete a torrent form transmission
     * @param torrentID - torrent id in transmission
     * @param torrentName - name of torrent
     */
    public void removeTorrent (String torrentID, String torrentName){
        ArrayList methodArgs = new ArrayList();
        methodArgs.add(torrentID);
        sendPostReq(PostMethod.REMOVE,methodArgs);
        Logger.Inst().log("TransmissionHandler","removeTorrent","Torrent Removed: " + torrentName);
    }
    /**
     * Calls sendPostReq to set the label of a torrent as "AniSnatcher"
     * @param torrentID The id of the torrent in transmission
     */
    private void setTorrentMutators(String torrentID){
        ArrayList methodArgs = new ArrayList();
        methodArgs.add(torrentID);
        String response = sendPostReq(PostMethod.SETMUTATORS, methodArgs);
        if (response != null){
            Logger.Inst().log("TransmissionHandler","setTorrentMutators","Mutators set for torrent: " + torrentID);
        } else {
            Logger.Inst().log("TransmissionHandler","setTorrentMutators","Failed to set mutators for: " + torrentID);
        }
    }

    /**
     * This will get information about torrent such as it status, currently incompelte
     * @param meta a torrent meta object which needs its values updating
     * @return int This returns sum of numA and numB.
     */
    public TorrentMeta getTorrentInfo(TorrentMeta meta){
        ArrayList methodArgs = new ArrayList();
        methodArgs.add(meta.clientID);
        String response = sendPostReq(PostMethod.GETINFO, methodArgs);
        if (response != null){
            JsonObject jsonObj = JsonParser.parseString(response).getAsJsonObject();
            if (jsonObj.has("arguments")){
                jsonObj = jsonObj.get("arguments").getAsJsonObject();
                if (jsonObj.has("torrents")) {
                    JsonArray array = jsonObj.get("torrents").getAsJsonArray();
                    if (array.size() > 0){
                        jsonObj = array.get(0).getAsJsonObject();
                        meta.percentDone = JSONUtils.getJSONDouble(jsonObj.get("percentDone"));
                        switch (JSONUtils.getJSONInt(jsonObj.get("status"))){
                            case(0):
                                meta.status = "Paused";
                                break;
                            case(3):
                                meta.status = "Queued";
                                break;
                            case(4):
                                meta.status = "Downloading";
                                break;
                            case(6):
                                meta.status = "Seeding";
                                break;
                        }
                    }
                }
            }
        } else {
        }
        return meta;
    }

    /**
     * Makes a post request to transmission, does different things depending on the PostMethod enum parsed to the function
     * @param method a PostMethod enum value used to the function know whats request to send to the transmission api
     * @param methodArgs an Arraylist containing any extra information needed for the post request
     * @return String returns the response body received from transmission
     */
    private String sendPostReq(PostMethod method, ArrayList methodArgs){
        String json = null;
        switch (method){
            case ADD:
                json = "{\"arguments\": {\"filename\": \""+ methodArgs.get(0) +"\"},\"method\": \"torrent-add\"}";
                break;
            case SETMUTATORS:
                json = "{\"arguments\": {\"ids\": ["+ methodArgs.get(0) +"],\"labels\" : [\"AniSnatcher\"]},\"method\": \"torrent-set\"}";
                break;
            case GETINFO:
                json = "{\"arguments\": {\"fields\": [\"name\",\"status\",\"percentDone\"],\"ids\": ["+ methodArgs.get(0) +"]},\"method\": \"torrent-get\"}";
                break;
            case REMOVE:
                json = "{\"arguments\": {\"ids\": ["+ methodArgs.get(0)+"],\"delete-local-data\": \"true\"},\"method\": \"torrent-remove\"}";
                break;
        }
        if (json != null){
            try{
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("X-Transmission-Session-Id", sessionID);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                byte[] encodedJSON = json.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(encodedJSON.length);
                conn.connect();
                try(OutputStream os = conn.getOutputStream()) {
                    os.write(encodedJSON);
                }
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String response = bufferedReader.lines().collect(Collectors.joining("\n"));
                String result = response.substring(response.indexOf("result") + 9,response.indexOf("\"",response.indexOf("result") + 9)).strip();
                if (conn.getResponseCode() == 200 && result.equals("success")){
                    return response;
                } else {
                    Logger.Inst().log("TransmissionHandler","sendPostReq", "Method Type: " + method.name());
                    Logger.Inst().log("TransmissionHandler","sendPostReq", "Result: " + response);
                }
            } catch (IOException e) {
                Logger.Inst().log("TransmissionHandler","sendPostReq", "Method Type: " + method.name());
                Logger.Inst().log("TransmissionHandler","sendPostReq", "Exception: " + e);
            }
        }
        return null;
    }
}
