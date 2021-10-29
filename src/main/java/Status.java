import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

public class Status {

    private static Connection con;
    private static Statement stmt;
    private static ResultSet rs;

    public static void CreateStatus(String projectName) {
        ArrayList<Well> wells, wellsInShift;

        try {

            StatusProperties sP = new StatusProperties(projectName);
            con = DriverManager.getConnection(sP.dburl, sP.dbuser, sP.dbpassword);
            stmt = con.createStatement();

            Timestamp db_timenow = getDBTime();
            wells = getWells();
            wellsInShift = getWellsInShift();
            WellValidator.checkVideo(wells, projectName);
            WellValidator.wellsValidate(wells, wellsInShift, db_timenow);

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                con.close();
                stmt.close();
                rs.close();
            } catch (SQLException se) {
            }
        }
    }

    public static Timestamp getDBTime() throws SQLException {
        rs = stmt.executeQuery("select now()");
        Timestamp db_timenow = null;

        while (rs.next()) {
            db_timenow = rs.getTimestamp(1);
        }
        return db_timenow;
    }

    public static ArrayList<Well> getWells() throws SQLException {
        ArrayList<Well> wells = new ArrayList<>();
        String query = "select ww.id, COALESCE(ww.alias, ww.name) as 'Name', ww.wellbore_id, ww.source_id, " +
                "ws.product_key, ww.timezone, ww.timeshift, ww.logs_offset, wb.current_depth, wgp.name" +
                " from WITS_WELL ww inner join WITS_SOURCE ws ON ww.source_id=ws.id inner join WITS_WELLBORE wb " +
                "ON ww.wellbore_id=wb.id inner join WITS_WELL_PROP wwp on ww.id=wwp.well_id" +
                " inner join WITS_WELL_GROUP wgp on wwp.group_id=wgp.id where wwp.status_id!=1 order by wgp.name, Name";
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
        return wells;
    }

    public static ArrayList<Well> getWellsInShift() throws SQLException {
        ArrayList<Well> wells = new ArrayList<>();
        String query = "select ww.id, COALESCE(ww.alias, ww.name) as 'Name', ww.wellbore_id, ww.source_id, " +
                "ws.product_key, ww.timezone, ww.timeshift, ww.logs_offset, wb.current_depth, " +
                "wgp.name from WITS_WELL ww inner join WITS_SOURCE ws ON ww.source_id=ws.id inner join WITS_WELLBORE " +
                "wb ON ww.wellbore_id=wb.id inner join WITS_WELL_PROP wwp on ww.id=wwp.well_id inner join " +
                "WITS_WELL_GROUP wgp on wwp.group_id=wgp.id where wwp.status_id=1 and " +
                "ww.modified_date > \"2021-10-22 16:00\" and ws.product_key not in (select ws.product_key " +
                "from WITS_WELL ww inner join WITS_SOURCE ws ON ww.source_id=ws.id inner join WITS_WELLBORE wb " +
                "ON ww.wellbore_id=wb.id inner join WITS_WELL_PROP wwp on ww.id=wwp.well_id  " +
                "where wwp.status_id!=1) order by wgp.name, Name;";
        rs = stmt.executeQuery(query);
        while (rs.next()) {
            Well well = new Well(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getString(5),
                    rs.getString(6), rs.getInt(7), rs.getString(8), rs.getInt(9), rs.getString(10));
            if (well.productKey.equals("RigSoftClient") || well.productKey.matches("WITSML.*"))
                continue;
            well.setRecords(con);
            if (well.isGRPorKRS)
                continue;
            wells.add(well);
            System.out.println(well);
        }
        return wells;
    }

    public static class StatusProperties {
        String dburl, dbuser, dbpassword, host, huser, hpassword;
        int hp=22;
        StatusProperties(String projectName) throws IOException {
            Properties properties = new Properties();
            properties.load(new FileReader("config/application.properties"));
            switch (projectName) {
                case "n":
                case "nova":
                case "novatek":
                    dburl = properties.getProperty("Nurl");
                    dbuser = properties.getProperty("Nuser");
                    dbpassword = properties.getProperty("Npassword");
                    host = properties.getProperty("Nhost");
                    huser = properties.getProperty("Nsuser");
                    hpassword = properties.getProperty("Nspassword");
                    hp=9082;
                    break;
                case "i":
                case "igs":
                    dburl = properties.getProperty("Iurl");
                    dbuser = properties.getProperty("Iuser");
                    dbpassword = properties.getProperty("Ipassword");
                    host = properties.getProperty("Ihost");
                    huser = properties.getProperty("Isuser");
                    hpassword = properties.getProperty("Ispassword");
                    break;
                case "e":
                case "eriell":
                    dburl = properties.getProperty("Eurl");
                    dbuser = properties.getProperty("Euser");
                    dbpassword = properties.getProperty("Epassword");
                    host = properties.getProperty("Ehost");
                    huser = properties.getProperty("Esuser");
                    hpassword = properties.getProperty("Espassword");
                    break;

            }
        }
    }

}
