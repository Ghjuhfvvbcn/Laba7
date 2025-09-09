package commands.commandsWithArgument;

import commands.CommandWithUser;
import data.MusicBand;
import commands.Executor;
import data.User;

public class Replace_if_lower implements CommandWithArgument<Long>, CommandWithUser {
    private static final String commandName = "replace_if_lower";
    private final Executor executor;
    private Long argument;

    public Replace_if_lower(Executor executor) {
        this.executor = executor;
    }

    @Override
    public String execute() {
        throw new UnsupportedOperationException("This command requires user authentication");
    }

    @Override
    public String execute(User user) {
        if (user == null) return "Error: Authentication required";
        return executor.replace_if_lower(argument, null, user);
    }

    @Override
    public String executeWithMusicBand(MusicBand musicBand, User user) {
        if (user == null) return "Error: Authentication required";
        if (argument == null) {
            return "Error: No key specified for replace_if_lower";
        }
        return executor.replace_if_lower(argument, musicBand, user);
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

    @Override
    public boolean requiresUser() {
        return true;
    }
}