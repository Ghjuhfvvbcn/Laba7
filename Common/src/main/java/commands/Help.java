package commands;

public class Help implements Command{
    private String commandName = "help";
    private Executor executor;

    public Help(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute(){
        return executor.help();
    }

    @Override
    public String getCommandName(){
        return commandName;
    }

    @Override
    public boolean requiresUser() {
        return false; // не требует аутентификации
    }
}
