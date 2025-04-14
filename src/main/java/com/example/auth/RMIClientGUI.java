package com.example.auth;

import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RMIClientGUI extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(RMIClientGUI.class.getName());
    private AuthService authService;

    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JTextField usernameField, passwordField, newPasswordField;
    private JButton createButton, updateButton, deleteButton, refreshButton;

    public RMIClientGUI() {
        super("Gestion des comptes utilisateurs");

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthService) registry.lookup("AuthService");

            LOGGER.info("Connecté au service d'authentification RMI");
            initComponents();
            refreshUserList();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion au service RMI", e);
            JOptionPane.showMessageDialog(this,
                    "Impossible de se connecter au service d'authentification RMI: " + e.getMessage(),
                    "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initComponents() {
        // Configuration de la fenêtre
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // Création du panneau principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panneau pour la liste des utilisateurs
        JPanel userListPanel = new JPanel(new BorderLayout());
        userListPanel.setBorder(BorderFactory.createTitledBorder("Liste des utilisateurs"));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && userList.getSelectedValue() != null) {
                usernameField.setText(userList.getSelectedValue());
                passwordField.setText("");
                newPasswordField.setText("");
            }
        });

        JScrollPane scrollPane = new JScrollPane(userList);
        refreshButton = new JButton("Rafraîchir");
        refreshButton.addActionListener(e -> refreshUserList());

        userListPanel.add(scrollPane, BorderLayout.CENTER);
        userListPanel.add(refreshButton, BorderLayout.SOUTH);

        // Panneau pour la gestion des comptes
        JPanel accountPanel = new JPanel(new GridBagLayout());
        accountPanel.setBorder(BorderFactory.createTitledBorder("Gestion des comptes"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Champs pour les informations du compte
        gbc.gridx = 0;
        gbc.gridy = 0;
        accountPanel.add(new JLabel("Nom d'utilisateur:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        accountPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        accountPanel.add(new JLabel("Mot de passe:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        accountPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        accountPanel.add(new JLabel("Nouveau mot de passe:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        newPasswordField = new JPasswordField(20);
        accountPanel.add(newPasswordField, gbc);

        // Boutons pour les actions
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        createButton = new JButton("Créer");
        createButton.addActionListener(e -> createAccount());

        updateButton = new JButton("Mettre à jour");
        updateButton.addActionListener(e -> updateAccount());

        deleteButton = new JButton("Supprimer");
        deleteButton.addActionListener(e -> deleteAccount());

        buttonPanel.add(createButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        // Ajout des panneaux au panneau principal
        mainPanel.add(userListPanel, BorderLayout.WEST);
        mainPanel.add(accountPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void refreshUserList() {
        try {
            List<String> users = authService.getAllUsers();
            userListModel.clear();
            for (String user : users) {
                userListModel.addElement(user);
            }
            LOGGER.info("Liste des utilisateurs rafraîchie: " + users.size() + " utilisateurs");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération de la liste des utilisateurs", e);
            JOptionPane.showMessageDialog(this,
                    "Impossible de récupérer la liste des utilisateurs: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createAccount() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Le nom d'utilisateur et le mot de passe sont obligatoires",
                    "Champs manquants", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean success = authService.createAccount(username, password);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Compte créé avec succès pour " + username,
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                refreshUserList();
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Impossible de créer le compte. L'utilisateur existe peut-être déjà.",
                        "Échec", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du compte", e);
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de la création du compte: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateAccount() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String newPassword = newPasswordField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Tous les champs sont obligatoires pour mettre à jour un compte",
                    "Champs manquants", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean success = authService.updateAccount(username, password, newPassword);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Mot de passe mis à jour avec succès pour " + username,
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Impossible de mettre à jour le compte. Vérifiez le nom d'utilisateur et le mot de passe.",
                        "Échec", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour du compte", e);
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de la mise à jour du compte: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteAccount() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Le nom d'utilisateur et le mot de passe sont obligatoires pour supprimer un compte",
                    "Champs manquants", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Êtes-vous sûr de vouloir supprimer le compte " + username + "?",
                "Confirmation de suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = authService.deleteAccount(username, password);
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "Compte supprimé avec succès: " + username,
                            "Succès", JOptionPane.INFORMATION_MESSAGE);
                    refreshUserList();
                    clearFields();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Impossible de supprimer le compte. Vérifiez le nom d'utilisateur et le mot de passe.",
                            "Échec", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la suppression du compte", e);
                JOptionPane.showMessageDialog(this,
                        "Erreur lors de la suppression du compte: " + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        newPasswordField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RMIClientGUI gui = new RMIClientGUI();
            gui.setVisible(true);
        });
    }
}