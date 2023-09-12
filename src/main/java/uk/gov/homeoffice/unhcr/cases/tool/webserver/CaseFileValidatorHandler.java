package uk.gov.homeoffice.unhcr.cases.tool.webserver;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.servlet.MultipartConfigElement;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.homeoffice.unhcr.cases.tool.CaseFileValidator;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class CaseFileValidatorHandler extends AbstractHandler {

    final static private Logger logger = LoggerFactory.getLogger(CaseFileValidatorHandler.class);

    final static private int CASEFILE_SIZE_LIMIT    = 50 * 1024 * 1024; //50MB limit

    final static private String MULTIPART_FORMDATA_TYPE = "multipart/form-data";

    final static private CaseFileValidator parentValidator = new CaseFileValidator();

    final static private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    final static private MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

    final static private String indexPageTemplate;
    static {
        try (InputStream inputStream = CaseFileValidatorHandler.class.getResourceAsStream("/uk/gov/homeoffice/unhcr/webserver/page/index.html")) {
            if (inputStream==null) throw new RuntimeException("Index page not found %s");

            indexPageTemplate = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    private static boolean isMultipartRequest(Request request) {
        return request.getContentType() != null && request.getContentType().startsWith(MULTIPART_FORMDATA_TYPE);
    }

    @Override
    public void handle(
            String target,
            Request jettyRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) throws IOException {

        try {

            final boolean jsonFlag = httpServletRequest.getParameter("json") != null;

            final boolean postRequest = "POST".equals(jettyRequest.getMethod());
            final boolean multipartRequest = (postRequest) && isMultipartRequest(jettyRequest);

            String caseFileName = "(none)";
            byte[] caseFileBytes = null;
            if (multipartRequest) {
                jettyRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);

                Part caseFilePart = jettyRequest.getPart("casefile");
                if (caseFilePart!=null) {
                    if (caseFilePart.getSize() > CASEFILE_SIZE_LIMIT)
                        throw new RuntimeException(String.format("Case file is too large. Limit %s", FileUtils.byteCountToDisplaySize(CASEFILE_SIZE_LIMIT)));

                    caseFileName = caseFilePart.getSubmittedFileName();
                    caseFileBytes = ByteStreams.toByteArray(caseFilePart.getInputStream());
                }
            } else if (postRequest) {
                if (jettyRequest.getContentLength() > CASEFILE_SIZE_LIMIT)
                    throw new RuntimeException(String.format("Case file is too large. Limit %s", FileUtils.byteCountToDisplaySize(CASEFILE_SIZE_LIMIT)));

                caseFileBytes = CharStreams.toString(jettyRequest.getReader()).getBytes();
                if (caseFileBytes.length==0) caseFileBytes = null;
            }

            ValidationResult validationResult = null;
            if (caseFileBytes!=null) {
                validationResult = parentValidator.validate(caseFileBytes);
            }

            if (jsonFlag) {
                String validationResultJson = "";
                if (validationResult!=null) {
                    List<String> validationErrors = validationResult.getErrors();
                    validationResultJson = gson.toJson(validationErrors.toArray(new String[0]));
                }

                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                httpServletResponse.setContentType("application/json;charset=utf-8");
                httpServletResponse.getWriter().println(validationResultJson);
            } else {
                String validationResultHtml = "";
                if (validationResult!=null) {
                    List<String> validationErrors = validationResult.getErrors();
                    if (validationErrors.isEmpty()) {
                        validationResultHtml = "<ui><li>OK</li></ui>";
                    } else {
                        validationResultHtml =
                                String.format("<ui>%s</ui",
                                        validationErrors.stream()
                                                .map(error -> String.format("<li>%s</li>\n", error))
                                                .collect(Collectors.joining())
                                );
                    }
                }

                String indexPageBody = indexPageTemplate
                        .replace("@name_and_version@", CaseFileValidator.NAME_AND_VERSION)
                        .replace("@case_file_name@", caseFileName)
                        .replace("@results_list@", validationResultHtml);

                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                httpServletResponse.setContentType("text/html;charset=utf-8");
                httpServletResponse.getWriter().println(indexPageBody);
            }
        } catch (Exception e) {
            e.printStackTrace();

            String errorMessage = String.format("Error: %s", e.getMessage());
            logger.error(errorMessage, e);
            httpServletResponse.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    String.format(errorMessage, e.getMessage())
            );
        } finally {
            jettyRequest.setHandled(true);
        }
    }
}
