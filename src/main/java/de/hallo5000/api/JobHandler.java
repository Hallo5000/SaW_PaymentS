package de.hallo5000.api;

import de.hallo5000.BankingConnection;
import de.hallo5000.PostgresManager;
import de.hallo5000.datatypes.Job;
import de.hallo5000.datatypes.Transaction;
import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.structures.Value;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class JobHandler {

    private final Properties defaults;

    public JobHandler(Properties defaults){
        this.defaults = defaults;
    }

    void listTransactions() throws IOException {
        // HBCI4Java wants ISO-format (YYYY-MM-DD) as String or as java.util.Date
        LocalDate now = LocalDate.now();
        LocalDate threeMonthsAgo = now.minusMonths(3);

        String startStr = threeMonthsAgo.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // create job for getting transactions of the last three months
        List<GVRKUms.UmsLine> transactions = getUmsLines(startStr, endStr);
        PostgresManager pm = new PostgresManager();
        for(GVRKUms.UmsLine transaction : transactions) {
            Transaction ta = new Transaction(transaction.customerref, transaction, "OTHER");
            pm.addTransaction(ta);

            StringBuilder sb = new StringBuilder(transaction.customerref+" | ");
            sb.append(transaction.valuta);

            Value v = transaction.value;
            if (v != null) {
                sb.append(": ");
                sb.append(v);
            }

            List<String> zweck = transaction.usage;
            if (zweck != null && !zweck.isEmpty()) {
                sb.append(" - ");
                sb.append(zweck.getFirst()); // first line of reference
            }

            System.out.println(sb);

        }
    }

    private List<GVRKUms.UmsLine> getUmsLines(String startStr, String endStr) throws IOException {
        Job umsatzJob = new Job("KUmsAllCamt", new HashMap<>()); //lowlevel: KUmsZeitCamt
        umsatzJob.setParam("startdate", startStr);
        umsatzJob.setParam("enddate", endStr);

        BankingConnection bankingConnection = new BankingConnection(defaults);
        // all jobs of type "KUmsAll"/"KUmsAllCamt" have results of "GVRKUms"
        GVRKUms result = (GVRKUms) bankingConnection.sendJob(umsatzJob);


        // print transactions
        List<GVRKUms.UmsLine> transactions = result.getFlatData();
        return transactions;
    }

}
