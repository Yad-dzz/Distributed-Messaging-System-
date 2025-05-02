 package com.example.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/messaging_system";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";
    
    private Connection connection;
    
    public DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Properties props = new Properties();
            props.setProperty("user", DB_USER);
            props.setProperty("password", DB_PASSWORD);
            props.setProperty("useSSL", "false");
            connection = DriverManager.getConnection(DB_URL, props);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean authenticateUser(String username, String password) {
        try {
            CallableStatement stmt = connection.prepareCall("{call authenticate_user(?, ?, ?)}");
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.registerOutParameter(3, Types.BOOLEAN);
            stmt.execute();
            return stmt.getBoolean(3);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void storeEmail(String sender, String recipient, String subject, String content) {
        try {
            CallableStatement stmt = connection.prepareCall("{call store_email(?, ?, ?, ?)}");
            stmt.setString(1, sender);
            stmt.setString(2, recipient);
            stmt.setString(3, subject);
            stmt.setString(4, content);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<Email> fetchEmails(String username) {
        List<Email> emails = new ArrayList<>();
        try {
            CallableStatement stmt = connection.prepareCall("{call fetch_emails(?)}");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Email email = new Email(
                    rs.getInt("id"),
                    rs.getString("sender"),
                    rs.getString("subject"),
                    rs.getTimestamp("sent_date"),
                    rs.getBoolean("is_read")
                );
                emails.add(email);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return emails;
    }
    
    public void deleteEmail(int emailId, String username) {
        try {
            CallableStatement stmt = connection.prepareCall("{call delete_email(?, ?)}");
            stmt.setInt(1, emailId);
            stmt.setString(2, username);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updatePassword(String username, String newPassword, String newPasswordHash) {
        try {
            CallableStatement stmt = connection.prepareCall("{call update_password(?, ?, ?)}");
            stmt.setString(1, username);
            stmt.setString(2, newPassword);
            stmt.setString(3, newPasswordHash);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT username FROM users WHERE status = 'active'");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public boolean deleteUser(String username) {
        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE users SET status = 'inactive' WHERE username = ?");
            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class Email {
    private int id;
    private String sender;
    private String subject;
    private Timestamp sentDate;
    private boolean isRead;
    
    public Email(int id, String sender, String subject, Timestamp sentDate, boolean isRead) {
        this.id = id;
        this.sender = sender;
        this.subject = subject;
        this.sentDate = sentDate;
        this.isRead = isRead;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public String getSender() { return sender; }
    public String getSubject() { return subject; }
    public Timestamp getSentDate() { return sentDate; }
    public boolean isRead() { return isRead; }
} 