package utils;

import data.*;
import commands.Command;
import commands.commandsWithArgument.CommandWithArgument;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;


public class Console {
    private static ArrayList<String> commands = new ArrayList<>(Arrays.asList(
            "help",
            "info",
            "show",
            "clear",
            "exit",
            "print_ascending",
            "print_descending",
            "remove_key",
            "remove_lower_key",
            "filter_starts_with_name",
            "insert",
            "update",
            "remove_lower",
            "replace_if_lower",
            "execute_script",
            "shut_down_the_server"
    ));

    private BufferedReader reader;

    public Console(){
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public Console(InputStream inputStream){
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    public static class CommandInput {
        public final String command;
        public final String argument;

        public CommandInput(String command, String argument) {
            this.command = command;
            this.argument = argument;
        }
    }

    public static CommandInput parseCommand(String s){
        s = s.trim();

        if (s.isEmpty()) {
            return new CommandInput("", null);
        }

        int firstSpace = -1;
        boolean inQuotes = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                firstSpace = i;
                break;
            }
        }

        if (firstSpace == -1) {
            return new CommandInput(s, null);
        }

        String command = s.substring(0, firstSpace);
        String argument = s.substring(firstSpace + 1).trim();

        if (argument.startsWith("\"") && argument.endsWith("\"")) {
            argument = argument.substring(1, argument.length() - 1);
        }

        return new CommandInput(command, argument);
    }

    public CommandInput readCommand(){
        String s = null;
        try{
            s = reader.readLine();
        }catch (IOException e){
            System.err.println("Error: " + e.getMessage());
        }
        if(s == null){
            return null;
        }
        String[] parts = s.trim().split("\\s+", 2);
        return new CommandInput(
                parts[0],
                parts.length > 1 ? parts[1] : null
        );
    }

    public static boolean isValidCommand(String command){
        return commands.contains(command);
    }

    public MusicBand readMusicBand(){
        try {
            String name = readMusicBandName("Enter the name of the music band (cannot be empty string): ", "The name of the music band cannot be empty. Please enter again: ");
            Coordinates coordinates = readCoordinates();
            int numberOfParticipants = readNumberOfParticipants("Enter number of participants (should be positive integer value): ", "The number of participants should be positive integer value. Please enter again: ");
            String description = readDescription("Enter the description of the music band (cannot be empty): ", "The description cannot be empty string. Please enter again: ");
            MusicGenre genre = readMusicGenre();
            Studio studio = readStudio();

            MusicBand musicBand = new MusicBand(name, coordinates, numberOfParticipants, description, genre, studio);
            return musicBand;
        }catch(EOFException e){
            System.out.println("Shutting down...");
            System.exit(0);
        }
        return null;
    }

    public String read() throws IOException{
        String s = reader.readLine();
        if(s == null){
            throw new IllegalArgumentException("Unexpected end of file");
        }else{
            return s;
        }
    }

    public String readLine() throws IOException{
        try {
            String s = reader.readLine();
            if (s == null) {
                throw new EOFException("End of input");
            }
            return s;
        } catch (IOException e) {
            if (e.getMessage().contains("Interrupted")) {
                throw new EOFException("Operation cancelled by user");
            }
            throw e;
        }
    }
    public String readLines() throws IOException{
        try {
            return reader.readLine();
        } catch (IOException e) {
            if (e.getMessage().contains("Interrupted")) {
                throw new EOFException("Operation cancelled by user");
            }
            throw e;
        }
    }

    public String readMusicBandName(String s1, String s2) throws EOFException {
        String s;
        String name;
        System.out.print(s1);
        while (true) {
            try {
                s = reader.readLine();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                return null;
            }
            if (s == null) throw new EOFException();
            if (!s.trim().isEmpty()) {
                name = s;
                break;
            }
            System.out.print(s2);
        }
        return name;
    }

    public Coordinates readCoordinates() throws EOFException{
        Double x = readCoordinateX("Enter the x coordinate (Double, not null value): ", "The x coordinate should be Double, not null value. Please enter again: ");
        Integer y = readCoordinateY("Enter the y coordinate (should be integer value, cannot be null): ", "The y coordinate should be Integer, not null value. Please enter again: ");
        return new Coordinates(x, y);
    }

    public Double readCoordinateX(String s1, String s2) throws EOFException{
        String s;
        Double x;
        System.out.print(s1);
        while(true) {
            try {
                s = reader.readLine();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                return null;
            }
            if(s == null){
                throw new EOFException();
            }
            if(!s.trim().isEmpty()){
                try{
                    x = Double.parseDouble(s);
                    break;
                }catch (NumberFormatException e){
                    System.out.print(s2);
                }
            }else{
                System.out.print(s2);
            }
        }
        return x;
    }

    public Integer readCoordinateY(String s1, String s2) throws EOFException{
        String s;
        Integer y;
        System.out.print(s1);
        while(true) {
            try{
                s = reader.readLine();
            }catch (IOException e){
                System.err.println("Error: " + e.getMessage());
                return null;
            }
            if(s == null){
                throw new EOFException();
            }
            if(!s.trim().isEmpty()){
                try{
                    y = Integer.parseInt(s);
                    break;
                }catch (NumberFormatException e){
                    System.out.print(s2);
                }
            }else{
                System.out.print(s2);
            }
        }
        return y;
    }

    public int readNumberOfParticipants(String s1, String s2) throws EOFException{
        String s;
        int numberOfParticipants;
        System.out.print(s1);
        while(true){
            try{
                s = reader.readLine();
            }catch (IOException e){
                System.err.println("Error: " + e.getMessage());
                return 0;
            }
            if(s == null){
                throw new EOFException();
            }
            if(!s.trim().isEmpty()){
                try{
                    numberOfParticipants = Integer.parseInt(s);
                    if(numberOfParticipants <= 0){
                        throw new NumberFormatException();
                    }
                    break;
                }catch(NumberFormatException e){
                    System.out.print(s2);
                }
            }else{
                System.out.print(s2);
            }
        }
        return numberOfParticipants;
    }

    public String readDescription(String s1, String s2) throws EOFException{
        String s;
        String description;
        System.out.print(s1);
        while(true){
            try{
                s = reader.readLine();
            }catch (IOException e){
                System.err.println("Error: " + e.getMessage());
                return null;
            }
            if(s == null){
                throw new EOFException();
            }
            if(!s.trim().isEmpty()){
                description = s;
                break;
            }
            System.out.print(s2);
        }
        return description;
    }

    public MusicGenre readMusicGenre() throws  EOFException{
        String s;
        MusicGenre genre;
        System.out.printf("Enter the music genre of the music band. Here the valid values: %s: ", Arrays.toString(MusicGenre.values()));
        while(true){
            try{
                s = reader.readLine();
            }catch(IOException e){
                System.err.println("Error: " + e.getMessage());
                return null;
            }
            if(s == null){
                throw new EOFException();
            }
            if(!s.trim().isEmpty()){
                try{
                    genre = MusicGenre.valueOf(s);
                    break;
                }catch(IllegalArgumentException e){
                    System.out.printf("There is no genre '%s'. Please enter again. Here the valid values: %s: ", s, Arrays.toString(MusicGenre.values()));
                }
            }else{
                System.out.printf("The genre cannot be empty string. Please enter again. Here the valid values: %s: ", Arrays.toString(MusicGenre.values()));
            }
        }
        return genre;
    }

    public Studio readStudio() throws EOFException{
        String studioName = readStudioName("Enter the name of studio (not empty string): ", "The name of studio cannot be empty. Please enter again: ");
        return new Studio(studioName);
    }

    public String readStudioName(String s1, String s2) throws EOFException{
        String s;
        String studioName;
        System.out.print(s1);
        while(true){
            try{
                s = reader.readLine();
            }catch (IOException e){
                System.err.println("Error: " + e.getMessage());
                return null;
            }
            if(s == null){
                throw new EOFException();
            }
            if(!s.trim().isEmpty()){
                studioName = s;
                break;
            }else{
                System.out.print(s2);
            }
        }
        return studioName;
    }

    public MusicBand readMusicBandFromScript() throws IOException {
        try {
            String name = readLineWithEOFCheck("name");
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Band name cannot be empty");
            }

            String xStr = readLineWithEOFCheck("x coordinate");
            Double x = Double.parseDouble(xStr);

            String yStr = readLineWithEOFCheck("y coordinate");
            Integer y = Integer.parseInt(yStr);

            String participantsStr = readLineWithEOFCheck("number of participants");
            int numberOfParticipants = Integer.parseInt(participantsStr);
            if (numberOfParticipants <= 0) {
                throw new IllegalArgumentException("Number of participants must be positive");
            }

            String description = readLineWithEOFCheck("description");
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Description cannot be empty");
            }

            String genreStr = readLineWithEOFCheck("genre");
            MusicGenre genre;
            try {
                genre = MusicGenre.valueOf(genreStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid music genre: " + genreStr +
                        ". Valid values: " + Arrays.toString(MusicGenre.values()));
            }

            String studioName = readLineWithEOFCheck("studio name");
            if (studioName == null || studioName.trim().isEmpty()) {
                throw new IllegalArgumentException("Studio name cannot be empty");
            }

            Coordinates coordinates = new Coordinates(x, y);
            Studio studio = new Studio(studioName);

            return new MusicBand(name.trim(), coordinates, numberOfParticipants,
                    description.trim(), genre, studio);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + e.getMessage());
        }
    }

    private String readLineWithEOFCheck(String fieldName) throws IOException {
        String line = read();
        if (line == null) {
            throw new IOException("Unexpected end of file while reading " + fieldName);
        }
        return line;
    }
}