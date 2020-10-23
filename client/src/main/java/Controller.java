import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


public class Controller implements Initializable {

    @FXML
    ListView<String> clientList;

    @FXML
    ListView<String> serverList;

    @FXML
    TextField loginField;

    @FXML
    TextField passwordField;

    @FXML
    HBox upperPanel;

    @FXML
    AnchorPane bottomPanel;

    @FXML
    Label clientName;


    private final String clientPath = "client/src/main/resources/client_dir";
    private final int BUFFER = 512;
    private boolean isAuthorized;

    private DataInputStream in;
    private DataOutputStream out;
    Socket socket;
    private Object EventHandler;
    private String fileRenameFrom;
    private String fileRenameTo;

    public void setAuthorized(boolean isAuthorized){
        this.isAuthorized = isAuthorized;

        if(!isAuthorized){
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
        } else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
            clientList.setEditable(true);
            serverList.setEditable(true);
            clientList.setCellFactory(TextFieldListCell.forListView());
            serverList.setCellFactory(TextFieldListCell.forListView());

            clientList.setOnEditCommit(new EventHandler<ListView.EditEvent<String>>() {
                @Override
                public void handle(ListView.EditEvent<String> event) {
                    fileRenameFrom = event.getSource().getItems().get(event.getIndex());
                    fileRenameTo = event.getNewValue();
                    clientList.getItems().set(event.getIndex(), event.getNewValue());
                    clientFileRename(fileRenameFrom, fileRenameTo);
                }
            });

            serverList.setOnEditCommit(new EventHandler<ListView.EditEvent<String>>() {
                @Override
                public void handle(ListView.EditEvent<String> event) {
                    fileRenameFrom = event.getSource().getItems().get(event.getIndex());
                    fileRenameTo = event.getNewValue();
                    serverList.getItems().set(event.getIndex(), event.getNewValue());
                    try {
                        serverFileRename(fileRenameFrom, fileRenameTo);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void initialize(URL location, ResourceBundle resources) {

        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setAuthorized(false);

            //поток на прием файлов от Сервера на Клиент
            new Thread(new Runnable() {
                @Override
                public void run(){
                    try{
                        //блок авторизации
                        while (true) {
                            String fileName = in.readUTF();
                            if (fileName.startsWith("./authOK")) {
                                setAuthorized(true);
                                String[] tokens = fileName.split(" ", 2);
                                setClientName(tokens[1]);
                                break;
                            } else setAuthorized(false);
                        }

                        refreshClientList();
                        sendCommandGetServerFiles();

                        while (true) {
                            String fileName = in.readUTF();
                            if (fileName.equals("./receiveFlag")) {
                                refreshServerList();
                            } else getFileFromServerToClient(fileName);
                            refreshClientList();
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

    //Прием файла Клиентом от Сервера
    public void getFileFromServerToClient(String fileName) throws IOException {
        System.out.println("Файл для сохранения на Клиенте: " + fileName);
        File file = new File(clientPath + "/" + fileName);
        FileOutputStream os = new FileOutputStream(file);

        if (!file.exists()) {
            file.createNewFile();
        } else {
            System.out.println("Такой файл уже есть на Клиенте");
        }

        //2. получить размер файла, который мы должны принять
        long fileLength = in.readLong();
        System.out.println("Готовимся принять: " + fileLength + " байт");

        //3. получить байты самого файла
        byte[] bytes = new byte[BUFFER];
        int count;
        while(file.length() != fileLength){
            count = in.read(bytes);
            os.write(bytes, 0, count);
        }
        System.out.println("Файл успешно получен Клиентом.");
        refreshClientList();
        os.close();
    }


    // Передачи файла Клиентом на Сервер
    public void putFileFromClientToServer(ActionEvent actionEvent) throws IOException {
        //выбор файла для передачи
        String file = clientList.getSelectionModel().getSelectedItem();
        System.out.println("Файл для передачи в Облако: " + file);
        out.writeUTF("./putFile" + " " + file);

        File currentFile = new File(clientPath + "/" + file);
        FileInputStream is = new FileInputStream(currentFile);
        System.out.println("Длина файла для передачи в Облако: " + currentFile.length());
        out.writeLong(currentFile.length());
        out.flush();

        int tmp;
        byte [] buffer = new byte[BUFFER];
        while ((tmp = is.read(buffer)) != -1) {  //проверка есть ли данные на потоке
            out.write(buffer, 0, tmp);
        }
        out.flush();
        is.close();
    }



   //метод для передачи имени файла от Клиента на Сервер. В дальнейшем этот файл должен быть передан на клиента
    public void putServerFileNameFromClientToServer(ActionEvent actionEvent) throws IOException {
        getFileFromServer();
    }

    //построение списка файлов в поле "Клиент" приложения
    private void refreshClientList() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                clientList.getItems().clear();
                File file = new File(clientPath);
                String[] files = file.list();
                if (files != null) {
                    for (String name : files) {
                        clientList.getItems().add(name);
                    }
                }
            }
        });
    }

    //указываем имя клиента на приложении
    private void setClientName(String userName){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                clientName.setText(userName);
            }
        });
    }

    //построение списка файлов в поле "Сервер" приложения
    private void refreshServerList() throws IOException {
        //получение списка файлов Сервера
        List<String> files = new ArrayList<>();
        int listSize = in.readInt();
        for (int i = 0; i < listSize; i++) {
            files.add(in.readUTF());
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                serverList.getItems().clear();
                serverList.getItems().addAll(files);
            }
        });
    }

    //посылка команды на сервер ""./getFilesList"
    private void sendCommandGetServerFiles() throws IOException {
        String fileName = serverList.getSelectionModel().getSelectedItem();
        out.writeUTF("./getFilesList");
        out.flush();
    }

    //посылка команды передачи файла с именем файла на Сервер
    private void  getFileFromServer() throws IOException {
        String fileName = serverList.getSelectionModel().getSelectedItem();
        out.writeUTF("./getFileFromServer" + " " + fileName);
        out.flush();
    }

    //посылка команды для удаления файла с Сервера
    private void  deleteFileFromServer() throws IOException {
        String fileName = serverList.getSelectionModel().getSelectedItem();
        out.writeUTF("./deleteFileFromServer" + " " + fileName);
        out.flush();
    }

    //отправка на Сервер логина и пароля
    public void tryToAuth(ActionEvent actionEvent) {
        try {
            out.writeUTF(loginField.getText() + " " + passwordField.getText());
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //удаление файла
    public void deleteFile(ActionEvent actionEvent) throws IOException {
        String clientFile = clientList.getSelectionModel().getSelectedItem();
        if (!(clientFile == null)){
            File currentFile = new File(clientPath + "/" + clientFile);
            currentFile.delete();
            System.out.println(currentFile.getName() + "   удален");
            refreshClientList();
        } else {
            deleteFileFromServer();
            sendCommandGetServerFiles();
        }
    }

    private void serverFileRename(String fileRenameFrom, String fileRenameTo) throws IOException {
        System.out.println("Переименовываем на сервере " + fileRenameFrom + " ---> " + fileRenameTo);
        out.writeUTF("./renameFileFromServer" + " " + fileRenameFrom + " " + fileRenameTo);
        out.flush();
    }

    public void  clientFileRename (String fileRenameFrom, String fileRenameTo){
        System.out.println("Переименовываем на клиенте " + fileRenameFrom + " ---> " + fileRenameTo);
        File currentFile = new File(clientPath + "/" + fileRenameFrom);
        File newFile = new File(clientPath + "/" + fileRenameTo);
        if(currentFile.renameTo(newFile)){
            System.out.println("Файл " + currentFile + " переименован в " + newFile +  " успешно");;
        }else{
            System.out.println("Файл не был переименован");
        }
    }
}
