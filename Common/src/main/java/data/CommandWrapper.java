package data;

import java.io.Serializable;

public class CommandWrapper implements Serializable {
    private String commandName;
    private Long key;
    private MusicBand musicBand;
    private Object argument;

    private String login;
    private String passwordHash; // Клиент должен прислать хэш пароля

    public String getCommandName() { return commandName; }
    public void setCommandName(String commandName) { this.commandName = commandName; }

    public Long getKey() { return key; }
    public void setKey(Long key) { this.key = key; }

    public MusicBand getMusicBand() { return musicBand; }
    public void setMusicBand(MusicBand musicBand) { this.musicBand = musicBand; }

    public Object getArgument() { return argument; }
    public void setArgument(Object argument) { this.argument = argument; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}