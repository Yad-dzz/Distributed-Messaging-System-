package com.example.auth;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.json.JSONArray;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {
    private static final Logger LOGGER = Logger.getLogger(AuthServiceImpl.class.getName());
    private static final String ACCOUNTS_FILE = "accounts.json";
    private Map<String, String> userCredentials;

    public AuthServiceImpl() throws RemoteException {
        super();
        userCredentials = new HashMap<>();
        loadAccounts();
    }

    private void loadAccounts() {
        try {
            File file = new File(ACCOUNTS_FILE);
            if (!file.exists()) {
                // Créer un fichier avec des comptes par défaut si le fichier n'existe pas
                userCredentials.put("admin", "password");
                userCredentials.put("user1", "pass1");
                userCredentials.put("user2", "pass2");
                saveAccounts();
            } else {
                // Charger les comptes à partir du fichier JSON
                String content = Files.readString(Paths.get(ACCOUNTS_FILE));
                JSONObject json = new JSONObject(content);
                JSONArray users = json.getJSONArray("users");

                for (int i = 0; i < users.length(); i++) {
                    JSONObject user = users.getJSONObject(i);
                    userCredentials.put(user.getString("username"), user.getString("password"));
                }

                LOGGER.info("Comptes utilisateurs chargés avec succès.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des comptes utilisateurs", e);
        }
    }

    private synchronized void saveAccounts() {
        try {
            JSONObject root = new JSONObject();
            JSONArray users = new JSONArray();

            for (Map.Entry<String, String> entry : userCredentials.entrySet()) {
                JSONObject user = new JSONObject();
                user.put("username", entry.getKey());
                user.put("password", entry.getValue());
                users.put(user);
            }

            root.put("users", users);

            Files.writeString(Paths.get(ACCOUNTS_FILE), root.toString(2));

            // Création des dossiers pour les utilisateurs si nécessaire
            for (String username : userCredentials.keySet()) {
                createUserDirectory(username);
            }

            LOGGER.info("Comptes utilisateurs sauvegardés avec succès.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la sauvegarde des comptes utilisateurs", e);
        }
    }

    private void createUserDirectory(String username) {
        try {
            String path = "mailserver/" + username;
            Files.createDirectories(Paths.get(path));
            LOGGER.info("Répertoire créé pour l'utilisateur: " + path);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Impossible de créer le répertoire pour l'utilisateur: " + username, e);
        }
    }

    @Override
    public synchronized boolean authenticate(String username, String password) throws RemoteException {
        if (username == null || password == null) {
            LOGGER.warning("Tentative d'authentification avec des identifiants nuls");
            return false;
        }

        LOGGER.info("Tentative d'authentification pour l'utilisateur: " + username);

        // Vérifier si l'utilisateur existe et si le mot de passe correspond
        if (userCredentials.containsKey(username)) {
            boolean authenticated = userCredentials.get(username).equals(password);
            LOGGER.info("Authentification " + (authenticated ? "réussie" : "échouée") +
                    " pour l'utilisateur: " + username);
            return authenticated;
        }

        LOGGER.info("Utilisateur inconnu: " + username);
        return false;
    }

    @Override
    public synchronized boolean createAccount(String username, String password) throws RemoteException {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            LOGGER.warning("Tentative de création de compte avec des identifiants invalides");
            return false;
        }

        if (userCredentials.containsKey(username)) {
            LOGGER.warning("Tentative de création d'un compte existant: " + username);
            return false;
        }

        userCredentials.put(username, password);
        saveAccounts();

        LOGGER.info("Compte créé pour l'utilisateur: " + username);
        return true;
    }

    @Override
    public synchronized boolean updateAccount(String username, String oldPassword, String newPassword) throws RemoteException {
        if (username == null || oldPassword == null || newPassword == null) {
            LOGGER.warning("Tentative de mise à jour de compte avec des identifiants invalides");
            return false;
        }

        if (!authenticate(username, oldPassword)) {
            LOGGER.warning("Échec de la mise à jour du compte, authentification échouée pour: " + username);
            return false;
        }

        userCredentials.put(username, newPassword);
        saveAccounts();

        LOGGER.info("Compte mis à jour pour l'utilisateur: " + username);
        return true;
    }
    @Override
    public synchronized boolean deleteAccount(String username, String password) throws RemoteException {
        if (username == null || password == null) {
            LOGGER.warning("Tentative de suppression de compte avec des identifiants invalides");
            return false;
        }

        if (!authenticate(username, password)) {
            LOGGER.warning("Échec de la suppression du compte, authentification échouée pour: " + username);
            return false;
        }

        userCredentials.remove(username);
        saveAccounts();

        // Supprimer le dossier de l'utilisateur
        deleteUserDirectory(username);

        LOGGER.info("Compte et répertoire supprimés pour l'utilisateur: " + username);
        return true;
    }



    @Override
    public boolean userExists(String username) throws RemoteException {
        boolean exists = userCredentials.containsKey(username);
        LOGGER.info("Vérification de l'existence de l'utilisateur " + username + ": " + exists);
        return exists;
    }

    @Override
    public List<String> getAllUsers() throws RemoteException {
        return new ArrayList<>(userCredentials.keySet());
    }


    private void deleteUserDirectory(String username) {
        String path = "mailserver/" + username;
        try {
            File userDir = new File(path);
            if (userDir.exists() && userDir.isDirectory()) {
                deleteRecursively(userDir);
                LOGGER.info("Répertoire supprimé pour l'utilisateur: " + username);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur lors de la suppression du répertoire de l'utilisateur: " + username, e);
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

}



