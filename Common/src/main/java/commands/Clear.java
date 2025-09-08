package commands;

public class Clear implements Command{
    private final String commandName = "clear";
    private final Executor executor;

    public Clear(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute() {
        return executor.clear();
    }

    @Override
    public String getCommandName(){return commandName;}
}