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

    static void main() throws IOException, URISyntaxException {
        URI path = Path.of(SaWPaymentS.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                ).getParent().resolve("defaults").toUri();

        File defaultsFile = new File(path);
        defaultsFile.createNewFile();
        InputStream inputStream = new FileInputStream(defaultsFile);

        Properties defaults = new Properties();
        defaults.load(SaWPaymentS.class.getClassLoader().getResourceAsStream("defaults"));
        defaults.load(inputStream);


        // HBCI4Java erwartet das Datum im ISO-Format (YYYY-MM-DD) als String
        // oder als java.util.Date (intern wird es konvertiert).
        LocalDate now = LocalDate.now();
        LocalDate threeMonthsAgo = now.minusMonths(3);

        String startStr = threeMonthsAgo.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Auftrag für das Abrufen der Umsätze erzeugen
        Job umsatzJob = new Job("KUmsAllCamt", new HashMap<>());
        umsatzJob.setParam("startdate", startStr);
        umsatzJob.setParam("enddate", endStr);

        JobHandler jobHandler = new JobHandler(defaults);
        GVRKUms result = (GVRKUms) jobHandler.sendJob(umsatzJob);


        // Alle Umsatzbuchungen ausgeben
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
                sb.append(zweck.getFirst()); // erste Zeile des Verwendungszwecks
            }

            // Ausgeben der Umsatz-Zeile
            System.out.println(sb);

            FileOutputStream outputStream = new FileOutputStream(defaultsFile);
            defaults.store(outputStream, null);
        }
    }
}
