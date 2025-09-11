package org.example;

import commands.*;
import commands.Executor;
import commands.commandsWithArgument.*;
import data.CommandWrapper;
import data.MusicBand;
import data.User;
import utils.CommandMap;
import utils.DatabaseManager;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;


public class ServerMain {

    private static final int PORT = 12345;
    private static final int BUFFER_SIZE = 65536;

    // Статические поля для управления состоянием сервера
    private static Map<String, Command> commands;
    private static Executor executor;
    private static DatabaseManager dbManager;
//    private static Set<String> connectedClients = new ConcurrentHashSet<>(); // Потокобезопасный Set
    private static Set<String> connectedClients = ConcurrentHashMap.newKeySet();

    // Пуллы потоков согласно заданию
    private static ExecutorService connectionPool = Executors.newCachedThreadPool();
    private static ExecutorService processingPool = Executors.newFixedThreadPool(10);
    private static ForkJoinPool sendingPool = ForkJoinPool.commonPool();

    public static void main(String[] args) {
        // Параметры подключения к БД
        String dbHost = "pg";
        String dbName = "studs";
        String dbUrl = "jdbc:postgresql://" + dbHost + "/" + dbName;
        String dbUser = "s465345";
        String dbPassword = "dcxOUhUo8IMFxqAD";

        try {
            // Инициализация подключения к БД
            dbManager = new DatabaseManager(dbUrl, dbUser, dbPassword);
            System.out.println("Database connection established successfully");

            // Инициализация Executor и загрузка команд
            executor = new Executor(dbManager);
            commands = executor.getCommands(); // Предполагается, что такой метод существует

            System.out.println("Server initialized. Loaded " + executor.getSizeOfCollection() + " music bands.");
            System.out.println("Thread pools initialized:");
            System.out.println("  - Connection pool (CachedThreadPool)");
            System.out.println("  - Processing pool (FixedThreadPool, 10 threads)");
            System.out.println("  - Sending pool (ForkJoinPool)");

            // Основной сетевой цикл
            runServer();

        } catch (SQLException e) {
            System.err.println("Fatal: Cannot connect to database: " + e.getMessage());
            System.exit(1);
        } finally {
            shutdownServer();
        }
    }

    /**
     * Основной метод запуска сервера с многопоточной обработкой
     */
    private static void runServer() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("Server started on port " + PORT + ". Waiting for connections...");

            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Блокирующее ожидание нового подключения
                socket.receive(packet);

                // Передаем обработку подключения в connectionPool (CachedThreadPool)
                connectionPool.submit(new ConnectionHandler(socket, packet));
            }
        } catch (SocketException e) {
            System.err.println("Server socket error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Server I/O error: " + e.getMessage());
        }
    }

    /**
     * Обработчик подключения - работает в CachedThreadPool
     */
    private static class ConnectionHandler implements Runnable {
        private final DatagramSocket socket;
        private final DatagramPacket originalPacket;
        private final byte[] packetDataCopy;

        public ConnectionHandler(DatagramSocket socket, DatagramPacket originalPacket) {
            this.socket = socket;
            this.originalPacket = originalPacket;

            // Создаем копию данных, так как оригинальный буфер будет переиспользован
            this.packetDataCopy = Arrays.copyOf(originalPacket.getData(), originalPacket.getLength());
        }

        @Override
        public void run() {
            try {
                // Десериализация команды из полученных данных
                ByteArrayInputStream byteStream = new ByteArrayInputStream(packetDataCopy);
                ObjectInputStream objectStream = new ObjectInputStream(byteStream);
                CommandWrapper commandWrapper = (CommandWrapper) objectStream.readObject();

                // Регистрируем нового клиента
                String clientKey = originalPacket.getAddress().getHostAddress() + ":" + originalPacket.getPort();
                if (connectedClients.add(clientKey)) {
                    System.out.println("New client connected: " + clientKey);
                }

                System.out.println("Received command from " + clientKey + ": " + commandWrapper.getCommandName());

                // Передаем обработку команды в processingPool (FixedThreadPool)
                processingPool.submit(new CommandProcessor(socket, originalPacket, commandWrapper, clientKey));

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error processing request from " + getClientInfo(originalPacket) + ": " + e.getMessage());
                sendErrorResponse(socket, originalPacket, "Error processing request: " + e.getMessage());
            }
        }
    }

    /**
     * Обработчик команд - работает в FixedThreadPool
     */
    private static class CommandProcessor implements Runnable {
        private final DatagramSocket socket;
        private final DatagramPacket receivePacket;
        private final CommandWrapper commandWrapper;
        private final String clientKey;

        public CommandProcessor(DatagramSocket socket, DatagramPacket receivePacket,
                                CommandWrapper commandWrapper, String clientKey) {
            this.socket = socket;
            this.receivePacket = receivePacket;
            this.commandWrapper = commandWrapper;
            this.clientKey = clientKey;
        }

        @Override
        public void run() {
            try {
                // Обрабатываем команду (синхронно в processingPool)
                Object response = processCommandWithMap(commandWrapper);

                // Передаем отправку ответа в sendingPool (ForkJoinPool)
                sendingPool.submit(new ResponseSender(socket, receivePacket, response, clientKey));

            } catch (Exception e) {
                System.err.println("Error executing command from " + clientKey + ": " + e.getMessage());
                sendErrorResponse(socket, receivePacket, "Error executing command: " + e.getMessage());
            }
        }
    }

    /**
     * Отправитель ответов - работает в ForkJoinPool
     */
    private static class ResponseSender implements Runnable {
        private final DatagramSocket socket;
        private final DatagramPacket receivePacket;
        private final Object response;
        private final String clientKey;

        public ResponseSender(DatagramSocket socket, DatagramPacket receivePacket,
                              Object response, String clientKey) {
            this.socket = socket;
            this.receivePacket = receivePacket;
            this.response = response;
            this.clientKey = clientKey;
        }

        @Override
        public void run() {
            try {
                // Отправляем ответ клиенту
                if (response instanceof ArrayList) {
                    // Отправка chunked response
                    ArrayList<String> responseList = (ArrayList<String>) response;
                    for (String responseString : responseList) {
                        sendResponse(socket, receivePacket, responseString);
                        Thread.sleep(10); // Небольшая задержка между пакетами
                    }
                } else {
                    // Отправка обычного ответа
                    sendResponse(socket, receivePacket, response);
                }

                System.out.println("Response sent to " + clientKey);

            } catch (IOException | InterruptedException e) {
                System.err.println("Error sending response to " + clientKey + ": " + e.getMessage());
            }
        }
    }

    /**
     * Основной метод обработки команд с аутентификацией
     */
    private static Object processCommandWithMap(CommandWrapper commandWrapper) {
        try {
            String commandName = commandWrapper.getCommandName();
            Command command = commands.get(commandName);

            if (command == null) {
                return "Error: Unknown command '" + commandName + "'";
            }

            // Команды, доступные без аутентификации
            boolean isAuthCommand = "register".equals(commandName) || "login".equals(commandName);

            // Проверка аутентификации для всех команд, кроме register/login
            User user = null;
            if (!isAuthCommand) {
                user = authenticateUser(commandWrapper);
                if (user == null) {
                    return "Error: Authentication required. Please login first.";
                }
            }

            // Обработка команд аутентификации
            if (isAuthCommand) {
                if (command instanceof RegisterCommand) {
                    ((RegisterCommand) command).setCredentials(
                            commandWrapper.getLogin(),
                            commandWrapper.getPasswordHash()
                    );
                } else if (command instanceof LoginCommand) {
                    ((LoginCommand) command).setCredentials(
                            commandWrapper.getLogin(),
                            commandWrapper.getPasswordHash()
                    );
                }
                return command.execute();
            }

            // Проверка прав доступа для команд, требующих аутентификации
            if (command.requiresUser() && user == null) {
                return "Error: Authentication required for command '" + commandName + "'";
            }

            // Обработка аргументов команд
            if (command instanceof CommandWithArgument) {
                processCommandArguments((CommandWithArgument<?>) command, commandWrapper, commandName);
            }

            // Выполнение команды
            return executeCommand(command, commandWrapper, user);

        } catch (Exception e) {
            return "Error processing command: " + e.getMessage();
        }
    }

    /**
     * Обработка аргументов команд
     */
    private static void processCommandArguments(CommandWithArgument<?> command,
                                                CommandWrapper wrapper, String commandName) {
        switch (commandName) {
            case "insert":
            case "update":
            case "remove_key":
            case "remove_lower_key":
            case "replace_if_lower":
                if (wrapper.getKey() != null) {
                    command.setArgument(wrapper.getKey().toString());
                }
                break;

            case "filter_starts_with_name":
            case "execute_script":
                if (wrapper.getArgument() != null) {
                    command.setArgument(wrapper.getArgument().toString());
                }
                break;
        }
    }

    /**
     * Аутентификация пользователя
     */
    private static User authenticateUser(CommandWrapper commandWrapper) {
        if (commandWrapper.getLogin() == null || commandWrapper.getPasswordHash() == null) {
            return null;
        }

        try {
            return dbManager.authenticateUser(commandWrapper.getLogin(), commandWrapper.getPasswordHash());
        } catch (SQLException e) {
            System.err.println("Database error during authentication: " + e.getMessage());
            return null;
        }
    }

    /**
     * Выполнение команды с передачей пользователя
     */
    private static Object executeCommand(Command command, CommandWrapper commandWrapper, User user) {
        try {
            String commandName = command.getCommandName();

            // Обработка команд с MusicBand
            if (commandName.equals("insert") || commandName.equals("update") ||
                    commandName.equals("replace_if_lower") || commandName.equals("remove_lower")) {

                if (commandWrapper.getMusicBand() != null) {
                    return executeMusicBandCommand(command, commandWrapper.getMusicBand(), user);
                }
                return "Error: No MusicBand data provided";
            }

            // Обработка команд, требующих пользователя
            if (command instanceof CommandWithUser) {
                return ((CommandWithUser) command).execute(user);
            }

            // Обработка команд без аутентификации
            return processSimpleCommand(command);

        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }

    /**
     * Выполнение команд с MusicBand
     */
    private static Object executeMusicBandCommand(Command command, MusicBand band, User user) {
        if (command instanceof Insert) {
            return ((Insert) command).executeWithMusicBand(band, user);
        } else if (command instanceof Update) {
            return ((Update) command).executeWithMusicBand(band, user);
        } else if (command instanceof Replace_if_lower) {
            return ((Replace_if_lower) command).executeWithMusicBand(band, user);
        } else if (command instanceof Remove_lower) {
            return ((Remove_lower) command).executeWithMusicBand(band, user);
        }
        return "Error: Unsupported command type";
    }

    /**
     * Обработка простых команд (разбивка на чанки)
     */
    private static Object processSimpleCommand(Command command) {
        String result = command.execute();
        int chunkSizeBytes = 60000;
        byte[] allBytes = result.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        if (allBytes.length <= chunkSizeBytes) {
            return result; // Не нужно разбивать на чанки
        }

        // Разбивка большого ответа на чанки
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < allBytes.length; i += chunkSizeBytes) {
            int end = Math.min(i + chunkSizeBytes, allBytes.length);
            byte[] chunkBytes = new byte[end - i];
            System.arraycopy(allBytes, i, chunkBytes, 0, chunkBytes.length);
            String chunk = new String(chunkBytes, java.nio.charset.StandardCharsets.UTF_8);
            chunks.add(chunk);
        }
        return chunks;
    }

    /**
     * Отправка ответа клиенту
     */
    private static void sendResponse(DatagramSocket socket, DatagramPacket receivePacket, Object response)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(response);
        oos.flush();

        byte[] responseData = baos.toByteArray();
        DatagramPacket responsePacket = new DatagramPacket(
                responseData, responseData.length,
                receivePacket.getAddress(), receivePacket.getPort()
        );
        socket.send(responsePacket);
    }

    /**
     * Отправка ошибки клиенту
     */
    private static void sendErrorResponse(DatagramSocket socket, DatagramPacket receivePacket, String errorMessage) {
        try {
            sendResponse(socket, receivePacket, errorMessage);
        } catch (IOException ex) {
            System.err.println("Failed to send error response: " + ex.getMessage());
        }
    }

    /**
     * Корректное завершение работы сервера
     */
    private static void shutdownServer() {
        System.out.println("Shutting down server...");

        // Завершаем работу пулов потоков
        shutdownPools();

        // Закрываем соединение с БД
        if (dbManager != null) {
            try {
                dbManager.close();
                System.out.println("Database connection closed");
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }

        System.out.println("Server shutdown complete");
    }

    /**
     * Корректное завершение работы пулов потоков
     */
    private static void shutdownPools() {
        System.out.println("Shutting down thread pools...");

        connectionPool.shutdown();
        processingPool.shutdown();
        sendingPool.shutdown();

        try {
            if (!connectionPool.awaitTermination(5, TimeUnit.SECONDS)) {
                connectionPool.shutdownNow();
            }
            if (!processingPool.awaitTermination(5, TimeUnit.SECONDS)) {
                processingPool.shutdownNow();
            }
            if (!sendingPool.awaitTermination(5, TimeUnit.SECONDS)) {
                sendingPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            connectionPool.shutdownNow();
            processingPool.shutdownNow();
            sendingPool.shutdownNow();
        }
    }

    /**
     * Вспомогательный метод для получения информации о клиенте
     */
    private static String getClientInfo(DatagramPacket packet) {
        return packet.getAddress().getHostAddress() + ":" + packet.getPort();
    }
}