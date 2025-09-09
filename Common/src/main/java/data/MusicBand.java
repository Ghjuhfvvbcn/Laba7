package data;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;

public final class MusicBand implements Comparable<MusicBand>, Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private Coordinates coordinates;
    private java.time.ZonedDateTime creationDate;
    private int numberOfParticipants;
    private String description;
    private MusicGenre genre;
    private Studio studio;
    private int ownerId; // Добавлено поле для идентификатора владельца

    // Конструктор для создания нового объекта
    public MusicBand(String name, Coordinates coordinates, int numberOfParticipants,
                     String description, MusicGenre genre, Studio studio, int ownerId) {
        this.id = GeneratorId.generateId();
        setName(name);
        setCoordinates(coordinates);
        creationDate = ZonedDateTime.now();
        setNumberOfParticipants(numberOfParticipants);
        setDescription(description);
        setGenre(genre);
        setStudio(studio);
        setOwnerId(ownerId); // Устанавливаем владельца
    }

    // Конструктор для загрузки из БД
    public MusicBand(Long id, String name, Coordinates coordinates, ZonedDateTime creationDate,
                     int numberOfParticipants, String description, MusicGenre genre,
                     Studio studio, int ownerId) {
        if (id == null) {
            throw new IllegalArgumentException("Id value cannot be null");
        } else if (id <= 0) {
            throw new IllegalArgumentException("Id should be a positive number");
        } else {
            this.id = id;
            GeneratorId.setId(id);
        }
        setName(name);
        setCoordinates(coordinates);
        if (creationDate == null) {
            throw new IllegalArgumentException("Creation date value cannot be null");
        } else {
            this.creationDate = creationDate;
        }
        setNumberOfParticipants(numberOfParticipants);
        setDescription(description);
        setGenre(genre);
        setStudio(studio);
        setOwnerId(ownerId); // Устанавливаем владельца
    }

    public MusicBand() {
        this.creationDate = ZonedDateTime.now();
    }

    // Добавлены геттер и сеттер для ownerId
    public void setOwnerId(int ownerId) {
        if (ownerId <= 0) {
            throw new IllegalArgumentException("Owner ID should be a positive number");
        } else {
            this.ownerId = ownerId;
        }
    }

    public int getOwnerId() {
        return ownerId;
    }

    // Остальные методы остаются без изменений...
    public void setCreationDate(ZonedDateTime creationDate) {
        if (creationDate == null) {
            throw new IllegalArgumentException("Creation date value cannot be null");
        } else {
            this.creationDate = creationDate;
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() { return id; }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name value cannot be empty or null");
        } else {
            this.name = name;
        }
    }

    public String getName() { return name; }

    public void setCoordinates(Coordinates coordinates) {
        if (coordinates == null) {
            throw new IllegalArgumentException("Coordinates value cannot be null");
        } else {
            this.coordinates = coordinates;
        }
    }

    public Coordinates getCoordinates() { return coordinates; }

    public ZonedDateTime getCreationDate() { return creationDate; }

    public void setNumberOfParticipants(int numberOfParticipants) {
        if (numberOfParticipants <= 0) {
            throw new IllegalArgumentException("Number of participants should be a positive number");
        } else {
            this.numberOfParticipants = numberOfParticipants;
        }
    }

    public int getNumberOfParticipants() { return numberOfParticipants; }

    public void setDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty or null");
        } else {
            this.description = description;
        }
    }

    public String getDescription() { return description; }

    public void setGenre(MusicGenre genre) {
        if (genre == null) {
            throw new IllegalArgumentException("Music genre cannot be null");
        } else {
            this.genre = genre;
        }
    }

    public MusicGenre getGenre() { return genre; }

    public void setStudio(Studio studio) {
        if (studio == null) {
            throw new IllegalArgumentException("Studio cannot be null");
        } else {
            this.studio = studio;
        }
    }

    public Studio getStudio() { return studio; }

    public static final Comparator<MusicBand> compareByDateAndName = Comparator
            .comparing(MusicBand::getCreationDate)
            .thenComparing(MusicBand::getName);

    @Override
    public int compareTo(MusicBand other_band) {
        return name.compareTo(other_band.getName());
    }

    @Override
    public String toString() {
        return String.format("MusicBand[\n" +
                        "id=%d\n" +
                        "name=%s\n" +
                        "coordinates=%s\n" +
                        "creationDate=%s\n" +
                        "numberOfParticipants=%d\n" +
                        "description=%s\n" +
                        "genre=%s\n" +
                        "studio=%s\n" +
                        "ownerId=%d\n" + // Добавлено в вывод
                        "]",
                id, name, coordinates, creationDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss z")),
                numberOfParticipants, description, genre, studio, ownerId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MusicBand)) return false;
        MusicBand o = (MusicBand) other;
        return Objects.equals(id, o.id) &&
                Objects.equals(name, o.name) &&
                Objects.equals(coordinates, o.coordinates) &&
                Objects.equals(creationDate, o.creationDate) &&
                numberOfParticipants == o.numberOfParticipants &&
                Objects.equals(description, o.description) &&
                Objects.equals(genre, o.genre) &&
                Objects.equals(studio, o.studio) &&
                ownerId == o.ownerId; // Добавлено в сравнение
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,
                name,
                coordinates,
                creationDate,
                numberOfParticipants,
                description,
                genre,
                studio,
                ownerId); // Добавлено в хэш
    }
}