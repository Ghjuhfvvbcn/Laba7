package utils;

import data.MusicBand;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;

public class WriterCSV {
    public static void loadToFile(File file_csv, TreeMap<Long, MusicBand> musicBands) throws IOException{
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file_csv))) {
            writer.write(
                    "id(Long)," +
                            "name(String)," +
                            "x(Double)," +
                            "y(Integer)," +
                            "creationDate(dd-MM-yyyy HH-mm-ss z)," +
                            "numberOfParticipants(int)," +
                            "description(String)," +
                            "genre(MusicGenre)," +
                            "studioName(String)"
            );
            writer.newLine();
            for (MusicBand band : musicBands.values()) {
                String bandString = String.format("%d,\"%s\",%s,%d,%s,%d,\"%s\",%s,\"%s\"",
                        band.getId(),
                        band.getName(),
                        band.getCoordinates().getX(),
                        band.getCoordinates().getY(),
                        band.getCreationDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss z")),
                        band.getNumberOfParticipants(),
                        band.getDescription(),
                        band.getGenre(),
                        band.getStudio().getName());
                writer.write(bandString);
                writer.newLine();
            }
        }
    }
}
