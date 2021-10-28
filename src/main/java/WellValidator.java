import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WellValidator {
    public static void checkGTITime(Well well, @SuppressWarnings("SpellCheckingInspection") Timestamp db_timenow) {
        well.isGTITimeOk = false;
        List<Record> rec = well.records.stream().filter(r -> r.recordNum == 1).collect(Collectors.toList());
        if (rec.isEmpty()) {
            return;
        }
        long time_diff_min = (db_timenow.getTime() - rec.get(0).date.getTime()) / 1000 / 60;
        if (time_diff_min < 10)
            well.isGTITimeOk = true;
    }

    public static void checkGTIDepth(Well well) {
        well.isGTIDepthOk = false;
        List<Record> rec = well.records.stream().filter(r -> r.recordNum == 2).collect(Collectors.toList());
        if (rec.isEmpty()) {
            return;
        }
        double dep_diff = rec.get(0).depth - well.currentDepth;
        if (Math.abs(dep_diff) < 30)
            well.isGTIDepthOk = true;
    }

    public static void checkZTLS(Well well) {
        well.isZTLSOk = false;
        List<Record> recs = well.records.stream().filter(r -> r.recordNum == 8 || r.recordNum == 55 || r.recordNum == 56 || r.recordNum == 68).collect(Collectors.toList());
        for (Record record : recs) {
            double dep_diff = record.depth - well.currentDepth;
            if (Math.abs(dep_diff) < 40) {
                well.isZTLSOk = true;
                break;
            }
        }
    }

    public static void wellsValidate(ArrayList<Well> wells, @SuppressWarnings("SpellCheckingInspection") Timestamp db_timenow) {
        ArrayList<Well> problemWells = new ArrayList<>();
        for (Well well: wells){
            WellValidator.checkGTITime(well, db_timenow);
            WellValidator.checkGTIDepth(well);
            WellValidator.checkZTLS(well);
        }
        String prevGroup = null, currGroup;
        int counter = 1;
        for (Well well : wells) {
            if (well.isGRPorKRS)
                continue;
            if (well.name.matches(".*ЗТЛС.*"))
                continue;
            currGroup = well.groupName;
            if (!currGroup.equals(prevGroup)) {
                counter = 1;
                if (!problemWells.isEmpty()) {
                    System.out.println("\tПроблемы эксплуатации:");
                    for (Well problemWell : problemWells) {
                        System.out.println("\t" + counter++ + ". " + problemWell.name + " - " + (problemWell.isGTITimeOk ? "" : "Не передаюттся данные ГТИ. ") +
                                (problemWell.isZTLSOk ? "" : "Не передаются данные ЗТЛС."));
                    }
                }
                problemWells = new ArrayList<>();
                counter = 1;
                System.out.println(currGroup + ":");
                System.out.println("\tШтатно работают:");
            }
            if (well.isGTITimeOk && well.isGTIDepthOk && well.isZTLSOk) {
                System.out.println("\t" + counter++ + ". " + well.name);
            } else if (well.isGTITimeOk && well.isGTIDepthOk) {
                Pattern pattern = Pattern.compile(", .*$");
                Matcher matcher = pattern.matcher(well.name);
                List<Well> sameWellZTLS;
                if (matcher.find())
                    sameWellZTLS = wells.stream().filter(w -> w.name.matches(".*"+ well.name.substring(matcher.start()+1, matcher.end()) + ".*\\(*ЗТЛС*\\).*")).collect(Collectors.toList());
                else
                    sameWellZTLS = wells.stream().filter(w -> w.name.matches(".*"+ well.name + ".*\\(*ЗТЛС*\\).*")).collect(Collectors.toList());
                if (!sameWellZTLS.isEmpty() && sameWellZTLS.get(0).isZTLSOk) {
                    System.out.println("\t" + counter++ + ". " + well.name);
                }
                else {
                    problemWells.add(well);
                }
            } else {
                problemWells.add(well);
            }
            prevGroup = currGroup;

        }
    }
}
