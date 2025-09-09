package org.example;

import commands.*;
import commands.commandsWithArgument.*;
import data.CommandWrapper;
import utils.CommandMap;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class ServerMain {

    private static final int PORT = 12345;
    private static final int BUFFER_SIZE = 65536;
    private static File file_csv;
    private static Map<String, Command> commands;
    private static Executor executor;

    private static Set<String> connectedClients = new HashSet<>();

    public static void main(String[] args) {
        // Параметры для подключения к БД (возможно, вынесете их в аргументы командной строки)
        String dbHost = "pg"; // Хост из задания
        String dbName = "studs"; // Имя базы данных из задания
        String dbUrl = "jdbc:postgresql://" + dbHost + "/" + dbName;
        String dbUser = "s465345"; // Здесь ваш личный логин для доступа к серверу кафедры
        String dbPassword = "dcxOUhUo8IMFxqAD"; // Ваш пароль

        // 1. Подключение к БД и автоматическое создание таблиц
        DatabaseManager dbManager;
        try {
            dbManager = new DatabaseManager(dbUrl, dbUser, dbPassword);
            System.out.println("hello from database");
        } catch (SQLException e) {
            System.err.println("Fatal: Cannot connect to database: " + e.getMessage());
            System.exit(1);
            return;
        }

        executor = new Executor(file_csv);
        commands = CommandMap.createMapWithCommands(executor);

        System.out.println("Server started. Loaded " + executor.getSizeOfCollection() + " music bands.");

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("Server started on all interfaces. Port: " + PORT);

            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                String clientKey = clientAddress.getHostAddress() + ":" + clientPort;
                if (!connectedClients.contains(clientKey)) {
                    connectedClients.add(clientKey);
                    System.out.println("Подключился новый клиент: " + clientAddress.getHostAddress() + ", порт: " + clientPort);
                }

                try {
                    ObjectInputStream ois = new ObjectInputStream(
                            new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength()));
                    CommandWrapper commandWrapper = (CommandWrapper) ois.readObject();
                    System.out.println("Received command: " + commandWrapper.getCommandName());

                    Object response = processCommandWithMap(commandWrapper);

                    if (response instanceof ArrayList) {
                        ArrayList<String> responseList = (ArrayList<String>) response;

                        for (String responseString : responseList) {
                            // Сериализуем каждую строку отдельно
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = new ObjectOutputStream(baos);
                            oos.writeObject(responseString); // Отправляем строку, а не весь список
                            oos.flush();

                            byte[] responseData = baos.toByteArray();

                            DatagramPacket responsePacket = new DatagramPacket(
                                    responseData, responseData.length, clientAddress, clientPort);
                            socket.send(responsePacket);

                            // Небольшая задержка между пакетами (опционально)
                            try { Thread.sleep(10); } catch (InterruptedException e) {}
                        }
                    } else {
                        // Обработка случая, когда response не ArrayList (старая логика)
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(response);
                        oos.flush();

                        byte[] responseData = baos.toByteArray();

                        DatagramPacket responsePacket = new DatagramPacket(
                                responseData, responseData.length, clientAddress, clientPort);
                        socket.send(responsePacket);
                    }

                } catch (Exception e) {
                    System.err.println("Error processing command: " + e.getMessage());
                    sendErrorResponse(socket, clientAddress, clientPort, "Error: " + e.getMessage());
                }
            }
        } catch (SocketException e) {
            System.err.println("Server socket error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Server I/O error: " + e.getMessage());
        }
    }

    private static Object processCommandWithMap(CommandWrapper commandWrapper) {
        try {
            String commandName = commandWrapper.getCommandName();

            Command command = commands.get(commandName);

            if (command == null) {
                return "Error: Unknown command '" + commandName + "'";
            }

            if (command instanceof CommandWithArgument) {
                CommandWithArgument<?> commandWithArg = (CommandWithArgument<?>) command;

                switch (commandName) {
                    case "insert":
                    case "update":
                    case "remove_key":
                    case "remove_lower_key":
                    case "replace_if_lower":
                        if (commandWrapper.getKey() != null) {
                            commandWithArg.setArgument(commandWrapper.getKey().toString());
                        } else {
                            return "Error: Command '" + commandName + "' requires a key argument";
                        }
                        break;

                    case "filter_starts_with_name":
                    case "execute_script":
                        if (commandWrapper.getArgument() != null) {
                            commandWithArg.setArgument(commandWrapper.getArgument().toString());
                        } else {
                            return "Error: Command '" + commandName + "' requires a string argument";
                        }
                        break;
                }
            }

            return executeCommand(command, commandWrapper);

        } catch (Exception e) {
            return "Error processing command: " + e.getMessage();
        }
    }

    private static Object executeCommand(Command command, CommandWrapper commandWrapper) {
        try {
            String commandName = command.getCommandName();

            if (commandName.equals("insert") || commandName.equals("update") || commandName.equals("replace_if_lower") || commandName.equals("remove_lower")) {
                if (commandWrapper.getMusicBand() != null) {
                    if (command instanceof Insert) {
                        return ((Insert) command).executeWithMusicBand(commandWrapper.getMusicBand());
                    } else if (command instanceof Update) {
                        return ((Update) command).executeWithMusicBand(commandWrapper.getMusicBand());
                    } else if (command instanceof Replace_if_lower) {
                        return ((Replace_if_lower) command).executeWithMusicBand(commandWrapper.getMusicBand());
                    } else if (command instanceof Remove_lower) {
                        return ((Remove_lower) command).executeWithMusicBand(commandWrapper.getMusicBand());
                    }
                }
                return "Error: No MusicBand data provided for command '" + commandName + "'";
            }

            String res = command.execute();
            int chunkSizeBytes = 60000;

            byte[] allBytes = res.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            java.util.List<String> chunks = new java.util.ArrayList<>();

            for (int i = 0; i < allBytes.length; i += chunkSizeBytes) {
                int end = Math.min(i + chunkSizeBytes, allBytes.length);
                byte[] chunkBytes = new byte[end - i];
                System.arraycopy(allBytes, i, chunkBytes, 0, chunkBytes.length);
                String chunk = new String(chunkBytes, java.nio.charset.StandardCharsets.UTF_8);
                chunks.add(chunk);
            }

            return chunks;

        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }

    private static void sendErrorResponse(DatagramSocket socket, InetAddress clientAddress, int clientPort, String errorMessage) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(errorMessage);
            oos.flush();

            byte[] responseData = baos.toByteArray();
            DatagramPacket responsePacket = new DatagramPacket(
                    responseData, responseData.length, clientAddress, clientPort);

            socket.send(responsePacket);
        } catch (IOException ex) {
            System.err.println("Failed to send error response: " + ex.getMessage());
        }
    }
}