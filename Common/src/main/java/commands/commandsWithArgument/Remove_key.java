package commands.commandsWithArgument;

import commands.CommandWithUser;
import commands.Executor;
import data.MusicBand;
import data.User;

public class Remove_key implements CommandWithArgument<Long>, CommandWithUser {
    private String commandName = "remove_key";
    private Executor executor;
    private Long argument;

    public Remove_key(Executor executor) {
        this.executor = executor;
    }

    @Override
    public String execute() {
        throw new UnsupportedOperationException("This command requires user authentication");
    }

    public String execute(User user) {
        if (user == null) return "Error: Authentication required";
        return executor.remove_key(argument, user);
    }

    @Override
    public String executeWithMusicBand(MusicBand band, User user){
        return "Error in class Remove_key in method executeWithMusicBand";
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