package uk.gov.homeoffice.unhcr.cases.tool.webserver;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.MultiPartFormInputStream;
import org.eclipse.jetty.server.MultiPartInputStreamParser;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceData;
import uk.gov.homeoffice.unhcr.cases.tool.CaseFileValidator;

import javax.servlet.MultipartConfigElement;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CaseFileValidatorHandler extends AbstractHandler {

    private static Logger logger = LoggerFactory.getLogger(CaseFileValidatorHandler.class);

    static final String MULTIPART_FORMDATA_TYPE = "multipart/form-data";

    final static String indexPageTemplate;
    static {
        try (InputStream inputStream = ReferenceData.class.getResourceAsStream("/uk/gov/homeoffice/unhcr/webserver/page/index.html");) {
            indexPageTemplate = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

//    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
//
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

            final String postBody;
            if (multipartRequest) {
                jettyRequest.getPart("casefile");
                
            } else {
                postBody = null;
            }





            if (jsonFlag) {


            } else {
                String indexPageBpdy = indexPageTemplate
                        .replace("@name_and_version@", CaseFileValidator.NAME_AND_VERSION)
                        .replace("@errors_list@", "ERRORS LIST");

                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                httpServletResponse.setContentType("text/html;charset=utf-8");
                httpServletResponse.getWriter().println(indexPageBpdy);
            }
        } catch (Exception e) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            httpServletResponse.setContentType("text/html;charset=utf-8");
            httpServletResponse.getWriter().println(e.getMessage());
            logger.error("");
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
