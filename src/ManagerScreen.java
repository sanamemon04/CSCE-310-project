import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import models.Book;
import models.User;

public class ManagerScreen extends JFrame {

    private User user;
    private JButton viewOrdersButton, updateStatusButton, addBookButton, logoutButton;
    private JButton viewBooksButton, updateAvailabilityButton; 
    private JList<Object> ordersList;
    private DefaultListModel<Object> ordersModel;
    private boolean showingBooks = false;
    // renderer to show Book nicely
    private ListCellRenderer<Object> bookRenderer = new DefaultListCellRenderer() {
       public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            String html;
            if (value instanceof Book) {
                Book b = (Book) value;
                html = String.format(
                    "<html><div style='width:600px;padding:6px;'>"
                    + "<b>%d: %s</b> — %s<br/>"
                    + "Buy: $%.2f &nbsp;&nbsp; Rent: $%.2f<br/>"
                    + "<i>%s</i>"
                    + "</div></html>",
                    b.getBookID(), escapeHtml(b.getTitle()), escapeHtml(b.getAuthor()),
                    b.getBuyPrice(), b.getRentPrice(),
                    b.isAvailable() ? "<span style='color:green;'>Available</span>" : "<span style='color:red;'>Unavailable</span>"
                );
            } else if (value instanceof com.google.gson.JsonObject) {
                JsonObject o = (JsonObject) value;
                int orderID = o.has("orderID") ? o.get("orderID").getAsInt() : -1;
                int userID = o.has("userID") ? o.get("userID").getAsInt() : -1;
                double total = o.has("totalAmount") ? o.get("totalAmount").getAsDouble() :
                               (o.has("total") ? o.get("total").getAsDouble() : 0.0);
                String status = o.has("paymentStatus") ? o.get("paymentStatus").getAsString() : "Unknown";
           StringBuilder itemsHtml = new StringBuilder();
                if (o.has("items") && o.get("items").isJsonArray()) {
                    for (JsonElement itEl : o.getAsJsonArray("items")) {
                        if (!itEl.isJsonObject()) continue;
                        JsonObject it = itEl.getAsJsonObject();
                        String title = it.has("title") ? it.get("title").getAsString() : ("Book#" + it.get("bookID").getAsInt());
                        String tt = it.has("transactionType") ? it.get("transactionType").getAsString() : "buy";
                       double price = it.has("price") ? it.get("price").getAsDouble() : 0.0;
                        itemsHtml.append(String.format("%s (%s) $%.2f<br/>", escapeHtml(title), escapeHtml(tt), price));
                    }
                } else {
                    itemsHtml.append("<div style='margin-top:6px;font-style:italic;color:#666;'>No items</div>");
                }
                html = String.format(
                    "<html><div style='width:600px;padding:6px;'>"
                    + "<b>Order %d</b> &nbsp; User: %d &nbsp; <b>Total: $%.2f</b> &nbsp; <i>%s</i><br/>"
                    + "<div style='margin-top:6px;'>%s</div>"
                    + "</div></html>",
                    orderID, userID, total, escapeHtml(status), itemsHtml.toString()
                );
            } else {
                html = "<html><div style='width:600px;padding:6px;'>" + escapeHtml(String.valueOf(value)) + "</div></html>";
            }

            JLabel label = (JLabel) super.getListCellRendererComponent(list, html, index, isSelected, cellHasFocus);
            label.setVerticalAlignment(SwingConstants.TOP);
            label.setPreferredSize(null); // allow variable height
            return label;
        }
    };

    public ManagerScreen(User user) {
        this.user = user;
        setTitle("Manager Dashboard");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        viewOrdersButton = new JButton("View Orders");
        updateStatusButton = new JButton("Update Payment Status");
        addBookButton = new JButton("Add Book");
        logoutButton = new JButton("Logout");
        viewBooksButton = new JButton("View All Books");
        updateAvailabilityButton = new JButton("Update Availability");

        leftPanel.add(viewOrdersButton);
        leftPanel.add(updateStatusButton);
        leftPanel.add(addBookButton);
        leftPanel.add(viewBooksButton);
        leftPanel.add(updateAvailabilityButton);

        rightPanel.add(logoutButton);

        topPanel.add(leftPanel, BorderLayout.CENTER);

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(rightPanel, BorderLayout.EAST);
        

        ordersModel = new DefaultListModel<>();
        ordersList = new JList<>(ordersModel);
        ordersList.setCellRenderer(bookRenderer);
        ordersList.setVisibleRowCount(8);
        ordersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(ordersList);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        viewOrdersButton.addActionListener(e -> loadOrders());
        updateStatusButton.addActionListener(e -> updatePaymentStatus());
        addBookButton.addActionListener(e -> addBook());
        viewBooksButton.addActionListener(e -> loadBooks());
        updateAvailabilityButton.addActionListener(e -> updateAvailability());
        logoutButton.addActionListener(e -> logout());
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void loadOrders() {
        try {

            JsonArray orders = ApiService.getAllOrders();
            showingBooks = false;
            ordersModel.clear();
            for (JsonElement el : orders) {
                ordersModel.addElement(el.getAsJsonObject());
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load orders: " + ex.getMessage());
        }
    }

    private void updatePaymentStatus() {
        Object sel = ordersList.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Select an order first.");
            return;
        }
        
        JsonObject selectedOrder = (JsonObject) sel;

        String current = selectedOrder.has("paymentStatus") ? selectedOrder.get("paymentStatus").getAsString() : "Pending";
        String newStatus = JOptionPane.showInputDialog(this,
                "Enter new payment status (Pending/Paid):",
                current);

        if (newStatus == null || newStatus.isEmpty()) return;

        try {
            int orderID = selectedOrder.get("orderID").getAsInt();
            ApiService.updatePaymentStatus(orderID, newStatus);
            JOptionPane.showMessageDialog(this, "Payment status updated!");
            loadOrders();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to update status: " + ex.getMessage());
        }
    }

    private void addBook() {
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JTextField buyField = new JTextField();
        JTextField rentField = new JTextField();

        Object[] fields = {
                "Title:", titleField,
                "Author:", authorField,
                "Buy Price:", buyField,
                "Rent Price:", rentField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add Book", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String title = titleField.getText();
            String author = authorField.getText();
            double buyPrice = Double.parseDouble(buyField.getText());
            double rentPrice = Double.parseDouble(rentField.getText());

            // construct models.Book and call ApiService.addBook
            Book book = new Book(0, title, author, buyPrice, rentPrice, true);
            ApiService.addBook(book);

            JOptionPane.showMessageDialog(this, "Book added successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to add book: " + ex.getMessage());
        }
    }

    private void updateAvailability() {
        Object sel = ordersList.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Select an item from the list.");
            return;
        }
        if (!showingBooks) {
            JOptionPane.showMessageDialog(this, "List currently shows orders. Click 'View All Books' first.");
            return;
        }
        if (!(sel instanceof Book)) {
            JOptionPane.showMessageDialog(this, "Unexpected item type selected.");
            return;
        }
        Book book = (Book) sel;
        String copiesStr = JOptionPane.showInputDialog(this,
                String.format("Book: %s\nCurrent copies: %d\nEnter new copies (0 or more):", book.getTitle(), book.getCopies()),
                String.valueOf(book.getCopies()));
        if (copiesStr == null) return;
        try {
            int newCopies = Integer.parseInt(copiesStr.trim());
            ApiService.updateBookCopies(book.getBookID(), newCopies);
            JOptionPane.showMessageDialog(this, "Copies updated.");
            loadBooks();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid number.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to update copies: " + ex.getMessage());
        }
    }

    private void loadBooks() {
        try {
            showingBooks = true;
            List<Book> books = ApiService.getAllBooksAsList();
            showingBooks = true;
            ordersModel.clear();
            for (Book b : books) ordersModel.addElement(b);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load books: " + ex.getMessage());
            ex.printStackTrace();
            showingBooks = false;
            JOptionPane.showMessageDialog(this, "Failed to load books: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        }
    }

    private void logout() {
        ApiService.logout();
        this.dispose();
        LoginScreen loginScreen = new LoginScreen();
        loginScreen.setVisible(true);
    }


    
}
