package commands.commandsWithArgument;

import commands.Executor;
import data.MusicBand;

public class Update implements CommandWithArgument<Long>{
    private String commandName = "update";
    private Long argument;
    private Executor executor;

    public Update(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute(){
        return executor.update(argument, null);
    }

    public String executeWithMusicBand(MusicBand musicBand){
        return executor.update(argument, musicBand);
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
    public Long getArgument(){return argument;}

    @Override
    public String getCommandName(){return commandName;}
}
