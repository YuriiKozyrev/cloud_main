import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;


public class Main {
    private Vector<ClientHandler> clients;
    ServerSocket server = null;
    Socket socket = null;


    public Main() {

        clients = new Vector<>();

        try{
            AuthService.connect();
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
            AuthService.disconnect();
        }
    }

    public boolean isLoginBusy(String login) {
        for (ClientHandler o : clients) {
            if (o.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void subscribe(ClientHandler client) {
        clients.add(client);
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
    }



}



