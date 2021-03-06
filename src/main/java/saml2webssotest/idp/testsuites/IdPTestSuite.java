package saml2webssotest.idp.testsuites;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.UUID;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Audience;
import org.opensaml.saml2.core.AudienceRestriction;
import org.opensaml.saml2.core.AuthnContext;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.security.x509.X509Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import saml2webssotest.common.TestStatus;
import saml2webssotest.common.TestSuite;
import saml2webssotest.common.standardNames.MD;
import saml2webssotest.common.standardNames.SAMLmisc;
import saml2webssotest.idp.IdPConfiguration;
import saml2webssotest.idp.IdPTestRunner;

/**
 * This is the module containing the abstract base classes that are required in every test suite, as well as any methods that 
 * would be useful for any test suite. All test suites should inherit the classes in this module and import the necessary methods 
 * and variables.
 * 
 * Each test case should be defined in the testcases variable below. See the documentation there for more information.
 * 
 * SAML2test uses a mock SP to test against. Each test suite can specify the configuration of the mock SP separately.
 * 
 * - The metadata that the mock SP should use can be generated in the get_idp_metadata() method. The metadata can be generated using 
 * the builders in saml2test.saml_builders.metadata_builders module.
 * 
 * - The SAML Responses that should be sent, have to be defined in each SAML_Response_Test class. It must be returned by the 
 * test_response() method. The SAML Responses can be generated using the builders in saml2test.saml_builders.response_builders module.
 * 
 * @author: Riaas Mokiem
 */
public abstract class IdPTestSuite implements TestSuite {
	/**
	 * Logger for this class
	 */
	private final Logger logger = LoggerFactory.getLogger(IdPTestRunner.class);

	/**
	 * Retrieves the EntityID for the mock SP
	 * 
	 * @return the EntityID for the mock SP
	 */
	public abstract String getmockSPEntityID();

	/**
	 * Retrieves the URL for the mock SP
	 * 
	 * @return the URL for the mock SP
	 */
	public abstract URL getMockSPURL();
	
	/**
	 * Retrieve the X.509 Certificate that should be used by the mock SP.
	 * 
	 * @param certLocation contains the location of the certificate file that should be used (e.g. "keys/mycert.pem")
	 * 			Can be null or empty, in which case a default certificate is used
	 * @return: the X.509 Certificate credentials
	 */
	public X509Credential getX509Credentials(String certLocation){
		BasicX509Credential credentials = new BasicX509Credential();
		String cert = "";
		
		// if a specific certificate location is provided, use the certificate from that location
		if ( certLocation != null &&  !certLocation.isEmpty() ){
			Path certPath = Paths.get(certLocation); 
			try {
				BufferedReader reader = Files.newBufferedReader(certPath, Charset.defaultCharset());
				String line;
				while ( (line = reader.readLine()) != null){
					cert += line + "\n";
				}
			} catch (IOException e) {
				logger.error("IOException occurred while accessing the user-provided file for the mock SP's X.509 Certificate", e);
			}
		}
		else {
			// use the default certificate
			cert = 	"-----BEGIN CERTIFICATE-----\r\n" + 
					"MIIC8jCCAlugAwIBAgIJAJHg2V5J31I8MA0GCSqGSIb3DQEBBQUAMFoxCzAJBgNV\r\n" + 
					"BAYTAlNFMQ0wCwYDVQQHEwRVbWVhMRgwFgYDVQQKEw9VbWVhIFVuaXZlcnNpdHkx\r\n" + 
					"EDAOBgNVBAsTB0lUIFVuaXQxEDAOBgNVBAMTB1Rlc3QgU1AwHhcNMDkxMDI2MTMz\r\n" + 
					"MTE1WhcNMTAxMDI2MTMzMTE1WjBaMQswCQYDVQQGEwJTRTENMAsGA1UEBxMEVW1l\r\n" + 
					"YTEYMBYGA1UEChMPVW1lYSBVbml2ZXJzaXR5MRAwDgYDVQQLEwdJVCBVbml0MRAw\r\n" + 
					"DgYDVQQDEwdUZXN0IFNQMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDkJWP7\r\n" + 
					"bwOxtH+E15VTaulNzVQ/0cSbM5G7abqeqSNSs0l0veHr6/ROgW96ZeQ57fzVy2MC\r\n" + 
					"FiQRw2fzBs0n7leEmDJyVVtBTavYlhAVXDNa3stgvh43qCfLx+clUlOvtnsoMiiR\r\n" + 
					"mo7qf0BoPKTj7c0uLKpDpEbAHQT4OF1HRYVxMwIDAQABo4G/MIG8MB0GA1UdDgQW\r\n" + 
					"BBQ7RgbMJFDGRBu9o3tDQDuSoBy7JjCBjAYDVR0jBIGEMIGBgBQ7RgbMJFDGRBu9\r\n" + 
					"o3tDQDuSoBy7JqFepFwwWjELMAkGA1UEBhMCU0UxDTALBgNVBAcTBFVtZWExGDAW\r\n" + 
					"BgNVBAoTD1VtZWEgVW5pdmVyc2l0eTEQMA4GA1UECxMHSVQgVW5pdDEQMA4GA1UE\r\n" + 
					"AxMHVGVzdCBTUIIJAJHg2V5J31I8MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEF\r\n" + 
					"BQADgYEAMuRwwXRnsiyWzmRikpwinnhTmbooKm5TINPE7A7gSQ710RxioQePPhZO\r\n" + 
					"zkM27NnHTrCe2rBVg0EGz7QTd1JIwLPvgoj4VTi/fSha/tXrYUaqc9AqU1kWI4WN\r\n" + 
					"+vffBGQ09mo+6CffuFTZYeOhzP/2stAPwCTU4kxEoiy0KpZMANI=\r\n" + 
					"-----END CERTIFICATE-----";
		}
		// retrieve the certificate
		X509Certificate idpCert = null;
		try {
			idpCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(cert.getBytes()));
		} catch (CertificateException e) {
			e.printStackTrace();
		}
		if (idpCert == null){
			return null;
		}
		else{
			credentials.setEntityCertificate(idpCert);
			credentials.setPublicKey(idpCert.getPublicKey());
			credentials.setPrivateKey(getIdPPrivateKey(null));
			
			return credentials;
		}
	}

    /**
     * Retrieve RSA private key that corresponds to the X.509 Certificate that is used by the mock SP
     * 
     * @param keyLocation contains the location of the private key file that should be used (e.g. "keys/mykey.pem"). 
     * 			Can be null or empty, in which case a default private key is used 
     * @return: the RSA private key in PEM format
     */
	public RSAPrivateKey getIdPPrivateKey(String keyLocation){
		RSAPrivateKey privateKey = null;
		String key = "";
		
		// if a specific key location is provided, use the private key from that location
		if ( keyLocation != null &&  !keyLocation.isEmpty() ){
			Path keyPath = Paths.get(keyLocation); 
			try {
				BufferedReader reader = Files.newBufferedReader(keyPath, Charset.defaultCharset());
				String line;
				while ( (line = reader.readLine()) != null){
					key += line + "\n";
				}
			} catch (IOException e) {
				logger.error("IOException occurred while accessing the user-provided file for the mock SP's private key", e);
			}
		}
		else {
			// use the default private key
			key = 	"-----BEGIN RSA PRIVATE KEY-----\r\n" + 
					"MIICXAIBAAKBgQDkJWP7bwOxtH+E15VTaulNzVQ/0cSbM5G7abqeqSNSs0l0veHr\r\n" + 
					"6/ROgW96ZeQ57fzVy2MCFiQRw2fzBs0n7leEmDJyVVtBTavYlhAVXDNa3stgvh43\r\n" + 
					"qCfLx+clUlOvtnsoMiiRmo7qf0BoPKTj7c0uLKpDpEbAHQT4OF1HRYVxMwIDAQAB\r\n" + 
					"AoGAbx9rKH91DCw/ZEPhHsVXJ6cYHxGcMoAWvnMMC9WUN+bNo4gNL205DLfsxXA1\r\n" + 
					"jqXFXZj3+38vSFumGPA6IvXrN+Wyp3+Lz3QGc4K5OdHeBtYlxa6EsrxPgvuxYDUB\r\n" + 
					"vx3xdWPMjy06G/ML+pR9XHnRaPNubXQX3UxGBuLjwNXVmyECQQD2/D84tYoCGWoq\r\n" + 
					"5FhUBxFUy2nnOLKYC/GGxBTX62iLfMQ3fbQcdg2pJsB5rrniyZf7UL+9FOsAO9k1\r\n" + 
					"8DO7G12DAkEA7Hkdg1KEw4ZfjnnjEa+KqpyLTLRQ91uTVW6kzR+4zY719iUJ/PXE\r\n" + 
					"PxJqm1ot7mJd1LW+bWtjLpxs7jYH19V+kQJBAIEpn2JnxdmdMuFlcy/WVmDy09pg\r\n" + 
					"0z0imdexeXkFmjHAONkQOv3bWv+HzYaVMo8AgCOksfEPHGqN4eUMTfFeuUMCQF+5\r\n" + 
					"E1JSd/2yCkJhYqKJHae8oMLXByNqRXTCyiFioutK4JPYIHfugJdLfC4QziD+Xp85\r\n" + 
					"RrGCU+7NUWcIJhqfiJECQAIgUAzfzhdj5AyICaFPaOQ+N8FVMLcTyqeTXP0sIlFk\r\n" + 
					"JStVibemTRCbxdXXM7OVipz1oW3PBVEO3t/VyjiaGGg=\r\n" + 
					"-----END RSA PRIVATE KEY-----";
		}
		
		try {
			BufferedReader br = new BufferedReader(new StringReader(key));
			Security.addProvider(new BouncyCastleProvider());
			PEMReader pr = new PEMReader(br);
			KeyPair kp = (KeyPair) pr.readObject();
			pr.close();
			br.close();
			privateKey = (RSAPrivateKey) kp.getPrivate();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return privateKey;
	}
	
	/**
	 * Create a minimal SAML AuthnRequest.
	 * 
	 * This creates the minimal SAML AuthnRequest that is valid for the Web SSO profile.
	 * 
	 * TODO: change this to return an AuthnRequest
	 * 
	 * It will:
	 * - generate random UUID-based ID's for the Assertion and Response
	 * - use the AssertionConsumerService URL for the POST binding as Recipient
	 * - use a validity period of 15 minutes from now
	 * - use the bearer method for SubjectConfirmation
	 * - set the AudienceRestriction to the SP Entity ID
	 * - use the Password authentication context
	 * - set all IssueInstant attributes to the current date and time
	 * 
	 * You can edit the Response as you see fit to customize it to your needs
	 * 
	 * @return the minimal SAML Response
	 */
	public Response createMinimalWebSSOResponse(){
		IdPConfiguration sp = IdPTestRunner.getIdPConfig();
		try {
			DefaultBootstrap.bootstrap();
		} catch (ConfigurationException e) {
			logger.error("Could not bootstrap OpenSAML", e);
		}
		XMLObjectBuilderFactory builderfac = Configuration.getBuilderFactory();
		Response response = (Response) builderfac.getBuilder(Response.DEFAULT_ELEMENT_NAME).buildObject(Response.DEFAULT_ELEMENT_NAME);
		Assertion assertion = (Assertion) builderfac.getBuilder(Assertion.DEFAULT_ELEMENT_NAME).buildObject(Assertion.DEFAULT_ELEMENT_NAME);
		Issuer issuer = (Issuer) builderfac.getBuilder(Issuer.DEFAULT_ELEMENT_NAME).buildObject(Issuer.DEFAULT_ELEMENT_NAME);
		Status status = (Status) builderfac.getBuilder(Status.DEFAULT_ELEMENT_NAME).buildObject(Status.DEFAULT_ELEMENT_NAME);
		StatusCode statuscode = (StatusCode) builderfac.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME).buildObject(StatusCode.DEFAULT_ELEMENT_NAME);
		Subject subject = (Subject) builderfac.getBuilder(Subject.DEFAULT_ELEMENT_NAME).buildObject(Subject.DEFAULT_ELEMENT_NAME);
		SubjectConfirmation subjectconf = (SubjectConfirmation) builderfac.getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME).buildObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
		SubjectConfirmationData subjectconfdata = (SubjectConfirmationData) builderfac.getBuilder(SubjectConfirmationData.DEFAULT_ELEMENT_NAME).buildObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
		Conditions conditions = (Conditions) builderfac.getBuilder(Conditions.DEFAULT_ELEMENT_NAME).buildObject(Conditions.DEFAULT_ELEMENT_NAME);
		AudienceRestriction audRes = (AudienceRestriction) builderfac.getBuilder(AudienceRestriction.DEFAULT_ELEMENT_NAME).buildObject(AudienceRestriction.DEFAULT_ELEMENT_NAME);
		Audience aud = (Audience) builderfac.getBuilder(Audience.DEFAULT_ELEMENT_NAME).buildObject(Audience.DEFAULT_ELEMENT_NAME);
		AuthnStatement authnstatement = (AuthnStatement) builderfac.getBuilder(AuthnStatement.DEFAULT_ELEMENT_NAME).buildObject(AuthnStatement.DEFAULT_ELEMENT_NAME);
		AuthnContext authncontext = (AuthnContext) builderfac.getBuilder(AuthnContext.DEFAULT_ELEMENT_NAME).buildObject(AuthnContext.DEFAULT_ELEMENT_NAME);
		AuthnContextClassRef authncontextclassref = (AuthnContextClassRef) builderfac.getBuilder(AuthnContextClassRef.DEFAULT_ELEMENT_NAME).buildObject(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);

		// create status for Response
		statuscode.setValue(SAMLmisc.STATUS_SUCCESS);
		status.setStatusCode(statuscode);
		// create Issuer for Assertion 
		issuer.setValue(getmockSPEntityID());
		// create Subject for Assertion
		subjectconfdata.setNotOnOrAfter(DateTime.now().plusMinutes(15));
		subjectconf.setSubjectConfirmationData(subjectconfdata);
		subjectconf.setMethod(SAMLmisc.CONFIRMATION_METHOD_BEARER);
		subject.getSubjectConfirmations().add(subjectconf);
		// create Conditions for Assertion
		aud.setAudienceURI(sp.getMDAttribute(MD.ENTITYDESCRIPTOR, MD.ENTITYID));
		audRes.getAudiences().add(aud);
		conditions.getAudienceRestrictions().add(audRes);
		// create AuthnStatement for Assertion
		authncontextclassref.setAuthnContextClassRef(SAMLmisc.AUTHNCONTEXT_PASSWORD);
		authncontext.setAuthnContextClassRef(authncontextclassref);
		authnstatement.setAuthnContext(authncontext);
		authnstatement.setAuthnInstant(DateTime.now());
		// add created elements to Assertion
		assertion.setID("_"+UUID.randomUUID().toString());
		assertion.setIssueInstant(DateTime.now());
		assertion.setIssuer(issuer);
		assertion.setSubject(subject);
		assertion.setConditions(conditions);
		assertion.getAuthnStatements().add(authnstatement);
		
		// add created elements to Response
		response.setID("_"+UUID.randomUUID().toString());
		response.setIssueInstant(DateTime.now());
		response.getAssertions().add(assertion);
		response.setStatus(status);
		
		return response;
	}
	
	public interface ConfigTestCase extends TestCase {
		
		/**
		 * Check the provided configuration.  
		 * 
		 * @return the status of the test
		 */
		TestStatus checkConfig(IdPConfiguration config);
	}

	public interface ResponseTestCase extends TestCase {
		
		/**
		 * Define if the login attempt should be SP-initiated
		 * 
		 * @return true if the login attempt should be SP-initiated, false if it should be IdP-initiated
		 */
		boolean isSPInitiated();

		/**
		 * Check the provided response retrieved through the provided binding
		 * 
		 * @return the status of the test
		 */
		TestStatus checkResponse(String response, String binding);
	}
}