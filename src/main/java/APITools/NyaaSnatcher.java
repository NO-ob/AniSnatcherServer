package APITools;

import MetaData.FilterTools;
import MetaData.Show;
import MetaData.TorrentFile;
import MetaData.TorrentMeta;
import Utils.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
/**
 * <h1>NyaaSnatcher</h1>
 * The Nyaa Snatcher class is used to scrape torrents from nyaa,si
 * <b>Note:</b> https://nyaa.si
 */
public class NyaaSnatcher {
    String baseURL="https://nyaa.si";
    /**
     * Function used to search Nyaa it takes a Show object as an input and will return a list of TorrentMeta objects
     * @param show A Show object
     * @return ArrayList<TorrentMeta>
     */
    public ArrayList<TorrentMeta> search(Show show){
        Logger.Inst().log("NyaaSnatcher","search","Got query for " + show.title_romaji);
        try{
            String title = "";
            if (show.title_romaji != null){
                title = show.title_romaji;
            } else if (show.title_en != null){
                title = show.title_en;
            } else {
                return null;
            }
            String url = baseURL + "/?f=0&c=1_2&q="+title.replace(" ", "+");
            Logger.Inst().log("NyaaSnatcher","search","Searching at: " + url);
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:53.0) Gecko/20100101 Firefox/53.0")
                    .followRedirects(false).execute();
            if (response.statusCode() == 200){
                Document document = Jsoup.connect(url).get();
                ArrayList<TorrentMeta> results = createTorrentMeta(document, show);
                return results;
            } else {
                Logger.Inst().log("NyaaSnatcher","search","Error status code is: " + response.statusCode());
                Logger.Inst().log("NyaaSnatcher","search","Message is: " + response.statusMessage());
            }
        } catch (IOException e){
            Logger.Inst().log("NyaaSnatcher","search","Exception in Search Function" + e);
        }
        return new ArrayList<TorrentMeta>();
    }
    /**
     * Creates and returns a list of TorrentMeta objects,
     * it filters the results from nyaa and throws out any torrents which are not appropriate
     * @param document A JSOUP document of a html page containing torrent data
     * @param show A show object to get torrents for
     * @return ArrayList<TorrentMeta>
     */
    private ArrayList<TorrentMeta> createTorrentMeta(Document document, Show show){
        ArrayList metaResults = new ArrayList();
        Elements torrentRows = document.select("table tbody tr");
        torrentRows.forEach(element -> {
            Element torrentInfo = element.select("td[colspan=2] a").last();
            String dirtyName = torrentInfo.text().toLowerCase(Locale.ROOT);
            Element seedElem = element.select("td[class=text-center]").get(3);
            if (seedElem.text().equals("0")){
                Logger.Inst().log("NyaaSnatcher","createTorrentMeta","Torrent Discarded: " + torrentInfo.text());
                Logger.Inst().log("NyaaSnatcher","createTorrentMeta","Zero seeders");
            } else {
                if (!dirtyName.contains("dub")){
                    String releaseGroup = "", resolution = "",sourceMediaType = "";
                    Pattern bracketsPattern = Pattern.compile("^(\\(|\\[|\\【)(.*?)(\\】|\\]|\\))", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = bracketsPattern.matcher(dirtyName);
                    boolean rgFound = false;
                    if (matcher.find()){
                        releaseGroup = matcher.group().substring(1,matcher.group().length() - 1);
                        dirtyName = dirtyName.replace(matcher.group(),"");
                        rgFound = true;
                    }
                    bracketsPattern = Pattern.compile("(\\(|\\[|\\【)(.*?)(\\】|\\]|\\))", Pattern.CASE_INSENSITIVE);
                    matcher = bracketsPattern.matcher(dirtyName);
                    while (matcher.find()){
                        Pattern resolutionPattern = Pattern.compile("480|720|1080|4k|2160", Pattern.CASE_INSENSITIVE);
                        Pattern sourceMediaPattern = Pattern.compile("BD|ISO|DVD|Bluray|WEB", Pattern.CASE_INSENSITIVE);
                        Matcher innerMatcher = resolutionPattern.matcher(matcher.group());
                        if (innerMatcher.find()){
                            resolution = innerMatcher.group();
                        }
                        innerMatcher = sourceMediaPattern.matcher(matcher.group());
                        if (innerMatcher.find()){
                            if (innerMatcher.group().equalsIgnoreCase("bluray")){
                                sourceMediaType = "BD";
                            } else {
                                sourceMediaType = innerMatcher.group().toUpperCase();
                            }
                        }
                        dirtyName = dirtyName.replace(matcher.group(),"");
                    }
                    String enNameRegex = FilterTools.buildNameRegex(show.title_en);
                    String jpNameRegex = FilterTools.buildNameRegex(show.title_jp);
                    String romajiNameRegex = FilterTools.buildNameRegex(show.title_romaji);
                    dirtyName = dirtyName.replaceAll("[^a-zA-Z0-9\\s]"," ");
                    dirtyName = dirtyName.replaceAll(" +"," ");
                    dirtyName = dirtyName.replaceAll(enNameRegex,"");
                    dirtyName = dirtyName.replaceAll(jpNameRegex,"");
                    dirtyName = dirtyName.replaceAll(romajiNameRegex,"");
                    dirtyName = dirtyName.replaceAll("v[0-9]","");
                    dirtyName = dirtyName.replaceAll("[0-9]","");
                    dirtyName = dirtyName.replaceAll("(season|ep|subtitle|sp|sub|specials|special|ova|complete|original|series|extras|batch|tv|[h.*?]264|[h.*?]265|)","");
                    dirtyName = dirtyName.replaceAll("\\W+","");
                    if (dirtyName.length() > 4 && show.title_en.contains(dirtyName)){
                        dirtyName = "";
                    } else if (dirtyName.length() > 4 && show.title_romaji.contains(dirtyName)){
                        dirtyName = "";
                    }
                    if (dirtyName.length() == 0){
                        TorrentMeta tmpMeta = new TorrentMeta();
                        Elements data = element.select("td[class=text-center]");
                        tmpMeta.releaseName = torrentInfo.text();
                        tmpMeta.pageURL = baseURL + torrentInfo.attr("href");
                        tmpMeta.releaseGroup = releaseGroup;
                        tmpMeta.sourceType = sourceMediaType;
                        tmpMeta.resolution = resolution;
                        tmpMeta.releaseGroup = releaseGroup;
                        tmpMeta.seedCount = Integer.parseInt(data.get(3).text());
                        tmpMeta.snatchCount = Integer.parseInt(data.get(5).text());
                        try {
                            TimeUnit.SECONDS.sleep(2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        tmpMeta = getTorrentPageData(tmpMeta);
                         if (tmpMeta.fileList.size() < show.episodes.size()){
                            Logger.Inst().log("NyaaSnatcher","createTorrentMeta","Torrent Discarded: " + torrentInfo.text());
                            Logger.Inst().log("NyaaSnatcher","createTorrentMeta","Torrent file count: " + tmpMeta.fileList.size() + "Should be >= " + show.episodes.size());
                        } else {
                            metaResults.add(tmpMeta);
                            Logger.Inst().log("NyaaSnatcher","createTorrentMeta","Torrent Kept: " + tmpMeta.toString());
                        }
                    } else {
                        Logger.Inst().log("NyaaSnatcher","createTorrentMeta","Torrent Discarded: " + torrentInfo.text());
                        Logger.Inst().log("NyaaSnatcher","createTorrentMeta","Torrent Leftovers: " + dirtyName);
                    }
                }
            }

        });
        Collections.sort(metaResults, Comparator.comparing(TorrentMeta::getSnatchCount).reversed());
        return metaResults;
    }

    /**
     * Scrapes extra information for a torrent from its own page on nyaa,
     * it will add a file list to the tmpMeta variable and then return it
     * @param tmpMeta an instance of TorrentMeta
     * @return TorrentMeta
     */
    private TorrentMeta getTorrentPageData(TorrentMeta tmpMeta){
        try{
            Connection.Response response = Jsoup.connect(tmpMeta.pageURL).followRedirects(false).execute();
            if (response.statusCode() == 200){
                Document document = Jsoup.connect(tmpMeta.pageURL).get();
                Elements fileList = document.select("i[class=fa fa-file]");
                if (!document.select("a[class=folder]").isEmpty()){
                    tmpMeta.torrentDirectory = document.select("a[class=folder]").first().textNodes().get(0).getWholeText();
                } else {
                    tmpMeta.torrentDirectory = "";
                }
                tmpMeta.torrentURL = baseURL + document.select("div[class=panel-footer clearfix] > a").first().attr("href");
                fileList.forEach(element -> {
                    element.parent().select("span").remove();
                    String filename = element.parent().textNodes().get(0).getWholeText();
                    tmpMeta.fileList.add(new TorrentFile(filename));
                });
            } else {
                Logger.Inst().log("NyaaSnatcher","getTorrentPageData","Error status code is: " + response.statusCode());
                Logger.Inst().log("NyaaSnatcher","getTorrentPageData","Message is: " + response.statusMessage());
            }
        } catch (IOException e){
            Logger.Inst().log("NyaaSnatcher","getTorrentPageData","Exception " + e);
            Logger.Inst().log("NyaaSnatcher","getTorrentPageData","Nyaa may be offline or you have no internet connection" + e);
        }
        return tmpMeta;
    }
}
