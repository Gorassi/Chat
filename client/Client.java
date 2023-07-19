package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;

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

    }
}
