import java.io.*;
import java.net.Socket;

public class ClientHandler {

    private static String login;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Main server;
    private static final String serverPath = "server/src/main/resources/server_dir";
    private File pathToClientFiles;
    private final int BUFFER = 256;

    public String getLogin(){
        return login;
    }


    public ClientHandler(final Socket socket, final Main server) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            //поток на прием от Клиента на Сервер
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        // блок авторизации
                        while (true) {
                            //1.получить логин и пароль для входа
                            String fileName = in.readUTF();

                            String[] tokens = fileName.split(" ");
                            String newLogin = AuthService.getLoginByLoginAndPass(tokens[0], tokens[1]);

                            if(newLogin != null) {
                                out.writeUTF("./authOK" + " " + newLogin);
                                login = newLogin;
                                server.subscribe(ClientHandler.this);
                                pathToClientFiles = createPathToClientFiles(login);
                                break;

                            } else System.out.println("Неверный логин/пароль");
                        }

                        //блок работы с файлами
                        while (true){
                            String fileName = in.readUTF();

                            if (fileName.equals("./getFilesList")) {
                                out.writeUTF("./receiveFlag");
                                sendFilesList();

                            } else if (fileName.startsWith("./getFileFromServer ")) {
                                String[] tokens = fileName.split(" ", 2);
                                System.out.println("Имя файла для передачи Клиенту: " + tokens[1]);
                                getFileFromServerToClient(tokens[1]);

                            } else if (fileName.startsWith("./deleteFileFromServer ")) {
                                String[] tokens = fileName.split(" ", 2);
                                deleteFile(tokens[1]);

                            } else if (fileName.startsWith("./renameFileFromServer")) {
                                String[] tokens = fileName.split(" ", 3);
                                serverFileRename(tokens[1], tokens[2]);

                            } else putFileFromClientToServer(fileName);
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
                    server.unsubscribe(ClientHandler.this);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFilesList() throws IOException {
        File dir = new File(String.valueOf(pathToClientFiles));
        String[] files = dir.list();
        if (files != null) {
            out.writeInt(files.length);
            for (String file : files) {
                out.writeUTF(file);
            }
        } else out.writeInt(0);
        out.flush();
    }

    //метод для приема файлов от Клиента на Сервер
    public void putFileFromClientToServer(String fileName) throws IOException {
        System.out.println("Принимаем файл для сохранения в Облако: " + fileName);
        File file = new File(pathToClientFiles + "/" + fileName);
        System.out.println(file.exists());
        if (!file.exists()) {
            file.createNewFile();
        } else {
            System.out.println("Такой файл уже есть в Облаке");
        }
        FileOutputStream os = new FileOutputStream(file);

        //2. получить размер файла, который мы должны принять
        long fileLength = in.readLong();
        System.out.println("Готовимся принять: " + fileLength + " байт");

        //3. получить байты самого файла
        byte[] buf = new byte[BUFFER];  //делаем буферизацию на уровне сервера
        for (int i = 0; i < (fileLength + (BUFFER -1)) / BUFFER; i++) {
            int cnt = in.read(buf);
            os.write(buf, 0, cnt);
        }
        System.out.println("Файл успешно принят в Облако.");

        os.close();
    }

    // метод для передачи файлов от Сервера на Клиент
    public void getFileFromServerToClient(String fileName) throws IOException {

        out.writeUTF(fileName);
        File currentFile = new File(pathToClientFiles + "/" + fileName);
        out.writeLong(currentFile.length());
        FileInputStream is = new FileInputStream(currentFile);

        int tmp;
        byte[] buffer = new byte[BUFFER];          //делаем буфер
        while ((tmp = is.read(buffer)) != -1) {  //проверка есть ли данные на потоке
            out.write(buffer, 0, tmp);
        }
        System.out.println("Файл успешно передан Клиенту.");
        out.flush();
        is.close();
    }

    //удаление файла на Сервере
    public void deleteFile(String fileName){
        File currentFile = new File(pathToClientFiles + "/" + fileName);
        currentFile.delete();
        System.out.println(currentFile.getName() + " Удален");
    }

    //переименование файла на Сервере
    public void  serverFileRename (String fileRenameFrom, String fileRenameTo){
        File currentFile = new File(pathToClientFiles + "/" + fileRenameFrom);
        File newFile = new File(pathToClientFiles + "/" + fileRenameTo);
        if(currentFile.renameTo(newFile)){
            System.out.println("Файл " + currentFile + " переименован в " + newFile +  " успешно");;
        }else System.out.println("Файл не был переименован");
    }

    //создание пути к клиентской папке
    public File createPathToClientFiles(String login){
         File file = new File(serverPath + "_" + login);
         file.mkdir();
         return file;
    }
}
