
import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.*;

import models.Book;
import models.User;
import models.jwtDecoder;

public class ApiService {

    private static User currentUser; // store current logged-in user

    // ---------------- Login ----------------
    public static User login(String username, String password) throws IOException {
        URL url = new URL("http://127.0.0.1:5000/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();

        if (responseCode != 200) throw new IOException("Login failed: " + sb.toString());

        JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
        String token = json.get("token").getAsString();

        // Decode JWT
        String userID = jwtDecoder.getUserID(token);
        String userType = jwtDecoder.getUserType(token);

        currentUser = new User(Integer.parseInt(userID), userType, token);
        return currentUser;
    }

    // ---------------- Logout ----------------
    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void register(JsonObject payload) throws IOException {
        URL url = new URL("http://127.0.0.1:5000/register");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 201) { // Flask returns 201 on success
            InputStream err = conn.getErrorStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(err));
            StringBuilder sbErr = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sbErr.append(line);
            in.close();
            throw new IOException("Registration failed: " + sbErr.toString());
        }
    }

    // ---------------- Customer: Search Books ----------------
public static java.util.List<Book> searchBooks(String token, String query) throws IOException {
    if (token == null) throw new IOException("Not logged in");

        String urlStr = "http://127.0.0.1:5000/books/search?q=" + URLEncoder.encode(query, "UTF-8");
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder sbErr = new StringBuilder();
            String line;
            while ((line = err.readLine()) != null) sbErr.append(line);
            throw new IOException("Search failed: " + sbErr.toString());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();

        com.google.gson.Gson gson = new com.google.gson.Gson();
        Book[] books = gson.fromJson(sb.toString(), Book[].class);
        return java.util.Arrays.asList(books);
    }
    // For customer screen convenience
    public static List<Book> searchBooks(String query) throws IOException {
        if (currentUser == null) throw new IOException("Not logged in");
        return searchBooks(currentUser.getToken(), query);
    }

    // Place order for customer
    public static JsonObject placeOrder(java.util.List<BookOrderItem> items) throws IOException {
        if (currentUser == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/order");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        Gson gson = new Gson();
        // wrap items in an object with key "items" so Flask can call request.json.get("items", [])
        JsonObject root = new JsonObject();
        root.add("items", gson.toJsonTree(items));
        String payload = root.toString();

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();

        if (responseCode != 200) throw new IOException("Order failed: " + sb.toString());

        return JsonParser.parseString(sb.toString()).getAsJsonObject();
    }

    // Helper class to send to server
    public static class BookOrderItem {
        public int bookID;
        public String action; // "buy" or "rent"

        public BookOrderItem(int bookID, String action) {
            this.bookID = bookID;
            this.action = action;
        }
    }

    // ---------------- Manager: Get All Orders ----------------
    public static JsonArray getAllOrders() throws IOException {
        if (currentUser == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/orders/all");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();

        if (responseCode != 200) throw new IOException("Fetch orders failed: " + sb.toString());
        return JsonParser.parseString(sb.toString()).getAsJsonArray();
    }

    // ---------------- Manager: Get All Books (Inventory) ----------------
    public static java.util.List<Book> getAllBooks() throws IOException {
        if (currentUser == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/books/all");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();

        if (responseCode != 200) throw new IOException("Fetch books failed: " + sb.toString());

        Gson gson = new Gson();
        Book[] books = gson.fromJson(sb.toString(), Book[].class);
        return java.util.Arrays.asList(books);
    }

        // ---------------- Manager: Update Book Availability ----------------
    public static void updateBookAvailability(int bookID, boolean isAvailable) throws IOException {
        if (currentUser == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/books/availability");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JsonObject payload = new JsonObject();
        payload.addProperty("bookID", bookID);
        // server expects numeric 0/1 in this workspace
        payload.addProperty("isAvailable", isAvailable ? 1 : 0);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            InputStream err = conn.getErrorStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(err));
            StringBuilder sbErr = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sbErr.append(line);
            in.close();
            throw new IOException("Update availability failed: " + sbErr.toString());
        }
    }

    // ---------------- Manager: Update Payment Status ----------------
    public static void updatePaymentStatus(int orderID, String status) throws IOException {
        if (currentUser == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/orders/status");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JsonObject payload = new JsonObject();
        payload.addProperty("orderID", orderID);
        payload.addProperty("paymentStatus", status);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            InputStream err = conn.getErrorStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(err));
            StringBuilder sbErr = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sbErr.append(line);
            in.close();
            throw new IOException("Update failed: " + sbErr.toString());
        }
    }

    // ---------------- Manager: Add Book ----------------
    public static void addBook(Book book) throws IOException {
        if (currentUser == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/books/add");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        Gson gson = new Gson();
        String payload = gson.toJson(book);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            InputStream err = conn.getErrorStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(err));
            StringBuilder sbErr = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sbErr.append(line);
            in.close();
            throw new IOException("Add book failed: " + sbErr.toString());
        }
    }
}
