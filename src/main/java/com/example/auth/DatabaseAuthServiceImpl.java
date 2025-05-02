package com.example.auth;

import com.example.database.DatabaseManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseAuthServiceImpl extends UnicastRemoteObject implements AuthService {
    private static final Logger LOGGER = Logger.getLogger(DatabaseAuthServiceImpl.class.getName());
    private final DatabaseManager dbManager;

    public DatabaseAuthServiceImpl() throws RemoteException {
        super();
        this.dbManager = new DatabaseManager();
    }

    @Override
    public boolean authenticate(String username, String password) throws RemoteException {
        if (username == null || password == null) {
            LOGGER.warning("Tentative d'authentification avec des identifiants nuls");
            return false;
        }

        LOGGER.info("Tentative d'authentification pour l'utilisateur: " + username);
        return dbManager.authenticateUser(username, password);
    }

    @Override
    public boolean createAccount(String username, String password) throws RemoteException {
        if (username == null || password == null) {
            LOGGER.warning("Tentative de création de compte avec des identifiants nuls");
            return false;
        }

        try {
            // Check if user already exists
            if (dbManager.authenticateUser(username, "dummy")) {
                LOGGER.warning("Tentative de création d'un compte existant: " + username);
                return false;
            }

            // Create new user with hashed password (using the same password for both clear and hash for now)
            dbManager.updatePassword(username, password, password);
            LOGGER.info("Nouveau compte créé: " + username);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du compte: " + username, e);
            return false;
        }
    }

    @Override
    public boolean updateAccount(String username, String oldPassword, String newPassword) throws RemoteException {
        if (username == null || oldPassword == null || newPassword == null) {
            LOGGER.warning("Tentative de mise à jour avec des identifiants nuls");
            return false;
        }

        try {
            // Verify old password
            if (!dbManager.authenticateUser(username, oldPassword)) {
                LOGGER.warning("Mot de passe incorrect pour la mise à jour du compte: " + username);
                return false;
            }

            // Update password
            dbManager.updatePassword(username, newPassword, newPassword);
            LOGGER.info("Mot de passe mis à jour pour le compte: " + username);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour du compte: " + username, e);
            return false;
        }
    }

    @Override
    public boolean deleteAccount(String username, String password) throws RemoteException {
        if (username == null || password == null) {
            LOGGER.warning("Tentative de suppression avec des identifiants nuls");
            return false;
        }

        try {
            // Verify password
            if (!dbManager.authenticateUser(username, password)) {
                LOGGER.warning("Mot de passe incorrect pour la suppression du compte: " + username);
                return false;
            }

            // Mark the account as inactive
            boolean success = dbManager.deleteUser(username);
            if (success) {
                LOGGER.info("Compte marqué comme supprimé: " + username);
            } else {
                LOGGER.warning("Échec de la suppression du compte: " + username);
            }
            return success;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la suppression du compte: " + username, e);
            return false;
        }
    }

    @Override
    public boolean userExists(String username) throws RemoteException {
        if (username == null) {
            LOGGER.warning("Tentative de vérification d'existence avec un nom d'utilisateur nul");
            return false;
        }

        try {
            // Try to authenticate with a dummy password to check if user exists
            return dbManager.authenticateUser(username, "dummy");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification de l'existence du compte: " + username, e);
            return false;
        }
    }

    @Override
    public List<String> getAllUsers() throws RemoteException {
        try {
            return dbManager.getAllUsers();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération de la liste des utilisateurs", e);
            return new ArrayList<>();
        }
    }
} 