

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import models.Book;
import models.User;

public class CustomerScreen extends JFrame {

    private User user;
    private JTextField searchField;
    private JButton searchButton, addToCartButton, viewCartButton, placeOrderButton, logoutButton;
    private JTable bookTable;
    private DefaultTableModel bookTableModel;
    private java.util.List<OrderItem> cart;
    private JButton profileButton;
    private JButton reviewsButton; 
    private JButton viewAllButton;
    

    public CustomerScreen(User user) {
        this.user = user;
        this.cart = new ArrayList<>();

        setTitle("Customer Screen");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 500);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top: search
        JPanel topPanel = new JPanel();
        searchField = new JTextField(30);
        searchButton = new JButton("Search");
        viewAllButton = new JButton("View All Books");
        topPanel.add(searchField);
        topPanel.add(searchButton);
        topPanel.add(viewAllButton);
        panel.add(topPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Title", "Author", "Buy Price", "Rent Price", "Available", "Copies","Action"};
        bookTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                // Allow editing only the Action column (last column)
                return column == (getColumnCount() - 1);
            }
        };
        bookTable = new JTable(bookTableModel);
        bookTable.setRowHeight(28); // slightly taller
        bookTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Dropdown always visible
        JComboBox<String> actionCombo = new JComboBox<>(new String[]{"buy", "rent"});
        actionCombo.setMaximumRowCount(2);
        TableColumn actionCol = bookTable.getColumnModel().getColumn(bookTableModel.findColumn("Action"));
        actionCol.setCellEditor(new DefaultCellEditor(actionCombo));
        actionCol.setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                     boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setText(value == null ? "buy" : value.toString());
                return l;
            }
        });

        panel.add(new JScrollPane(bookTable), BorderLayout.CENTER);

        // Bottom buttons
        // JPanel bottom = new JPanel();
        // addToCartButton = new JButton("Add to Cart");
        // viewCartButton = new JButton("View Cart");
        // placeOrderButton = new JButton("Place Order");
        // logoutButton = new JButton("Logout");
        // profileButton = new JButton("Profile"); 
        // reviewsButton = new JButton("Reviews");

        // bottom.add(profileButton); 
        // bottom.add(addToCartButton);
        // bottom.add(viewCartButton);
        // bottom.add(placeOrderButton);
        // bottom.add(reviewsButton);
        // bottom.add(logoutButton);
        // panel.add(bottom, BorderLayout.SOUTH);''
        JPanel bottom = new JPanel();
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        addToCartButton = new JButton("Add to Cart");
        viewCartButton = new JButton("View Cart");
        placeOrderButton = new JButton("Place Order");
        logoutButton = new JButton("Logout");
        profileButton = new JButton("Profile");
        reviewsButton = new JButton("Reviews");

        leftButtons.add(profileButton);
        leftButtons.add(addToCartButton);
        leftButtons.add(viewCartButton);
        leftButtons.add(placeOrderButton);
        leftButtons.add(reviewsButton);

        rightButtons.add(logoutButton); // logout on the right

        bottom.add(leftButtons);
        bottom.add(rightButtons);
        panel.add(bottom, BorderLayout.SOUTH);

        add(panel);

        // Actions
        searchButton.addActionListener(e -> searchBooks());
        viewAllButton.addActionListener(e -> viewAllBooks());
        addToCartButton.addActionListener(e -> addToCart());
        viewCartButton.addActionListener(e -> viewCart());
        placeOrderButton.addActionListener(e -> placeOrder());
        logoutButton.addActionListener(e -> logout());
        profileButton.addActionListener(e -> viewProfile());
        reviewsButton.addActionListener(e -> viewReviews());
    }

    private void searchBooks() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter search term.");
            return;
        }

        try {
            java.util.List<Book> books = ApiService.searchBooks(query);
            bookTableModel.setRowCount(0);
            for (Book b : books) {
                bookTableModel.addRow(new Object[]{
                        b.getBookID(),
                        b.getTitle(),
                        b.getAuthor(),
                        b.getBuyPrice(),
                        b.getRentPrice(),
                        b.isAvailable() ? "Yes" : "No",
                        b.getCopies(),
                        "buy"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void viewAllBooks() {
        try {
            JsonArray arr = ApiService.getAllBooks();
            populateBooksFromJson(arr);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load books: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void populateBooksFromJson(JsonArray arr) {
        bookTableModel.setRowCount(0);
        for (JsonElement el : arr) {
            JsonObject b = el.getAsJsonObject();
            int id = b.has("bookID") && !b.get("bookID").isJsonNull() ? b.get("bookID").getAsInt() : -1;
            String title = b.has("title") && !b.get("title").isJsonNull() ? b.get("title").getAsString() : "";
            String author = b.has("author") && !b.get("author").isJsonNull() ? b.get("author").getAsString() : "";
            double buy = b.has("buyPrice") && !b.get("buyPrice").isJsonNull() ? b.get("buyPrice").getAsDouble() : 0.0;
            double rent = b.has("rentPrice") && !b.get("rentPrice").isJsonNull() ? b.get("rentPrice").getAsDouble() : 0.0;
            boolean avail = b.has("isAvailable") && !b.get("isAvailable").isJsonNull() && b.get("isAvailable").getAsBoolean();
            int copies = b.has("copies") && !b.get("copies").isJsonNull() ? b.get("copies").getAsInt() : 0;

            bookTableModel.addRow(new Object[]{id, title, author, String.format("%.2f", buy), String.format("%.2f", rent), avail, copies, "buy"});
        }
    }

    // ...existing code...
    private void addToCart() {
        // commit any in-progress edit (e.g. action combobox)
        if (bookTable.isEditing()) {
            try { bookTable.getCellEditor().stopCellEditing(); } catch (Exception ignored) {}
        }

        int[] viewRows = bookTable.getSelectedRows();
        if (viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select books to add.");
            return;
        }

        java.util.List<String> skipped = new ArrayList<>();
        int addedCount = 0;

        for (int vr : viewRows) {
            int row = bookTable.convertRowIndexToModel(vr); // handle sorting/filtering
            Object idObj = bookTableModel.getValueAt(row, 0);
            if (idObj == null) continue;
            int bookID = (idObj instanceof Number) ? ((Number) idObj).intValue() : Integer.parseInt(idObj.toString());

            String title = String.valueOf(bookTableModel.getValueAt(row, 1));
            String author = String.valueOf(bookTableModel.getValueAt(row, 2));

            Object buyObj = bookTableModel.getValueAt(row, 3);
            double buyPrice = (buyObj instanceof Number) ? ((Number) buyObj).doubleValue() : Double.parseDouble(String.valueOf(buyObj));

            Object rentObj = bookTableModel.getValueAt(row, 4);
            double rentPrice = (rentObj instanceof Number) ? ((Number) rentObj).doubleValue() : Double.parseDouble(String.valueOf(rentObj));

            Object availObj = bookTableModel.getValueAt(row, 5);
            boolean avail = "Yes".equalsIgnoreCase(String.valueOf(availObj)) || "true".equalsIgnoreCase(String.valueOf(availObj));

            Object copiesObj = bookTableModel.getValueAt(row, 6);
            int copies = (copiesObj instanceof Number) ? ((Number) copiesObj).intValue() : Integer.parseInt(String.valueOf(copiesObj));

            Object actionObj = bookTableModel.getValueAt(row, 7);
            String action = actionObj == null ? "buy" : actionObj.toString();

            if (!avail || copies <= 0) {
                skipped.add(title + " (ID:" + bookID + ")");
                continue;
            }

            Book book = new Book(bookID, title, author, buyPrice, rentPrice, avail);
            book.setCopies(copies);
            cart.add(new OrderItem(book, action)); // uses existing nested OrderItem
            addedCount++;
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Added ").append(addedCount).append(" item").append(addedCount == 1 ? "" : "s").append(" to cart.");
        if (!skipped.isEmpty()) msg.append(" Skipped: ").append(String.join(", ", skipped));

        JOptionPane.showMessageDialog(this, msg.toString());
    }
// ...existing code...

    private void viewCart() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty.");
            return;
        }

        StringBuilder sb = new StringBuilder("Cart Items:\n\n");
        double total = 0;
        for (OrderItem oi : cart) {
            double price = oi.getAction().equals("buy") ? oi.getBook().getBuyPrice() : oi.getBook().getRentPrice();
            total += price;
            sb.append(oi.getBook().getTitle())
            .append(" - ").append(oi.getAction())
            .append(" - $").append(String.format("%.2f", price)).append("\n");
        }
        sb.append("\nTotal: $").append(String.format("%.2f", total));
        JOptionPane.showMessageDialog(this, sb.toString(), "Cart", JOptionPane.INFORMATION_MESSAGE);
    }

    private void placeOrder() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty.");
            return;
        }

        java.util.List<ApiService.BookOrderItem> items = new ArrayList<>();
        for (OrderItem oi : cart) {
            // send action string so the server can read item.get("action")
            items.add(new ApiService.BookOrderItem(oi.getBook().getBookID(), oi.getAction()));
        }

        try {
            com.google.gson.JsonObject bill = ApiService.placeOrder(items);
            JOptionPane.showMessageDialog(this, formatBill(bill), "Bill", JOptionPane.INFORMATION_MESSAGE);
            cart.clear();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Order failed: " + ex.getMessage());
            ex.printStackTrace();
        }

          // Reset the search screen: clear search text and results table
            searchField.setText("");
            bookTableModel.setRowCount(0);
       
    }


    private String formatBill(JsonObject bill) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order ID: ").append(bill.get("orderID").getAsString()).append("\nItems:\n");
        for (var item : bill.getAsJsonArray("bill")) {
            JsonObject i = item.getAsJsonObject();
            sb.append(i.get("title").getAsString())
              .append(" - ").append(i.get("transactionType").getAsString())
              .append(" - $").append(i.get("price").getAsDouble()).append("\n");
        }
        sb.append("Total: $").append(bill.get("totalAmount").getAsDouble());
        return sb.toString();
    }


    // ---------- New: Display profile + order history ----------
    private void viewProfile() {
        try {
            JsonArray orders = ApiService.getMyOrders();
            if (orders.size() == 0) {
                JOptionPane.showMessageDialog(this, "No past orders found.");
                return;
            }

            StringBuilder sb = new StringBuilder();
             java.util.List<Integer> returnableItemIDs = new ArrayList<>();
            for (JsonElement el : orders) {
                JsonObject o = el.getAsJsonObject();
                sb.append("Order ID: ").append(o.get("orderID").getAsString())
                  .append("  |  Total: $").append(o.get("totalAmount").getAsString())
                  .append("  |  Status: ").append(o.has("paymentStatus") ? o.get("paymentStatus").getAsString() : "Unknown")
                  .append("\n");
                sb.append("Items:\n");
                if (o.has("items") && o.get("items").isJsonArray()) {
                    for (JsonElement itEl : o.getAsJsonArray("items")) {
                        JsonObject it = itEl.getAsJsonObject();
                        int itemID = it.has("itemID") ? it.get("itemID").getAsInt() : -1;
                        boolean isReturned = it.has("isReturned") && !it.get("isReturned").isJsonNull() && it.get("isReturned").getAsBoolean();
                        String title = it.has("title") && !it.get("title").isJsonNull() ? it.get("title").getAsString() : ("Book#" + it.get("bookID").getAsInt());
                        sb.append("  - [ItemID:").append(itemID).append("] ").append(title)
                          .append(" (").append(it.get("transactionType").getAsString()).append(") $")
                          .append(String.format("%.2f", it.get("price").getAsDouble()));
                        if (it.get("transactionType").getAsString().equals("rent")) {
                            sb.append("  Due: ").append(it.has("dueDate") && !it.get("dueDate").isJsonNull() ? it.get("dueDate").getAsString() : "N/A");
                            sb.append("  Returned: ").append(isReturned ? "Yes" : "No");
                            if (!isReturned) returnableItemIDs.add(itemID);
                        }
                        sb.append("\n");
                    }
                } else {
                    sb.append("  (no items)\n");
                }
                sb.append("\n");
            }

             JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(700, 400));
            JOptionPane.showMessageDialog(this, sp, "Order History", JOptionPane.INFORMATION_MESSAGE);


            if (!returnableItemIDs.isEmpty()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "You have unreturned rentals. Return one now?",
                        "Return Rental", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    String input = JOptionPane.showInputDialog(this, "Enter ItemID to return (examples: " + returnableItemIDs.toString() + "):");
                    if (input != null && !input.isEmpty()) {
                        try {
                            int itemID = Integer.parseInt(input.trim());
                            if (!returnableItemIDs.contains(itemID)) {
                                JOptionPane.showMessageDialog(this, "ItemID not returnable or invalid.");
                            } else {
                                ApiService.returnRental(itemID);
                                JOptionPane.showMessageDialog(this, "Return processed.");
                            }
                        } catch (NumberFormatException nfe) {
                            JOptionPane.showMessageDialog(this, "Invalid ItemID.");
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "Return failed: " + ex.getMessage());
                        }
                    }
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load profile: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

     // New: view and add reviews for selected book
    private void viewReviews() {
        int row = bookTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to view reviews.");
            return;
        }
        int bookID = (int) bookTableModel.getValueAt(row, 0);
        String title = (String) bookTableModel.getValueAt(row, 1);
        try {
            JsonArray reviews = ApiService.getBookReviews(bookID);
            StringBuilder sb = new StringBuilder();
            sb.append("Reviews for: ").append(title).append("\n\n");
            if (reviews.size() == 0) sb.append("(no reviews yet)\n");
            for (JsonElement el : reviews) {
                JsonObject r = el.getAsJsonObject();
                String user = r.has("username") && !r.get("username").isJsonNull() ? r.get("username").getAsString() : ("User#" + r.get("userID").getAsInt());
                int rating = r.has("rating") ? r.get("rating").getAsInt() : 0;
                String text = r.has("reviewText") && !r.get("reviewText").isJsonNull() ? r.get("reviewText").getAsString() : "";
                String created = r.has("createdAt") ? r.get("createdAt").getAsString() : "";
                sb.append(user).append("  -  ").append(rating).append("/5").append("  ").append(created).append("\n");
                if (!text.isEmpty()) sb.append("  ").append(text).append("\n");
                sb.append("\n");
            }

             JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(700, 400));

            Object[] options = {"Add Review", "Close"};
            int choice = JOptionPane.showOptionDialog(this, sp, "Reviews", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
            if (choice == 0) {
                showAddReviewDialog(bookID, title);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load reviews: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    private void showAddReviewDialog(int bookID, String title) {
        JPanel p = new JPanel(new BorderLayout(0,5));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Rating:"));
        JComboBox<Integer> ratingCombo = new JComboBox<>(new Integer[]{1,2,3,4,5});
        top.add(ratingCombo);
        p.add(top, BorderLayout.NORTH);
        JTextArea ta = new JTextArea(6,50);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        p.add(new JScrollPane(ta), BorderLayout.CENTER);

        int ok = JOptionPane.showConfirmDialog(this, p, "Add Review for: " + title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        int rating = (Integer) ratingCombo.getSelectedItem();
        String text = ta.getText().trim();

        try {
            JsonObject created = ApiService.postBookReview(bookID, rating, text);
            JOptionPane.showMessageDialog(this, "Review submitted.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to submit review: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static class OrderItem {
        private Book book;
        private String action;
        public OrderItem(Book b, String a) { book=b; action=a; }
        public Book getBook() { return book; }
        public String getAction() { return action; }
    }

    private void logout() {
        try {
            ApiService.logout(); // clear client-side session/token
        } catch (Exception ignored) { }
        this.dispose();
        SwingUtilities.invokeLater(() -> {
            LoginScreen login = new LoginScreen();
            login.setVisible(true);
        });
    }

    public static void main(String[] args) {
        User dummy = new User(1,"customer","dummyToken");
        SwingUtilities.invokeLater(() -> new CustomerScreen(dummy).setVisible(true));
    }

}

