package models;

public class Book {
    private int bookID;
    private String title;
    private String author;
    private String genre; 
    private Integer publicationYear; 
    private double buyPrice;
    private double rentPrice;
    
    // private int availableInt; // 0 or 1 from API
    private boolean isAvailable;
    private int copies;
    private String location;
    
    public Book() {} // for Gson

    public Book(int bookID, String title, String author, double buyPrice, double rentPrice, boolean isAvailable) {
        this.bookID = bookID;
        this.title = title;
        this.author = author;
        this.buyPrice = buyPrice;
        this.rentPrice = rentPrice;
        // this.availableInt = isAvailable ? 1 : 0;
        this.isAvailable = isAvailable;
        this.copies = 1;
        this.location = null;
    }


    // Getters
    public int getBookID() { return bookID; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public Integer getPublicationYear() { return publicationYear; }
    public double getBuyPrice() { return buyPrice; }
    public double getRentPrice() { return rentPrice; }
    public int getCopies() { return copies; }
    public String getLocation() { return location; }

    // Convert 0/1 to boolean
    // public boolean isAvailable() { return availableInt == 1; }
    // boolean availability
    public boolean isAvailable() { return isAvailable; }


    // Setters
    public void setBookID(int bookID) { this.bookID = bookID; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }
    public void setRentPrice(double rentPrice) { this.rentPrice = rentPrice; }
    public void setCopies(int copies) { this.copies = copies; }
    public void setGenre(String genre) { this.genre = genre; } // NEW
    public void setPublicationYear(Integer y) { this.publicationYear = y; } // NEW

    // // Gson will set availableInt
    // public void setAvailableInt(int availableInt) { this.availableInt = availableInt; }
    // Gson will populate this boolean field directly; provide setter variant too
    public void setIsAvailable(boolean isAvailable) { this.isAvailable = isAvailable; }
    public void setLocation(String location) { this.location = location; }
    
    @Override
    public String toString() {
        return String.format("%d: %s — %s [%s, %s] (Buy: $%.2f, Rent: $%.2f) Copies:%d Location:%s",
            bookID, title, author,
            genre == null ? "N/A" : genre,
            publicationYear == null ? "N/A" : publicationYear.toString(),
            buyPrice, rentPrice, copies, location == null ? "N/A" : location);
    }
//     @Override
//     public String toString() {
//         return title + " by " + author + " (Buy: $" + buyPrice + ", Rent: $" + rentPrice + ")";
//     }
}

