package com.bookstore;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainWindow extends JFrame {
    private BookStore store;
    private User currentUser;
    private JTabbedPane tabbedPane;
    private Cart cart;

    public MainWindow(BookStore store) {
        this.store = store;
        this.cart = new Cart();
        setTitle("Book Store");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Убираем connect() и createTables(), так как они уже вызваны в BookStore
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 16));
        updateTabs();
        add(tabbedPane);
        getContentPane().setBackground(new Color(240, 248, 255));
        setVisible(true);

        // Обработчик закрытия окна
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    store.getDb().close();
                } catch (SQLException e) {
                    System.err.println("Ошибка при закрытии базы данных: " + e.getMessage());
                }
                System.exit(0);
            }
        });
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
        List<Category> categories = store.readCategories();
        System.out.println("Количество категорий в updateTabs: " + categories.size());

        for (Category category : categories) {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.setBackground(new Color(245, 245, 220));
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Description", "Cover", "Rating", "Like", "Dislike"}, 0);
            JTable table = new JTable(model);
            table.setRowHeight(50);
            table.setFont(new Font("Arial", Font.PLAIN, 12));
            table.getColumn("Cover").setCellRenderer(new ImageRenderer());
            table.getColumn("Cover").setPreferredWidth(100);
            table.getColumn("Rating").setCellRenderer(new RatingRenderer());
            table.getColumn("Like").setCellRenderer(new ButtonRenderer("Like"));
            table.getColumn("Dislike").setCellRenderer(new ButtonRenderer("Dislike"));
            table.getColumn("Like").setCellEditor(new ButtonEditor(new JButton("Like"), table, store));
            table.getColumn("Dislike").setCellEditor(new ButtonEditor(new JButton("Dislike"), table, store));
            loadBooks(category, model);

            panel.add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            buttonPanel.setBackground(new Color(245, 245, 220));
            if (currentUser != null) {
                System.out.println("Текущий пользователь: " + currentUser.getLogin() + ", роль: " + currentUser.getRole());
                if (currentUser.getRole().equals("Admin")) {
                    System.out.println("Отображаем кнопки админа для " + category.getName());
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
                    System.out.println("Отображаем кнопки клиента для " + category.getName());
                    JButton takeButton = new JButton("Take Book");
                    JButton reviewButton = new JButton("Reviews");
                    styleButton(takeButton);
                    styleButton(reviewButton);
                    takeButton.addActionListener(e -> takeBook(category, table));
                    reviewButton.addActionListener(e -> showReviewsDialog(category, table));
                    buttonPanel.add(takeButton);
                    buttonPanel.add(reviewButton);
                }
            } else {
                System.out.println("Пользователь не авторизован, кнопки не отображаются");
                JButton takeButton = new JButton("Take Book");
                JButton reviewButton = new JButton("Reviews");
                styleButton(takeButton);
                styleButton(reviewButton);
                takeButton.addActionListener(e -> takeBook(category, table));
                reviewButton.addActionListener(e -> showReviewsDialog(category, table));
                buttonPanel.add(takeButton);
                buttonPanel.add(reviewButton);
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
            tabindex:            topPanel.add(logoutButton);
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

    private void showReviewsDialog(Category category, JTable table) {
        if (table.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to view reviews.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int bookId = (int) table.getValueAt(table.getSelectedRow(), 0);
        JDialog dialog = new JDialog(this, "Reviews for Book ID: " + bookId, true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Reviews");
        List<Review> reviews = store.getReviews(bookId);
        Map<Integer, DefaultMutableTreeNode> reviewNodes = new HashMap<>();

        // Создаём узлы для всех отзывов
        for (Review review : reviews) {
            String displayText = review.getUserLogin() + ": " + review.getText() +
                    " (Likes: " + review.getLikes() + ", Dislikes: " + review.getDislikes() + ")";
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(displayText);
            reviewNodes.put(review.getId(), node);
        }

        // Строим дерево с учётом parent_id
        for (Review review : reviews) {
            DefaultMutableTreeNode node = reviewNodes.get(review.getId());
            if (review.getParentId() == null) {
                root.add(node);
            } else {
                DefaultMutableTreeNode parentNode = reviewNodes.get(review.getParentId());
                if (parentNode != null) {
                    // Добавляем информацию о том, кому это ответ
                    Review parentReview = reviews.stream()
                            .filter(r -> r.getId() == review.getParentId())
                            .findFirst()
                            .orElse(null);
                    if (parentReview != null) {
                        node.setUserObject("В ответ на @" + parentReview.getUserLogin() + ": " + review.getText() +
                                " (Likes: " + review.getLikes() + ", Dislikes: " + review.getDislikes() + ")");
                    }
                    parentNode.add(node);
                } else {
                    root.add(node); // Если родитель не найден, добавляем в корень
                }
            }
        }

        JTree reviewTree = new JTree(root);
        reviewTree.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane treeScrollPane = new JScrollPane(reviewTree);
        dialog.add(treeScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        if (currentUser != null) {
            JButton likeButton = new JButton("Like");
            JButton dislikeButton = new JButton("Dislike");
            JButton replyButton = new JButton("Reply");
            styleButton(likeButton);
            styleButton(dislikeButton);
            styleButton(replyButton);

            TreeSelectionListener selectionListener = e -> {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) reviewTree.getLastSelectedPathComponent();
                likeButton.setEnabled(selectedNode != null && selectedNode.getUserObject() != root.getUserObject());
                dislikeButton.setEnabled(selectedNode != null && selectedNode.getUserObject() != root.getUserObject());
                replyButton.setEnabled(selectedNode != null && selectedNode.getUserObject() != root.getUserObject());
            };
            reviewTree.addTreeSelectionListener(selectionListener);

            likeButton.addActionListener(e -> {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) reviewTree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.getUserObject() != root.getUserObject()) {
                    Review selectedReview = reviews.get(reviewNodes.values().stream().toList().indexOf(selectedNode));
                    try {
                        String existingReaction = store.getDb().getUserReaction(currentUser.getLogin(), selectedReview.getId());
                        if (existingReaction == null) {
                            store.getDb().saveReaction(currentUser.getLogin(), selectedReview.getId(), "LIKE");
                            selectedReview.setLikes(selectedReview.getLikes() + 1);
                            store.getDb().updateReviewLikes(selectedReview.getId(), selectedReview.getLikes(), selectedReview.getDislikes());
                            selectedNode.setUserObject("В ответ на @" + (selectedReview.getParentId() != null ? reviews.stream().filter(r -> r.getId() == selectedReview.getParentId()).findFirst().get().getUserLogin() : "") + ": " + selectedReview.getText() + " (Likes: " + selectedReview.getLikes() + ", Dislikes: " + selectedReview.getDislikes() + ")");
                            reviewTree.updateUI();
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Error liking review: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            dislikeButton.addActionListener(e -> {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) reviewTree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.getUserObject() != root.getUserObject()) {
                    Review selectedReview = reviews.get(reviewNodes.values().stream().toList().indexOf(selectedNode));
                    try {
                        String existingReaction = store.getDb().getUserReaction(currentUser.getLogin(), selectedReview.getId());
                        if (existingReaction == null) {
                            store.getDb().saveReaction(currentUser.getLogin(), selectedReview.getId(), "DISLIKE");
                            selectedReview.setDislikes(selectedReview.getDislikes() + 1);
                            store.getDb().updateReviewLikes(selectedReview.getId(), selectedReview.getLikes(), selectedReview.getDislikes());
                            selectedNode.setUserObject("В ответ на @" + (selectedReview.getParentId() != null ? reviews.stream().filter(r -> r.getId() == selectedReview.getParentId()).findFirst().get().getUserLogin() : "") + ": " + selectedReview.getText() + " (Likes: " + selectedReview.getLikes() + ", Dislikes: " + selectedReview.getDislikes() + ")");
                            reviewTree.updateUI();
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Error disliking review: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            replyButton.addActionListener(e -> {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) reviewTree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.getUserObject() != root.getUserObject()) {
                    Review selectedReview = reviews.get(reviewNodes.values().stream().toList().indexOf(selectedNode));
                    String replyText = JOptionPane.showInputDialog(this, "Enter your reply:");
                    if (replyText != null && !replyText.trim().isEmpty()) {
                        try {
                            store.addReview(bookId, currentUser.getLogin(), replyText, selectedReview.getId());
                            Review newReply = new Review(reviews.size() + 1, bookId, currentUser.getLogin(), replyText, 0, 0);
                            newReply.setParentId(selectedReview.getId());
                            reviews.add(newReply);
                            DefaultMutableTreeNode replyNode = new DefaultMutableTreeNode("В ответ на @" + selectedReview.getUserLogin() + ": " + replyText + " (Likes: 0, Dislikes: 0)");
                            selectedNode.add(replyNode);
                            reviewNodes.put(newReply.getId(), replyNode);
                            reviewTree.updateUI();
                        } catch (SQLException ex) {
                            JOptionPane.showMessageDialog(this, "Error replying to review: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });

            buttonPanel.add(likeButton);
            buttonPanel.add(dislikeButton);
            buttonPanel.add(replyButton);
        }

        JTextArea reviewArea = new JTextArea(3, 40);
        reviewArea.setLineWrap(true);
        reviewArea.setWrapStyleWord(true);
        JButton addReviewButton = new JButton("Add Review");
        styleButton(addReviewButton);
        addReviewButton.addActionListener(e -> {
            String text = reviewArea.getText().trim();
            if (!text.isEmpty()) {
                try {
                    store.addReview(bookId, currentUser.getLogin(), text, null);
                    Review newReview = new Review(reviews.size() + 1, bookId, currentUser.getLogin(), text, 0, 0);
                    reviews.add(newReview);
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(currentUser.getLogin() + ": " + text + " (Likes: 0, Dislikes: 0)");
                    root.add(newNode);
                    reviewNodes.put(newReview.getId(), newNode);
                    reviewTree.updateUI();
                    reviewArea.setText("");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error adding review: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(new JScrollPane(reviewArea), BorderLayout.CENTER);
        inputPanel.add(addReviewButton, BorderLayout.EAST);
        dialog.add(inputPanel, BorderLayout.SOUTH);
        dialog.add(buttonPanel, BorderLayout.NORTH);
        dialog.setVisible(true);
    }

    private void handleReaction(int bookId, JTable reviewTable, DefaultTableModel reviewModel, String reaction) {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please login to react", "Warning", JOptionPane.WARNING_MESSAGE);
            showLoginOrRegisterDialog();
            return;
        }

        int selectedRow = reviewTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a review to react to", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            List<Review> reviews = store.getDb().getReviews(bookId);
            int reviewId = reviews.get(selectedRow).getId();
            String userReaction = store.getDb().getUserReaction(currentUser.getLogin(), reviewId);
            if (userReaction != null) {
                JOptionPane.showMessageDialog(this, "You have already reacted to this review", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int likes = (int) reviewModel.getValueAt(selectedRow, 2);
            int dislikes = (int) reviewModel.getValueAt(selectedRow, 3);
            if (reaction.equals("LIKE")) {
                likes++;
            } else if (reaction.equals("DISLIKE")) {
                dislikes++;
            }
            store.getDb().saveReaction(currentUser.getLogin(), reviewId, reaction);
            store.getDb().updateReviewLikes(reviewId, likes, dislikes);
            loadReviews(bookId, reviewModel, null);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadReviews(int bookId, DefaultTableModel model, Integer parentId) {
        model.setRowCount(0);
        List<Review> reviews = store.getReviews(bookId);
        loadReviewHierarchy(reviews, model, parentId, 0);
    }

    private void loadReviewHierarchy(List<Review> reviews, DefaultTableModel model, Integer parentId, int depth) {
        for (Review review : reviews) {
            if ((parentId == null && review.getParentId() == null) || (parentId != null && parentId.equals(review.getParentId()))) {
                String indent = "  ".repeat(depth * 2);
                String displayText = review.getText();
                if (review.getParentId() != null) {
                    Review parentReview = reviews.stream()
                            .filter(r -> r.getId() == review.getParentId())
                            .findFirst()
                            .orElse(null);
                    if (parentReview != null) {
                        displayText = "<html><font color='gray' size='2'>Reply to " + parentReview.getUserLogin() + "</font><br>" + review.getText() + "</html>";
                    }
                }
                model.addRow(new Object[]{
                        review.getUserLogin(),
                        indent + displayText,
                        review.getLikes(),
                        review.getDislikes()
                });
                loadReviewHierarchy(review.getReplies(), model, review.getId(), depth + 1);
            }
        }
    }

    private void addReview(int bookId, DefaultTableModel reviewModel, Integer parentId) {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please login to leave a review", "Warning", JOptionPane.WARNING_MESSAGE);
            showLoginOrRegisterDialog();
            return;
        }

        JTextArea reviewText = new JTextArea(5, 20);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(new JLabel(parentId == null ? "Your Review:" : "Your Reply:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(reviewText), BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, parentId == null ? "Add Review" : "Add Reply", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String text = reviewText.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Review cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                store.addReview(bookId, currentUser.getLogin(), text, parentId);
                loadReviews(bookId, reviewModel, null);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding review: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
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
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setBackground(new Color(135, 206, 235));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(120, 40));
    }

    private void loadBooks(Category category, DefaultTableModel model) {
        try {
            java.sql.ResultSet rs = store.getDb().getBooks(category.getName());
            model.setRowCount(0);
            category.readBooks().clear();
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                String description = rs.getString("description");
                String coverPath = rs.getString("cover_path");
                category.createBook(id, name, price, description, coverPath);
                int rating = store.getDb().getBookRating(id);
                model.addRow(new Object[]{id, name, price, description, coverPath, rating, "Like", "Dislike"});
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
                    Image img = icon.getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH);
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

    private class ButtonRenderer implements TableCellRenderer {
        private final JButton button;

        ButtonRenderer(String text) {
            button = new JButton(text);
            button.setFont(new Font("Arial", Font.PLAIN, 12));
            button.setBackground(new Color(135, 206, 235));
            button.setForeground(Color.BLACK);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return button;
        }
    }

    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button;
        private final JTable table;
        private final BookStore store;
        private int row;

        ButtonEditor(JButton button, JTable table, BookStore store) {
            this.button = button;
            this.table = table;
            this.store = store;
            button.addActionListener(e -> {
                int bookId = (int) table.getValueAt(row, 0);
                String reaction = button.getText().toUpperCase();
                try {
                    if (currentUser == null) {
                        JOptionPane.showMessageDialog(MainWindow.this, "Please login to rate", "Warning", JOptionPane.WARNING_MESSAGE);
                        showLoginOrRegisterDialog();
                    } else if (store.getDb().getUserBookReaction(currentUser.getLogin(), bookId) != null) {
                        JOptionPane.showMessageDialog(MainWindow.this, "You have already rated this book", "Warning", JOptionPane.WARNING_MESSAGE);
                    } else {
                        store.getDb().saveBookReaction(currentUser.getLogin(), bookId, reaction);
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        loadBooks((Category) store.readCategories().stream().filter(c -> c.getName().equals(tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()))).findFirst().get(), model);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(MainWindow.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }
    }

    private class RatingRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = new JLabel();
            if (value != null) {
                int rating = (int) value;
                label.setText(String.valueOf(rating));
                if (rating > 0) {
                    label.setForeground(new Color(0, 100, 0));
                } else if (rating < 0) {
                    label.setForeground(Color.RED);
                } else {
                    label.setForeground(Color.GRAY);
                }
            } else {
                label.setText("0");
                label.setForeground(Color.GRAY);
            }
            label.setHorizontalAlignment(JLabel.CENTER);
            return label;
        }
    }
}