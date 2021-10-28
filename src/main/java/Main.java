import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;


@SuppressWarnings("SpellCheckingInspection")
public class Main {
    // JDBC URL, username and password of MySQL server

    // JDBC variables for opening and managing connection
    private static Connection con;
    private static Statement stmt;
    private static ResultSet rs;
    private static ResultSet rs2;

    public static void main(String[] args) {
        ArrayList<Well> wells = new ArrayList<>();
        String query = "select ww.id, COALESCE(ww.alias, ww.name) as 'Name', ww.wellbore_id, ww.source_id, ws.product_key, ww.timezone, ww.timeshift, ww.logs_offset, wb.current_depth, wgp.name" +
                " from WITS_WELL ww inner join WITS_SOURCE ws ON ww.source_id=ws.id inner join WITS_WELLBORE wb ON ww.wellbore_id=wb.id inner join WITS_WELL_PROP wwp on ww.id=wwp.well_id" +
                " inner join WITS_WELL_GROUP wgp on wwp.group_id=wgp.id where wwp.status_id!=1 order by wgp.name, Name";

        try {
            Properties properties = new Properties();
            properties.load(new FileReader("/home/vladikshk/IdeaProjects/statusScript/src/main/resources/application.properties"));

            String url = properties.getProperty("url");
            String user = properties.getProperty("user");
            String password = properties.getProperty("password");

            // opening database connection to MySQL server
            con = DriverManager.getConnection(url, user, password);

            // getting Statement object to execute query
            stmt = con.createStatement();
            rs = stmt.executeQuery("select now()");
            Timestamp db_timenow = null;
            while (rs.next()) {
                db_timenow = rs.getTimestamp(1);
            }

            // executing SELECT query
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                Well well = new Well(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getString(5),
                        rs.getString(6), rs.getInt(7), rs.getString(8), rs.getInt(9), rs.getString(10));
                if (well.productKey.equals("RigSoftClient"))
                    continue;
                well.setRecords(con);
                wells.add(well);
                System.out.println(well);
            }
            System.out.println();


            WellValidator.wellsValidate(wells, db_timenow);

        } catch (SQLException | IOException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close connection ,stmt and resultset here
            try {
                con.close();
            } catch (SQLException se) { /*can't do anything */ }
            try {
                stmt.close();
            } catch (SQLException se) { /*can't do anything */ }
            try {
                rs.close();
            } catch (SQLException se) { /*can't do anything */ }
        }
    }
}
