package org.example;

import data.CommandWrapper;
import data.MusicBand;
import utils.Console;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class ClientMain {

    private static String SERVER_HOST = "localhost";
    private static int SERVER_PORT = 12345;

    // Новые поля для хранения учетных данных
    private static String userLogin;
    private static String userPasswordHash;

    public static void main(String[] args) {
        if (!authenticateUser()) {
            System.out.println("Shutting down...");
            return;
        }

        System.out.println("Client started. Type 'help' for available commands.");

        Console console = new Console();

        while (true) {
            Console.CommandInput input = console.readCommand();
            if (input == null) {
                System.out.println("Shutting down...");
                break;
            }

            if (!Console.isValidCommand(input.command)) {
                System.out.printf("There is no command '%s'\n", input.command);
                continue;
            }

            if (input.command.equals("exit")) {
                System.out.println("Shutting down...");
                break;
            }

            try {
                String validationError = validateCommandInput(input);
                if (validationError != null) {
                    System.out.println(validationError);
                    continue;
                }

                MusicBand musicBand = null;
                if (input.command.equals("remove_lower") ||
                        input.command.equals("insert") ||
                        input.command.equals("update") ||
                        input.command.equals("replace_if_lower")) {

                    System.out.println("Please enter MusicBand details:");
                    musicBand = console.readMusicBand();
                    if (musicBand == null) {
                        System.out.println("Error: Failed to read MusicBand");
                        continue;
                    }
                }

                Object response = sendCommandToServer(input, musicBand);

                // ИЗМЕНЕНО: Добавлена обработка chunk'ов (ArrayList) от сервера
                if (response instanceof ArrayList) {
                    // Сервер отправил ответ, разбитый на части - собираем и выводим
                    ArrayList<String> chunks = (ArrayList<String>) response;
                    for (String chunk : chunks) {
                        System.out.print(chunk); // Выводим все части подряд без переносов
                    }
                    System.out.println(); // Добавляем перенос строки после всего сообщения
                } else if (response instanceof Object[]) {
                    Arrays.stream((Object[]) response).forEach(System.out::println);
                } else {
                    System.out.println(response);
                }
            } catch (IOException e) {
                System.err.println("Server communication error: " + e.getMessage());
                System.out.println("Retrying in 3 seconds...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 algorithm not available", e);
        }
    }

    private static boolean  authenticateUser() {
        Console console = new Console();

        System.out.println("=== Authentication ===");
        try {
            String login = null;
            String password = null;
            String choice = null;
            String con;

            while (login == null) {
                System.out.print("Login (or 'exit' to quit): ");
                con = console.readLine();
                if (!con.trim().isEmpty()) {
                    if (con.equalsIgnoreCase("exit")) {
                        return false;
                    } else {
                        login = con;
                    }
                } else {
                    System.out.println("Login cannot be empty");
                }
            }


            while (password == null) {
                System.out.print("Password (or 'exit' to quit): ");
                con = console.readLine();
                if (!con.trim().isEmpty()) {
                    if (con.equalsIgnoreCase("exit")) {
                        return false;
                    } else {
                        password = con;
                    }
                } else {
                    System.out.println("Password cannot be empty");
                }
            }

            while (choice == null || !choice.equalsIgnoreCase("r") && !choice.equalsIgnoreCase("l") && !choice.equalsIgnoreCase("exit")) {
                System.out.print("Register (r) or Login (l)? (or 'exit' to quit): ");
                con = console.readLine();
                if (!con.trim().isEmpty()) {
                    if (con.equalsIgnoreCase("exit")) {
                        return false;
                    } else if (con.equalsIgnoreCase("r") || con.equalsIgnoreCase("l")){
                        choice = con;
                    }
                } else {
                    System.out.println("Invalid choice. Use 'r' for register or 'l' for login");
                }
            }

            String passwordHash = hashPassword(password);
            String commandName = choice.equalsIgnoreCase("r") ? "register" : "login";

            // Отправляем команду аутентификации
            Object response = sendAuthCommand(commandName, login, passwordHash);
            System.out.println(response);

            if (response.toString().contains("successfully") ||
                    response.toString().contains("successful")) {
                userLogin = login;
                userPasswordHash = passwordHash;
                return true;
            }
        } catch (IOException e){
            System.err.println("Authenticate error: " + e.getMessage());
        }
        return false;
    }

//    private static Object sendAuthCommand(String commandName, String login, String passwordHash) throws IOException {
//        try (DatagramChannel channel = DatagramChannel.open()) {
//            // ИЗМЕНЕНО: Переход на блокирующий режим
//            channel.configureBlocking(true); // Блокирующий режим - ждем ответа
//            channel.socket().setSoTimeout(5000); // 5 секунд
//            try{
//                channel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
//
//                // Создаем CommandWrapper для аутентификации
//                CommandWrapper wrapper = new CommandWrapper();
//                wrapper.setCommandName(commandName);
//                wrapper.setLogin(login);
//                wrapper.setPasswordHash(passwordHash);
//
//                // Отправляем и получаем ответ
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                ObjectOutputStream oos = new ObjectOutputStream(baos);
//                oos.writeObject(wrapper);
//                oos.flush();
//
//                byte[] requestData = baos.toByteArray();
//                ByteBuffer buffer = ByteBuffer.wrap(requestData);
//                channel.write(buffer);
//
//                // ИЗМЕНЕНО: Блокирующее чтение - канал будет ждать ответа
//                ByteBuffer responseBuffer = ByteBuffer.allocate(65536);
//                int bytesRead = channel.read(responseBuffer); // Блокируется здесь до получения данных
//
//                if (bytesRead == -1) {
//                    throw new IOException("Connection closed by server");
//                }
//
//                responseBuffer.flip();
//                byte[] responseData = new byte[responseBuffer.remaining()];
//                responseBuffer.get(responseData);
//
//                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(responseData));
//                return ois.readObject();
//            }catch (NoRouteToHostException e) {
//                // Сервер недоступен
//                throw new IOException("Server is unreachable", e);
//            } catch (ConnectException e) {
//                // Соединение отклонено
//                throw new IOException("Connection refused by server", e);
//            } catch (SocketTimeoutException e) {
//                // Таймаут соединения
//                throw new IOException("Connection timeout", e);
//            }
//
//        } catch (ClassNotFoundException e) {
//            throw new IOException("Error deserializing response", e);
//        }
//    }

    private static Object sendAuthCommand(String commandName, String login, String passwordHash) throws IOException {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.configureBlocking(true);
            channel.socket().setSoTimeout(5000); // 5 секунд таймаут на чтение

            try {
                channel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));

                // Создаем CommandWrapper для аутентификации
                CommandWrapper wrapper = new CommandWrapper();
                wrapper.setCommandName(commandName);
                wrapper.setLogin(login);
                wrapper.setPasswordHash(passwordHash);

                // Отправляем и получаем ответ
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(wrapper);
                oos.flush();

                byte[] requestData = baos.toByteArray();
                ByteBuffer buffer = ByteBuffer.wrap(requestData);
                channel.write(buffer);

                ByteBuffer responseBuffer = ByteBuffer.allocate(65536);
                int bytesRead = channel.read(responseBuffer); // Блокируется здесь

                if (bytesRead == -1) {
                    throw new IOException("Connection closed by server");
                }

                responseBuffer.flip();
                byte[] responseData = new byte[responseBuffer.remaining()];
                responseBuffer.get(responseData);

                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(responseData));
                return ois.readObject();

            } catch (SocketTimeoutException e) {
                // Таймаут чтения - сервер не ответил
                throw new IOException("Server did not respond within timeout", e);
            } catch (PortUnreachableException e) {
                // Порт недоступен (ICMP сообщение)
                throw new IOException("Server port is unreachable", e);
            } catch (IOException e) {
                // Общая обработка других IO исключений
                if (e.getMessage() != null) {
                    throw new IOException("Network error: " + e.getMessage(), e);
                } else {
                    throw new IOException("Unknown network error", e);
                }
            }

        } catch (ClassNotFoundException e) {
            throw new IOException("Error deserializing response", e);
        }
    }

    private static String validateCommandInput(Console.CommandInput input) {
        switch (input.command) {
            case "insert":
            case "update":
            case "replace_if_lower":
            case "remove_key":
            case "remove_lower_key":
                if (input.argument == null || input.argument.trim().isEmpty()) {
                    return "Error: Command '" + input.command + "' requires a numeric argument (key)";
                }
                try {
                    Long arg = Long.parseLong(input.argument.trim());
                    if (arg <= 0) {
                        throw new NumberFormatException("вот-вот");
                    }
                } catch (NumberFormatException e) {
                    return "Error: Argument for '" + input.command + "' must be a positive integer";
                }
                break;

            case "filter_starts_with_name":
                if (input.argument == null || input.argument.trim().isEmpty()) {
                    return "Error: Command '" + input.command + "' requires a string argument";
                }
                break;
        }
        return null;
    }

    private static Object sendCommandToServer(Console.CommandInput input, MusicBand musicBand) throws IOException {
        try (DatagramChannel channel = DatagramChannel.open()) {
            // ИЗМЕНЕНО: Переход на блокирующий режим
            channel.configureBlocking(true); // Блокирующий режим - канал будет ждать операций
            channel.socket().setSoTimeout(5000); // ← ДОБАВЬТЕ ЭТУ СТРОЧКУ!

            try {
                channel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));

//                if (!channel.isConnected()) {
//                    throw new IOException("Failed to establish address");
//                }

                CommandWrapper commandWrapper = createCommandWrapper(input, musicBand);
                if (commandWrapper == null) {
                    return "Error: Failed to create command wrapper";
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(commandWrapper);
                oos.flush();

                byte[] requestData = baos.toByteArray();
                ByteBuffer buffer = ByteBuffer.wrap(requestData);
                channel.write(buffer);

                // ИЗМЕНЕНО: Упрощенная логика чтения в блокирующем режиме
                ByteBuffer responseBuffer = ByteBuffer.allocate(65536);
                int bytesRead = channel.read(responseBuffer); // Блокируется здесь до получения ответа

                if (bytesRead == -1) {
                    throw new IOException("Connection closed by server");
                }

                responseBuffer.flip();
                byte[] responseData = new byte[responseBuffer.remaining()];
                responseBuffer.get(responseData);

                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(responseData));
                return ois.readObject();
            }catch (SocketTimeoutException e) {
                // Таймаут чтения - сервер не ответил
                throw new IOException("Server did not respond within timeout", e);
            } catch (PortUnreachableException e) {
                // Порт недоступен (ICMP сообщение)
                throw new IOException("Server port is unreachable", e);
            } catch (IOException e) {
                String errorMsg = e.getMessage();
                if (errorMsg == null) {
                    errorMsg = "Unknown network error";
                } else if (errorMsg.contains("Connection refused")) {
                    errorMsg = "Server refused connection (may be offline)";
                } else if (errorMsg.contains("Network is unreachable")) {
                    errorMsg = "Network is unreachable";
                }
                throw new IOException(errorMsg, e);
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Error deserializing response", e);
        }
    }

    private static CommandWrapper createCommandWrapper(Console.CommandInput input, MusicBand musicBand) {
        try {
            CommandWrapper wrapper = new CommandWrapper();
            wrapper.setCommandName(input.command);

            // ДОБАВЛЯЕМ УЧЕТНЫЕ ДАННЫЕ К КАЖДОМУ ЗАПРОСУ
            wrapper.setLogin(userLogin);
            wrapper.setPasswordHash(userPasswordHash);

            switch (input.command) {
                case "remove_lower":
                    wrapper.setMusicBand(musicBand);
                    break;

                case "insert":
                case "update":
                case "replace_if_lower":
                    if (input.argument == null || input.argument.trim().isEmpty()) {
                        System.out.println("Error: Command '" + input.command + "' requires a key argument");
                        return null;
                    }
                    try {
                        long key = Long.parseLong(input.argument);
                        wrapper.setKey(key);
                        wrapper.setArgument(key);
                        wrapper.setMusicBand(musicBand);
                    } catch (NumberFormatException e) {
                        System.out.println("Error: Key must be a positive integer for command '" + input.command + "'");
                        return null;
                    }
                    break;

                case "remove_key":
                case "remove_lower_key":
                    if (input.argument == null || input.argument.trim().isEmpty()) {
                        System.out.println("Error: Command '" + input.command + "' requires a key argument");
                        return null;
                    }
                    try {
                        long key = Long.parseLong(input.argument);
                        wrapper.setKey(key);
                        wrapper.setArgument(key);
                    } catch (NumberFormatException e) {
                        System.out.println("Error: Key must be a valid number for command '" + input.command + "'");
                        return null;
                    }
                    break;

                case "filter_starts_with_name":
                case "execute_script":
                    if (input.argument == null || input.argument.trim().isEmpty()) {
                        System.out.println("Error: Command '" + input.command + "' requires a string argument");
                        return null;
                    }
                    wrapper.setArgument(input.argument);
                    break;
            }

            return wrapper;

        } catch (Exception e) {
            System.out.println("Error creating command: " + e.getMessage());
            return null;
        }
    }
}