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
        commands = CommandMap.createMapWithCommands(this, dbManager);
    }

    public Map<String, Command> getCommands(){
        return commands;
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

    public String clear(User user) {
        if (user == null) return "Error: Authentication required";

        collectionLock.writeLock().lock();
        try {
            if (musicBands == null) {
                return "The collection is 'null'";
            } else if (!musicBands.isEmpty()) {
                // Очищаем только элементы, принадлежащие пользователю
                int deletedCount = dbManager.clearUserMusicBands(user.getId());

                if (deletedCount > 0) {
                    // После успеха в БД удаляем только элементы пользователя из памяти
                    musicBands.entrySet().removeIf(entry -> entry.getValue().getOwnerId() == user.getId());
                    return "The collection was successfully cleared. " + deletedCount + " elements removed";
                } else {
                    return "No elements found for this user to clear.";
                }
            } else {
                return "The collection is empty";
            }
        } catch (SQLException e) {
            return "Database error during clear: " + e.getMessage();
        } finally {
            collectionLock.writeLock().unlock();
        }
    }

    public String exit() {
        return "Shutting down...";
    }

    public String print_ascending() {
        collectionLock.readLock().lock();
        try {
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
        } finally {
            collectionLock.readLock().unlock();
        }
    }

    public String print_descending() {
        collectionLock.readLock().lock();
        try {
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
        } finally {
            collectionLock.readLock().unlock();
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
            if (band.getOwnerId() != user.getId()) {
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

    public String remove_lower_key(Long key, User user) {
        if (user == null) return "Error: Authentication required";

        collectionLock.writeLock().lock();
        try {
            // Получаем ключи для удаления (меньше указанного)
            Set<Long> keysToRemove = musicBands.headMap(key, false).keySet();

            if (keysToRemove.isEmpty()) {
                return "No elements found with keys lower than: " + key;
            }

            // Удаляем элементы из БД (только принадлежащие пользователю)
            int deletedCount = 0;
            for (Long k : keysToRemove) {
                MusicBand band = musicBands.get(k);
                if (band != null && band.getOwnerId() == user.getId()) {
                    boolean success = dbManager.removeMusicBand(k, user.getId());
                    if (success) {
                        deletedCount++;
                    }
                }
            }

            // Удаляем из памяти только те элементы, которые успешно удалились из БД
            if (deletedCount > 0) {
                // Создаем копию для безопасного удаления во время итерации
                Set<Long> successfullyRemoved = new HashSet<>();
                for (Long k : keysToRemove) {
                    MusicBand band = musicBands.get(k);
                    if (band != null && band.getOwnerId() == user.getId()) {
                        // Проверяем, был ли элемент удален из БД
                        // (предполагаем, что если removeMusicBand вернул true, то элемент удален)
                        successfullyRemoved.add(k);
                    }
                }

                // Удаляем из памяти
                successfullyRemoved.forEach(musicBands::remove);

                return "Successfully deleted " + deletedCount + " items";
            } else {
                return "Failed to delete any items from database.";
            }

        } catch (SQLException e) {
            return "Database error during remove_lower_key: " + e.getMessage();
        } finally {
            collectionLock.writeLock().unlock();
        }
    }

    public String filter_starts_with_name(String name) {
        collectionLock.readLock().lock();
        try {
            List<MusicBand> bands = musicBands.values().stream()
                    .filter(band -> band.getName().startsWith(name))
                    .collect(Collectors.toList());

            if (bands.isEmpty()) {
                return "No music groups found whose names start with \"" + name + "\"";
            }

            String bandsString = bands.stream()
                    .map(MusicBand::toString)
                    .collect(Collectors.joining("\n"));

            return "Found " + bands.size() +
                    " music groups whose names start with \"" + name + "\"\n" +
                    bandsString;
        } finally {
            collectionLock.readLock().unlock();
        }
    }

//    public String insert(Long key, MusicBand band, User user) { // Добавили аргумент User
//        if (user == null) return "Error: Authentication required";
//
//        collectionLock.writeLock().lock();
//        try {
//            if (musicBands.containsKey(key)) {
//                return "The collection already contains the key: " + key;
//            }
//            // Пытаемся вставить в БД
//            boolean success = dbManager.insertMusicBand(key, band, user.getId());
//            if (success) {
//                // Только после успеха в БД добавляем в память
//                band.setId(key);
//                musicBands.put(key, band);
//                return "Music band inserted successfully.";
//            } else {
//                return "Failed to insert music band into database.";
//            }
//        } catch (SQLException e) {
//            return "Database error during insert: " + e.getMessage();
//        } finally {
//            collectionLock.writeLock().unlock();
//        }
//    }
public String insert(Long key, MusicBand band, User user) {
    if (user == null) return "Error: Authentication required";

    collectionLock.writeLock().lock();
    try {
        if (musicBands.containsKey(key)) {
            return "The collection already contains the key: " + key;
        }

        // Пытаемся вставить в БД
        boolean success = dbManager.insertMusicBand(key, band, user.getId());
        if (success) {
            // Добавляем в коллекцию с переданным ключом
            musicBands.put(key, band);
            return "Music band inserted successfully with key: " + key +
                    " and generated ID: " + band.getId();
        } else {
            return "Failed to insert music band into database.";
        }
    } catch (SQLException e) {
        return "Database error during insert: " + e.getMessage();
    } finally {
        collectionLock.writeLock().unlock();
    }
}

    public String update(Long id, MusicBand band, User user) {
        if (user == null) return "Error: Authentication required";

        collectionLock.writeLock().lock();
        try {
            if (!musicBands.containsKey(id)) {
                return "The collection doesn't contain the key " + id;
            }

            // Проверяем, принадлежит ли элемент пользователю
            MusicBand existingBand = musicBands.get(id);
            if (existingBand.getOwnerId() != user.getId()) {
                return "Error: You don't have permission to update this band";
            }

            // Обновляем в БД
            boolean success = dbManager.updateMusicBand(id, band, user.getId());
            if (success) {
//                Только после успеха в БД обновляем в памяти
//                band.setId(id);
//                band.setOwnerId(user.getId()); // Сохраняем владельца
//                musicBands.put(id, band);
//                return "Music band updated successfully.";
                // Исправляем: сохраняем оригинальный ID объекта, а не пользовательский ключ
                band.setId(existingBand.getId()); // ← existingBand имеет правильный ID
                band.setOwnerId(user.getId());
                musicBands.put(id, band); // Ключ коллекции остается прежним (id)
                return "Music band updated successfully.";
            } else {
                return "Failed to update music band in database.";
            }
        } catch (SQLException e) {
            return "Database error during update: " + e.getMessage();
        } finally {
            collectionLock.writeLock().unlock();
        }
    }

    public String remove_lower(MusicBand band, User user) {
        if (user == null) return "Error: Authentication required";

        collectionLock.writeLock().lock();
        try {
            if (musicBands.isEmpty()) {
                return "The collection is empty";
            }

            // Получаем элементы для удаления (только принадлежащие пользователю)
            List<Long> keysToRemove = musicBands.entrySet().stream()
                    .filter(entry -> {
                        MusicBand currentBand = entry.getValue();
                        return currentBand.getOwnerId() == user.getId() &&
                                compareByDateAndName.compare(currentBand, band) > 0;
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (keysToRemove.isEmpty()) {
                return "No elements found to remove for this user";
            }

            // Удаляем элементы из БД
            int deletedCount = 0;
            for (Long key : keysToRemove) {
                boolean success = dbManager.removeMusicBand(key, user.getId());
                if (success) {
                    deletedCount++;
                }
            }

            // Удаляем из памяти только успешно удаленные из БД элементы
            if (deletedCount > 0) {
                for (Long key : keysToRemove) {
                    // Проверяем, что элемент еще существует и принадлежит пользователю
                    MusicBand existingBand = musicBands.get(key);
                    if (existingBand != null && existingBand.getOwnerId() == user.getId()) {
                        musicBands.remove(key);
                    }
                }
                return deletedCount + " bands were successfully removed";
            } else {
                return "Failed to remove any bands from database.";
            }

        } catch (SQLException e) {
            return "Database error during remove_lower: " + e.getMessage();
        } finally {
            collectionLock.writeLock().unlock();
        }
    }

//    public String replace_if_lower(Long key, MusicBand newBand, User user) {
//        if (user == null) return "Error: Authentication required";
//
//        collectionLock.writeLock().lock();
//        try {
//            if (musicBands.isEmpty()) {
//                return "The collection is empty";
//            }
//
//            // Проверяем существование ключа и права доступа
//            if (!musicBands.containsKey(key)) {
//                return "The collection doesn't contain the key " + key;
//            }
//
//            MusicBand oldBand = musicBands.get(key);
//            if (oldBand.getOwnerId() != user.getId()) {
//                return "Error: You don't have permission to replace this band";
//            }
//
//            // Проверяем условие замены (новый элемент должен быть "меньше")
//            if (compareByDateAndName.compare(oldBand, newBand) <= 0) {
//                return "New value is not lower than existing value.";
//            }
//
//            // Заменяем в БД
//            newBand.setId(key);
//            newBand.setOwnerId(user.getId()); // Сохраняем владельца
//
//            boolean success = dbManager.updateMusicBand(key, newBand, user.getId());
//            if (success) {
//                // Только после успеха в БД заменяем в памяти
//                musicBands.put(key, newBand);
//                return "Music band replaced successfully.";
//            } else {
//                return "Failed to replace music band in database.";
//            }
//
//        } catch (SQLException e) {
//            return "Database error during replace_if_lower: " + e.getMessage();
//        } finally {
//            collectionLock.writeLock().unlock();
//        }
//    }
public String replace_if_lower(Long key, MusicBand newBand, User user) {
    if (user == null) return "Error: Authentication required";

    collectionLock.writeLock().lock();
    try {
        if (musicBands.isEmpty()) {
            return "The collection is empty";
        }

        // Проверяем существование ключа и права доступа
        if (!musicBands.containsKey(key)) {
            return "The collection doesn't contain the key " + key;
        }

        MusicBand oldBand = musicBands.get(key);
        if (oldBand.getOwnerId() != user.getId()) {
            return "Error: You don't have permission to replace this band";
        }

        // Проверяем условие замены (новый элемент должен быть "меньше")
        if (compareByDateAndName.compare(oldBand, newBand) <= 0) {
            return "New value is not lower than existing value.";
        }

        // Заменяем в БД
        newBand.setId(oldBand.getId()); // ← ИСПРАВЛЕНО: используем оригинальный ID объекта
        newBand.setOwnerId(user.getId()); // Сохраняем владельца

        boolean success = dbManager.updateMusicBand(key, newBand, user.getId());
        if (success) {
            // Только после успеха в БД заменяем в памяти
            musicBands.put(key, newBand);
            return "Music band replaced successfully.";
        } else {
            return "Failed to replace music band in database.";
        }

    } catch (SQLException e) {
        return "Database error during replace_if_lower: " + e.getMessage();
    } finally {
        collectionLock.writeLock().unlock();
    }
}

    public String execute_script(String filename, User user) {
        if (user == null) return "Error: Authentication required";

        File scriptFile = new File(filename);
        String canonicalPath = null;

        try {
            canonicalPath = scriptFile.getCanonicalPath();

            // Используем readLock для проверки рекурсии (только чтение)
            collectionLock.readLock().lock();
            try {
                if (executingScripts.contains(canonicalPath)) {
                    return "Error: Recursive script execution detected for: " + filename;
                }
            } finally {
                collectionLock.readLock().unlock();
            }

            if (!scriptFile.exists() || !scriptFile.isFile()) {
                return "Error: Script file not found: " + filename;
            }
            if (!scriptFile.canRead()) {
                return "Error: Cannot read script file: " + filename;
            }

            // Добавляем в список исполняемых скриптов (требует writeLock)
            collectionLock.writeLock().lock();
            try {
                executingScripts.add(canonicalPath);
            } finally {
                collectionLock.writeLock().unlock();
            }

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
                            // Передаем пользователя в рекурсивный вызов
                            result.append(execute_script(input.argument, user)).append("\n");
                        } else {
                            result.append(processScriptCommand(input, lineNumber, user)).append("\n");
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
                // Удаляем из списка исполняемых скриптов (требует writeLock)
                collectionLock.writeLock().lock();
                try {
                    executingScripts.remove(canonicalPath);
                } finally {
                    collectionLock.writeLock().unlock();
                }
            }
        }
    }

    private String processScriptCommand(Console.CommandInput input, int lineNumber, User user) {
        try {
            Command command = commands.get(input.command);
            if (command == null) {
                return "Line " + lineNumber + ": Unknown command: " + input.command;
            }

            // Проверяем аутентификацию для команд, требующих пользователя
            if (command.requiresUser() && user == null) {
                return "Line " + lineNumber + ": Error: Authentication required for command '" + input.command + "'";
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

                    // Используем CommandWithUser для команд с MusicBand
                    if (command instanceof CommandWithUser) {
                        return ((CommandWithUser) command).executeWithMusicBand(band, user);
                    }

                } catch (IOException e) {
                    return "Line " + lineNumber + ": Error: Unexpected end of file while reading MusicBand";
                } catch (IllegalArgumentException e) {
                    return "Line " + lineNumber + ": Error in MusicBand data: " + e.getMessage();
                }
            }

            // Для команд без MusicBand, но требующих пользователя
            if (command instanceof CommandWithUser) {
                return ((CommandWithUser) command).execute(user);
            }

            // Для команд, не требующих пользователя
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
