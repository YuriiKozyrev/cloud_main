import java.io.*;
import java.net.Socket;

public class ClientHandler {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Main server;
    private static final String serverPath = "server/src/main/resources/server_dir";
    private final int BUFFER = 256;


    public ClientHandler(final Socket socket, Main server) {
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
                        while (true) {
                            //1.получить имя файла
                            String fileName = in.readUTF();

                            if (fileName.equals("./getFilesList")) {
                                out.writeUTF("./receiveFlag");
                                getFilesList();

                            } else if (fileName.startsWith("./getFileFromServer ")) {
                                String[] tokens = fileName.split(" ", 2);
                                System.out.println("Имя файла для передачи Клиенту: " + tokens[1]);
                                putFileFromServerToClient(tokens[1]);

                            } else getFileFromClientToServer(fileName);
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

    private void getFilesList() throws IOException {
        File dir = new File(serverPath);
        String[] files = dir.list();
        if (files != null) {
            out.writeInt(files.length);
            for (String file : files) {
                out.writeUTF(file);
            }
        } else {
            out.writeInt(0);
        }
        out.flush();
    }

    //метод для приема файлов от Клиента на Сервер
    public void getFileFromClientToServer(String fileName) throws IOException {
        System.out.println("Файл для сохранения в Облако: " + fileName);
        File file = new File("server/src/main/resources/server_dir/" + fileName);
        if (!file.exists()) {
            file.createNewFile();
        } else {
            System.out.println("Такой файл уже есть на сервере");
        }
        FileOutputStream os = new FileOutputStream(fileName);

        //2. получить размер файла, который мы должны принять
        long fileLength = in.readLong();
        System.out.println("Готовимся принять: " + fileLength + " байт");

        //3. получить байты самого файла
        byte[] buf = new byte[256];  //делаем буферизацию на уровне сервера
        for (int i = 0; i < (fileLength + (BUFFER -1)) / BUFFER; i++) {
            int cnt = in.read(buf);
            os.write(buf, 0, cnt);
        }
        System.out.println("Файл успешно принят в Облако.");
        os.close();
    }

    // метод для передачи файлов от Сервера на Клиент
    public void putFileFromServerToClient(String fileName) throws IOException {

        out.writeUTF(fileName);
        File currentFile = new File(serverPath + "/" + fileName);
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
}
