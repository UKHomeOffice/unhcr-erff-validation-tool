package uk.gov.homeoffice.unhcr.cases.tool.webserver;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.*;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceData;

import java.io.*;
import java.net.URL;

public class CaseFileValidatorWebServer {

    static public void start(int webServerPort) throws Exception {
        String jvmVersion = StringUtils.defaultString(System.getProperty("java.version"), "N/A");
        if (
                (jvmVersion.startsWith("1."))||
                (jvmVersion.startsWith("10"))
        ) {
            String errorMessage =
                    "To start web server, Java version 11 (or higher) is required.\n" +
                    "Newer Java can be downloaded from https://www.java.com/";

            System.out.println(errorMessage);
            System.exit(1);
        }

        Server server = new Server(webServerPort);
        server.setHandler(new CaseFileValidatorHandler());

        //enable multi-part forms (for file upload)
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        server.addConnector(connector);

        server.start();
        server.join();
    }
}
