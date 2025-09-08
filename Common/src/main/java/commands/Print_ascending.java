package commands;

public class Print_ascending implements Command{
    private final String commandName = "print_ascending";
    private final Executor executor;

    public Print_ascending(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute(){
        return executor.print_ascending();
    }

    @Override
    public String getCommandName(){return commandName;}
}
