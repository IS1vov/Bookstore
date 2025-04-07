package com.bookstore;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;

public class MainWindow extends JFrame {
    private BookStore store;
    private User currentUser;
    private JTabbedPane tabbedPane;
    private Cart cart;

    public MainWindow(BookStore store) {
        this.store = store;
        this.cart = new Cart();
        setTitle("Book Store");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Центрируем окно

        try {
            store.getDb().connect();
            store.getDb().createTables();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Login window
        showLoginDialog();

        // Main UI
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 14));
        updateTabs();
        add(tabbedPane);
        getContentPane().setBackground(new Color(240, 248, 255)); // Светлый фон
        setVisible(true);
    }

    private void showLoginDialog() {
        JTextField loginField = new JTextField(10);
        JPasswordField passwordField = new JPasswordField(10);
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Login:"));
        panel.add(loginField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Login", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String login = loginField.getText();
            String password = new String(passwordField.getPassword());
            currentUser = store.findUser(login);
            if (currentUser == null || !currentUser.authenticate(password)) {
                JOptionPane.showMessageDialog(this, "Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    private void updateTabs() {
        tabbedPane.removeAll();

        // Category tabs
        for (Category category : store.readCategories()) {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.setBackground(new Color(245, 245, 220)); // Бежевый фон
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Description"}, 0);
            JTable table = new JTable(model);
            table.setRowHeight(25);
            table.setFont(new Font("Arial", Font.PLAIN, 12));
            loadBooks(category, model);

            panel.add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            buttonPanel.setBackground(new Color(245, 245, 220));
            if (currentUser.getRole().equals("Admin")) {
                JButton addButton = new JButton("Add Book");
                JButton editButton = new JButton("Edit Book");
                JButton deleteButton = new JButton("Delete Book");
                styleButton(addButton);
                styleButton(editButton);
                styleButton(deleteButton);

                addButton.addActionListener(e -> showAddBookDialog(category));
                editButton.addActionListener(e -> showEditBookDialog(category, table));
                deleteButton.addActionListener(e -> deleteBook(category, table));
                buttonPanel.add(addButton);
                buttonPanel.add(editButton);
                buttonPanel.add(deleteButton);
            } else if (currentUser.getRole().equals("Client")) {
                JButton takeButton = new JButton("Take Book");
                styleButton(takeButton);
                takeButton.addActionListener(e -> takeBook(category, table));
                buttonPanel.add(takeButton);
            }
            panel.add(buttonPanel, BorderLayout.SOUTH);
            tabbedPane.addTab(category.getName(), panel);
        }

        // Cart tab (only for Client)
        if (currentUser.getRole().equals("Client")) {
            JPanel cartPanel = new JPanel(new BorderLayout(10, 10));
            cartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            cartPanel.setBackground(new Color(245, 245, 220));
            DefaultTableModel cartModel = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Description"}, 0);
            JTable cartTable = new JTable(cartModel);
            cartTable.setRowHeight(25);
            cartTable.setFont(new Font("Arial", Font.PLAIN, 12));
            loadCart(cartModel);

            cartPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

            JPanel cartButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            cartButtonPanel.setBackground(new Color(245, 245, 220));
            JButton removeButton = new JButton("Remove from Cart");
            styleButton(removeButton);
            removeButton.addActionListener(e -> removeFromCart(cartTable));
            cartButtonPanel.add(removeButton);

            JLabel totalLabel = new JLabel("Total: $" + calculateTotal());
            totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
            totalLabel.setForeground(new Color(0, 100, 0)); // Зелёный текст
            cartButtonPanel.add(totalLabel);

            cartPanel.add(cartButtonPanel, BorderLayout.SOUTH);
            tabbedPane.addTab("Cart", cartPanel);
        }

        // Account tab
        JPanel accountPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        accountPanel.setBackground(new Color(245, 245, 220));
        JLabel userLabel = new JLabel("Logged in as: " + currentUser.getLogin() + " (" + currentUser.getRole() + ")");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JButton logoutButton = new JButton("Log Out");
        styleButton(logoutButton);
        logoutButton.addActionListener(e -> logOut());
        accountPanel.add(userLabel);
        accountPanel.add(logoutButton);
        tabbedPane.addTab("Account", accountPanel);
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Arial", Font.PLAIN, 12));
        button.setBackground(new Color(135, 206, 235)); // Голубой фон
        button.setForeground(Color.BLACK); // Чёрный текст для контраста
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
    }

    private void loadBooks(Category category, DefaultTableModel model) {
        try {
            ResultSet rs = store.getDb().getBooks(category.getName());
            model.setRowCount(0);
            category.readBooks().clear();
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                String description = rs.getString("description");
                category.createBook(id, name, price, description);
                model.addRow(new Object[]{id, name, price, description});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading books", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadCart(DefaultTableModel model) {
        model.setRowCount(0);
        for (Book book : cart.getBooks()) {
            model.addRow(new Object[]{book.getId(), book.getName(), book.getPrice(), book.getDescription()});
        }
    }

    private String calculateTotal() {
        double total = cart.getBooks().stream().mapToDouble(Book::getPrice).sum();
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(total);
    }

    private void showAddBookDialog(Category category) {
        JTextField nameField = new JTextField(10);
        JTextField priceField = new JTextField(10);
        JTextField descField = new JTextField(10);
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Price:"));
        panel.add(priceField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Book", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double price = Double.parseDouble(priceField.getText());
                Book book = new Book(0, nameField.getText(), price, descField.getText(), category);
                store.getDb().saveBook(book);
                updateTabs();
            } catch (NumberFormatException | SQLException e) {
                JOptionPane.showMessageDialog(this, "Invalid input or DB error", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditBookDialog(Category category, JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to edit", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) table.getValueAt(selectedRow, 0);
        JTextField nameField = new JTextField((String) table.getValueAt(selectedRow, 1), 10);
        JTextField priceField = new JTextField(table.getValueAt(selectedRow, 2).toString(), 10);
        JTextField descField = new JTextField((String) table.getValueAt(selectedRow, 3), 10);
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Price:"));
        panel.add(priceField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Book", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double price = Double.parseDouble(priceField.getText());
                store.getDb().updateBook(id, nameField.getText(), price, descField.getText());
                updateTabs();
            } catch (NumberFormatException | SQLException e) {
                JOptionPane.showMessageDialog(this, "Invalid input or DB error", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteBook(Category category, JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to delete", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) table.getValueAt(selectedRow, 0);
        try {
            store.getDb().deleteBook(id);
            updateTabs();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting book", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void takeBook(Category category, JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to take", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) table.getValueAt(selectedRow, 0);
        String name = (String) table.getValueAt(selectedRow, 1);
        double price = (double) table.getValueAt(selectedRow, 2);
        String description = (String) table.getValueAt(selectedRow, 3);
        Book book = new Book(id, name, price, description, category);
        cart.addBook(book);
        updateTabs();
        JOptionPane.showMessageDialog(this, "Book added to cart", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void removeFromCart(JTable cartTable) {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to remove", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        cart.removeBook(selectedRow);
        updateTabs();
        JOptionPane.showMessageDialog(this, "Book removed from cart", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void logOut() {
        cart.clear();
        currentUser = null;
        showLoginDialog();
        updateTabs();
    }
}