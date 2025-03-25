package com.example.smtp;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SMTPClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private int clientNumber;  // Added client number tracking

    private String sender = null;
    private List<String> recipients = new ArrayList<>();
    private StringBuilder emailContent = new StringBuilder();
    private boolean receivingData = false;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^<([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})>$");

    private enum State {
        WAITING_FOR_HELO_EHLO,
        WAITING_FOR_MAIL_FROM,
        WAITING_FOR_RCPT_TO,
        WAITING_FOR_DATA,
        RECEIVING_DATA,
        COMPLETED
    }

    private State currentState;

    // Modified constructor to accept clientNumber
    public SMTPClientHandler(Socket socket, int clientNumber) {
        this.clientSocket = socket;
        this.clientNumber = clientNumber;  // Store client number
        this.currentState = State.WAITING_FOR_HELO_EHLO;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            System.out.println("Handling Client #" + clientNumber); // Log client number
            out.println("220 SMTP Server Ready - Client #" + clientNumber);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Client #" + clientNumber + " Sent: " + inputLine);
                handleSMTPCommand(inputLine.trim());
                if (currentState == State.COMPLETED) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Client #" + clientNumber + " disconnected.");
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSMTPCommand(String command) {
        if (command.isEmpty()) {
            out.println("500 Empty command");
            return;
        }
        command = command.toUpperCase();

        switch (currentState) {
            case WAITING_FOR_HELO_EHLO:
                if (command.startsWith("HELO")) {
                    out.println("250 Hello");
                    currentState = State.WAITING_FOR_MAIL_FROM;
                }
                else if (command.startsWith("EHLO")) {
                    out.println("250-Hello");
                    out.println("250-8BITMIME");
                    out.println("250-SIZE 10485760");
                    out.println("250 HELP");
                    currentState = State.WAITING_FOR_MAIL_FROM;
                } else {
                    out.println("503 Bad sequence of commands. Expected HELO or EHLO");
                }
                break;

            case WAITING_FOR_MAIL_FROM:
                if (command.startsWith("MAIL FROM:")) {
                    sender = extractEmail(command);
                    if (sender == null) {
                        out.println("501 Syntax error in MAIL FROM address");
                    } else {
                        out.println("250 OK");
                        currentState = State.WAITING_FOR_RCPT_TO;
                    }
                }
                else {
                    out.println("503 Bad sequence of commands. Expected MAIL FROM");
                }
                break;

            case WAITING_FOR_RCPT_TO:
                if (command.startsWith("RCPT TO:")) {
                    String recipient = extractEmail(command);
                    if (recipient == null) {
                        out.println("501 Syntax error in RCPT TO address");
                    } else if (Files.exists(Paths.get("mailserver/" + recipient))) {
                        recipients.add(recipient);
                        out.println("250 OK - User Exists");
                    } else {
                        out.println("550 No such user");
                    }
                } else if (command.startsWith("DATA")) {
                    if (!recipients.isEmpty()) {
                        out.println("354 Start mail input; end with <CRLF>.<CRLF>");
                        emailContent.setLength(0);
                        receivingData = true;
                        currentState = State.RECEIVING_DATA;
                    } else {
                        out.println("554 Transaction failed - No valid recipients");
                    }
                } else {
                    out.println("503 Bad sequence of commands. Expected RCPT TO or DATA");
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
        String extracted = command.replaceAll(".*<|>", "").trim();
        if (!EMAIL_PATTERN.matcher("<" + extracted + ">").matches()) {
            return null;
        }
        return extracted;
    }
}
