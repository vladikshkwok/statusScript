import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;


public class Well {
    int id, wellboreId, sourceId, timeshift;
    double currentDepth;
    String name, productKey, timeZone, logsOffset, groupName;
    boolean isGRPorKRS;
    //    int rec1, rec2, rec8, rec12, rec13, rec55, rec56;
    ArrayList<Record> records = new ArrayList<>();

    public Well(int id, String name, int wellboreId, int sourceId, String productKey, String timeZone, int timeshift, String logsOffset, double currentDepth, String groupName) {
        this.id = id;
        this.wellboreId = wellboreId;
        this.sourceId = sourceId;
        this.timeshift = timeshift;
        this.currentDepth = currentDepth;
        this.name = name;
        this.productKey = productKey;
        this.timeZone = timeZone;
        this.logsOffset = logsOffset;
        this.groupName = groupName;
    }

    public void setRecords(Connection connection) {
        try {
            ResultSet rs2, rs = connection.createStatement().executeQuery("select distinct log_id from record_idx_" + this.wellboreId + " order by log_id");
            while (rs.next()) {
                int recordId = rs.getInt(1);
                if (recordId == 1 || recordId == 2 || recordId == 8 || recordId == 12 || recordId == 13 || recordId == 55 || recordId == 56) {
                    rs2 = connection.createStatement().executeQuery("select max(date), max(depth) from WITS_RECORD" + rs.getInt(1) + "_IDX_" + this.wellboreId);
                    while (rs2.next()) {
                        Timestamp time = rs2.getTimestamp(1);

                        this.records.add(new Record(recordId, time, rs2.getInt(2)));
                    }
                } else if (recordId == 70 || recordId == 43)
                    isGRPorKRS = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Well{" +
                "id=" + id +
                ", wellboreId=" + wellboreId +
                ", sourceId=" + sourceId +
                ", timeshift=" + timeshift +
                ", currentDepth=" + currentDepth +
                ", name='" + name + '\'' +
                ", productKey='" + productKey + '\'' +
                ", timeZone='" + timeZone + '\'' +
                ", logsOffset='" + logsOffset + '\'' +
                ", records=" + records +
                '}';
    }
}

class Record {
    int recordNum, depth;
    Timestamp date;

    public Record(int recordNum, Timestamp date, int depth) {
        this.recordNum = recordNum;
        this.depth = depth;
        this.date = date;
    }

    @Override
    public String toString() {
        return "Record{" +
                "recordNum=" + recordNum +
                ", depth=" + depth +
                ", date='" + date + '\'' +
                '}';
    }
}
