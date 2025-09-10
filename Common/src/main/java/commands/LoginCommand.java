package commands;

import data.User;
import utils.DatabaseManager;

import java.sql.SQLException;

public class LoginCommand implements Command {
    private final String commandName = "login";
    private final DatabaseManager dbManager;
    private String login;
    private String passwordHash;

    public LoginCommand(DatabaseManager dbManager) {
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
            User user = dbManager.authenticateUser(login, passwordHash);
            if (user != null) {
                return "Login successful. User ID: " + user.getId();
            } else {
                return "Error: Invalid login or password.";
            }
        } catch (SQLException e) {
            return "Database error during login: " + e.getMessage();
        }
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public boolean requiresUser() {
        return false; // Логин не требует авторизации
    }
}