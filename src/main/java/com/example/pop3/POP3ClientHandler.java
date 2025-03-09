package com.example.pop3;

import java.io.*;
import java.net.Socket;

public class POP3ClientHandler implements Runnable {
    private Socket clientSocket;

    public POP3ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            out.println("+OK POP3 Server Ready");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received: " + inputLine);
                // Process POP3 commands here
                if (inputLine.startsWith("USER")) {
                    out.println("+OK");
                } else if (inputLine.startsWith("PASS")) {
                    out.println("+OK");
                } else if (inputLine.startsWith("STAT")) {
                    out.println("+OK 0 0");
                } else if (inputLine.startsWith("LIST")) {
                    out.println("+OK 0 messages");
                } else if (inputLine.startsWith("RETR")) {
                    out.println("+OK");
                } else if (inputLine.startsWith("DELE")) {
                    out.println("+OK");
                } else if (inputLine.startsWith("QUIT")) {
                    out.println("+OK");
                    break;
                } else {
                    out.println("-ERR Command not recognized");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}