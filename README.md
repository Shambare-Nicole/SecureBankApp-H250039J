# Secure Banking Application

Secure Banking Application is a console-based Java project developed to demonstrate object-oriented programming, secure coding practices, and file-based persistence. The application allows users to register, log in, create bank accounts, deposit and withdraw funds, and view transaction history while keeping all data stored in text files.

## Project Objective

The primary goal of this project is to design and implement a secure banking system in Java that follows core OOP principles, validates user input, protects sensitive data, and persists application data into local files without using a database.

## Features Implemented

- User registration and login system
- Password hashing using PBKDF2 with HMAC-SHA256
- Password salt generation for secure storage
- Account creation for registered users
- Balance checking for each account
- Deposit and withdrawal operations with validation
- Maximum transaction and balance checks
- Transaction history tracking
- Persistent storage using text files for users, accounts, and transactions
- Input validation for usernames, passwords, and financial values
- Secure configuration constants for application rules

## Technologies Used

- Java 25
- Maven
- JUnit 5
- File-based storage using plain text files

## Project Structure

- src/main/java/com/securebank/Main.java – application entry point and console menu
- src/main/java/com/securebank/service/BankingService.java – core business logic and persistence management
- src/main/java/com/securebank/model/User.java – user entity model
- src/main/java/com/securebank/model/BankAccount.java – account model and balance operations
- src/main/java/com/securebank/model/Transaction.java – transaction record model
- src/main/java/com/securebank/config/AppConfig.java – constants and security configuration
- src/test/java/com/securebank/service/BankingServiceTest.java – automated tests for banking logic

## How to Run

1. Open a terminal in the project root.
2. Run the test suite:

```bash
mvn test
```

3. Start the application:

```bash
mvn exec:java -Dexec.mainClass=com.securebank.Main
```

## Application Flow

When the application starts, the user is presented with a menu to:

- register a new user
- log in as an existing user
- exit the program

After login, the user can:

- create a new bank account
- view account balances
- deposit funds
- withdraw funds
- review transaction history
- log out

## Data Persistence

The application stores data in text files within the project data directory:

- users.txt
- accounts.txt
- transactions.txt

This ensures the application retains user credentials, account details, and transaction records between runs without requiring a database.

## Security Notes

- Passwords are never stored in plaintext.
- Each password is hashed using a random salt and PBKDF2.
- System limits prevent invalid financial values from being processed.
- Usernames and passwords are validated before acceptance.

## Testing

The project includes automated tests to validate:

- minimum transaction rules
- consistent balance and limit checks
- deposit and withdrawal functionality
- password hashing and verification

## Conclusion

This project demonstrates a practical implementation of a secure banking application in Java, combining object-oriented design, validation, security controls, and persistent storage in a simple but effective console application.
