package data;

import java.io.Serializable;

public class CommandWrapper implements Serializable {
    private String commandName;
    private Long key;
    private MusicBand musicBand;
    private Object argument;

    public String getCommandName() { return commandName; }
    public void setCommandName(String commandName) { this.commandName = commandName; }

    public Long getKey() { return key; }
    public void setKey(Long key) { this.key = key; }

    public MusicBand getMusicBand() { return musicBand; }
    public void setMusicBand(MusicBand musicBand) { this.musicBand = musicBand; }

    public Object getArgument() { return argument; }
    public void setArgument(Object argument) { this.argument = argument; }
}