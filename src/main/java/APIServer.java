import APITools.KitsuHandler;
import APITools.NyaaSnatcher;
import APITools.TransmissionHandler;
import MetaData.Show;
import MetaData.TorrentMeta;
import Utils.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class APIServer {
    private static int port = Integer.parseInt(SettingsHandler.Inst().serverPort);
    private static TorrentManager torrentManager = new TorrentManager();
    /**
     * Starts the http server and makes it listen at some url endpoints
     */
    public static void initServer(){
        HttpServer server = null;
        torrentManager.initialize();
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            HttpContext context = server.createContext("/api/search");
            HttpContext snatchContext = server.createContext("/api/snatch");
            HttpContext libraryContext = server.createContext("/api/library");
            context.setHandler(APIServer::handleRequest);
            snatchContext.setHandler(APIServer::handleSnatch);
            libraryContext.setHandler(APIServer::handleLibrary);
            server.start();
            InetAddress localIP = InetAddress.getLocalHost();
            Logger.Inst().log("APIServer","initServer","Server Listening on: " + localIP + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.Inst().log("APIServer","initServer","Server Exception: " +e);
        }
    }
    /**
     * Will get show metadata using the kitsuHandler and return it to the client
     * @param exchange this will be passed from the Httpcontext when a request is made on the /api/ endpoint
     */
    private static void handleLibrary(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        //printRequestInfo(exchange);
        String response = "";
        boolean sentResponse = false;
        if (exchange.getRequestURI().getQuery() != null){
            Logger.Inst().log("APIServer","handleRequest","Got query for: " + exchange.getAttribute("query"));
            switch(exchange.getRequestURI().getQuery().split("=")[0]){
                case("update"):
                    if(exchange.getRequestURI().getQuery().split("=")[1].equals("true")){
                       response = updateAndGetLibrary();
                    } else {
                        response = getLibrary();
                    }
                    break;
            }
            sentResponse = sendCompleteReq(response,exchange);
        }
        if (!sentResponse){
            sendError(exchange);
        }
    }
    private static void sendError(HttpExchange exchange){
        try {
            String response = "Invalid Request";
            exchange.sendResponseHeaders(400, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            Logger.Inst().log("APIServer","handleRequest","Returned 400 to client");
            os.close();
        } catch (IOException e) {
            Logger.Inst().log("APIServer","sendCompleteReq","Exception in sending response: "+e);
        }
    }
    private static boolean sendCompleteReq(String response, HttpExchange exchange){
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            Logger.Inst().log("APIServer","sendCompleteReq","Returned results to client");
            os.close();
            return true;
        } catch (IOException e) {
            Logger.Inst().log("APIServer","sendCompleteReq","Exception in sending response: "+e);
        }
        return false;
    }
    /**
     * Will get show metadata using the kitsuHandler and return it to the client
     * @param exchange this will be passed from the Httpcontext when a request is made on the /api/ endpoint
     */
    private static void handleRequest(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        //printRequestInfo(exchange);
        String response = "";
        boolean sentResponse = false;
        if (exchange.getRequestURI().getQuery() != null){
           Logger.Inst().log("APIServer","handleRequest","Got query for: " + exchange.getAttribute("query"));
           switch(exchange.getRequestURI().getQuery().split("=")[0]){
               case("query"):
                   response = showQuery((exchange.getRequestURI().getQuery().split("=")[1]));
                   break;
           }
           sentResponse = sendCompleteReq(response,exchange);
        }
        if (!sentResponse){
            sendError(exchange);
        }
    }
    /**
     * Will recieve a show ibject as json in a post request and then store the show in the database
     * and add a torrent for the show to transmission
     * @param exchange this will be passed from the Httpcontext when a request is made on the /api/ endpoint
     */
    private static void handleSnatch(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        //printRequestInfo(exchange);
        String response = "";
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        String line = "";
        while((line = bufferedReader.readLine()) != null){
            response += line;
        }
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
        if(response.length() > 0){
            JsonObject jsonObj = JsonParser.parseString(response).getAsJsonObject();
            Show show = new Show(jsonObj);
            torrentManager.snatchShow(show);
        }
    }
    /**
     * Performs a search on the kitsu api and converts the results into json to be sent to the client
     * @param query A string to be searched on the kitsu api
     */
    private static String showQuery(String query){
        ArrayList<Show> results = searchShow(query);
        return JSONUtils.showListToJSON(results);
    }
    /**
     * Converts the showlist of the torrentManager to json
     */
    private static String getLibrary(){
        return JSONUtils.showListToJSON(torrentManager.showList);
    }
    /**
     * Converts the showlist of the torrentManager to json
     */
    private static String updateAndGetLibrary(){
        torrentManager.updateShows();
        return JSONUtils.showListToJSON(torrentManager.showList);
    }
    public static ArrayList<Show> searchShow(String query){
        KitsuHandler kitsuHandler = new KitsuHandler();
        ArrayList<Show> results = kitsuHandler.search(query, 10);
        Logger.Inst().log("APIServer","searchShow","Got query for: " + query + ", returned " + results.size() + " results");
        return results;
    }
}
