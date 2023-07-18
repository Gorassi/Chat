package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Введите порт сервера:");
        int port = ConsoleHelper.readInt();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ConsoleHelper.writeMessage("Чат сервер запущен.");


            while (true) {
                // Ожидаем входящее соединение и запускаем отдельный поток при его принятии
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage("Произошла ошибка при запуске или работе сервера.");
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("New connection has made with remote address : "
            + socket.getRemoteSocketAddress());
            String userName = null;
            try (Connection connection = new Connection((socket))) {
                ;
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Error data exchange with remote adress : "
                        + socket.getRemoteSocketAddress());

            }
            if(userName!= null){
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }

                ConsoleHelper.writeMessage("Connection with address : "
                        + socket.getRemoteSocketAddress() + " closed");

        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));

                Message message = connection.receive();
                if (message.getType() != MessageType.USER_NAME) {
                    ConsoleHelper.writeMessage("Получено сообщение от " + socket.getRemoteSocketAddress() + ". Тип сообщения не соответствует протоколу.");
                    continue;
                }

                String userName = message.getData();

                if (userName.isEmpty()) {
                    ConsoleHelper.writeMessage("Попытка подключения к серверу с пустым именем от " + socket.getRemoteSocketAddress());
                    continue;
                }

                if (connectionMap.containsKey(userName)) {
                    ConsoleHelper.writeMessage("Попытка подключения к серверу с уже используемым именем от " + socket.getRemoteSocketAddress());
                    continue;
                }
                connectionMap.put(userName, connection);

                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return userName;
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                if (entry.getKey().equals(userName)) continue;
                connection.send(new Message(MessageType.USER_ADDED, entry.getKey()));
            }
        }

        private void serverMainLoop(Connection connection, String userName)
                throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                MessageType messageType = message.getType();
                if (messageType == MessageType.TEXT) {
                    String text = message.getData();
                    text = userName + ": " + text;
                    sendBroadcastMessage(new Message(MessageType.TEXT, text));
                } else {
                    ConsoleHelper.writeMessage("Ошибки в MainLoop: сообщение не ТЕКСТ");
                }
            }
        }
    }

    public static void sendBroadcastMessage(Message message){
        for(Connection connection : connectionMap.values()){
            try {
                connection.send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при рассылке сообщений");

            }
        }
    }

}