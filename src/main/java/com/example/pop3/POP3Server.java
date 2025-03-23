package com.example.pop3;

import java.net.ServerSocket;
import java.net.Socket;

public class POP3Server {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(110)) {
            System.out.println("POP3 Server is running on port 110...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client USERonnected: " + clientSocket.getInetAddress());
                // Handle client connection in a new thread
                new Thread(new POP3ClientHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}