CREATE DATABASE IF NOT EXISTS music_digging;

USE music_digging;

CREATE TABLE music (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       title VARCHAR(200) NOT NULL,
                       artist VARCHAR(200) NOT NULL,
                       album VARCHAR(200),
                       genre VARCHAR(100),
                       release_year INT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);