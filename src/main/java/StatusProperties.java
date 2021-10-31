import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalAmount;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class StatusProperties {
    String dburl, dbuser, dbpassword, host, huser, hpassword;
    LocalDate shiftDate;
    int hp = 22, cbProjId;

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
                cbProjId = 3;
                hp = 9082;
                shiftDate = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
                break;
            case "i":
            case "igs":
                dburl = properties.getProperty("Iurl");
                dbuser = properties.getProperty("Iuser");
                dbpassword = properties.getProperty("Ipassword");
                host = properties.getProperty("Ihost");
                huser = properties.getProperty("Isuser");
                hpassword = properties.getProperty("Ispassword");
                cbProjId = 6;
                Calendar day = Calendar.getInstance();
                day.setTime(new Date());
                day.add(Calendar.DATE, -1);
                shiftDate = day.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                break;
            case "e":
            case "eriell":
                dburl = properties.getProperty("Eurl");
                dbuser = properties.getProperty("Euser");
                dbpassword = properties.getProperty("Epassword");
                host = properties.getProperty("Ehost");
                huser = properties.getProperty("Esuser");
                hpassword = properties.getProperty("Espassword");
                cbProjId = 12;
                day = Calendar.getInstance();
                day.setTime(new Date());
                day.add(Calendar.DATE, -1);
                shiftDate = day.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                break;
            default:
                System.out.println("Введите название проекта");
        }
    }
}
