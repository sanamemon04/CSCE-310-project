package models;
import java.util.Base64;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class jwtDecoder {
    public static String getUserID(String token) {
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
        return obj.get("sub").getAsString(); // identity stored as userID
    }

    public static String getUserType(String token) {
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
        return obj.get("userType").getAsString(); // stored in additional_claims
    }
    
}
