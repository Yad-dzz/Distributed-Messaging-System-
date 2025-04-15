package com.example.pop3;

import com.example.auth.AuthService;

import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class POP3Server {
    private static final int PORT = 110;
    private static final int MAX_THREADS = 10; // Limit concurrent connections
    private static final AtomicInteger clientCounter = new AtomicInteger(0); // Track client count
    private static AuthService authService;  // Declare the AuthService reference

    public static void main(String[] args) throws MalformedURLException, NotBoundException, RemoteException {
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        authService = (AuthService) Naming.lookup("rmi://localhost/AuthService");
        System.out.println("Successfully connected to AuthService.");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("POP3 Server is running on port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientNumber = clientCounter.incrementAndGet(); // Get and increment client count

                System.out.println("Client #" + clientNumber + " connected: " + clientSocket.getInetAddress());

                // Assign client connection to a thread from the pool
                threadPool.execute(new POP3ClientHandler(clientSocket, clientNumber,authService));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown(); // Ensure the thread pool is closed when the server stops
        }
    }
}
