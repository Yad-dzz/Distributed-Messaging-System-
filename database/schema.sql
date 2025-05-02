-- Create the database
CREATE DATABASE IF NOT EXISTS messaging_system;
USE messaging_system;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password_clear VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('active', 'inactive') DEFAULT 'active',
    INDEX idx_username (username)
);

-- Emails table
CREATE TABLE IF NOT EXISTS emails (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject TEXT,
    content TEXT,
    sent_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    INDEX idx_recipient (recipient),
    INDEX idx_sender (sender)
);

-- Stored Procedures

-- Authenticate user
DELIMITER //
CREATE PROCEDURE authenticate_user(IN p_username VARCHAR(255), IN p_password VARCHAR(255), OUT p_result BOOLEAN)
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) INTO v_count 
    FROM users 
    WHERE username = p_username AND password_clear = p_password;
    
    SET p_result = (v_count > 0);
END //
DELIMITER ;

-- Store email
DELIMITER //
CREATE PROCEDURE store_email(
    IN p_sender VARCHAR(255),
    IN p_recipient VARCHAR(255),
    IN p_subject TEXT,
    IN p_content TEXT
)
BEGIN
    INSERT INTO emails (sender, recipient, subject, content)
    VALUES (p_sender, p_recipient, p_subject, p_content);
END //
DELIMITER ;

-- Fetch emails for user
DELIMITER //
CREATE PROCEDURE fetch_emails(IN p_username VARCHAR(255))
BEGIN
    SELECT id, sender, subject, sent_date, is_read
    FROM emails
    WHERE recipient = p_username
    ORDER BY sent_date DESC;
END //
DELIMITER ;

-- Delete email
DELIMITER //
CREATE PROCEDURE delete_email(IN p_email_id INT, IN p_username VARCHAR(255))
BEGIN
    DELETE FROM emails 
    WHERE id = p_email_id AND recipient = p_username;
END //
DELIMITER ;

-- Update password
DELIMITER //
CREATE PROCEDURE update_password(
    IN p_username VARCHAR(255),
    IN p_new_password VARCHAR(255),
    IN p_new_password_hash VARCHAR(255)
)
BEGIN
    UPDATE users 
    SET password_clear = p_new_password,
        password_hash = p_new_password_hash
    WHERE username = p_username;
END //
DELIMITER ; 