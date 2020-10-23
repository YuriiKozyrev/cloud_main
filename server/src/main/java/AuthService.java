import java.sql.*;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    public static Connection getConnection(){
        return connection;
    }

    public static Statement getStmt(){
        return stmt;
    }

    public static void connect(){
        try{
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:cloud.db");
            stmt = connection.createStatement();
            System.out.println("Подлючение к БД выполнено.");

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getLoginByLoginAndPass(String login, String pass) {
        String sql = String.format("SELECT login FROM users where login = '%s' and password = '%s'", login, pass);

        try {
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                String str = rs.getString(1);
                return rs.getString(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
