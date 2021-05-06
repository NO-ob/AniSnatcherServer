package Utils;

import MetaData.Show;
import com.google.gson.JsonElement;

import java.util.ArrayList;

public class JSONUtils {
    // https://stackoverflow.com/questions/9324760/gson-jsonobject-unsupported-operation-exception-null-getasstring
    public static String getJSONString(JsonElement jsonElement) {
        if (jsonElement != null){
            return jsonElement.isJsonNull() ? "" : jsonElement.getAsString();
        } else {
            return "";
        }
    }
    public static int getJSONInt(JsonElement jsonElement) {
        if (jsonElement != null){
            return jsonElement.isJsonNull() ? -1 : jsonElement.getAsInt();
        } else {
            return -1;
        }
    }
    public static double getJSONDouble(JsonElement jsonElement) {
        if (jsonElement != null){
            return jsonElement.isJsonNull() ? -1 : jsonElement.getAsDouble();
        } else {
            return -1;
        }
    }
    public static String showListToJSON(ArrayList<Show> shows){
        String json = "{ \"shows\" : [";
        for (int i = 0; i < shows.size(); i++){
            json += shows.get(i).toJSON();
            if (i != shows.size() -1){
                json+= ",";
            }
        }
        json +="]}";
        return json;
    }
}
