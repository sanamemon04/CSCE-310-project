CREATE DATABASE IF NOT EXISTS bookstore;
USE bookstore;

CREATE TABLE Account (
    userID INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    passwordHash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    userType ENUM('customer','manager') NOT NULL
);

CREATE TABLE Book (
    bookID INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    buyPrice DECIMAL(10,2) NOT NULL,
    rentPrice DECIMAL(10,2) NOT NULL,
    isAvailable BOOLEAN DEFAULT TRUE
);

CREATE TABLE Orders (
    orderID INT AUTO_INCREMENT PRIMARY KEY,
    userID INT NOT NULL,
    orderDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    totalAmount DECIMAL(10,2),
    paymentStatus ENUM('Pending','Paid') DEFAULT 'Pending',
    FOREIGN KEY (userID) REFERENCES Account(userID)
);

CREATE TABLE OrderItems (
    itemID INT AUTO_INCREMENT PRIMARY KEY,
    orderID INT NOT NULL,
    bookID INT NOT NULL,
    transactionType ENUM('buy','rent') NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (orderID) REFERENCES Orders(orderID),
    FOREIGN KEY (bookID) REFERENCES Book(bookID)
);


IINSERT INTO Account (username, passwordHash, email, userType) VALUES
('alice', '$2b$12$orGaEcKkawORkKOVllbDsOB04qJq4o/KJhVrJowC54Or4U.D3Ufu.', 'alice@example.com', 'customer'),
('bob',   '$2b$12$MYEU/ZApXKHb5j1VaRFm2OKQu1uwa5VpDEgD8VftSog/iG1h.sa7.', 'bob@example.com', 'manager');


INSERT INTO Book (title, author, buyPrice, rentPrice, isAvailable) VALUES
('The Great Gatsby', 'F. Scott Fitzgerald', 10.99, 3.99, TRUE),
('1984', 'George Orwell', 12.50, 4.25, TRUE),
('Dune', 'Frank Herbert', 15.00, 5.00, TRUE);