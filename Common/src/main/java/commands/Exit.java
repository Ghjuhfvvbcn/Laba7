package commands;

public class Exit implements Command{
    private final String commandName = "exit";
    private final Executor executor;

    public  Exit(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute(){
        return executor.exit();
    }

    @Override
    public String getCommandName(){return commandName;}

    @Override
    public boolean requiresUser() {
        return false; // не требует аутентификации
    }
}
