package com.example.smtp;
import java.net.ServerSocket;
import java.net.Socket;

public class SMTPServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(25)) {
            System.out.println("SMTP Server is running on port 25...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                // Handle client connection in a new thread
                new Thread(new SMTPClientHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}