package de.hallo5000;

import java.io.Console;
import java.io.File;
import java.util.*;

import org.kapott.hbci.GV.HBCIJob;
import org.kapott.hbci.GV_Result.HBCIJobResult;
import org.kapott.hbci.callback.AbstractHBCICallback;
import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIVersion;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.structures.Konto;

public class JobHandler {

    private static String BLZ;
    private static String USER;
    private static String PIN;

    private final Properties defaults;

    public JobHandler(Properties defaults){
        this.defaults = defaults;
    }

    private final static HBCIVersion VERSION = HBCIVersion.HBCI_300;

    public HBCIJobResult sendJob(Job job){
        Properties props = new Properties(); // optional kernel parameter
        props.setProperty("client.passport.default","PinTan"); // Legt als Verfahren PIN/TAN fest.
        props.setProperty("client.passport.PinTan.init","1"); // Stellt sicher, dass der Passport initialisiert wird

        HBCIUtils.init(props,new MyHBCICallback());
        //HBCIUtils.setParam("client.product.name","<your registration>");


        System.out.println(HBCIUtils.getBankInfo(BLZ));
        if(BLZ == null || HBCIUtils.getBankInfo(BLZ) == null){
            System.out.println("BLZ eingeben:");
            BLZ = cliInput(false);
            System.out.println("BLZ speichern? [Y/n]");
            if(!cliInput(false).equalsIgnoreCase("n")) defaults.setProperty("BLZ", BLZ);
        }
        USER = defaults.getProperty("USER");
        if(USER == null || USER.isEmpty()){
            System.out.println("Username eingeben:");
            USER = cliInput(false);
            System.out.println("Username speichern? [Y/n]");
            if(!cliInput(false).equalsIgnoreCase("n")) defaults.setProperty("USER", USER);
        }
        System.out.println("PIN eingeben:");
        PIN = cliInput(true);


        // In der Passport-Datei speichert HBCI4Java die Daten des Bankzugangs (Bankparameterdaten, Benutzer-Parameter, etc.).
        // Die Datei kann problemlos gelöscht werden. Sie wird beim nächsten Mal automatisch neu erzeugt,
        // wenn der Parameter "client.passport.PinTan.init" den Wert "1" hat.
        // Wir speichern die Datei der Einfachheit halber im aktuellen Verzeichnis.
        final File passportFile = new File("testpassport.dat");

        HBCIPassport passport = AbstractHBCIPassport.getInstance(passportFile);
        // passport config
        passport.setCountry("DE");
        passport.setHost(HBCIUtils.getBankInfo(BLZ).getPinTanAddress()); // server address
        passport.setPort(443); // TCP-Port des Servers. Bei PIN/TAN immer 443, da das ja über HTTPS läuft.
        passport.setFilterType("Base64"); // pin/tan encoded in base64

        // Das Handle ist die eigentliche HBCI-Verbindung zum Server + zum Server verbinden
        HBCIJobResult result;
        try(HBCIHandler handle = new HBCIHandler(VERSION.getId(), passport)){
            Konto[] accounts = passport.getAccounts();
            if(accounts == null || accounts.length == 0) error("Keine Konten ermittelbar");

            String IBAN = defaults.getProperty("IBAN");
            if(IBAN == null || IBAN.isEmpty()){
                System.out.println("IBAN eingeben:");
                IBAN = cliInput(false);
                System.out.println("IBAN speichern? [Y/n]");
                if(!cliInput(false).equalsIgnoreCase("n")) defaults.setProperty("IBAN", IBAN);
            }

            Konto finalAcc = accounts[0];
            String finalIBAN = IBAN;
            if(!IBAN.isEmpty()) finalAcc = Arrays.stream(accounts).filter(k -> k.iban.equalsIgnoreCase(finalIBAN)).toList().getFirst();

            HBCIJob hbciJob = handle.newJob(job.getType());
            hbciJob.setParam("my", finalAcc); // festlegen, welches Konto abgefragt werden soll.
            for(String key : job.getParams().keySet()){
                hbciJob.setParam(key, job.getParams().get(key));
            }
            hbciJob.addToQueue(); // Zur Liste der auszuführenden Aufträge hinzufügen

            // Alle Aufträge aus der Liste ausführen.
            HBCIExecStatus status = handle.execute();

            // Prüfen, ob die Kommunikation mit der Bank grundsätzlich geklappt hat
            if(!status.isOK()) error(status.toString());

            // Das Ergebnis des Jobs können wir auf "GVRKUms" casten. Jobs des Typs "KUmsAll"/"KUmsAllCamt"
            // liefern immer diesen Typ.
            result = hbciJob.getJobResult();




        }finally{ // Sicherstellen, dass Passport nach Beendigung geschlossen werden. (handle wird durchs try-with-resources geschlossen)
            passport.close();
        }
        // Prüfen, ob der Abruf der Umsätze geklappt hat
        if(!result.isOK()) error(result.toString());
        return result;
    }

    /**
    * Über diesen Callback kommuniziert HBCI4Java mit dem Benutzer und fragt die benötigten
    * Informationen wie Benutzerkennung, PIN usw. ab.
    */
    private static class MyHBCICallback extends AbstractHBCICallback {
        /**
         * @see org.kapott.hbci.callback.HBCICallback#log(java.lang.String, int, java.util.Date, java.lang.StackTraceElement)
         */
        @Override
        public void log(String msg, int level, Date date, StackTraceElement trace) {
            System.out.println(msg);
        }

        /**
         * @see org.kapott.hbci.callback.HBCICallback#callback(org.kapott.hbci.passport.HBCIPassport, int, java.lang.String, int, java.lang.StringBuffer)
         */
        @Override
        public void callback(HBCIPassport passport, int reason, String msg, int datatype, StringBuffer retData) {
            // Diese Funktion ist wichtig. Über die fragt HBCI4Java die benötigten Daten von uns ab.
            switch(reason){
                //paste BLZ
                case NEED_BLZ:
                    retData.replace(0,retData.length(),BLZ);
                    break;

                //paste USER(name)
                case NEED_USERID:
                    retData.replace(0,retData.length(),USER);
                    break;

                //paste PIN
                case NEED_PT_PIN:
                    retData.replace(0,retData.length(),PIN);
                    break;

                // Mit dem Passwort verschlüsselt HBCI4Java die Passport-Datei.
                // Wir nehmen hier der Einfachheit halber direkt die PIN. In der Praxis
                // sollte hier aber ein stärkeres Passwort genutzt werden.
                // Die Ergebnis-Daten müssen in dem StringBuffer "retData" platziert werden.
                case NEED_PASSPHRASE_LOAD: //fallthrough
                case NEED_PASSPHRASE_SAVE:
                    retData.replace(0,retData.length(),PIN);
                    break;

                // Die Kundenkennung. Meist identisch mit der Benutzerkennung.
                // Bei manchen Banken kann man die auch leer lassen
                case NEED_CUSTOMERID:
                    retData.replace(0,retData.length(),USER);
                    break;

                // //////////////////////////////////////////////////////////////////////
                // Die folgenden Callbacks sind nur für die Ausführung TAN-pflichtiger
                // Geschäftsvorfälle bei der Verwendung des PIN/TAN-Verfahrens nötig.
                // Z.Bsp. beim Versand einer Überweisung
                // "NEED_PT_SECMECH" kann jedoch auch bereits vorher auftreten.

                // PushTAN Decoupled.
                // Hier muss lediglich ein Warte-Dialog mit einem OK/Fortsetzen-Button angezeigt werden,
                // bis der User in der App die Zahlung freigegeben hat.
                case NEED_PT_DECOUPLED:
                    System.out.println(msg);
                    System.out.println("press ENTER to proceed");
                    cliInput(true);
                    break;

                // Beim Verfahren smsTAN ist es möglich, mehrere Handynummern mit
                // Aliasnamen bei der Bank zu hinterlegen. Auch wenn nur eine Handynummer
                // bei der Bank hinterlegt ist, kann es durchaus passieren,
                // dass die Bank dennoch die Aufforderung zur Auswahl des TAN-Mediums
                // sendet.
                case NEED_PT_TANMEDIA:

                    // Als Parameter werden die verfügbaren TAN-Medien in 'retData' übergeben.
                    // Der Aufbau des String ist wie folgt:
                    // <name1>|<name2>|...

                    // Der Callback muss den vom User ausgewählten Aliasnamen
                    // zurückliefern. Falls retData.toString() kein "|" enthält, ist davon
                    // auszugehen, dass nur eine mögliche Option existiert.

                    String[] devices = retData.toString().split("\\|");
                    if(devices.length > 0) retData.replace(0, retData.length(), devices[0]);
                    break;

                // //////////////////////////////////////////////////////////////////////

                // Manche Fehlermeldungen werden hier ausgegeben
                case HAVE_ERROR:
                    JobHandler.log(msg);
                    break;

                default:
                    // Wir brauchen nicht alle der Callbacks
                    //removed: NEED_PT_QRTAN, NEED_PT_PHOTOTAN, HAVE_VOP_RESULT, NEED_PT_TAN, NEED_PT_SECMECH
                    break;
            }
        }

        /**
         * @see org.kapott.hbci.callback.HBCICallback#status(org.kapott.hbci.passport.HBCIPassport, int, java.lang.Object[])
         */
        @Override
        public void status(HBCIPassport passport, int statusTag, Object[] o) {
            // So ähnlich wie log(String, int, Date, StackTraceElement) jedoch für Status-Meldungen.
        }
    }

    private static void log(String msg) {
        System.out.println(msg);
    }

    private static void error(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    public static String cliInput(boolean maskPwd) {
        Console console = System.console();
        if(console == null){
            error("Couldn't get Console instance");
        }

        char[] inputArray = maskPwd ? console.readPassword() : console.readLine().toCharArray();
        return new String(inputArray);

    }

}
