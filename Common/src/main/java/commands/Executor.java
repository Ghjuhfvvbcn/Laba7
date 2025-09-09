package commands;

import data.MusicBand;
import data.User;
import utils.*;
import commands.commandsWithArgument.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static data.MusicBand.compareByDateAndName;

public class Executor {
    private TreeMap<Long, MusicBand> musicBands;
    private final Map<String, Command> commands;
    private final ZonedDateTime initializationDate;
    private Console consoleScript;
    private final Stack<FileInputStream> scriptStack = new Stack<>();
    private final Set<String> executingScripts = new HashSet<>();

    private final DatabaseManager dbManager; // <-- НОВОЕ
    private final ReentrantReadWriteLock collectionLock = new ReentrantReadWriteLock(); // <-- НОВОЕ




    public Executor(DatabaseManager dbManager){
        this.dbManager = dbManager;
        try {
            this.musicBands = dbManager.loadCollection(); // Загружаем из БД, а не из файла!
        } catch (SQLException e) {
            System.err.println("Failed to load collection from DB: " + e.getMessage());
            this.musicBands = new TreeMap<>();
        }
        initializationDate = ZonedDateTime.now();
        commands = CommandMap.createMapWithCommands(this);
    }

    public String help(){
        return "-help : вывести справку по доступным командам\n" +
                "-info : вывести информацию о коллекции (тип, дата инициализации, количество элементов и т.д.)\n" +
                "-show : вывести все элементы коллекции в строковом представлении\n" +
                "-insert null {element} : добавить новый элемент с заданным ключом\n" +
                "-update id {element} : обновить значение элемента коллекции, id которого равен заданному\n" +
                "-remove_key null : удалить элемент из коллекции по его ключу\n" +
                "-clear : очистить коллекцию\n" +
                "-execute_script file_name : считать и исполнить скрипт из указанного файла\n" +
                "-exit : завершить программу (без сохранения в файл)\n" +
                "-remove_lower {element} : удалить из коллекции все элементы, меньшие, чем заданный\n" +
                "-replace_if_lower null {element} : заменить значение по ключу, если новое значение меньше старого\n" +
                "-remove_lower_key null : удалить из коллекции все элементы, ключ которых меньше, чем заданный\n" +
                "-filter_starts_with_name name : вывести элементы, значение поля name которых начинается с заданной подстроки\n" +
                "-print_ascending : вывести элементы коллекции в порядке возрастания\n" +
                "-print_descending : вывести элементы коллекции в порядке убывания";
    }

    public String info() {
        collectionLock.readLock().lock();
        try {
            if (musicBands == null) {
                return "The collection is 'null'" ;
            } else if (!musicBands.isEmpty()) {
                return String.format("Type: TreeMap<Long, MusicBand>\n" +
                                "Initialization date: %s\n" +
                                "Size of collection: %d\n" +
                                "First key: %d\n" +
                                "Last key: %d",
                        initializationDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss z")),
                        musicBands.size(),
                        musicBands.firstKey(),
                        musicBands.lastKey());
            } else {
                return String.format("Type: TreeMap<Long, MusicBand>\n" +
                                "Initialization date: %s\n" +
                                "Size of collection: 0",
                        initializationDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss z")));
            }
        }finally {
            collectionLock.readLock().unlock();
        }
    }

    public String show() {
        collectionLock.readLock().lock();
        try {
            if (musicBands == null) {
                return "The collection is 'null'" ;
            } else if (!musicBands.isEmpty()) {
                String bandsString = musicBands.values().stream()
                        .map(MusicBand::toString)
                        .collect(Collectors.joining("\n"));

                return "The collection contains " + musicBands.size() + " items:\n" + bandsString;
            } else {
                return "The collection is empty" ;
            }
        }finally {
            collectionLock.readLock().unlock();
        }
    }

    public String clear() {
        if (musicBands == null) {
            return "The collection is 'null'";
        } else if (!musicBands.isEmpty()) {
            int sizeBefore = musicBands.size();
            musicBands.clear();
            return "The collection was successfully cleared. " + sizeBefore + " elements removed";
        } else {
            return "The collection is empty";
        }
    }

    public String exit() {
        return "Shutting down...";
    }

    public String print_ascending() {
        if (musicBands == null) {
            return "The collection is 'null'";
        } else if (musicBands.isEmpty()) {
            return "The collection is empty";
        } else {
            String bandsString = musicBands.values().stream()
                    .sorted()
                    .map(MusicBand::toString)
                    .collect(Collectors.joining("\n"));

            return "Collection elements in ascending order (by 'name'):\n" + bandsString;
        }
    }

    public String print_descending() {
        if (musicBands == null) {
            return "The collection is 'null'";
        } else if (musicBands.isEmpty()) {
            return "The collection is empty";
        } else {
            String bandsString = musicBands.values().stream()
                    .sorted(Comparator.reverseOrder())
                    .map(MusicBand::toString)
                    .collect(Collectors.joining("\n"));

            return "Collection elements in descending order (by 'name'):\n" + bandsString;
        }
    }

    public String remove_key(Long key, User user) {
        if (user == null) return "Error: Authentication required";

        collectionLock.writeLock().lock();
        try {
            if (!musicBands.containsKey(key)) {
                return "No music band found with key: " + key;
            }

            // Проверяем, принадлежит ли элемент пользователю (если нужно)
            MusicBand band = musicBands.get(key);
            if (band.getOwnerId() != user.getId() && !user.isAdmin()) {
                return "Error: You don't have permission to remove this band";
            }

            // Удаляем из БД
            boolean success = dbManager.removeMusicBand(key, user.getId());
            if (success) {
                // Только после успеха в БД удаляем из памяти
                musicBands.remove(key);
                return "Music band removed successfully.";
            } else {
                return "Failed to remove music band from database.";
            }
        } catch (SQLException e) {
            return "Database error during removal: " + e.getMessage();
        } finally {
            collectionLock.writeLock().unlock();
        }
    }

    public String remove_lower_key(Long key) {
        int sizeBefore = musicBands.size();
        musicBands.headMap(key, false).clear();
        int sizeAfter = musicBands.size();
        return "Successfully deleted " + (sizeBefore - sizeAfter) + " items";
    }

    public String filter_starts_with_name(String name) {
        List<MusicBand> bands = musicBands.values().stream()
                .filter(band -> band.getName().startsWith(name))
                .collect(Collectors.toList());

        String bandsString = bands.stream()
                .map(MusicBand::toString)
                .collect(Collectors.joining("\n"));

        return "Found " + bands.size() +
                " music groups whose names start with \"" + name + "\"\n" +
                bandsString;
    }

    public String insert(Long key, MusicBand band, User user) { // Добавили аргумент User
        if (user == null) return "Error: Authentication required";

        collectionLock.writeLock().lock();
        try {
            if (musicBands.containsKey(key)) {
                return "The collection already contains the key: " + key;
            }
            // Пытаемся вставить в БД
            boolean success = dbManager.insertMusicBand(key, band, user.getId());
            if (success) {
                // Только после успеха в БД добавляем в память
                band.setId(key);
                musicBands.put(key, band);
                return "Music band inserted successfully.";
            } else {
                return "Failed to insert music band into database.";
            }
        } catch (SQLException e) {
            return "Database error during insert: " + e.getMessage();
        } finally {
            collectionLock.writeLock().unlock();
        }
    }

    public String update(Long id, MusicBand band) {
        if (!musicBands.containsKey(id)) {
            return "The collection doesn't contain the key " + id;
        } else {
            band.setId(id);
            musicBands.put(id, band);
            saveCollection();
            return "Music band updated successfully.";
        }
    }

    public String remove_lower(MusicBand band) {
        if (musicBands.isEmpty()) {
            return "The collection is empty";
        }

        long countBefore = musicBands.size();
        musicBands.values().removeIf(musicBand ->
                compareByDateAndName.compare(musicBand, band) > 0);
        long countAfter = musicBands.size();

        saveCollection();
        return (countBefore - countAfter) + " bands were successfully removed";
    }

    public String replace_if_lower(Long key, MusicBand newBand) {
        if (musicBands.isEmpty()) {
            return "The collection is empty";
        }

        newBand.setId(key);
        boolean replaced = musicBands.computeIfPresent(key, (k, oldBand) ->
                compareByDateAndName.compare(oldBand, newBand) > 0 ? newBand : oldBand
        ) != musicBands.get(key);

        if (replaced) {
            saveCollection();
            return "Music band replaced successfully.";
        } else {
            return musicBands.containsKey(key) ?
                    "New value is not lower than existing value." :
                    "The collection doesn't contain the key " + key;
        }
    }

    public String execute_script(String filename) {
        File scriptFile = new File(filename);
        String canonicalPath = null;

        try {
            canonicalPath = scriptFile.getCanonicalPath();

            if (executingScripts.contains(canonicalPath)) {
                return "Error: Recursive script execution detected for: " + filename;
            }

            if (!scriptFile.exists() || !scriptFile.isFile()) {
                return "Error: Script file not found: " + filename;
            }
            if (!scriptFile.canRead()) {
                return "Error: Cannot read script file: " + filename;
            }

            executingScripts.add(canonicalPath);

            Console previousConsole = consoleScript;

            try(FileInputStream scriptStream = new FileInputStream(scriptFile)) {
                consoleScript = new Console(scriptStream);

                StringBuilder result = new StringBuilder();
                result.append("Executing script: ").append(filename).append("\n");

                String line;
                int lineNumber = 0;

                while ((line = consoleScript.readLine()) != null) {
                    lineNumber++;
                    if (line.trim().isEmpty()) continue;

                    try {
                        Console.CommandInput input = Console.parseCommand(line);

                        if (!Console.isValidCommand(input.command)) {
                            result.append("Line ").append(lineNumber).append(": Unknown command: '")
                                    .append(input.command).append("'\n");
                            continue;
                        }

                        if (input.command.equals("execute_script")) {
                            if (input.argument == null || input.argument.trim().isEmpty()) {
                                result.append("Line ").append(lineNumber)
                                        .append(": Error: execute_script requires filename\n");
                                continue;
                            }
                            result.append(execute_script(input.argument)).append("\n");
                        } else {
                            result.append(processScriptCommand(input, lineNumber)).append("\n");
                        }

                    } catch (Exception e) {
                        result.append("Line ").append(lineNumber).append(": Error: ")
                                .append(e.getMessage()).append("\n");
                    }
                }

                return result.toString();
            } finally{
                consoleScript = previousConsole;
            }

        } catch (IOException e) {
            return "Error reading script: " + e.getMessage();
        } finally {
            if (canonicalPath != null) {
                executingScripts.remove(canonicalPath);
            }
        }
    }

    private String processScriptCommand(Console.CommandInput input, int lineNumber) {
        try {
            Command command = commands.get(input.command);
            if (command == null) {
                return "Line " + lineNumber + ": Unknown command: " + input.command;
            }

            if (command instanceof CommandWithArgument) {
                CommandWithArgument<?> cmdWithArg = (CommandWithArgument<?>) command;

                try {
                    cmdWithArg.setArgument(input.argument);
                } catch (IllegalArgumentException e) {
                    return "Line " + lineNumber + ": Invalid argument for '" + input.command + "': " + e.getMessage();
                }
            }

            if (input.command.equals("insert") || input.command.equals("update") ||
                    input.command.equals("remove_lower") || input.command.equals("replace_if_lower")) {

                try {
                    MusicBand band = consoleScript.readMusicBandFromScript();
                    if (band == null) {
                        return "Line " + lineNumber + ": Error reading MusicBand data";
                    }

                    if (band.getNumberOfParticipants() <= 0) {
                        return "Line " + lineNumber + ": Error: Number of participants must be positive";
                    }
                    if (band.getName() == null || band.getName().trim().isEmpty()) {
                        return "Line " + lineNumber + ": Error: Band name cannot be empty";
                    }

                    if (command instanceof Insert) {
                        return ((Insert) command).executeWithMusicBand(band);
                    } else if (command instanceof Update) {
                        return ((Update) command).executeWithMusicBand(band);
                    } else if (command instanceof Remove_lower) {
                        return ((Remove_lower) command).executeWithMusicBand(band);
                    } else if (command instanceof Replace_if_lower) {
                        return ((Replace_if_lower) command).executeWithMusicBand(band);
                    }

                } catch (IOException e) {
                    return "Line " + lineNumber + ": Error: Unexpected end of file while reading MusicBand";
                } catch (IllegalArgumentException e) {
                    return "Line " + lineNumber + ": Error in MusicBand data: " + e.getMessage();
                }
            }

            return command.execute();

        } catch (Exception e) {
            return "Line " + lineNumber + ": Error executing command: " + e.getMessage();
        }
    }

    public String saveCollection() {
//        try {
//            WriterCSV.loadToFile(file_csv, musicBands);
//            return "The collection was successfully saved to the file '" + file_csv + "'";
//        } catch (IOException e) {
//            return "Error saving collection: " + e.getMessage();
//        }
        return null;
    }

    public int getSizeOfCollection(){
        return musicBands.size();
    }
}
