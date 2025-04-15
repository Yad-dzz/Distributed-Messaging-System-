package com.example.pop3;

import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import com.example.auth.AuthService;

public class POP3ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String user;
    private AuthService authService;

    private int clientNumber;
    private boolean authenticated;
    private List<File> emails;
    private List<File> markedForDeletion;
    private String timestamp; // Pour APOP

    public POP3ClientHandler(Socket socket, int clientNumber, AuthService authService) {
        this.clientSocket = socket;
        this.authenticated = false;
        this.emails = new ArrayList<>();
        this.markedForDeletion = new ArrayList<>();
        this.clientNumber = clientNumber;
        this.timestamp = "<" + System.currentTimeMillis() + "@mailsystem>"; // Timestamp pour APOP
        this.authService = authService;
    }


    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Envoyer une réponse initiale au client avec le timestamp pour APOP
            out.println("+OK POP3 server ready " + timestamp);

            // Lire les commandes du client
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Client #" + clientNumber + " Sent: " + inputLine);


                // Normaliser la commande pour être insensible à la casse
                String normalizedInput = inputLine.trim().toUpperCase();

                // Séparer la commande et ses arguments
                String[] parts = normalizedInput.split("\\s+", 2); // Séparer en 2 parties : commande et arguments
                String command = parts[0];
                String arguments = (parts.length > 1) ? parts[1] : "";

                // Valider et traiter les commandes
                if (command.equals("USER")) {
                    if (arguments.isEmpty()) {
                        out.println("-ERR Invalid syntax: Usage: USER <username>");
                    } else {
                        handleUser(inputLine);
                    }
                } else if (command.equals("PASS")) {
                    if (arguments.isEmpty()) {
                        out.println("-ERR Invalid syntax: Usage: PASS <password>");
                    } else {
                        handlePass(inputLine);
                    }
                } else if (command.equals("APOP")) {
                    if (arguments.isEmpty()) {
                        out.println("-ERR Invalid syntax: Usage: APOP <username> <digest>");
                    } else {
                        handleApop(inputLine); // Conserver l'input original pour le digest
                    }
                } else if (command.equals("STAT")) {
                    if (!arguments.isEmpty()) {
                        out.println("-ERR Invalid syntax: Usage: STAT");
                    } else {
                        handleStat();
                    }
                } else if (command.equals("LIST")) {
                    if (arguments.isEmpty()) {
                        handleList(""); // LIST sans arguments
                    } else {
                        handleList(inputLine); // LIST avec un numéro de message
                    }
                } else if (command.equals("RETR")) {
                    if (arguments.isEmpty()) {
                        out.println("-ERR Invalid syntax: Usage: RETR <message_number>");
                    } else {
                        handleRetr(inputLine);
                    }
                } else if (command.equals("DELE")) {
                    if (arguments.isEmpty()) {
                        out.println("-ERR Invalid syntax: Usage: DELE <message_number>");
                    } else {
                        handleDele(inputLine);
                    }
                } else if (command.equals("NOOP")) {
                    if (!arguments.isEmpty()) {
                        out.println("-ERR Invalid syntax: Usage: NOOP");
                    } else {
                        handleNoop();
                    }
                } else if (command.equals("RSET")) {
                    if (!arguments.isEmpty()) {
                        out.println("-ERR Invalid syntax: Usage: RSET");
                    } else {
                        handleRset();
                    }
                } else if (command.equals("QUIT")) {
                    if (!arguments.isEmpty()) {
                        out.println("-ERR Invalid syntax: Usage: QUIT");
                    } else {
                        handleQuit();
                        break; // Quitter la boucle après QUIT
                    }
                } else if (command.equals("TOP")) {
                    if (arguments.isEmpty()) {
                        out.println("-ERR Invalid syntax: Usage: TOP <message_number> <n>");
                    } else {
                        handleTop(arguments);
                    }
                } else if (command.equals("UIDL")) {
                    if (arguments.isEmpty()) {
                        handleUidl(""); // UIDL sans arguments
                    } else {
                        handleUidl(inputLine); // UIDL avec un numéro de message
                    }
                } else {
                    out.println("-ERR Command not recognized");
                }
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
    private void handleUser(String inputLine) throws RemoteException {
        user = inputLine.substring(5).trim();
        File userDir = new File("mailserver/" + user);
        if (userDir.exists() && userDir.isDirectory() && authService.userExists(user)) {
            out.println("+OK User accepted");
            loadEmails(userDir); // Charger les emails de l'utilisateur
        } else {
            out.println("-ERR User not found");
        }
    }
    private void handlePass(String inputLine) {
        if (user != null) {
            String providedPassword = inputLine.replaceFirst("^PASS\\s+", "").trim();

            try {
                if (authService.authenticate(user, providedPassword)) {
                    authenticated = true;
                    out.println("+OK User authenticated");
                } else {
                    out.println("-ERR Invalid password");
                }
            } catch (RemoteException e) {
                out.println("-ERR Authentication service error: " + e.getMessage());
            }
        } else {
            out.println("-ERR User not specified");
        }
    }


    private void handleApop(String inputLine) {
        String[] parts = inputLine.split(" ");
        if (parts.length < 3) {
            out.println("-ERR Invalid APOP command");
            return;
        }

        String username = parts[1];
        String digest = parts[2];

        // Simuler une vérification de digest (pour l'exemple, le secret est "password")
        String secret = "loucif";
        String computedDigest = md5(timestamp + secret);

        if (computedDigest.equalsIgnoreCase(digest)) {
            authenticated = true;
            out.println("+OK User authenticated");
        } else {
            out.println("-ERR Authentication failed");
        }
    }

    private void handleStat() {
        if (!authenticated) {
            out.println("-ERR Not authenticated");
            return;
        }
        File userDir = new File("mailserver/" + user);
        loadEmails(userDir);
        int messageCount = 0;
        long maildropSize = 0;
        // Parcourir les emails et ignorer ceux marqués pour suppression
        for (int i = 0; i < emails.size(); i++) {
            File emailFile = emails.get(i);
            if (!markedForDeletion.contains(emailFile)) {
                messageCount++;
                maildropSize += emailFile.length();
            }}

        // Réponse au format "+OK <nombre de messages> <taille du maildrop>"
        out.println("+OK " + messageCount + " " + maildropSize);
    }

    private void handleList(String inputLine) {
        if (!authenticated) {
            out.println("-ERR Not authenticated");
            return;
        }
        File userDir = new File("mailserver/" + user);
        loadEmails(userDir);
        String[] parts = inputLine.split(" ");
        if (parts.length == 1) {
            // List all messages except password.txt
            out.println("+OK"); // Start response
            int messageCount = 0;
            long maildropSize = 0;

            for (File emailFile : emails) {
                if (!markedForDeletion.contains(emailFile) && !emailFile.getName().equals("password.txt")) {
                    messageCount++; // Ensures proper numbering
                    maildropSize += emailFile.length();
                    out.println(messageCount + " " + emailFile.length());
                }
            }

            out.println("."); // End response
        } else if (parts.length == 2) {
            // List specific message size
            try {
                int messageNumber = Integer.parseInt(parts[1]);
                int actualIndex = 0;

                for (File emailFile : emails) {
                    if (!markedForDeletion.contains(emailFile) && !emailFile.getName().equals("password.txt")) {
                        actualIndex++;
                        if (actualIndex == messageNumber) {
                            out.println("+OK " + messageNumber + " " + emailFile.length());
                            return;
                        }}}
                out.println("-ERR No such message");
            } catch (NumberFormatException e) {
                out.println("-ERR Invalid message number");
            }
        } else {
            out.println("-ERR Invalid syntax");
        }
    }

    private void handleRetr(String inputLine) {
        if (!authenticated) {
            out.println("-ERR Not authenticated");
            return;
        }
        File userDir = new File("mailserver/" + user);
        loadEmails(userDir);
        try {
            int messageNumber = Integer.parseInt(inputLine.substring(5).trim());
            if (messageNumber < 1 || messageNumber > emails.size()) {
                out.println("-ERR No such message");
                return;
            }

            File emailFile = emails.get(messageNumber - 1);
            if (markedForDeletion.contains(emailFile)) {
                out.println("-ERR Message marked for deletion");
                return;
            }

            // Envoyer le contenu du message
            out.println("+OK");
            try (BufferedReader reader = new BufferedReader(new FileReader(emailFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.println(line);
                }
            }
            out.println(".");
        } catch (NumberFormatException e) {
            out.println("-ERR Invalid message number");
        } catch (IOException e) {
            out.println("-ERR Error reading message");
        }
    }

    private void handleDele(String inputLine) {
        if (!authenticated) {
            out.println("-ERR Not authenticated");
            return;
        }
        File userDir = new File("mailserver/" + user);
        loadEmails(userDir);
        try {
            int messageNumber = Integer.parseInt(inputLine.substring(5).trim());
            if (messageNumber < 1 || messageNumber > emails.size()) {
                out.println("-ERR No such message");
                return;
            }
            File emailFile = emails.get(messageNumber - 1);
            if (markedForDeletion.contains(emailFile)) {
                out.println("-ERR Message already marked for deletion");
                return;
            }
            // Marquer le message pour suppression
            markedForDeletion.add(emailFile);
            out.println("+OK Message marked for deletion");
        } catch (NumberFormatException e) {
            out.println("-ERR Invalid message number");
        }
    }

    private void handleNoop() {
        if (!authenticated) {
            out.println("-ERR Not authenticated");
            return;
        }
        out.println("+OK");
    }

    private void handleRset() {
        if (!authenticated) {
            out.println("-ERR Not authenticated");
            return;
        }

        // Réinitialiser les messages marqués pour suppression
        markedForDeletion.clear();
        out.println("+OK All deletions reset");
    }

    private void handleQuit() {
        if (!authenticated) {
            out.println("-ERR Not authenticated");
            return;
        }

        // Supprimer les messages marqués
        for (File emailFile : markedForDeletion) {
            emailFile.delete();
        }
        out.println("+OK POP3 server signing off");
    }

    private void handleTop(String inputLine) {
        if (!authenticated) {
            out.println("-ERR Not authenticated");
            return;
        }

        String[] parts = inputLine.split(" ");
        if (parts.length < 3) {
            out.println("-ERR Invalid TOP command");
            return;
        }

        try {
            int messageNumber = Integer.parseInt(parts[1]);
            int lines = Integer.parseInt(parts[2]);

            if (messageNumber < 1 || messageNumber > emails.size()) {
                out.println("-ERR No such message");
                return;
            }

            File emailFile = emails.get(messageNumber - 1);
            out.println("+OK");
            try (BufferedReader reader = new BufferedReader(new FileReader(emailFile))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < lines) {
                    out.println(line);
                    lineCount++;
                }
            }
            out.println(".");
        } catch (NumberFormatException | IOException e) {
            out.println("-ERR Invalid TOP command");
        }
    }

    private void handleUidl(String inputLine) {
        if (!authenticated) {
            out.println("-ERR Not authenticated");
            return;
        }

        String[] parts = inputLine.split(" ");
        if (parts.length == 1) {
            // UIDL sans argument : liste tous les messages (triés du plus ancien au plus récent)
            out.println("+OK");
            for (int i = 0; i < emails.size(); i++) {
                out.println((i + 1) + " " + emails.get(i).getName());
            }
            out.println(".");
        } else {
            // UIDL avec un numéro de message spécifique
            try {
                int messageNumber = Integer.parseInt(parts[1]);
                if (messageNumber < 1 || messageNumber > emails.size()) {
                    out.println("-ERR No such message");
                } else {
                    out.println("+OK " + messageNumber + " " + emails.get(messageNumber - 1).getName());
                }
            } catch (NumberFormatException e) {
                out.println("-ERR Invalid message number");
            }
        }
    }
    private void loadEmails(File userDir) {
        emails.clear();
        File[] files = userDir.listFiles();
        if (files != null) {
            // Sort files by last modified date (oldest to newest)
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));

            for (File file : files) {
                // Exclude password.txt from the list
                if (file.isFile() && !file.getName().equalsIgnoreCase("password.txt")) {
                    emails.add(file);
                }
            }
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}