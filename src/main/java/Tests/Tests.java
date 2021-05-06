package Tests;

import APITools.KitsuHandler;
import APITools.NyaaSnatcher;
import APITools.TransmissionHandler;
import MetaData.Show;
import MetaData.TorrentFile;
import MetaData.TorrentMeta;
import Utils.DBHandler;
import Utils.MatcherUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Tests {
    public static boolean testDBInsert(){
        KitsuHandler kitsuHandler = new KitsuHandler();
        ArrayList<Show> results = kitsuHandler.search("geass", 10);
        boolean result = true;
        DBHandler dbHandler = new DBHandler();
        dbHandler.dbConnect();
        dbHandler.insertShow(results.get(0));
        Show show = results.get(0);
        if (show.DBID == -1){
            result = false;
        } else {
           for (int i = 0; i < show.episodes.size(); i++){
               if (show.episodes.get(i).DBID == -1){
                   result = false;
               }

           }
        }
        try {
            dbHandler.dbConn.close();
        } catch (SQLException e){

        }
        return result;
    }
    public static boolean testEpisodeMatching(){
        KitsuHandler kitsuHandler = new KitsuHandler();
        ArrayList<Show> results = kitsuHandler.search("fate+zero", 10);
        NyaaSnatcher nyaaSnatcher = new NyaaSnatcher();
        ArrayList<TorrentMeta> torrents = nyaaSnatcher.search(results.get(0));
        int matchCount = 0;
        for (int y = 0; y < torrents.size(); y++){
            matchCount = 0;
            for (int i = 0; i < torrents.get(y).fileList.size(); i++){
                for (int x = 0; x < results.get(0).episodes.size(); x++ ){
                    boolean matched = MatcherUtils.fileEpisodeMatch(torrents.get(y).fileList.get(i).fileName,results.get(0).episodes.get(x).number);
                    if (matched){
                        matchCount ++;
                        break;
                    }
                }
            }
            System.out.println(torrents.get(y).releaseName);
            System.out.println("results ep count is " + results.get(0).episodes.size());
            System.out.println("torrent filelist size is " + torrents.get(y).fileList.size());
            System.out.println("matched size is" + matchCount);
        }

        return true;
    }
    public static boolean testGetShowTorrentandDBInsert(){
        KitsuHandler kitsuHandler = new KitsuHandler();
        ArrayList<Show> results = kitsuHandler.search("fate+zero", 10);
        NyaaSnatcher nyaaSnatcher = new NyaaSnatcher();
        ArrayList<TorrentMeta> torrents = nyaaSnatcher.search(results.get(0));
        TransmissionHandler transmissionHandler = new TransmissionHandler();
        DBHandler dbHandler = new DBHandler();
        dbHandler.dbConnect();
        Show show = results.get(0);
        dbHandler.insertShow(show);
        TorrentMeta meta = torrents.get(0);
        meta.clientID = transmissionHandler.addTorrent(meta.torrentURL,meta.releaseName);
        dbHandler.insertTorrent(show,meta);
        System.out.println(show.toJSON());
        System.out.println(meta.toJSON());
        return true;
    }
    public static boolean testSelects(){
        DBHandler dbHandler = new DBHandler();
        dbHandler.dbConnect();
        System.out.println(dbHandler.selectTorrentMeta(1).toJSON());
        System.out.println(dbHandler.selectTorrentMeta(2).toJSON());
        System.out.println(dbHandler.selectTorrentMeta(3).toJSON());
        return true;
    }
}
