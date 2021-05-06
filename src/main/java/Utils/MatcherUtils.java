package Utils;
import java.util.Locale;
import java.util.regex.Pattern;

public class MatcherUtils {

    public static boolean fileEpisodeMatch(String filename, int episodeNumber){
        String tmpFileName = filename.toLowerCase(Locale.ROOT).replace("10bit","");
        if(tmpFileName.matches("(?:.*?)(?:\\s+|-+|_+)(ed|op|nced|special)(?:\\s+|-+|_+|[0-9]+)(?:.*?)")){
            return false;
        }
        Pattern matcherPattern = Pattern.compile("(?:.*?)(?:e|x|episode|Ep|_|-|\\s|\\.)(?:\\s*0*)("+episodeNumber+")(?:\\.+|\\s+|-+|_+|v[0-9]+)(?:.*?)(?:.mkv|.mp4|.avi)(?:.*?)", Pattern.CASE_INSENSITIVE);
        boolean match = filename.matches(matcherPattern.pattern());
        if (match){
            //System.out.println(filename + " matches episode " + episodeNumber);
        } else {
            //System.out.println(filename + " doesn't match " + episodeNumber);
        }
        return match;
    }
}
