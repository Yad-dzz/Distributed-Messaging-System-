package com.example.smtp;

import java.io.*;
import java.net.Socket;

public class SMTPClientHandler implements Runnable {
    private Socket clientSocket;

    public SMTPClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            out.println("220 SMTP Server Ready");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received: " + inputLine);
                // Process SMTP commands here
                if (inputLine.startsWith("HELO")) {
                    out.println("250 Hello");
                } else if (inputLine.startsWith("MAIL FROM")) {
                    out.println("250 OK");
                } else if (inputLine.startsWith("RCPT TO")) {
                    out.println("250 OK");
                } else if (inputLine.startsWith("DATA")) {
                    out.println("354 Start mail input; end with <CRLF>.<CRLF>");
                } else if (inputLine.equals(".")) {
                    out.println("250 OK");
                } else if (inputLine.startsWith("QUIT")) {
                    out.println("221 Bye");
                    break;
                } else {
                    out.println("500 Command not recognized");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}