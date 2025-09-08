package commands.commandsWithArgument;

import commands.Command;

public interface CommandWithArgument<T> extends Command {
    void setArgument(String argument);
    T getArgument();
}
