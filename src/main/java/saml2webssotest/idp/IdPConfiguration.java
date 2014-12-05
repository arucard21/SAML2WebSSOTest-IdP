package saml2webssotest.idp;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import saml2webssotest.common.Interaction;
import saml2webssotest.common.standardNames.MD;

public class IdPConfiguration {
	/**
	 * Contains the URL to the page where IdP-initiated authentication should start (on the target IdP)
	 */
	private URL idpInitURL;
	/**
	 * Contains the metadata from the target IdP. This is used for metadata test cases and to access the target IdP in order to receive its responses
	 */
	private Document metadata;
	/**
	 * Contains the interactions to be used before logging in
	 */
	private ArrayList<Interaction> preResponseInteractions = new ArrayList<Interaction>();
	
	public URL getIdPInitURL() {
		return idpInitURL;
	}

	public void setStartPage(URL startPage) {
		this.idpInitURL = startPage;
	}

	public Document getMetadata() {
		return metadata;
	}
	
	public void setMetadata(Document md) {
		metadata = md;
	}
	/**
	 * Retrieve all nodes with the requested tag name from the metadata
	 * 
	 * @param tagName is the name of the requested nodes
	 * @return a list of nodes with the requested tag name
	 */
	public List<Node> getMDNodes(String tagName) {
		// make sure the metadata is available
		if (metadata == null)
			return null;
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		NodeList allNodes = metadata.getElementsByTagNameNS(MD.NAMESPACE, tagName);
		//convert NodeList to List of Node objects
		for (int i = 0; i < allNodes.getLength(); i++){
			nodes.add(allNodes.item(i));
		}
		return nodes;
	}
	
	/**
	 * Retrieve the values of the requested attributes for the nodes with the requested tag name
	 * from the metadata
	 *  
	 * @param tagName is the name of the requested nodes
	 * @param attrName is the name of the attribute that should be present on the requested nodes
	 * @return a list of the values of the requested attributes for the requested nodes
	 */
	public List<String> getMDAttributes(String tagName, String attrName) {
		//make sure the metadata is available
		if (metadata == null)
			return null;
		
		ArrayList<String> resultAttributes = new ArrayList<String>();
		NodeList allACS = metadata.getElementsByTagNameNS(MD.NAMESPACE, tagName);
		for (int i = 0; i < allACS.getLength(); i++){
			Node acs = allACS.item(i);
			resultAttributes.add(acs.getAttributes().getNamedItem(attrName).getNodeValue());
		}
		return resultAttributes;
	}
	
	/**
	 * Retrieve the value of a single attribute for the node with the requested tag name from 
	 * the metadata. 
	 * 
	 * If more than one attribute is found, this will return null. 
	 * Use {@link #getMDAttributes(String, String)} instead.
	 *  
	 * @param tagName is the name of the requested nodes
	 * @param attrName is the name of the attribute that should be present on the requested nodes
	 * @return the value of the requested attribute, or null if none or multiple attributes were found
	 */
	public String getMDAttribute(String tagName, String attrName) {
		List<String> allIDs = getMDAttributes(tagName, attrName);
		if(allIDs.size() == 1){
			return allIDs.get(0);
		}
		else {
			return null;
		}
	}
	
	/**
	 * Retrieve the location of the AssertionConsumerService for a specific
	 * binding for the SP that is being tested from the metadata
	 * 
	 * @param binding specifies for which binding the location should be retrieved
	 * @return the location for the requested binding or null if it is not found
	 */
	public String getMDSSOLocation(String binding) {
		ArrayList<Node> ssoNodes = (ArrayList<Node>) getMDNodes(MD.SINGLESIGNONSERVICE);
		// check all ACS nodes for the requested binding
		for (Node sso : ssoNodes) {
			if (sso.getAttributes().getNamedItem(MD.BINDING)
					.getNodeValue().equalsIgnoreCase(binding))
				// return the location for the requested binding
				return sso.getAttributes().getNamedItem(MD.LOCATION)
						.getNodeValue();
		}
		// the requested binding could not be found
		return null;
	}
	/**
	 * @return the preloginInteractions
	 */
	public ArrayList<Interaction> getPreResponseInteractions() {
		return preResponseInteractions;
	}
	/**
	 * @param preloginInteraction is the interaction object that should be added to the preloginInteractions
	 */
	public void setPreResponseInteractions(ArrayList<Interaction> preLoginInteractions) {
		this.preResponseInteractions = preLoginInteractions;
	}
}
