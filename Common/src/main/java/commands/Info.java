package commands;

public class Info implements Command {
    private final String commandName = "info";
    private final Executor executor;

    public Info(Executor executor) {
        this.executor = executor;
    }

    @Override
    public String execute() {
        return executor.info();
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public boolean requiresUser() {
        return false; // не требует аутентификации
    }
}
