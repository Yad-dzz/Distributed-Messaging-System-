package com.example.smtp;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SMTPClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private String sender = null;
    private String recipient = null;
    private StringBuilder emailContent = new StringBuilder();
    private boolean receivingData = false;

    // Définition des états de l'automate
    private enum State {
        WAITING_FOR_HELO,
        WAITING_FOR_MAIL_FROM,
        WAITING_FOR_RCPT_TO,
        WAITING_FOR_DATA,
        RECEIVING_DATA,
        COMPLETED
    }

    private State currentState;

    public SMTPClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.currentState = State.WAITING_FOR_HELO; // L'automate commence en attente de HELO
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            out.println("220 SMTP Server Ready");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received: " + inputLine);
                handleSMTPCommand(inputLine);
                if (currentState == State.COMPLETED) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSMTPCommand(String command) {
        switch (currentState) {
            case WAITING_FOR_HELO:
                if (command.startsWith("HELO")) {
                    out.println("250 Hello");
                    currentState = State.WAITING_FOR_MAIL_FROM;
                } else {
                    out.println("503 Expected HELO first");
                }
                break;

            case WAITING_FOR_MAIL_FROM:
                if (command.startsWith("MAIL FROM:")) {
                    sender = extractEmail(command);
                    out.println("250 OK");
                    currentState = State.WAITING_FOR_RCPT_TO;
                } else {
                    out.println("503 Expected MAIL FROM");
                }
                break;

            case WAITING_FOR_RCPT_TO:
                if (command.startsWith("RCPT TO:")) {
                    recipient = extractEmail(command);
                    if (Files.exists(Paths.get("mailserver/" + recipient))) {
                        out.println("250 OK - User Exists");
                        currentState = State.WAITING_FOR_DATA;
                    } else {
                        out.println("550 No such user");
                    }
                } else {
                    out.println("503 Expected RCPT TO");
                }
                break;

            case WAITING_FOR_DATA:
                if (command.startsWith("DATA")) {
                    out.println("354 Start mail input; end with <CRLF>.<CRLF>");
                    emailContent.setLength(0); // Réinitialise le contenu du mail
                    receivingData = true;
                    currentState = State.RECEIVING_DATA;
                } else {
                    out.println("503 Expected DATA");
                }
                break;

            case RECEIVING_DATA:
                if (command.equals(".")) {
                    saveEmail();
                    out.println("250 OK - Email Stored");
                    currentState = State.COMPLETED;
                } else {
                    emailContent.append(command).append("\n");
                }
                break;

            case COMPLETED:
                if (command.startsWith("QUIT")) {
                    out.println("221 Bye");
                } else {
                    out.println("503 Session closing");
                }
                break;

            default:
                out.println("500 Unknown state");
                break;
        }
    }

    private void saveEmail() {
        if (sender == null || recipient == null || emailContent.length() == 0) return;

        String userDir = "mailserver/" + recipient;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = userDir + "/" + timestamp + ".txt";

        try {
            Files.createDirectories(Paths.get(userDir));
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                writer.write("From: " + sender + "\n");
                writer.write("To: " + recipient + "\n");
                writer.write("Date: " + new Date() + "\n");
                writer.write("Subject: (No Subject)\n\n");
                writer.write(emailContent.toString());
            }
            System.out.println("Email saved to: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractEmail(String command) {
        return command.replaceAll(".*<|>", "").trim(); // Extrait l'email du format SMTP
    }
}
