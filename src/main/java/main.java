import APITools.KitsuHandler;
import APITools.NyaaSnatcher;
import APITools.TransmissionHandler;
import MetaData.Show;
import MetaData.TorrentMeta;
import Tests.Tests;
import Utils.DBHandler;
import Utils.SettingsHandler;
import Utils.TorrentManager;

import java.sql.SQLException;
import java.util.ArrayList;

public class main {
    public static void main(String[] args) {
        //KitsuHandler kitsuHandler = new KitsuHandler();
        //ArrayList<Show> results = kitsuHandler.search(args[1], 10);

        //Tests.testDBInsert();
        //Tests.testEpisodeMatching();
        //NyaaSnatcher nyaaSnatcher = new NyaaSnatcher();
        //ArrayList<TorrentMeta> torrents = nyaaSnatcher.search(results.get(0));
        //TransmissionHandler uwu = new TransmissionHandler();
        //uwu.addTorrent("https://nyaa.si/download/1365507.torrent","test torrent");
        //Tests.testSelects();
        //TorrentManager torrentManager = new TorrentManager();
        //torrentManager.initialize();
        //Tests.testGetShowTorrentandDBInsert();
        SettingsHandler.Inst();
        APIServer.initServer();
    }
}
