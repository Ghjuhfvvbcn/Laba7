package commands;

import data.MusicBand;

public class Remove_lower implements Command{
    private final String commandName = "remove_lower";
    private final Executor executor;

    public Remove_lower(Executor executor){
        this.executor = executor;
    }

    @Override
    public String execute(){
        return executor.remove_lower(null);
    }

    public String executeWithMusicBand(MusicBand musicBand) {
        return executor.remove_lower(musicBand);
    }

    @Override
    public String getCommandName(){return commandName;}
}
