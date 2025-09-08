package commands.commandsWithArgument;

import commands.Executor;
import data.MusicBand;

public class Insert implements CommandWithArgument<Long>{
    private String commandName = "insert";
    private Executor executor;
    private Long argument;

    public Insert(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute(){
        return executor.insert(argument, null);
    }

    public String executeWithMusicBand(MusicBand band){
        return executor.insert(argument, band);
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
    public Long getArgument(){
        return argument;
    }

    @Override
    public String getCommandName(){
        return commandName;
    }
}
