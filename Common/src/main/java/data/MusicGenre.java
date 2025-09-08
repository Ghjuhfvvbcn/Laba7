package data;

import java.io.Serializable;

public enum MusicGenre implements Serializable {
    ROCK("Rock"),
    PSYCHEDELIC_CLOUD_RAP("Psychedelic cloud rap"),
    JAZZ("Jazz"),
    SOUL("Soul"),
    POST_ROCK("Post rock");

    private String genre;

    MusicGenre(String genre){
        this.genre = genre;
    }

    public String getGenre(){return genre;}
}