package com.example.smtp;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class SMTPClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private String sender = null;
    private List<String> recipients = new ArrayList<>();
    private StringBuilder emailContent = new StringBuilder();
    private boolean receivingData = false;

    // Définition des états de l'automate
    private enum State {
        WAITING_FOR_HELO_EHLO,
        WAITING_FOR_MAIL_FROM,
        WAITING_FOR_RCPT_TO,
        WAITING_FOR_DATA,
        RECEIVING_DATA,
        COMPLETED
    }

    private State currentState;

    public SMTPClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.currentState = State.WAITING_FOR_HELO_EHLO; // L'automate commence en attente de HELO/EHLO
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
                handleSMTPCommand(inputLine.toUpperCase()); // Convert command to uppercase
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
            case WAITING_FOR_HELO_EHLO:
                if (command.startsWith("HELO")) {
                    out.println("250 Hello");
                    currentState = State.WAITING_FOR_MAIL_FROM;
                } else if (command.startsWith("EHLO")) {
                    out.println("250-Hello");
                    out.println("250-8BITMIME");
                    out.println("250-SIZE 10485760");
                    out.println("250 HELP");
                    currentState = State.WAITING_FOR_MAIL_FROM;
                } else {
                    out.println("503 Bad sequence of commands");
                }
                break;

            case WAITING_FOR_MAIL_FROM:
                if (command.startsWith("MAIL FROM:")) {
                    sender = extractEmail(command);
                    out.println("250 OK");
                    currentState = State.WAITING_FOR_RCPT_TO;
                } else {
                    out.println("503 Bad sequence of commands");
                }
                break;

            case WAITING_FOR_RCPT_TO:
                if (command.startsWith("RCPT TO:")) {
                    String recipient = extractEmail(command);
                    if (Files.exists(Paths.get("mailserver/" + recipient))) {
                        recipients.add(recipient);
                        out.println("250 OK - User Exists");
                    } else {
                        out.println("550 No such user");
                    }
                } else if (command.startsWith("DATA")) {
                    if (!recipients.isEmpty()) {
                        out.println("354 Start mail input; end with <CRLF>.<CRLF>");
                        emailContent.setLength(0); // Réinitialise le contenu du mail
                        receivingData = true;
                        currentState = State.RECEIVING_DATA;
                    } else {
                        out.println("554 Transaction failed - No valid recipients");
                    }
                } else {
                    out.println("503 Bad sequence of commands");
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
                    currentState = State.COMPLETED;
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
        if (sender == null || recipients.isEmpty() || emailContent.length() == 0) return;

        for (String recipient : recipients) {
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
    }

    private String extractEmail(String command) {
        return command.replaceAll(".*<|>", "").trim(); // Extrait l'email du format SMTP
    }
}