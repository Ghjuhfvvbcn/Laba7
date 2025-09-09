package commands;

import data.MusicBand;
import data.User;

public interface CommandWithUser {
    String execute(User user);
    String executeWithMusicBand(MusicBand band, User user);
}
