package commands;

public class Show implements Command{
    private final String commandName = "show";
    private final Executor executor;

    public Show(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute(){
        return executor.show();
    }

    @Override
    public String getCommandName(){return commandName;}

    @Override
    public boolean requiresUser() {
        return false; // не требует аутентификации
    }
}
