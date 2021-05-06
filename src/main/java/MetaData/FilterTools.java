package MetaData;

public class FilterTools {
    public static String buildNameRegex(String title){
        String nameRegex = "";
        String sanitizedTitle = title.replaceAll("[^a-zA-Z0-9\\s]"," ");
        sanitizedTitle = sanitizedTitle.replaceAll(" +"," ");
        String[] splitTitle = sanitizedTitle.split(" ");
        for (int i = 0;i < splitTitle.length; i++){
            nameRegex += splitTitle[i].toLowerCase();
            if (i != splitTitle.length - 1){
                nameRegex += "|";
            }
        }
        return nameRegex;
    }
}
