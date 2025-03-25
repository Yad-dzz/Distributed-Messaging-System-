package com.example.smtp;

import java.io.*;
import java.net.Socket;

public class SMTPClientMulti {
    private static final String SMTP_SERVER = "localhost"; // Change if needed
    private static final int SMTP_PORT = 25;

    public static void main(String[] args) {
        String[][] clients = {
                {"0", "loucif@gmail.com"},
                {"1", "kenef@gmail.com"},
                {"2", "lahgui@gmail.com"}
        };

        String recipient = "merabet@gmail.com";

        for (String[] client : clients) {
            int clientId = Integer.parseInt(client[0]);
            String sender = client[1];

            new Thread(() -> sendEmail(clientId, sender, recipient)).start();
        }
    }

    private static void sendEmail(int clientId, String sender, String recipient) {
        try (Socket socket = new Socket(SMTP_SERVER, SMTP_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Client " + clientId + " received: " + reader.readLine());

            String[] commands = {
                    "HELO client" + clientId,
                    "MAIL FROM: <" + sender + ">",
                    "RCPT TO: <" + recipient + ">",
                    "DATA",
                    " Test Email from Client " ,
                    ".",  // End DATA command
                    "QUIT"
            };

            for (String cmd : commands) {
                System.out.println("Client " + clientId + " sending: " + cmd);
                writer.println(cmd);
                writer.flush();
                String response = reader.readLine();
                System.out.println("Client " + clientId + " received: " + response);
            }
        } catch (IOException e) {
            System.err.println("Client " + clientId + " error: " + e.getMessage());
        }
    }
}
