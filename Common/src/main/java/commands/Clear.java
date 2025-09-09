package commands;

import data.User;

public class Clear implements Command {
    private final String commandName = "clear";
    private final Executor executor;

    public Clear(Executor executor) {
        this.executor = executor;
    }

    @Override
    public String execute() {
        throw new UnsupportedOperationException("This command requires user authentication");
    }

    public String execute(User user) {
        if (user == null) return "Error: Authentication required";
        return executor.clear(user);
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }
}