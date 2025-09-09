package commands.commandsWithArgument;

import commands.Executor;

public class Filter_starts_with_name implements CommandWithArgument<String> {
    private final String commandName = "filter_starts_with_name";
    private final Executor executor;
    private String argument;

    public Filter_starts_with_name(Executor executor) {
        this.executor = executor;
    }

    @Override
    public String execute() {
        return executor.filter_starts_with_name(argument);
    }

    @Override
    public void setArgument(String argument) throws IllegalArgumentException {
        if (argument == null || argument.trim().isEmpty()) {
            throw new IllegalArgumentException("Command '" + commandName + "' failed: Argument cannot be empty or null");
        }
        this.argument = argument;
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public String getArgument() {
        return argument;
    }

    @Override
    public boolean requiresUser() {
        return false; // Команда только для чтения, не требует аутентификации
    }
}