package saml2webssotest.idp.mockSPHandlers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import saml2webssotest.common.SAMLUtil;
import saml2webssotest.common.standardNames.SAMLmisc;
import saml2webssotest.idp.IdPTestRunner;

public class SamlWebSSOHandler extends AbstractHandler{
	
	private final Logger logger = LoggerFactory.getLogger(SamlWebSSOHandler.class);
	/**
	 * Handle a request received by the mock SP.
	 * 
	 * It should retrieve and decode the SAML Response and send it to the test runner. If the response should be sent over a synchronous 
	 * connection, it should also send that response (this is the artifact binding, which is not yet implemented).
	 * 
	 * @param target is the identifier for the resource that should handle the request, usually the URI from the HTTP Request
	 * @param baseRequest is the original unwrapped request
	 * @param request is the request that the handler received
	 * @param response is the response that will be sent
	 */
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest abstractRequest, HttpServletResponse response) throws IOException, ServletException {
		Request request = (abstractRequest instanceof Request) ? (Request) abstractRequest : HttpChannel.getCurrentHttpChannel().getRequest();
		String method = request.getMethod();
		String samlResponse = null;

        if (method.equalsIgnoreCase("GET")) {
            // retrieve the SAML Request and binding
        	String respParam = request.getParameter(SAMLmisc.URLPARAM_SAMLRESPONSE_REDIRECT);
        	
            if (respParam != null) {
            	IdPTestRunner.setSamlResponseBinding(SAMLmisc.BINDING_HTTP_REDIRECT);
            	samlResponse = SAMLUtil.decodeSamlMessageForRedirect(respParam);
                IdPTestRunner.setSamlResponse(samlResponse);

                logger.debug("SAML Response received through GET by the mock SP");
            }
            else if (request.getParameter(SAMLmisc.URLPARAM_SAMLARTIFACT) != null){
            	IdPTestRunner.setSamlResponseBinding(SAMLmisc.BINDING_HTTP_ARTIFACT);
                // TODO: implement for BINDING_HTTP_ARTIFACT
            }
            else{
            	logger.debug("No SAML response received (with GET)");
            }
        }
        else if (method.equalsIgnoreCase("POST")) {
            // get the POST variables
        	String respParam = request.getParameter(SAMLmisc.URLPARAM_SAMLRESPONSE_POST);
            
            if (respParam != null){
            	IdPTestRunner.setSamlResponseBinding(SAMLmisc.BINDING_HTTP_POST);
            	samlResponse = SAMLUtil.decodeSamlMessageForPost(respParam);
            	IdPTestRunner.setSamlResponse(samlResponse);

            	logger.debug("SAML Response received through POST by the mock SP");
            		
            }
            else if (request.getParameter(SAMLmisc.URLPARAM_SAMLARTIFACT) != null){
            	IdPTestRunner.setSamlResponseBinding(SAMLmisc.BINDING_HTTP_ARTIFACT);
                // TODO: implement for BINDING_HTTP_ARTIFACT
            }
            else{
            	logger.debug("No SAML response received binding (with POST)");
            }
        }
        else{
        	logger.debug("No SAML response received (with neither GET nor POST)");
        }
        
        // Show a simple page as response
    	response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"><html><head><title>SAML2Tester Mock SP</title></head><body><p>The request has been handled and the following SAML Response was received:</p><br><br>"+samlResponse+"</body></html>");
		request.setHandled(true);
	}
}