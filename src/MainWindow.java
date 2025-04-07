package com.bookstore;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
        setSize(1200, 800); // Увеличен размер для десктопа
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            store.getDb().connect();
            store.getDb().createTables();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 16)); // Увеличен шрифт
        updateTabs();
        add(tabbedPane);
        getContentPane().setBackground(new Color(240, 248, 255));
        setVisible(true);
    }

    private void showLoginOrRegisterDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        panel.add(new JLabel("Please login or register to continue"));
        panel.add(loginButton);
        panel.add(registerButton);

        JDialog dialog = new JDialog(this, "Login or Register", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        loginButton.addActionListener(e -> {
            dialog.dispose();
            showLoginDialog();
        });
        registerButton.addActionListener(e -> {
            dialog.dispose();
            showRegisterDialog();
        });

        dialog.setVisible(true);
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

        int result = JOptionPane.showConfirmDialog(this, panel, "Login", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String login = loginField.getText().trim();
            String password = new String(passwordField.getPassword());
            currentUser = store.findUser(login);
            if (currentUser == null || !currentUser.authenticate(password)) {
                JOptionPane.showMessageDialog(this, "Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
                showLoginOrRegisterDialog();
            } else {
                updateTabs();
            }
        }
    }

    private void showRegisterDialog() {
        JTextField nameField = new JTextField(10);
        JTextField loginField = new JTextField(10);
        JPasswordField passwordField = new JPasswordField(10);
        JPasswordField confirmPasswordField = new JPasswordField(10);
        JButton uploadButton = new JButton("Upload Avatar");
        JLabel avatarLabel = new JLabel("No avatar selected");

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Login:"));
        panel.add(loginField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Confirm Password:"));
        panel.add(confirmPasswordField);
        panel.add(new JLabel("Avatar:"));
        panel.add(uploadButton);
        panel.add(new JLabel(""));
        panel.add(avatarLabel);

        File[] selectedFile = {null};
        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "png"));
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = fileChooser.getSelectedFile();
                avatarLabel.setText(selectedFile[0].getName());
            }
        });

        int result = JOptionPane.showConfirmDialog(this, panel, "Register", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String login = loginField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (name.isEmpty() || login.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields must be filled", "Error", JOptionPane.ERROR_MESSAGE);
                showRegisterDialog();
                return;
            }
            if (login.equals("admin")) {
                JOptionPane.showMessageDialog(this, "Login 'admin' is reserved", "Error", JOptionPane.ERROR_MESSAGE);
                showRegisterDialog();
                return;
            }
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
                showRegisterDialog();
                return;
            }
            if (store.findUser(login) != null) {
                JOptionPane.showMessageDialog(this, "Login already exists", "Error", JOptionPane.ERROR_MESSAGE);
                showRegisterDialog();
                return;
            }

            try {
                String avatarPath = uploadAvatar(selectedFile[0]);
                store.registerUser(login, name, password, avatarPath);
                JOptionPane.showMessageDialog(this, "Registration successful! Please login.", "Success", JOptionPane.INFORMATION_MESSAGE);
                showLoginDialog();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Registration failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                showRegisterDialog();
            }
        }
    }

    private String uploadAvatar(File sourceFile) {
        if (sourceFile == null) return null;
        try {
            File destFile = new File("avatars/" + sourceFile.getName());
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return destFile.getPath();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error uploading avatar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void updateTabs() {
        tabbedPane.removeAll();

        for (Category category : store.readCategories()) {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.setBackground(new Color(245, 245, 220));
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Description", "Cover"}, 0);
            JTable table = new JTable(model);
            table.setRowHeight(50);
            table.setFont(new Font("Arial", Font.PLAIN, 12));
            table.getColumn("Cover").setCellRenderer(new ImageRenderer());
            table.getColumn("Cover").setPreferredWidth(100);
            loadBooks(category, model);

            panel.add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            buttonPanel.setBackground(new Color(245, 245, 220));
            if (currentUser != null && currentUser.getRole().equals("Admin")) {
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
            } else {
                JButton takeButton = new JButton("Take Book");
                styleButton(takeButton);
                takeButton.addActionListener(e -> takeBook(category, table));
                buttonPanel.add(takeButton);
            }
            panel.add(buttonPanel, BorderLayout.SOUTH);
            tabbedPane.addTab(category.getName(), panel);
        }

        if (currentUser != null && currentUser.getRole().equals("Client")) {
            JPanel cartPanel = new JPanel(new BorderLayout(10, 10));
            cartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            cartPanel.setBackground(new Color(245, 245, 220));
            DefaultTableModel cartModel = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Description", "Cover"}, 0);
            JTable cartTable = new JTable(cartModel);
            cartTable.setRowHeight(50);
            cartTable.setFont(new Font("Arial", Font.PLAIN, 12));
            cartTable.getColumn("Cover").setCellRenderer(new ImageRenderer());
            cartTable.getColumn("Cover").setPreferredWidth(100);
            loadCart(cartModel);

            cartPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

            JPanel cartButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            cartButtonPanel.setBackground(new Color(245, 245, 220));
            JButton removeButton = new JButton("Remove from Cart");
            styleButton(removeButton);
            removeButton.addActionListener(e -> removeFromCart(cartTable));
            cartButtonPanel.add(removeButton);

            JLabel totalLabel = new JLabel("Total: " + calculateTotal());
            totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
            totalLabel.setForeground(new Color(0, 100, 0));
            cartButtonPanel.add(totalLabel);

            cartPanel.add(cartButtonPanel, BorderLayout.SOUTH);
            tabbedPane.addTab("Cart", cartPanel);
        }

        JPanel accountPanel = new JPanel(new BorderLayout(10, 10));
        accountPanel.setBackground(new Color(245, 245, 220));
        JLabel userLabel = new JLabel("Logged in as: " + (currentUser != null ? currentUser.getName() : "Guest"));
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        topPanel.setBackground(new Color(245, 245, 220));
        topPanel.add(userLabel);

        if (currentUser == null) {
            JButton loginButton = new JButton("Login/Register");
            styleButton(loginButton);
            loginButton.addActionListener(e -> showLoginOrRegisterDialog());
            topPanel.add(loginButton);
        } else {
            JButton logoutButton = new JButton("Log Out");
            styleButton(logoutButton);
            logoutButton.addActionListener(e -> logOut());
            topPanel.add(logoutButton);
        }
        accountPanel.add(topPanel, BorderLayout.NORTH);

        if (currentUser != null) {
            DefaultTableModel usersModel = new DefaultTableModel(new String[]{"Login", "Name", "Avatar"}, 0);
            JTable usersTable = new JTable(usersModel);
            usersTable.setRowHeight(50);
            usersTable.getColumn("Avatar").setCellRenderer(new ImageRenderer());
            usersTable.getColumn("Avatar").setPreferredWidth(100);
            loadUsers(usersModel);

            JPanel usersPanel = new JPanel(new BorderLayout(10, 10));
            usersPanel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
            JPanel usersButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            usersButtonPanel.setBackground(new Color(245, 245, 220));
            JButton switchButton = new JButton("Switch Account");
            JButton deleteButton = new JButton("Delete Account");
            styleButton(switchButton);
            styleButton(deleteButton);
            switchButton.addActionListener(e -> switchAccount(usersTable));
            deleteButton.addActionListener(e -> deleteAccount(usersTable));
            usersButtonPanel.add(switchButton);
            usersButtonPanel.add(deleteButton);
            usersPanel.add(usersButtonPanel, BorderLayout.SOUTH);
            accountPanel.add(usersPanel, BorderLayout.CENTER);
        }

        tabbedPane.addTab("Account", accountPanel);
    }

    private void loadUsers(DefaultTableModel model) {
        model.setRowCount(0);
        if (currentUser != null) {
            for (User user : store.getAllUsers()) {
                if (!user.getLogin().equals(currentUser.getLogin())) {
                    model.addRow(new Object[]{user.getLogin(), user.getName(), user.getAvatarPath()});
                }
            }
        }
    }

    private void switchAccount(JTable usersTable) {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an account to switch", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String login = (String) usersTable.getValueAt(selectedRow, 0);
        JPasswordField passwordField = new JPasswordField(10);
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.add(new JLabel("Enter password for " + login + ":"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Switch Account", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String password = new String(passwordField.getPassword());
            User user = store.findUser(login);
            if (user != null && user.authenticate(password)) {
                currentUser = user;
                updateTabs();
                JOptionPane.showMessageDialog(this, "Switched to " + login, "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid password", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteAccount(JTable usersTable) {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an account to delete", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String login = (String) usersTable.getValueAt(selectedRow, 0);
        if (login.equals("admin")) {
            JOptionPane.showMessageDialog(this, "Cannot delete admin account", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + login + "?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                store.removeUser(login);
                updateTabs();
                JOptionPane.showMessageDialog(this, "Account deleted", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting account: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Arial", Font.PLAIN, 14)); // Увеличен шрифт кнопок
        button.setBackground(new Color(135, 206, 235));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(120, 40)); // Увеличен размер кнопок
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
                String coverPath = rs.getString("cover_path");
                category.createBook(id, name, price, description, coverPath);
                model.addRow(new Object[]{id, name, price, description, coverPath});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading books: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadCart(DefaultTableModel model) {
        model.setRowCount(0);
        for (Book book : cart.getBooks()) {
            model.addRow(new Object[]{book.getId(), book.getName(), book.getPrice(), book.getDescription(), book.getCoverPath()});
        }
    }

    private String calculateTotal() {
        double total = cart.getBooks().stream().mapToDouble(Book::getPrice).sum();
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(total) + " €";
    }

    private String uploadCover(File sourceFile) {
        if (sourceFile == null) return null;
        try {
            File destFile = new File("covers/" + sourceFile.getName());
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return destFile.getPath();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error uploading cover: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void showAddBookDialog(Category category) {
        JTextField nameField = new JTextField(10);
        JTextField priceField = new JTextField(10);
        JTextField descField = new JTextField(10);
        JButton uploadButton = new JButton("Upload Cover");
        JLabel coverLabel = new JLabel("No cover selected");

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Price:"));
        panel.add(priceField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);
        panel.add(new JLabel("Cover:"));
        panel.add(uploadButton);
        panel.add(new JLabel(""));
        panel.add(coverLabel);

        File[] selectedFile = {null};
        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "png"));
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = fileChooser.getSelectedFile();
                coverLabel.setText(selectedFile[0].getName());
            }
        });

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Book", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String priceText = priceField.getText().trim();
            if (priceText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Price cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                double price = Double.parseDouble(priceText);
                if (price < 0) {
                    JOptionPane.showMessageDialog(this, "Price cannot be negative", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Name cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String coverPath = uploadCover(selectedFile[0]);
                Book book = new Book(0, name, price, descField.getText(), category, coverPath);
                store.getDb().saveBook(book);
                updateTabs();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid price format: " + priceText, "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        JButton uploadButton = new JButton("Upload Cover");
        JLabel coverLabel = new JLabel(table.getValueAt(selectedRow, 4) != null ? table.getValueAt(selectedRow, 4).toString() : "No cover");

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Price:"));
        panel.add(priceField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);
        panel.add(new JLabel("Cover:"));
        panel.add(uploadButton);
        panel.add(new JLabel(""));
        panel.add(coverLabel);

        File[] selectedFile = {null};
        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "png"));
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = fileChooser.getSelectedFile();
                coverLabel.setText(selectedFile[0].getName());
            }
        });

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Book", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String priceText = priceField.getText().trim();
            if (priceText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Price cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                double price = Double.parseDouble(priceText);
                if (price < 0) {
                    JOptionPane.showMessageDialog(this, "Price cannot be negative", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Name cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String coverPath = selectedFile[0] != null ? uploadCover(selectedFile[0]) : (table.getValueAt(selectedRow, 4) != null ? table.getValueAt(selectedRow, 4).toString() : null);
                store.getDb().updateBook(id, name, price, descField.getText(), coverPath);
                updateTabs();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid price format: " + priceText, "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Error deleting book: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void takeBook(Category category, JTable table) {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please login to take a book", "Warning", JOptionPane.WARNING_MESSAGE);
            showLoginOrRegisterDialog();
            return;
        }

        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to take", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) table.getValueAt(selectedRow, 0);
        String name = (String) table.getValueAt(selectedRow, 1);
        double price = (double) table.getValueAt(selectedRow, 2);
        String description = (String) table.getValueAt(selectedRow, 3);
        String coverPath = table.getValueAt(selectedRow, 4) != null ? table.getValueAt(selectedRow, 4).toString() : null;
        Book book = new Book(id, name, price, description, category, coverPath);
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
        updateTabs();
    }

    private class ImageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = new JLabel();
            if (value != null && !value.toString().equals("No cover") && !value.toString().isEmpty()) {
                try {
                    ImageIcon icon = new ImageIcon(value.toString());
                    Image img = icon.getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH); // Увеличен размер картинок
                    label.setIcon(new ImageIcon(img));
                } catch (Exception e) {
                    label.setText("No image");
                }
            } else {
                label.setText("No image");
            }
            label.setHorizontalAlignment(JLabel.CENTER);
            return label;
        }
    }
}