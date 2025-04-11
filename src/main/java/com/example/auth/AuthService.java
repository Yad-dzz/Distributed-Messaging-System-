package com.example.auth;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthService extends Remote {

    /**
     * Authenticates a user with username and password.
     * @param username The user's username.
     * @param password The user's password.
     * @return true if credentials are correct, false otherwise.
     * @throws RemoteException if there is a network or server error.
     */
    boolean login(String username, String password) throws RemoteException;

    boolean createAccount(String username, String password) throws RemoteException;

    boolean updatePassword(String username, String newPassword) throws RemoteException;


    boolean deleteAccount(String username) throws RemoteException;
}
