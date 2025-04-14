package com.example.smtp;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.rmi.Naming;
import com.example.auth.AuthService;

public class SMTPServer {
    private static final int PORT = 25;
    private static final int MAX_THREADS = 2;  // Limit concurrent connections
    private static final AtomicInteger clientCounter = new AtomicInteger(0);  // Track client count
    private static AuthService authService;  // Declare the AuthService reference

    public static void main(String[] args) {
        // Initialize the thread pool for handling client connections
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

        try {
            // Lookup the RMI AuthService once at the server startup
            authService = (AuthService) Naming.lookup("rmi://localhost/AuthService");
            System.out.println("Successfully connected to AuthService.");

            // Create server socket to listen on port 25 for SMTP connections
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("SMTP Server is running on port " + PORT + "...");

                while (true) {
                    // Accept incoming client connections
                    Socket clientSocket = serverSocket.accept();
                    int clientNumber = clientCounter.incrementAndGet();  // Get and increment client count

                    System.out.println("Client #" + clientNumber + " connected: " + clientSocket.getInetAddress());

                    // Assign client connection to a thread from the pool
                    threadPool.execute(new SMTPClientHandler(clientSocket, authService));  // Pass authService to the handler
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown(); // Ensure the thread pool is closed when the server stops
        }
    }
}
