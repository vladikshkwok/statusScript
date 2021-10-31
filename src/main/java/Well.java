import java.sql.*;
import java.util.ArrayList;

public class Well {
    final int id;
    final int wellboreId;
    final int sourceId;
    final int timeshift;
    final double currentDepth;
    final String name;
    final String productKey;
    final String timeZone;
    final String logsOffset;
    final String groupName;
    boolean isGRPorKRS, isGTITimeOk=true, isGTIDepthOk=true, isZTLSOk=true, isVideoOk=true;
    //    int rec1, rec2, rec8, rec12, rec13, rec55, rec56;
    final ArrayList<Record> records = new ArrayList<>();
    ArrayList<String> cameras;

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
        cameras = new ArrayList<>();
    }

    public void setRecords(Connection connection) {
        try {
            ResultSet rs2, rs = connection.createStatement().executeQuery("select distinct log_id from record_idx_" + this.wellboreId + " order by log_id");
            while (rs.next()) {
                int recordId = rs.getInt(1);
                if (recordId == 1 || recordId == 2 || recordId == 8 || recordId == 55 || recordId == 56 || recordId == 68) {
                    rs2 = connection.createStatement().executeQuery("select CONVERT_TZ(max(date), '+00:00', '" + this.logsOffset + "'), max(depth) from WITS_RECORD" + rs.getInt(1) + "_IDX_" + this.wellboreId);
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
                ", groupName='" + groupName + '\'' +
                ", isGRPorKRS=" + isGRPorKRS +
                ", isGTITimeOk=" + isGTITimeOk +
                ", isGTIDepthOk=" + isGTIDepthOk +
                ", isZTLSOk=" + isZTLSOk +
                ", isVideoOk=" + isVideoOk +
                ", records=" + records +
                '}';
    }
}

class Record {
    final int recordNum;
    final int depth;
    final Timestamp date;

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
