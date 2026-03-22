package de.hallo5000;

import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.structures.Value;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class SaWPaymentS {

    public static URI execPath;

    static void main() throws IOException, URISyntaxException {
        // path in which the .jar is located
        execPath = Path.of(SaWPaymentS.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                ).getParent().toUri();

        File defaultsFile = new File(execPath.resolve("defaults"));
        defaultsFile.createNewFile();
        InputStream inputStream = new FileInputStream(defaultsFile);

        Properties defaults = new Properties();
        defaults.load(SaWPaymentS.class.getClassLoader().getResourceAsStream("defaults"));
        defaults.load(inputStream);


        // HBCI4Java wants ISO-format (YYYY-MM-DD) as String or as java.util.Date
        LocalDate now = LocalDate.now();
        LocalDate threeMonthsAgo = now.minusMonths(3);

        String startStr = threeMonthsAgo.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // create job for getting transactions of the last three months
        Job umsatzJob = new Job("KUmsAllCamt", new HashMap<>());
        umsatzJob.setParam("startdate", startStr);
        umsatzJob.setParam("enddate", endStr);

        JobHandler jobHandler = new JobHandler(defaults);
        // all jobs of type "KUmsAll"/"KUmsAllCamt" have results of "GVRKUms"
        GVRKUms result = (GVRKUms) jobHandler.sendJob(umsatzJob);


        // print transactions
        List<GVRKUms.UmsLine> buchungen = result.getFlatData();
        for(GVRKUms.UmsLine buchung : buchungen) {
            StringBuilder sb = new StringBuilder();
            sb.append(buchung.valuta);

            Value v = buchung.value;
            if (v != null) {
                sb.append(": ");
                sb.append(v);
            }

            List<String> zweck = buchung.usage;
            if (zweck != null && !zweck.isEmpty()) {
                sb.append(" - ");
                sb.append(zweck.getFirst()); // first line of reference
            }

            System.out.println(sb);

            FileOutputStream outputStream = new FileOutputStream(defaultsFile);
            defaults.store(outputStream, null);
        }
    }
}
