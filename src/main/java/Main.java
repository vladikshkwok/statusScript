import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;


public class Main {
    // JDBC URL, username and password of MySQL server

    // JDBC variables for opening and managing connection
    private static Connection con;
    private static Statement stmt;
    private static ResultSet rs;
    private static ResultSet rs2;

    public static void main(String args[]) {
        ArrayList<Well> wells = new ArrayList<>();

        String query = "select ww.id, COALESCE(ww.alias, ww.name) as 'Name', ww.wellbore_id, ww.source_id, ws.product_key, ww.timezone, ww.timeshift, ww.logs_offset, wb.current_depth, wgp.name" +
                " from WITS_WELL ww inner join WITS_SOURCE ws ON ww.source_id=ws.id inner join WITS_WELLBORE wb ON ww.wellbore_id=wb.id inner join WITS_WELL_PROP wwp on ww.id=wwp.well_id" +
                " inner join WITS_WELL_GROUP wgp on wwp.group_id=wgp.id where wwp.status_id!=1 order by wgp.name, Name";

        try {
            Properties properties = new Properties();
            properties.load(new FileReader("/home/vladikshk/IdeaProjects/statusScript/src/main/resources/application.properties"));

            String url=properties.getProperty("url");
            String user=properties.getProperty("user");
            String password=properties.getProperty("password");

            // opening database connection to MySQL server
            con = DriverManager.getConnection(url, user, password);

            // getting Statement object to execute query
            stmt = con.createStatement();

            // executing SELECT query
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                Well well = new Well(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getString(5),
                        rs.getString(6), rs.getInt(7), rs.getString(8), rs.getInt(9), rs.getString(10));
                well.setRecords(con);
                wells.add(well);
                System.out.println(well.toString());
            }
            System.out.println();
            String prevGroup = null, currGroup;
            int counter = 1;
            for (Well well : wells) {
                if (well.isGRPorKRS)
                    continue;
                currGroup = well.groupName;
                if (!currGroup.equals(prevGroup)) {
                    counter = 1;
                    System.out.println(currGroup + ":");
                }
                System.out.println("\t" + counter++ + ". " + well.name + " Рекорд 1 последний раз обновлялся: " + (well.records.isEmpty() ? "" : (well.records.get(0).date)));
                prevGroup = currGroup;
            }

        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
