package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {

    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
         new Client().run();
    }

    public void run(){
        Thread thread = getSocketThread();
        thread.setDaemon(true);
        thread.start();
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Waiting is interrupted on Client.line 22");
                System.exit(-1);
            }
        }
        if(clientConnected){
            ConsoleHelper.writeMessage("Соединение установлено.\n" +
                    "Для выхода наберите команду 'exit'.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }
        while(clientConnected){
            String str = ConsoleHelper.readString();
            if(shouldSendTextFromConsole()) sendTextMessage(str);
            if(str.equals("exit")) break;
        }
    }
    
    protected String getServerAddress(){
        ConsoleHelper.writeMessage("Enter, please, ip server :");
        return ConsoleHelper.readString();
    }
    
    protected int getServerPort(){
        ConsoleHelper.writeMessage("Enter, please, server port :");
        return ConsoleHelper.readInt();
    }
    
    protected String getUserName(){
        ConsoleHelper.writeMessage("Enter, please, ip server :");
         return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole(){return true;}

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }
    
    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Didn't send message.");
            clientConnected = false;
        }
    }

    public class SocketThread extends Thread{

        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " added to Chat");
        }

        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " left the Chat");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException{

            while(true) {
                Message message = connection.receive();
                if(message.getType()== MessageType.NAME_REQUEST){
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                    continue;
                }
                if(message.getType()==MessageType.NAME_ACCEPTED){
                    notifyConnectionStatusChanged(true);
                    break;
                }
                throw new IOException("Unexpected MessageType");
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            Message message = null;
            while(true) {
                message = connection.receive();
                MessageType type = message.getType();
                if(type == null) throw new IOException("Unexpected MessageType");
                switch (type) {
                    case TEXT:
                        processIncomingMessage(message.getData());
                        break;
                    case USER_ADDED:
                        informAboutAddingNewUser(message.getData());
                        break;
                    case USER_REMOVED:
                        informAboutDeletingNewUser(message.getData());
                        break;
                    default:
                        throw new IOException("Unexpected MessageType");
                }
            }
        }

        @Override
        public void run(){
            String ip = getServerAddress();
            int port = getServerPort();
            try {
                Socket socket = new Socket(ip, port);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }

    }
}
