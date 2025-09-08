package commands.commandsWithArgument;

import commands.Executor;

public class Remove_key implements CommandWithArgument<Long>{
    private String commandName = "remove_key";
    private Executor executor;
    private Long argument;

    public Remove_key(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute(){
        return executor.remove_key(argument);
    }

    @Override
    public void setArgument(String argument){
        Long arg;
        try{
            arg = Long.parseLong(argument.trim());
            if(arg <= 0) throw new NumberFormatException();
        }catch(NumberFormatException | NullPointerException e){
            throw new IllegalArgumentException("Command '" + commandName + "' failed: '" + argument + "' is not a valid Long number.");
        }
        this.argument = arg;
    }


    @Override
    public String getCommandName(){
        return commandName;
    }

    @Override
    public Long getArgument(){
        return argument;
    }
}
