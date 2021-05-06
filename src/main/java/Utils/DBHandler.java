package Utils;

import MetaData.Episode;
import MetaData.Show;
import MetaData.TorrentFile;
import MetaData.TorrentMeta;
import org.sqlite.core.DB;

import java.sql.*;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;

public class DBHandler {
    public Connection dbConn = null;
    public boolean dbConnect(){
        try {
            //String dbPath = "jdbc:sqlite:" + System.getProperty("user.home") + "/mnt/Eucli/.AniSnatcher/database.db";
            String dbPath = "jdbc:sqlite:" + SettingsHandler.Inst().databaseDirectory + "database.db";
            dbConn = DriverManager.getConnection(dbPath);
            Statement query = dbConn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS Show" +
                    "(showID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "sourceID TEXT , " +
                    "sourceType TEXT , " +
                    "synopsis TEXT , " +
                    "title_en TEXT ," +
                    "title_jp TEXT ," +
                    "title_romaji TEXT," +
                    "posterURL TEXT," +
                    "airingStatus TEXT);";
            query.executeUpdate(sql);
            sql = "CREATE TABLE IF NOT EXISTS Episode" +
                    "(episodeID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "sourceID TEXT ," +
                    "synopsis TEXT ," +
                    "title_en TEXT ," +
                    "title_jp TEXT ," +
                    "title_romaji TEXT," +
                    "episodeNum INTEGER," +
                    "seriesNum INTEGER," +
                    "showID INTEGER);"; // Show FK
            query.executeUpdate(sql);
            sql = "CREATE TABLE IF NOT EXISTS TorrentFile" +
                    "(fileID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "fileName TEXT," +
                    "torrentID INTEGER," +
                    "episodeID INTEGER);";
            query.executeUpdate(sql);
            sql = "CREATE TABLE IF NOT EXISTS Torrent" +
                    "(torrentID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "clientType TEXT," +
                    "showID INTEGER," +
                    "releaseName TEXT," +
                    "releaseGroup TEXT," +
                    "resolution TEXT," +
                    "sourceType TEXT," +
                    "torrentURL TEXT," +
                    "pageURL TEXT," +
                    "clientID TEXT NOT NULL," +
                    "status TEXT," +
                    "percentDone DOUBLE," +
                    "path TEXT);";
            query.executeUpdate(sql);
            query.close();
            if (dbConn != null){
                Logger.Inst().log("DBHandler","dbConnect","Connected to database");
                return true;
            }
        } catch (SQLException e){
            Logger.Inst().log("DBHandler","dbConnect","Exception while connecting to database: " + e);
        }
        return false;
    }
    public void insertShow(Show show){
        int showDBID = -1;
        if (!showExists(show.sourceID)){
            String sql = "INSERT INTO Show ('sourceType','sourceID','synopsis','title_en','title_jp','title_romaji','posterURL','airingStatus') " +
                    "VALUES ('Kitsu',?,?,?,?,?,?,?);";
            try {
                PreparedStatement pstmt = dbConn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
                pstmt.setString(1,show.sourceID);
                pstmt.setString(2,show.synopsis);
                pstmt.setString(3,show.title_en);
                pstmt.setString(4,show.title_jp);
                pstmt.setString(5,show.title_romaji);
                pstmt.setString(6,show.posterURL);
                pstmt.setString(7,show.status);
                showDBID = insertAndGetID(pstmt,"insertShow");
                if (showDBID != -1){
                    for (int i = 0; i < show.episodes.size(); i++){
                        insertEpisode(show.episodes.get(i),showDBID);
                    }
                }
                show.DBID = showDBID;
            } catch (SQLException e) {
                Logger.Inst().log("DBHandler","insertShow","Exception in prepared statement creation: " + e);
            }
        } else {
            show.DBID = getShowID(show.sourceID);
            for (int i = 0; i < show.episodes.size(); i++){
                show.episodes.get(i).DBID = getEpisodeID(show.episodes.get(i).sourceID);
            }
        }
    }
    public void updateTorrentStats(TorrentMeta meta){
        String sql = "UPDATE Torrent SET status = ?, percentDone = ? WHERE torrentID = ?";
        try {
            PreparedStatement pstmt = dbConn.prepareStatement(sql);
            pstmt.setString(1,meta.status);
            pstmt.setDouble(2,meta.percentDone);
            pstmt.setInt(3, meta.DBID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.Inst().log("DBHandler","updateTorrentStats","Exception updating torrent stats: " + e);
        }

    }
    public int insertAndGetID(PreparedStatement pstmt, String caller){
        int DBID = -1;
        if(dbConn!=null){
            try {
                pstmt.execute();
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    DBID = generatedKeys.getInt(1);
                }
                pstmt.close();
            } catch (Exception e) {
                Logger.Inst().log("DBHandler","insertAndGetID","Exception in db query from " + caller +" : " + e);
            }
        } else {
            Logger.Inst().log("DBHandler","insertAndGetID","DBConn is null caller is: " + caller);
        }
        return DBID;
    }
    public ArrayList<TorrentFile> selectTorrentFiles(int torrentID){
        ArrayList<TorrentFile> fileList = new ArrayList<>();
        try{
            String sql = "SELECT * FROM TorrentFile WHERE torrentID = ? ORDER BY episodeID ASC;";
            PreparedStatement pstmt = dbConn.prepareStatement(sql);
            pstmt.setInt(1,torrentID);
            ResultSet results = selectAndGetResults(pstmt, "selectTorrentFiles");
            if (results != null){
                while (results.next()) {
                    TorrentFile tmp = new TorrentFile(
                            results.getString("fileName"),
                            results.getInt("torrentID"),
                            results.getInt("episodeID")
                            );
                    tmp.DBID = results.getInt("fileID");
                    fileList.add(tmp);
                }
            }
            pstmt.close();
        } catch (SQLException e) {
            Logger.Inst().log("DBHandler","selectTorrentFiles","Exception in prepared statement creation: " + e);
        }
        return fileList;
    }
    public TorrentMeta selectTorrentMeta(int showID){
        TorrentMeta tmp = new TorrentMeta();
        try{
            String sql = "SELECT * FROM Torrent WHERE showID = ?";
            PreparedStatement pstmt = dbConn.prepareStatement(sql);
            pstmt.setInt(1,showID);
            ResultSet results = selectAndGetResults(pstmt, "selectTorrent<Meta>");
            if (results != null){
                while (results.next()) {
                    tmp.DBID = results.getInt("torrentID");
                    tmp.showID = results.getInt("showID");
                    tmp.releaseName = results.getString("releaseName");
                    tmp.releaseGroup = results.getString("releaseGroup");
                    tmp.resolution = results.getString("resolution");
                    tmp.sourceType = results.getString("sourceType");
                    tmp.torrentURL = results.getString("torrentURL");
                    tmp.pageURL = results.getString("pageURL");
                    tmp.clientID = results.getString("clientID");
                    tmp.status = results.getString("status");
                    tmp.percentDone = results.getDouble("percentDone");
                    tmp.torrentDirectory = results.getString("path");
                    tmp.DBID = results.getInt("torrentID");
                }
                tmp.fileList = selectTorrentFiles(tmp.DBID);
            }
            pstmt.close();
        } catch (SQLException e) {
            Logger.Inst().log("DBHandler","selectTorrentMeta","Exception in prepared statement creation: " + e);
        }
        return tmp;
    }

    public ArrayList<Show> selectShows(){
        ArrayList<Show> shows = new ArrayList<>();
        try{
            String sql = "SELECT * FROM Show";
            PreparedStatement pstmt = dbConn.prepareStatement(sql);
            ResultSet results = selectAndGetResults(pstmt, "selectShows");
            if (results != null){
                while (results.next()) {
                    Show tmp = new Show();
                    tmp.DBID = results.getInt("showID");
                    tmp.sourceID = results.getString("sourceID");
                    tmp.synopsis = results.getString("synopsis");
                    tmp.title_en = results.getString("title_en");
                    tmp.title_jp = results.getString("title_jp");
                    tmp.title_romaji = results.getString("title_romaji");
                    tmp.posterURL = results.getString("posterURL");
                    tmp.status = results.getString("airingStatus");
                    tmp.episodes = selectEpisodes(tmp.DBID);
                    tmp.torrentMeta = selectTorrentMeta(tmp.DBID);
                    shows.add(tmp);
                }
            }
            pstmt.close();
        } catch (SQLException e) {
            Logger.Inst().log("DBHandler","selectShows","Exception in prepared statement creation: " + e);
        }
        return shows;
    }
    public ArrayList<Episode> selectEpisodes(int showID){
        ArrayList<Episode> episodeList = new ArrayList<>();
        /*CREATE TABLE IF NOT EXISTS Episode" +
                    "(episodeID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "sourceID TEXT ," +
                    "synopsis TEXT ," +
                    "title_en TEXT ," +
                    "title_jp TEXT ," +
                    "title_romaji TEXT," +
                    "episodeNum INTEGER," +
                    "seriesNum INTEGER," +
                    "showID INTEGER);";*/
        try{
            String sql = "SELECT * FROM Episode WHERE showID = ? ORDER BY episodeNum ASC;";
            PreparedStatement pstmt = dbConn.prepareStatement(sql);
            pstmt.setInt(1,showID);
            ResultSet results = selectAndGetResults(pstmt, "selectTorrentFiles");
            if (results != null){
                while (results.next()) {
                    Episode tmp = new Episode();
                    tmp.DBID = results.getInt("episodeID");
                    tmp.sourceID = results.getString("sourceID");
                    tmp.synopsis = results.getString("synopsis");
                    tmp.title_en = results.getString("title_en");
                    tmp.title_jp = results.getString("title_jp");
                    tmp.title_romaji = results.getString("title_romaji");
                    tmp.number = results.getInt("episodeNum");
                    tmp.seasonNumber = results.getInt("seriesNum");
                    episodeList.add(tmp);
                }
            }
            pstmt.close();
        } catch (SQLException e) {
            Logger.Inst().log("DBHandler","selectEpisodes","Exception in prepared statement creation: " + e);
        }
        return episodeList;
    }

    public ResultSet selectAndGetResults(PreparedStatement pstmt, String caller){
        if(dbConn!=null){
            try {
                ResultSet results = pstmt.executeQuery();
                return results;
            } catch (Exception e) {
                Logger.Inst().log("DBHandler","selectAndGetResults","Exception in db query from " + caller +" : " + e);
            }
        }
        return null;
    }

    public boolean insertTorrent(Show show, TorrentMeta torrentMeta){
            if (!torrentExists(show.DBID)){
                String sql = "INSERT INTO Torrent ('clientType'," +
                        "'clientID'," +
                        "'showID'," +
                        "'releaseName'," +
                        "'releaseGroup'," +
                        "'resolution'," +
                        "'sourceType'," +
                        "'torrentURL'," +
                        "'pageURL'," +
                        "'status'," +
                        "'percentDone'," +
                        "'path') " +
                        "VALUES ('Transmission',?,?,?,?,?,?,?,?,?,?,?);";

                try{
                    PreparedStatement pstmt = dbConn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
                    pstmt.setString(1,torrentMeta.clientID);
                    pstmt.setInt(2,show.DBID);
                    pstmt.setString(3,torrentMeta.releaseName);
                    pstmt.setString(4,torrentMeta.releaseGroup);
                    pstmt.setString(5,torrentMeta.resolution);
                    pstmt.setString(6,torrentMeta.sourceType);
                    pstmt.setString(7,torrentMeta.torrentURL);
                    pstmt.setString(8,torrentMeta.pageURL);
                    pstmt.setString(9,torrentMeta.status);
                    pstmt.setDouble(10,torrentMeta.percentDone);
                    pstmt.setString(11,torrentMeta.torrentDirectory);
                    torrentMeta.DBID = insertAndGetID(pstmt,"insertTorrent");
                    if (torrentMeta.DBID != -1){
                        torrentMeta.fileList = matchEpisodes(torrentMeta,show);
                        // add code to remove torrent if fileList is empty
                        if (torrentMeta.fileList.size() < show.episodes.size()){
                            remove("Torrent","torrentID",torrentMeta.DBID);
                            return false;
                        } else {
                            for (int i = 0; i < torrentMeta.fileList.size(); i++){
                                insertTorrentFile(torrentMeta.fileList.get(i));
                            }
                            return true;
                        }

                    }
                } catch (SQLException e) {
                    Logger.Inst().log("DBHandler","insertTorrent","Exception in prepared statement creation: " + e);
                }
            } else {
                torrentMeta.DBID = getTorrentID(show.DBID);
                torrentMeta.fileList = selectTorrentFiles(torrentMeta.DBID);
                return true;
            }
            return false;
    }

    public void remove(String table,String idName,int DBID){
        String sql = "DELETE FROM "+table+" WHERE "+idName+" = ?;";
        if(dbConn!=null){
            try {
                PreparedStatement pstmt = dbConn.prepareStatement(sql);
                pstmt.setInt(1,DBID);
                pstmt.executeUpdate();
            } catch (Exception e) {
                Logger.Inst().log("DBHandler","remove","Exception removing data " + e);
            }
        }
    }
    public void insertTorrentFile(TorrentFile file){
        try{
            String sql = "INSERT INTO TorrentFile ('fileName'," +
                    "'torrentID'," +
                    "'episodeID')" +
                    "VALUES (?,?,?);";
            PreparedStatement pstmt = dbConn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1,file.fileName);
            pstmt.setInt(2,file.torrentDBID);
            pstmt.setInt(3,file.episodeDBID);
            file.DBID = insertAndGetID(pstmt,"insertTorrent - TorrentFile");
        } catch (SQLException e) {
            Logger.Inst().log("DBHandler","insertTorrentFile","Exception in prepared statement creation: " + e);
            Logger.Inst().log("DBHandler","insertTorrentFile","Object causing exception: " + file.toJSON());
        }

    }
    private ArrayList<TorrentFile> matchEpisodes(TorrentMeta torrentMeta, Show show){
        ArrayList<TorrentFile> newFileList = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < torrentMeta.fileList.size(); i++){
            for (int x = 0 + count; x < show.episodes.size(); x++ ){
                boolean matched = MatcherUtils.fileEpisodeMatch(torrentMeta.fileList.get(i).fileName,show.episodes.get(x).number);
                if (matched){
                    TorrentFile tmp = new TorrentFile(torrentMeta.fileList.get(i).fileName,torrentMeta.DBID,show.episodes.get(x).DBID);
                    newFileList.add(tmp);
                    count++;
                    break;
                }
            }
        }
        return newFileList;
    }

    public void insertEpisode(Episode episode, int showID){

        String sql = "INSERT INTO Episode ('sourceID','synopsis','title_en','title_jp','title_romaji','episodeNum','seriesNum','showID') " +
                "VALUES (?,?,?,?,?,?,?,?);";
        try {
            PreparedStatement pstmt = dbConn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1,episode.sourceID);
            pstmt.setString(2,episode.synopsis);
            pstmt.setString(3,episode.title_en);
            pstmt.setString(4,episode.title_jp);
            pstmt.setString(5,episode.title_romaji);
            pstmt.setInt(6,episode.number);
            pstmt.setInt(7,episode.seasonNumber);
            pstmt.setInt(8, showID);
            episode.DBID = insertAndGetID(pstmt,"insertEpisode");
        } catch (SQLException e) {
            Logger.Inst().log("DBHandler","insertEpisode","Exception in prepared statement creation: " + e);
        }

    }


    public int getShowID(String sourceID){
        int id = -1;
        if(dbConn!=null){
            String sql = "SELECT * FROM Show WHERE sourceID=" + sourceID + ";";
            id = getID(sql,"getShowID","showID");
        }
        return id;
    }
    public int getEpisodeID(String sourceID){
        int id = -1;
        if(dbConn!=null){
            String sql = "SELECT * FROM Episode WHERE sourceID=" + sourceID + ";";
            id = getID(sql,"getEpisodeID","episodeID");
        }
        return id;
    }
    public int getTorrentID(int showID){
        int id = -1;
        if(dbConn!=null){
            String sql = "SELECT * FROM Torrent WHERE clientID=" + showID + ";";
            id = getID(sql,"getTorrentID","showID");
        }
        return id;
    }
    public int getID(String sql, String caller, String idName){
        int id = -1;
        if(dbConn!=null){
            try {
                Statement query = dbConn.createStatement();
                ResultSet results = query.executeQuery(sql);
                while (results.next()){
                    id = results.getInt(idName);
                }
                query.close();
            } catch (Exception e) {
                Logger.Inst().log("DBHandler","getID","Exception in db query from " + caller +" : " + e);
            }
        }
        return id;
    }
    public boolean torrentExists(int showID){return getTorrentID(showID) != -1;}
    public boolean showExists(String sourceID){
        return getShowID(sourceID) != -1;
    }
}
