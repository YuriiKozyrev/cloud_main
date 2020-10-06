import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


public class Controller implements Initializable {

    public ListView<String> clientListView;
    public ListView<String> serverListView;
    private final String clientPath = "client/src/main/resources/client_dir";
    private final int BUFFER = 256;

    private DataInputStream in;
    private DataOutputStream out;

    public void initialize(URL location, ResourceBundle resources) {

        try {
            final Socket socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            refreshClientList();
            sendCommandGetServerFiles();

            //поток на прием файлов от Сервера на Клиент
            new Thread(new Runnable() {
                @Override
                public void run(){
                    try{
                        while (true){
                            //1.получить имя файла
                            String fileName = in.readUTF();

                            if (fileName.equals("./receiveFlag")) {

                                //получение списка файлов Сервера
                                List<String> files = new ArrayList<>();
                                int listSize = in.readInt();
                                for (int i = 0; i < listSize; i++) {
                                    files.add(in.readUTF());
                                }

                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                            serverListView.getItems().clear();
                                            serverListView.getItems().addAll(files);
                                    }
                                });
                            }
                             else getFileFromServerToClient(fileName);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
        e.printStackTrace();
        }
    }

    //метод для приема файлов от Сервера на Клиент
    public void getFileFromServerToClient(String fileName) throws IOException {
        System.out.println("Файл для сохранения на Клиенте: " + fileName);
        File file = new File("client/src/main/resources/client_dir/" + fileName);
        if (!file.exists()) {
            file.createNewFile();
        } else {
            System.out.println("Такой файл уже есть на клиенте");
        }
        FileOutputStream os = new FileOutputStream(file);

        //2. получить размер файла, который мы должны принять
        long fileLength = in.readLong();
        System.out.println("Готовимся принять: " + fileLength + " байт");

        //3. получить байты самого файла
        byte[] buffer = new byte[256];  //делаем буферизацию на уровне сервера
        for (int i = 0; i < (fileLength + (BUFFER -1)) / BUFFER; i++) {
            int cnt = in.read(buffer);
            os.write(buffer, 0, cnt);
        }
        System.out.println("Файл успешно получен Клиентом.");
        os.close();
    }

    // метод для передачи файла от Клиента на Сервер
    public void putFileFromClientToServer(ActionEvent actionEvent) throws IOException {
        //выбор файла для передачи
        String file = clientListView.getSelectionModel().getSelectedItem();
        System.out.println("Файл для передачи в Облако: " + file);
        out.writeUTF(file);

        File currentFile = new File(clientPath + "/" + file);

        out.writeLong(currentFile.length());
        FileInputStream is = new FileInputStream(currentFile);

        int tmp;
        byte[] buffer = new byte[BUFFER];          //делаем буфер
        while ((tmp = is.read(buffer)) != -1) {  //проверка есть ли данные на потоке
            out.write(buffer, 0, tmp);

        }
        System.out.println("Файл успешно передан в Облако.");
        out.flush();
        is.close();
    }

   //метод для передачи имени файла от Клиента на Сервер. В дальнейшем этот файл должен быть передан на клиента
    public void putServerFileNameToServer(ActionEvent actionEvent) throws IOException {
        getFileFromServer();
    }

    //построение списка файлов в поле "Клиент" приложения
    private void refreshClientList() {
        clientListView.getItems().clear();
        File file = new File(clientPath);
        String[] files = file.list();
        if (files != null) {
            for (String name : files) {
                clientListView.getItems().add(name);
            }
        }
    }

    //посылка команды на сервер ""./getFilesList"
    private void sendCommandGetServerFiles() throws IOException {
        out.writeUTF("./getFilesList");
        out.flush();
    }

    //посылка команды передачи файла с именем файла на Сервер
    private void  getFileFromServer() throws IOException {
        String fileName = serverListView.getSelectionModel().getSelectedItem();
        out.writeUTF("./getFileFromServer" + " " + fileName);
        out.flush();
    }

    //обновление списков файлов в поле "Клиент" и "Сервер" приложения
    public void refreshList(ActionEvent actionEvent) throws IOException {
        refreshClientList();
        sendCommandGetServerFiles();
    }
}
