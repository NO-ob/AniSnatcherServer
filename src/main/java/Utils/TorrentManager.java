package Utils;

import APITools.KitsuHandler;
import APITools.NyaaSnatcher;
import APITools.TransmissionHandler;
import MetaData.Show;
import MetaData.TorrentMeta;

import java.util.ArrayList;

public class TorrentManager {
    DBHandler dbHandler = new DBHandler();
    boolean currentlyActive = false;
    public ArrayList<Show> showList = new ArrayList<>();
    public void initialize(){
        dbHandler.dbConnect();
        getShowsFromDB();
        runUpdateThread();
    }
    public void snatchShow(Show show){
        Thread thread = new Thread(() -> {
            if (currentlyActive){
                try {
                    int ms = 1000;
                    Logger.Inst().log("TorrentManager","snatchShow","Manager Active sleeping for: " + ms);
                    Thread.sleep(ms);
                    snatchShow(show);
                } catch (InterruptedException e) {
                    Logger.Inst().log("TorrentManager","snatch","Exception in sleeping " + e);
                }
            } else {
                currentlyActive = true;
                boolean showExists = false;
                NyaaSnatcher nyaaSnatcher = new NyaaSnatcher();
                ArrayList<TorrentMeta> torrents = nyaaSnatcher.search(show);
                for (int i = 0; i < showList.size(); i++){
                    if (showList.get(i).DBID == show.DBID) {
                        if (showList.get(i).torrentMeta == null){
                            showList.remove(i);
                        } else {
                            showExists = true;
                            Logger.Inst().log("TorrentManager","snatchShow","Show and torrent already exist doing nothing");
                        }
                    }
                }
                boolean snatchSuccess = false;
                if (!torrents.isEmpty() && !showExists){
                    dbHandler.insertShow(show);
                    for(int i = 0; i < torrents.size(); i++){
                        snatchSuccess = trySnatch(torrents.get(i),show,dbHandler);
                        if (snatchSuccess){
                            showList.add(show);
                            break;
                        } else {
                            Logger.Inst().log("TorrentManager","snatchShow","Trying next torrent: ");
                        }
                    }
                }
                if (!snatchSuccess){
                    dbHandler.remove("Show","showID",show.DBID);
                    dbHandler.remove("Episode","showID",show.DBID);
                    Logger.Inst().log("TorrentManager","snatchShow","Failed to snatch show");
                }
                currentlyActive = false;
            }
        });
        thread.start();
    }
    static boolean trySnatch(TorrentMeta meta, Show show,DBHandler dbHandler){
        TransmissionHandler transmissionHandler = new TransmissionHandler();
        meta.clientID = transmissionHandler.addTorrent(meta.torrentURL,meta.releaseName);
        if (dbHandler.insertTorrent(show,meta)){
            show.torrentMeta = meta;
            Logger.Inst().log("TorrentManager","trySnatch","Added torrent: " + meta.releaseName);
            return true;
        } else {
            transmissionHandler.removeTorrent(meta.clientID,meta.releaseName);
            Logger.Inst().log("TorrentManager","trySnatch","Deleted torrent because files don't match episodes: " + meta.releaseName);
            return false;
        }
    }

    public void updateShows(){
        if (!currentlyActive){
            currentlyActive = true;
            Logger.Inst().log("TorrentManager","updateShows","Doing Update");
            for (int i = 0; i < showList.size(); i++){
                TransmissionHandler transmissionHandler = new TransmissionHandler();
                if (showList.get(i).torrentMeta != null){
                    transmissionHandler.getTorrentInfo(showList.get(i).torrentMeta);
                    dbHandler.updateTorrentStats(showList.get(i).torrentMeta);
                }
            }
            currentlyActive = false;
        }
    }
    private void runUpdateThread(){
        Thread thread = new Thread(() -> {
            Logger.Inst().log("TorrentManager","runUpdateThread","New updateThread started");
            updateShows();
            try {
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                Logger.Inst().log("TorrentManager","updateShowsThread","Exception in sleeping " + e);
            }
            runUpdateThread();
        });
        thread.start();
    }
    private void getShowsFromDB(){
        showList = dbHandler.selectShows();
    }
}
