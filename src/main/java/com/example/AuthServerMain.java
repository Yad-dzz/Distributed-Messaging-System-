package com.example;

import com.example.auth.AuthServiceImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AuthServerMain {
    public static void main(String[] args) {
        try {
            AuthServiceImpl authService = new AuthServiceImpl();
            Registry registry = LocateRegistry.createRegistry(1099); // default RMI port
            registry.rebind("AuthService", authService);
            System.out.println("🔐 AuthService RMI Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}