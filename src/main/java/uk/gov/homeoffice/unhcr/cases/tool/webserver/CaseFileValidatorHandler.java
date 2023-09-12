package uk.gov.homeoffice.unhcr.cases.tool.webserver;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
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
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceData;
import uk.gov.homeoffice.unhcr.cases.tool.CaseFileValidator;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class CaseFileValidatorHandler extends AbstractHandler {

    private static Logger logger = LoggerFactory.getLogger(CaseFileValidatorHandler.class);

    static final int CASEFILE_SIZE_LIMIT    = 50 * 1024 * 1024; //50MB limit
    static final String MULTIPART_FORMDATA_TYPE = "multipart/form-data";

    static final CaseFileValidator parentValidator = new CaseFileValidator();

    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    final static String indexPageTemplate;
    static {
        try (InputStream inputStream = ReferenceData.class.getResourceAsStream("/uk/gov/homeoffice/unhcr/webserver/page/index.html");) {
            indexPageTemplate = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

    public static boolean isMultipartRequest(Request request) {
        return request.getContentType() != null && request.getContentType().startsWith(MULTIPART_FORMDATA_TYPE);
    }
//
//    public static void enableMultipartSupport(HttpServletRequest request) {
//        request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
//    }

    //TODO html page (post with file) and html response
    //TODO json page (POST with xml body) and json response

    @Override
    public void handle(
            String target,
            Request jettyRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) throws IOException, ServletException {

//        if (multipartRequest) {
//            enableMultipartSupport(jettyRequest);
//        }
        try {

            final boolean jsonFlag = httpServletRequest.getParameter("json") != null;
            boolean multipartRequest = ("POST".equals(jettyRequest.getMethod())) && isMultipartRequest(jettyRequest);

            String caseFileName = "(none)";
            byte[] caseFileBytes = null;
            if (multipartRequest) {
                jettyRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);

                Part caseFilePart = jettyRequest.getPart("casefile");
                if (caseFilePart!=null) {
                    if (caseFilePart.getSize() > CASEFILE_SIZE_LIMIT)
                        throw new RuntimeException(String.format("Case file is too large. Limit %s", FileUtils.byteCountToDisplaySize(CASEFILE_SIZE_LIMIT)));

                    caseFileName = caseFilePart.getSubmittedFileName();
                    caseFileBytes = ByteStreams.toByteArray(
                        ByteStreams.limit(caseFilePart.getInputStream(), CASEFILE_SIZE_LIMIT)
                    );
                }
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
                httpServletResponse.setContentType("text/html;charset=utf-8");
                httpServletResponse.getWriter().println(validationResultJson);
            } else {
                String validationResultHtml = "";
                if (validationResult!=null) {
                    List<String> validationErrors = validationResult.getErrors();
                    if (validationErrors.isEmpty()) {
                        validationResultHtml = "<li>OK</li>";
                    } else {
                        validationResultHtml = validationErrors.stream()
                                .map(error -> String.format("<li>%s</li>\n", error))
                                .collect(Collectors.joining());
                    }
                }

                String indexPageBody = indexPageTemplate
                        .replace("@name_and_version@", CaseFileValidator.NAME_AND_VERSION)
                        .replace("@case_file_name@", caseFileName)
                        .replace("@errors_list@", validationResultHtml);

                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                httpServletResponse.setContentType("text/html;charset=utf-8");
                httpServletResponse.getWriter().println(indexPageBody);
            }
        } catch (Exception e) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            httpServletResponse.setContentType("text/html;charset=utf-8");
            httpServletResponse.getWriter().println(e.getMessage());
            logger.error("Error", e);

            System.out.println(e.getMessage());
            e.printStackTrace();
            //httpServletResponse.sendError();

        } finally {
//            if (multipartRequest) {
//                MultiPartFormInputStream multipartInputStream = (MultiPartFormInputStream) jettyRequest.getAttribute(Request.__MULTIPART_INPUT_STREAM);
//                if (multipartInputStream != null) {
//                    try {
//                        // a multipart request to a servlet will have the parts cleaned up correctly, but
//                        // the repeated call to deleteParts() here will safely do nothing.
//                        multipartInputStream.deleteParts();
//                    } catch (Exception e) {
//                    }
//                }
//            }

            jettyRequest.setHandled(true);
        }
    }
}
