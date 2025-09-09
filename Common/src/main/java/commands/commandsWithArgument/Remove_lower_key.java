package commands.commandsWithArgument;

import commands.Executor;
import data.User;

public class Remove_lower_key implements CommandWithArgument<Long> {
    private final String commandName = "remove_lower_key";
    private Executor executor;
    private Long argument;

    public Remove_lower_key(Executor executor) {
        this.executor = executor;
    }

    @Override
    public String execute() {
        throw new UnsupportedOperationException("This command requires user authentication");
    }

    public String execute(User user) {
        if (user == null) return "Error: Authentication required";
        return executor.remove_lower_key(argument, user);
    }

    @Override
    public void setArgument(String argument) {
        Long arg;
        try {
            arg = Long.parseLong(argument.trim());
            if (arg <= 0) throw new NumberFormatException();
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Command '" + commandName + "' failed: '" + argument + "' is not a valid Long number.");
        }
        this.argument = arg;
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public Long getArgument() {
        return argument;
    }

    public boolean requiresUser() {
        return true;
    }
}