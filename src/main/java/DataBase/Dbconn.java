package DataBase;

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

    private static volatile Dbconn instance;
    private static Connection conn = null;
    private static PreparedStatement statmt = null;
    private static Props props;
    private ResultSet rst;

    public Dbconn() {
        //Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    public static Dbconn getInstance() throws ClassNotFoundException {
        if (instance == null) {
            synchronized (Dbconn.class){
                if (instance == null) {
                    instance = new Dbconn();
                }
            }
        }
        return instance;
    }

    private void setConn() {
        Context ctx;

        try {
            ctx = new InitialContext();
            DataSource ds = (DataSource)ctx.lookup("java:comp/env/jdbc/SSC-Data");
            conn = ds.getConnection();
            //conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int InsertCert(String certB228, String certSerial, String certFrom, String certTo, String certOrg,
                          String certSubject, String certThumbprint, String certInn, String certKpp, String certOgrn) throws SQLException, IOException {

        props = Props.getInstance();

        this.checkConn();
        this.setPrepStatmt(
                "INSERT INTO " + props.getDbName() + " (bsf, serial, dtFrom, dtTo, company, ownerORVi, thumbPrint, inn, kpp, ogrn, activeFlag) VALUES " +
                "(?,?,?,?,?,?,?,?,?,?,1)"
        );

        statmt.setString(1,certB228);
        statmt.setString(2,certSerial);
        statmt.setString(3,certFrom);
        statmt.setString(4,certTo);
        statmt.setString(5,certOrg);
        statmt.setString(6,certSubject);
        statmt.setString(7,certThumbprint);
        statmt.setString(8,certInn);
        statmt.setString(9,certKpp);
        statmt.setString(10,certOgrn);
        //statmt.setInt(11,1);

        int result = this.Update();

        this.closeStatement();
        //this.closeConnection();
        return result;
    }

    private void setPrepStatmt(String sql) throws SQLException {
        statmt = conn.prepareStatement(sql);
    }

    private ResultSet Recordset() throws SQLException {
        rst = statmt.executeQuery();
        return rst;
    }

    private int Update() throws SQLException {
        return statmt.executeUpdate();
    }

    private boolean ExecSQL(String sql) throws SQLException {
        return statmt.execute(sql);
    }

    private boolean Exec() throws SQLException {
        return statmt.execute();
    }

    private void checkConn() throws SQLException {
        if (conn == null) {
            this.setConn();
        }else if (conn.isClosed()){
            conn = null;
            this.setConn();
        }
    }

    private void closeRecordSet() throws SQLException {
        rst.close();
    }

    private void closeStatement() throws SQLException {
        statmt.close();
    }

    private void closeConnection() throws SQLException {
        conn.close();
    }
}