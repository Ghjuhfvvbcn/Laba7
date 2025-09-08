package commands.commandsWithArgument;

import commands.Executor;

public class Execute_script implements CommandWithArgument<String> {
    private final String commandName = "execute_script";
    private final Executor executor;
    private String argument;

    public Execute_script(Executor executor) {
        this.executor = executor;
    }

    @Override
    public String execute() {
        if (argument == null || argument.trim().isEmpty()) {
            return "Error: Script filename is required";
        }
        return executor.execute_script(argument);
    }

    @Override
    public void setArgument(String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            throw new IllegalArgumentException("Script filename cannot be empty");
        }
        this.argument = argument.trim();
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public String getArgument() {
        return argument;
    }
}