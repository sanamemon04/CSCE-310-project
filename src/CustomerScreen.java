

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
        topPanel.add(searchField);
        topPanel.add(searchButton);
        panel.add(topPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Title", "Author", "Buy Price", "Rent Price", "Available", "Action"};
        bookTableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { return column == 6; }
        };
        bookTable = new JTable(bookTableModel);
        bookTable.setRowHeight(28); // slightly taller
        bookTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Dropdown always visible
        JComboBox<String> actionCombo = new JComboBox<>(new String[]{"buy", "rent"});
        actionCombo.setMaximumRowCount(2);
        TableColumn actionCol = bookTable.getColumnModel().getColumn(6);
        actionCol.setCellEditor(new DefaultCellEditor(actionCombo));
        actionCol.setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JComboBox<String> combo = new JComboBox<>(new String[]{"buy", "rent"});
                combo.setSelectedItem(value);
                return combo;
            }
        });

        panel.add(new JScrollPane(bookTable), BorderLayout.CENTER);

        // Bottom buttons
        JPanel bottom = new JPanel();
        addToCartButton = new JButton("Add to Cart");
        viewCartButton = new JButton("View Cart");
        placeOrderButton = new JButton("Place Order");
        logoutButton = new JButton("Logout");
        profileButton = new JButton("Profile"); 
        reviewsButton = new JButton("Reviews");

        bottom.add(profileButton); 
        bottom.add(addToCartButton);
        bottom.add(viewCartButton);
        bottom.add(placeOrderButton);
        bottom.add(logoutButton);
        bottom.add(reviewsButton);
        panel.add(bottom, BorderLayout.SOUTH);

        add(panel);

        // Actions
        searchButton.addActionListener(e -> searchBooks());
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
                        "buy"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void addToCart() {
        int[] rows = bookTable.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select books to add.");
            return;
        }

        java.util.List<String> skipped = new ArrayList<>();
        int addedCount = 0;

        for (int r : rows) {
            int bookID = (int) bookTableModel.getValueAt(r, 0);
            String title = (String) bookTableModel.getValueAt(r, 1);
            String author = (String) bookTableModel.getValueAt(r, 2);
            double buyPrice = (double) bookTableModel.getValueAt(r, 3);
            double rentPrice = (double) bookTableModel.getValueAt(r, 4);
            boolean avail = "Yes".equals(String.valueOf(bookTableModel.getValueAt(r, 5)));
            String action = (String) bookTableModel.getValueAt(r, 6);

            if (!avail) {
                skipped.add(title + " (ID:" + bookID + ")");
                continue; // do not add unavailable book
            }

            Book book = new Book(bookID, title, author, buyPrice, rentPrice, avail);
            cart.add(new OrderItem(book, action));
                        addedCount++;

        }

        String msg = "";
        if (addedCount > 0) msg += "Added " + addedCount + " item(s) to cart.";
        if (!skipped.isEmpty()) {
            if (!msg.isEmpty()) msg += "\n";
            msg += "The following items were unavailable and were not added:\n" + String.join(", ", skipped);
        }
        JOptionPane.showMessageDialog(this, msg);
        // JOptionPane.showMessageDialog(this, "Added to cart.");
    }

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
                        sb.append("  - ").append(it.has("title") && !it.get("title").isJsonNull() ? it.get("title").getAsString() : ("Book#" + it.get("bookID").getAsInt()))
                          .append(" (").append(it.get("transactionType").getAsString()).append(") $")
                          .append(String.format("%.2f", it.get("price").getAsDouble()))
                          .append("\n");
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

