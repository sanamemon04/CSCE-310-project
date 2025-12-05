
import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.*;

import java.nio.charset.StandardCharsets;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

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


// new: search with optional genre and year
    public static List<Book> searchBooks(String query, String genre, Integer publicationYear) throws IOException {
        StringBuilder qs = new StringBuilder();
        if (query != null && !query.isEmpty()) qs.append("q=").append(URLEncoder.encode(query, StandardCharsets.UTF_8.toString()));
        if (genre != null && !genre.isEmpty()) {
            if (qs.length() > 0) qs.append("&");
            qs.append("genre=").append(URLEncoder.encode(genre, StandardCharsets.UTF_8.toString()));
        }
        if (publicationYear != null) {
            if (qs.length() > 0) qs.append("&");
            qs.append("publicationYear=").append(publicationYear);
        }

        String urlStr = "http://127.0.0.1:5000/books/search";
        if (qs.length() > 0) urlStr += "?" + qs.toString();

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (currentUser != null && currentUser.getToken() != null) {
            conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
        }

        int code = conn.getResponseCode();
        InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) sb.append(line);
            if (code != 200) throw new IOException("Search failed: " + sb.toString());

            Gson gson = new Gson();
            Type listType = new TypeToken<List<Book>>() {}.getType();
            return gson.fromJson(sb.toString(), listType);
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
        if (currentUser == null || currentUser.getToken() == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/orders/all");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) sb.append(line);
            if (responseCode != 200) throw new IOException("Fetch all books failed: " + sb.toString());
            return JsonParser.parseString(sb.toString()).getAsJsonArray();
        }
    }

    // ---------------- Manager: Get All Books (Inventory) ----------------
     public static com.google.gson.JsonArray getAllBooks() throws IOException {

        if (currentUser == null || currentUser.getToken() == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/books/all");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) sb.append(line);
            if (responseCode != 200) throw new IOException("Fetch all books failed: " + sb.toString());
            return JsonParser.parseString(sb.toString()).getAsJsonArray();
        }
    }

    public static List<Book> getAllBooksAsList() throws IOException {
        com.google.gson.JsonArray arr = getAllBooks();
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Book>>() {}.getType();
        return gson.fromJson(arr, listType);
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
    // ...existing code...
    public static void addBook(Book book) throws IOException {
        URL url = new URL("http://127.0.0.1:5000/books/add");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (currentUser != null && currentUser.getToken() != null) {
            conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
        }
        conn.setDoOutput(true);

        JsonObject body = new JsonObject();
        body.addProperty("title", book.getTitle());
        body.addProperty("author", book.getAuthor());
        body.addProperty("genre", book.getGenre());
        if (book.getPublicationYear() != null) body.addProperty("publicationYear", book.getPublicationYear());
        body.addProperty("buyPrice", book.getBuyPrice());
        body.addProperty("rentPrice", book.getRentPrice());
        body.addProperty("isAvailable", book.isAvailable() ? 1 : 0);
        body.addProperty("copies", book.getCopies());
        body.addProperty("location", book.getLocation());

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes("utf-8"));
            os.flush();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) sb.append(line);
            if (code < 200 || code >= 300) throw new IOException("Add book failed: " + sb.toString());
        }
    }

        // NEW: update genre/publicationYear/location/copies
        public static void updateBookMetadata(int bookID, String genre, Integer publicationYear, String location, Integer copies) throws IOException {
        if (currentUser == null || currentUser.getToken() == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/books/metadata");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        payload.addProperty("bookID", bookID);
        if (genre != null) payload.addProperty("genre", genre);
        if (publicationYear != null) payload.addProperty("publicationYear", publicationYear);
        if (location != null) payload.addProperty("location", location);
        if (copies != null) payload.addProperty("copies", copies);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(data);
            os.flush();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) sb.append(line);
            if (code < 200 || code >= 300) throw new IOException("Update metadata failed: " + sb.toString());
        }
    }

        
    

    // ---------------- Customer: Get My Orders ----------------
    public static JsonArray getMyOrders() throws IOException {
        if (currentUser == null || currentUser.getToken() == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/orders/my");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            if (responseCode != 200) throw new IOException("Fetch my orders failed: " + sb.toString());
            return JsonParser.parseString(sb.toString()).getAsJsonArray();
        }
    
    }


    // ---------------- Customer: Return Rental ----------------
    public static void returnRental(int itemID) throws IOException {
        if (currentUser == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/orders/return");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JsonObject payload = new JsonObject();
        payload.addProperty("itemID", itemID);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes("utf-8"));
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) sb.append(line);
            if (code < 200 || code >= 300) throw new IOException("Return failed: " + sb.toString());
        }
    }


     // ---------------- Manager: Set Book Copies ----------------
    public static void updateBookCopies(int bookID, int copies) throws IOException {
        if (currentUser == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/books/copies");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JsonObject payload = new JsonObject();
        payload.addProperty("bookID", bookID);
        payload.addProperty("copies", copies);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes("utf-8"));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) sb.append(line);
            throw new IOException("Update copies failed: " + sb.toString());
        }
    }


    // GET reviews for a specific book
    public static JsonArray getBookReviews(int bookID) throws IOException {
        URL url = new URL("http://127.0.0.1:5000/books/" + bookID + "/reviews");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) sb.append(line);
            if (code != 200) throw new IOException("Fetch reviews failed: " + sb.toString());
            return JsonParser.parseString(sb.toString()).getAsJsonArray();
        }
    }

    // POST a review (requires authentication token available as currentUser.getToken())
    public static JsonObject postBookReview(int bookID, int rating, String text) throws IOException {
        URL url = new URL("http://127.0.0.1:5000/books/" + bookID + "/reviews");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        if (currentUser != null && currentUser.getToken() != null) {
            conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
        }
        JsonObject body = new JsonObject();
        body.addProperty("rating", rating);
        body.addProperty("text", text == null ? "" : text);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes("utf-8"));
        }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) sb.append(line);
            if (code < 200 || code >= 300) throw new IOException("Post review failed: " + sb.toString());
            return JsonParser.parseString(sb.toString()).getAsJsonObject();
        }
    }

    public static void updateBookLocation(int bookID, String location) throws IOException {
        if (currentUser == null) throw new IOException("Not logged in");

        URL url = new URL("http://127.0.0.1:5000/books/location");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + currentUser.getToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JsonObject payload = new JsonObject();
        payload.addProperty("bookID", bookID);
        payload.addProperty("location", location == null ? "" : location);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes("utf-8"));
            os.flush();
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) sb.append(line);
            throw new IOException("Update location failed: " + sb.toString());
        }
    }

}
