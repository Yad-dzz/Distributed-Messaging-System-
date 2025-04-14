package com.example.auth;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface AuthService extends Remote {
    // Authentification d'un utilisateur
    boolean authenticate(String username, String password) throws RemoteException;

    // Création d'un compte utilisateur
    boolean createAccount(String username, String password) throws RemoteException;

    // Mise à jour d'un compte utilisateur (mot de passe)
    boolean updateAccount(String username, String oldPassword, String newPassword) throws RemoteException;

    // Suppression d'un compte utilisateur
    boolean deleteAccount(String username, String password) throws RemoteException;

    // Vérification de l'existence d'un utilisateur (pour SMTP - MAIL FROM)
    boolean userExists(String username) throws RemoteException;

    // Récupération de la liste des utilisateurs (pour l'interface client)
    List<String> getAllUsers() throws RemoteException;
}