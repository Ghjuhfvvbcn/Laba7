package commands.commandsWithArgument;

import data.MusicBand;
import commands.Executor;

public class Replace_if_lower implements CommandWithArgument{
    private static final String commandName = "replace_if_lower";
    private final Executor executor;
    private Long argument;

    public Replace_if_lower(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute(){
        return executor.replace_if_lower(argument, null);
    }

    public String executeWithMusicBand(MusicBand musicBand) {
        if (argument == null) {
            return "Error: No key specified for replace_if_lower";
        }
        return executor.replace_if_lower(argument, musicBand);
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
    public String getCommandName(){return commandName;}

    @Override
    public Long getArgument(){return argument;}
}
