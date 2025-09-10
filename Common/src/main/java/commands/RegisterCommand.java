package commands;

import data.User;
import utils.DatabaseManager;

import java.sql.SQLException;

public class RegisterCommand implements Command {
    private final String commandName = "register";
    private final DatabaseManager dbManager;
    private String login;
    private String passwordHash;

    public RegisterCommand(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void setCredentials(String login, String passwordHash) {
        this.login = login;
        this.passwordHash = passwordHash;
    }

    @Override
    public String execute() {
        if (login == null || passwordHash == null) {
            return "Error: Login and password required";
        }

        try {
            boolean success = dbManager.registerUser(login, passwordHash);
            if (success) {
                return "User registered successfully.";
            } else {
                return "Error: User with this login already exists.";
            }
        } catch (SQLException e) {
            return "Database error during registration: " + e.getMessage();
        }
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public boolean requiresUser() {
        return false; // Регистрация не требует авторизации
    }
}