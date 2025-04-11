package com.example.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {

    private static final String USERS_FILE = "server/users.json";
    private Map<String, String> users;
    private Gson gson = new Gson();

    public AuthServiceImpl() throws RemoteException {
        super();
        loadUsers();
    }

    private void loadUsers() {
        try {
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                users = new HashMap<>();
                saveUsers(); // Create the file
                return;
            }
            FileReader reader = new FileReader(file);
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            users = gson.fromJson(reader, type);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            users = new HashMap<>();
        }
    }

    private void saveUsers() {
        try (FileWriter writer = new FileWriter(USERS_FILE)) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized boolean login(String username, String password) throws RemoteException {
        return users.containsKey(username) && users.get(username).equals(password);
    }

    @Override
    public synchronized boolean createAccount(String username, String password) throws RemoteException {
        if (users.containsKey(username)) return false;
        users.put(username, password);
        saveUsers();
        return true;
    }

    @Override
    public synchronized boolean updatePassword(String username, String newPassword) throws RemoteException {
        if (!users.containsKey(username)) return false;
        users.put(username, newPassword);
        saveUsers();
        return true;
    }

    @Override
    public synchronized boolean deleteAccount(String username) throws RemoteException {
        if (!users.containsKey(username)) return false;
        users.remove(username);
        saveUsers();
        return true;
    }
}
