package de.hallo5000;

import de.hallo5000.api.JobHandler;
import de.hallo5000.api.RESTendpoint;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
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

        //load defaults from jar to Properties
        Properties defaults = new Properties();
        defaults.load(SaWPaymentS.class.getClassLoader().getResourceAsStream("defaults"));

        //load custom file to Properties
        File defaultsFile = new File(execPath.resolve("defaults"));
        if(!defaultsFile.createNewFile()){
            InputStream inputStream = new FileInputStream(defaultsFile);
            defaults.load(inputStream);
        }

        RESTendpoint restendpoint = new RESTendpoint(new JobHandler(defaults));

        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            //save defaults to file when shutting down
            try(FileOutputStream outputStream = new FileOutputStream(defaultsFile)){
                restendpoint.stopHttpServer();
                defaults.store(outputStream, null);
                mainThread.join();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

        restendpoint.startHttpServer();
    }
}
