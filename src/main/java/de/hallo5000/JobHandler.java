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
        Properties props = new Properties(); // optional parameters
        props.setProperty("client.passport.default","PinTan"); // sets pin/tan as secmech
        props.setProperty("client.passport.PinTan.init","1"); // initializes passport creation

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


        // In the passport file the accounts/servers data is saved
        // the file will be generated if it's not existing and "client.passport.PinTan.init" is set to "1"
        // the file is saved next to the executing jar
        final File passportFile = new File(SaWPaymentS.execPath.resolve("testpassport.dat"));

        HBCIPassport passport = AbstractHBCIPassport.getInstance(passportFile);
        // passport config
        passport.setCountry("DE");
        passport.setHost(HBCIUtils.getBankInfo(BLZ).getPinTanAddress()); // server address
        passport.setPort(443); // TCP-Port des Servers. Bei PIN/TAN immer 443, da das ja über HTTPS läuft.
        passport.setFilterType("Base64"); // pin/tan encoded in base64

        // the actual HBCI-connection to the server in the initialization of it
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
            hbciJob.setParam("my", finalAcc); // the account to be used
            for(String key : job.getParams().keySet()){
                hbciJob.setParam(key, job.getParams().get(key));
            }
            hbciJob.addToQueue();

            // execute queued jobs
            HBCIExecStatus status = handle.execute();

            if(!status.isOK()) error(status.toString());

            result = hbciJob.getJobResult();
        }finally{ // handle is closed by the try-with-resources
            passport.close();
        }
        if(!result.isOK()) error(result.toString());
        return result;
    }

    //callback for the hbci4j connection
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

                // passphrase to encrypt the passport with
                case NEED_PASSPHRASE_LOAD: //fallthrough
                case NEED_PASSPHRASE_SAVE:
                    retData.replace(0,retData.length(),PIN);
                    break;

                // should be identical to userid
                case NEED_CUSTOMERID:
                    retData.replace(0,retData.length(),USER);
                    break;

                // //////////////////////////////////////////////////////////////////////
                // the following callbacks are needed when a TAN is requested
                // "NEED_PT_SECMECH" could be called before

                // waiting for confirmation in the app
                case NEED_PT_DECOUPLED:
                    System.out.println(msg);
                    System.out.println("press ENTER to proceed");
                    cliInput(true);
                    break;

                // select a tan device
                case NEED_PT_TANMEDIA:
                    // retData: "<tanDevice1>|<tanDevice2>|..."
                    // retData should be changed to just the name of one device

                    String[] devices = retData.toString().split("\\|");
                    if(devices.length > 0) retData.replace(0, retData.length(), devices[0]);
                    break;

                // //////////////////////////////////////////////////////////////////////

                // some errors use this callback
                case HAVE_ERROR:
                    JobHandler.log(msg);
                    break;

                default:
                    // we don't need all callbacks
                    //removed: NEED_PT_QRTAN, NEED_PT_PHOTOTAN, HAVE_VOP_RESULT, NEED_PT_TAN, NEED_PT_SECMECH
                    break;
            }
        }

        /**
         * @see org.kapott.hbci.callback.HBCICallback#status(org.kapott.hbci.passport.HBCIPassport, int, java.lang.Object[])
         */
        @Override
        public void status(HBCIPassport passport, int statusTag, Object[] o) {
            // similar log(String, int, Date, StackTraceElement) but for status messages
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
