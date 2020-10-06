import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    ServerSocket server = null;
    Socket socket = null;

    public Main() {

        try{
            server = new ServerSocket(8189);
            System.out.println("Сервер запущен");

            while(true) {
                Socket socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(socket, this);
            }

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try{
                socket.close();
                System.out.println("Клиент отключился");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try{
                server.close();
                System.out.println("Сервер отключился");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}



