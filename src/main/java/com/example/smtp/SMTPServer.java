package com.example.smtp;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SMTPServer {
    private static final int PORT = 25;
    private static final int MAX_THREADS = 2;  // Limit concurrent connections
    private static final AtomicInteger clientCounter = new AtomicInteger(0);  // Track client count

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("SMTP Server is running on port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientNumber = clientCounter.incrementAndGet();  // Get and increment client count

                System.out.println("Client #" + clientNumber + " connected: " + clientSocket.getInetAddress());

                // Assign client connection to a thread from the pool
                threadPool.execute(new SMTPClientHandler(clientSocket, clientNumber));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown(); // Ensure the thread pool is closed when the server stops
        }
    }
}
