package commands;

public class Print_descending implements Command{
    private final String commandName = "print_descending";
    private final Executor executor;

    public Print_descending(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute(){
        return executor.print_descending();
    }

    @Override
    public String getCommandName(){return commandName;}

    @Override
    public boolean requiresUser() {
        return false; // не требует аутентификации
    }
}