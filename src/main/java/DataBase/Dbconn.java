package DataBase;

/*
* Класс подключения к БД
* */

import utils.Props;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Dbconn {

    private static volatile Dbconn Instance;
    private static Connection Conn = null;
    private static PreparedStatement Statmt = null;
    private final Props props = Props.getInstance();
    private ResultSet rst;

    private Dbconn() throws IOException {
        //Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    public static Dbconn getInstance() throws IOException {
        if (Instance == null) {
            synchronized (Dbconn.class){
                if (Instance == null) {
                    Instance = new Dbconn();
                }
            }
        }
        return Instance;
    }

    private void setConn() {
        Context ctx;

        try {
            ctx = new InitialContext();
            DataSource ds = (DataSource)ctx.lookup("java:comp/env/jdbc/SSC-Data");
            Conn = ds.getConnection();
            //conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int InsertCert(String certB228, String certSerial, String certFrom, String certTo, String certOrg,
                          String certSubject, String certThumbprint, String certInn, String certKpp, String certOgrn) throws SQLException, IOException {

        this.checkConn();
        this.setPrepStatmt(
                "INSERT INTO " + props.getDbName() + " (bsf, serial, dtFrom, dtTo, company, ownerORVi, thumbPrint, inn, kpp, ogrn, activeFlag) VALUES " +
                "(?,?,?,?,?,?,?,?,?,?,1)"
        );

        Statmt.setString(1,certB228);
        Statmt.setString(2,certSerial);
        Statmt.setString(3,certFrom);
        Statmt.setString(4,certTo);
        Statmt.setString(5,certOrg);
        Statmt.setString(6,certSubject);
        Statmt.setString(7,certThumbprint);
        Statmt.setString(8,certInn);
        Statmt.setString(9,certKpp);
        Statmt.setString(10,certOgrn);
        //statmt.setInt(11,1);

        int result = this.Update();

        this.closeStatement();
        //this.closeConnection();
        return result;
    }

    private void setPrepStatmt(String sql) throws SQLException {
        Statmt = Conn.prepareStatement(sql);
    }

    private ResultSet Recordset() throws SQLException {
        rst = Statmt.executeQuery();
        return rst;
    }

    private int Update() throws SQLException {
        return Statmt.executeUpdate();
    }

    private boolean ExecSQL(String sql) throws SQLException {
        return Statmt.execute(sql);
    }

    private boolean Exec() throws SQLException {
        return Statmt.execute();
    }

    private void checkConn() throws SQLException {
        if (Conn == null) {
            this.setConn();
        }else if (Conn.isClosed()){
            Conn = null;
            this.setConn();
        }
    }

    private void closeRecordSet() throws SQLException {
        rst.close();
    }

    private void closeStatement() throws SQLException {
        Statmt.close();
    }

    private void closeConnection() throws SQLException {
        Conn.close();
    }
}