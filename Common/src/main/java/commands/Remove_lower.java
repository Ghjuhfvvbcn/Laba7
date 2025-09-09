package commands;

import data.MusicBand;
import data.User;

public class Remove_lower implements CommandWithUser {
    private final String commandName = "remove_lower";
    private final Executor executor;

    public Remove_lower(Executor executor) {
        this.executor = executor;
    }

    @Override
    public String execute() {
        throw new UnsupportedOperationException("This command requires user authentication");
    }

    @Override
    public String execute(User user) {
        if (user == null) return "Error: Authentication required";
        return executor.remove_lower(null, user);
    }

    @Override
    public String executeWithMusicBand(MusicBand musicBand, User user) {
        if (user == null) return "Error: Authentication required";
        return executor.remove_lower(musicBand, user);
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