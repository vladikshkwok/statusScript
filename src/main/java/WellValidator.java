import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WellValidator {
    public static void checkGTITime(Well well, Timestamp db_timenow) {
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

    public static void checkVideo(ArrayList<Well> wells, String projectName) {
        try {
            Status.StatusProperties sP = new Status.StatusProperties(projectName);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            Session session = jsch.getSession(sP.huser, sP.host, sP.hp);
            session.setPassword(sP.hpassword);
            session.setConfig(config);
            session.connect();
            System.out.println("Connected to " + sP.host + " to get information about cameras");

            for (Well well : wells) {
                if (well.productKey.matches(".*WITSML.*"))
                    continue;
                String cameras = "";
                Channel channel = session.openChannel("exec");
                Pattern pattern = Pattern.compile("-[0-9]+-");
                Matcher matcher = pattern.matcher(well.productKey);
                String gboxNum = "";
                if (matcher.find())
                    gboxNum = well.productKey.substring(matcher.start() + 1, matcher.end() - 1);
                String command1 = "counter=1; " +
                        "for camera in $(grep -E \"camera[0-9]+_stream\" ~/stream/log/bb/gbox-" + gboxNum +
                        "/connect.conf | grep -oE \"([0-9]{1,3}\\.){3}[0-9]{1,3}\"); do " +
                        "echo -n \"$counter.$camera=\"; " +
                        "tail -1 ~/stream/log/bb/gbox-" + gboxNum + "/archive$counter.$camera; " +
                        "(( counter++ )) ; " +
                        "done";
                ((ChannelExec) channel).setCommand(command1);
                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);
                InputStream in = channel.getInputStream();
                channel.connect();
                byte[] tmp = new byte[1024];
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0) break;
                        cameras += new String(tmp, 0, i);
                    }
                    if (channel.isClosed()) {
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ee) {
                    }
                }
                well.cameras = (ArrayList<String>) Arrays.stream(cameras.split("\n")).collect(Collectors.toList());
                channel.disconnect();
            }
            session.disconnect();
            System.out.println("DONE");
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Well well : wells) {
            well.isVideoOk = true;
            for (int i = 0; i < well.cameras.size(); i++) {
                if (well.cameras.get(i).matches(".*OK.*"))
                    well.cameras.set(i, "OK");
                else {
                    System.out.println(well.name + " камера " + well.cameras.get(i) + " not ok");
                    well.isVideoOk = false;
                }
            }
        }
    }

    public static void wellsValidate(ArrayList<Well> wells, ArrayList<Well> wellsInShift, Timestamp db_timenow) {
        ArrayList<Well> problemWells = new ArrayList<>();
        for (Well well : wells) {
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
                        String videoProblems = "";
                        if (!problemWell.isVideoOk) {
                            for (String camera : problemWell.cameras) {
                                if (!camera.equals("OK")) {
                                    Pattern pattern = Pattern.compile("\\d.\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}");
                                    Matcher matcher = pattern.matcher(camera);
                                    if (matcher.find())
                                        videoProblems += "Отсутствует запись видеоархива с камеры " +
                                            camera.substring(matcher.start(), matcher.end()) +
                                            " с ";
                                    pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}");
                                    matcher = pattern.matcher(camera);
                                    if (matcher.find())
                                        videoProblems += camera.substring(matcher.start(), matcher.end()) + ". ";
                                }
                            }
                        }
                        System.out.println("\t" + counter++ + ". " + problemWell.name + " - " + (problemWell.isGTITimeOk ? "" : "Не передаюттся данные ГТИ. ") +
                                (problemWell.isZTLSOk ? "" : "Не передаются данные ЗТЛС. ") + (problemWell.isVideoOk ? "" : videoProblems));
                    }
                }

                problemWells = new ArrayList<>();
                counter = 1;
                String finalPrevGroup = prevGroup;
                List<Well> shiftWellsInThisGroup = wellsInShift.stream().filter(w -> w.groupName.equals(finalPrevGroup)).collect(Collectors.toList());
                if (!shiftWellsInThisGroup.isEmpty()) {
                    System.out.println("\tДвижка:");
                    for (Well shiftWell : shiftWellsInThisGroup) {
                        System.out.println("\t" + counter++ + ". " + shiftWell.name);
                    }
                }
                counter = 1;
                System.out.println(currGroup + ":");
                System.out.println("\tШтатно работают:");
            }
            if (well.isGTITimeOk && well.isGTIDepthOk && well.isVideoOk && well.isZTLSOk) {
                System.out.println("\t" + counter++ + ". " + well.name);
            } else if (!well.isZTLSOk) {
                Pattern pattern = Pattern.compile(", .*$");
                Matcher matcher = pattern.matcher(well.name);
                List<Well> sameWellZTLS;
                if (matcher.find())
                    sameWellZTLS = wells.stream().filter(w -> w.name.matches(".*" + well.name.substring(matcher.start() + 1, matcher.end()) + ".*\\(*ЗТЛС*\\).*")).collect(Collectors.toList());
                else
                    sameWellZTLS = wells.stream().filter(w -> w.name.matches(".*" + well.name + ".*\\(*ЗТЛС*\\).*")).collect(Collectors.toList());
                if (!sameWellZTLS.isEmpty() && sameWellZTLS.get(0).isZTLSOk) {
                    System.out.println("\t" + counter++ + ". " + well.name);
                } else {
                    problemWells.add(well);
                }
            } else {
                problemWells.add(well);
            }
            prevGroup = currGroup;

        }
    }
}
