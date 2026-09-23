package com.securebank;

import com.securebank.model.Transaction;
import com.securebank.service.BankingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public final class Main {

    private static final BankingService BANKING_SERVICE = new BankingService();
    private static final Scanner SCANNER = new Scanner(System.in);
    private static String currentUser;

    private Main() {
    }

    public static void main(String[] args) {
        System.out.println("===================================");
        System.out.println("Secure Banking Application");
        System.out.println("===================================");

        while (true) {
            if (currentUser == null) {
                showAuthenticationMenu();
            } else {
                showBankingMenu();
            }
        }
    }

    private static void showAuthenticationMenu() {
        System.out.println("\n1. Register");
        System.out.println("2. Login");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");

        String option = readLine();
        switch (option) {
            case "1":
                registerUser();
                break;
            case "2":
                loginUser();
                break;
            case "3":
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option. Please try again.");
                break;
        }
    }

    private static void showBankingMenu() {
        System.out.println("\nWelcome, " + currentUser + "!");
        System.out.println("1. Create account");
        System.out.println("2. View balance");
        System.out.println("3. Deposit funds");
        System.out.println("4. Withdraw funds");
        System.out.println("5. View transactions");
        System.out.println("6. Logout");
        System.out.print("Choose an option: ");

        String option = readLine();
        switch (option) {
            case "1":
                createAccount();
                break;
            case "2":
                viewBalance();
                break;
            case "3":
                depositFunds();
                break;
            case "4":
                withdrawFunds();
                break;
            case "5":
                viewTransactions();
                break;
            case "6":
                currentUser = null;
                System.out.println("You have been logged out.");
                break;
            default:
                System.out.println("Invalid option. Please try again.");
                break;
        }
    }

    private static void registerUser() {
        System.out.print("Enter username: ");
        String username = readLine();
        System.out.print("Enter password (minimum 10 chars): ");
        String password = readLine();

        try {
            BANKING_SERVICE.registerUser(username, password);
            System.out.println("User registered successfully.");
        } catch (IllegalArgumentException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private static void loginUser() {
        System.out.print("Enter username: ");
        String username = readLine();
        System.out.print("Enter password: ");
        String password = readLine();

        if (BANKING_SERVICE.login(username, password)) {
            currentUser = username;
            System.out.println("Login successful.");
        } else {
            System.out.println("Login failed. Please check your details.");
        }
    }

    private static void createAccount() {
        System.out.print("Enter account name: ");
        String accountName = readLine();
        try {
            String accountNumber = BANKING_SERVICE.createAccount(currentUser, accountName);
            System.out.println("Account created successfully. Account Number: " + accountNumber);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewBalance() {
        List<String> accounts = BANKING_SERVICE.listAccountNumbersForUser(currentUser);
        if (accounts.isEmpty()) {
            System.out.println("No accounts found for this user.");
            return;
        }

        System.out.println("Your accounts:");
        for (String accountNumber : accounts) {
            System.out.println("- " + accountNumber + " => " + BANKING_SERVICE.getBalance(accountNumber));
        }

        System.out.print("Enter account number to view details: ");
        String accountNumber = readLine();
        try {
            System.out
                    .println("Balance for account " + accountNumber + ": " + BANKING_SERVICE.getBalance(accountNumber));
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void depositFunds() {
        System.out.print("Enter account number: ");
        String accountNumber = readLine();
        System.out.print("Enter deposit amount: ");
        String input = readLine();

        try {
            BigDecimal amount = new BigDecimal(input);
            BANKING_SERVICE.deposit(accountNumber, amount);
            System.out.println("Deposit successful. New balance: " + BANKING_SERVICE.getBalance(accountNumber));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void withdrawFunds() {
        System.out.print("Enter account number: ");
        String accountNumber = readLine();
        System.out.print("Enter withdrawal amount: ");
        String input = readLine();

        try {
            BigDecimal amount = new BigDecimal(input);
            BANKING_SERVICE.withdraw(accountNumber, amount);
            System.out.println("Withdrawal successful. New balance: " + BANKING_SERVICE.getBalance(accountNumber));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewTransactions() {
        System.out.print("Enter account number: ");
        String accountNumber = readLine();
        try {
            List<Transaction> transactions = BANKING_SERVICE.getTransactionsForAccount(accountNumber);
            if (transactions.isEmpty()) {
                System.out.println("No transactions recorded for this account.");
                return;
            }

            System.out.println("Transaction history for " + accountNumber + ":");
            for (Transaction transaction : transactions) {
                System.out.println(transaction.getTimestamp() + " | "
                        + transaction.getType() + " | "
                        + transaction.getAmount() + " | Balance: " + transaction.getBalanceAfter());
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static String readLine() {
        return SCANNER.nextLine().trim();
    }
}
