package models;

public class User {
    private int userID;
    private String userType;
    private String token;

    public User(int userID, String userType, String token) {
        this.userID = userID;
        this.userType = userType;
        this.token = token;
    }

    public int getUserID() { return userID; }
    public String getUserType() { return userType; }
    public String getToken() { return token; }
}
