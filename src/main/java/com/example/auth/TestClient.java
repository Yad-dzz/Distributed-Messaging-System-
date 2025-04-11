package com.example.auth;

import com.example.auth.AuthService;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestClient {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            AuthService service = (AuthService) registry.lookup("AuthService");

            System.out.println("Account created: " + service.createAccount("alice", "1234"));
            System.out.println("Login success: " + service.login("alice", "1234"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
