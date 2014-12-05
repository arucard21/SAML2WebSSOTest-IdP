# SAML2WebSSOTest-IdP
Framework for automated testing of SAML 2.0 IdP entities, written in Java.

# Description
SAML2WebSSOTest-IdP provides a framework for the automated testing of SAML 2.0 IdP entities that use the Web SSO profile. This is commonly known as Single Sign-On (though not all Single Sign-On solutions use SAML). This framework allows you to create new test cases or run existing ones. Currently, only a test suite for the SAML2Int (http://saml2int.org) profile is available (NOT YET), but more can be added to the repository if they are supplied. When you run the test(s), the test results are output in JSON format.

### Limitations:
- Artifact binding is not supported

### Prerequisites
- You need to have an IdP available and you must be able to retrieve the IdP's metadata.

## Usage:

1. Retrieve the mock SP metadata by running SAML2WebSSOTest-IdP with the parameters ```-T/--testsuite``` and ```-m/--metadata```, e.g ```java -jar SAML2WebSSOTest-IdP -T SAML2Int" -m``` when running from JAR or ```SAML2WebSSOTest.IdP.IdPTestRunner -T SAML2Int -m``` when running in an IDE. This will retrieve the metadata for the test suite you specified with ```-t/--testsuite```
2. Configure your IdP to use the mock SP's metadata
3. Copy the ```targetIdP.json``` file and fill in the necessary options. This is described in the Configuration section below
4. Optionally copy the ```slf4j.properties``` file as well to specify the logging configuration
5. Run the test cases in a test suite with the parameters ```-T/--testsuite```, ```-c/--idpconfig``` and ```-t/--testcase```, e.g. ```java -jar SAML2WebSSOTest-IdP -T SAML2Int -c /path/to/targetIdP.properties -t MetadataAvailable``` when running from JAR or ```SAML2WebSSOTest.IdP.IdPTestRunner -T SAML2Int -c /path/to/targetIdP.properties -t MetadataAvailable``` when running in an IDE. You can also run this without the ```-t/--testcase``` parameter, this will cause the test to run all test cases in the test suite.

Some additional useful commands are:
- ```SAML2WebSSOTest.IdP.IdPTestRunner -h``` : Show the help message, containing an overview of all available parameters.
- ```SAML2WebSSOTest.IdP.IdPTestRunner -L``` : Show a list of all available test suites 
- ```SAML2WebSSOTest.IdP.IdPTestRunner -T <test suite> -l``` : Show a list of all available test cases in the given test suite

## Configuration:

The configuration is stored in a `targetIdP.json` file, which you can edit and keep in your current working directory.

```
{
	"metadata": "<string>",
	preResponseInteractions: [
		{ interactionType: "<form/link/element>", lookupAttribute: "<id/name/href/text>", lookupValue: "<value>" }
	]
}
```

You need to provide the following information (make sure the resulting JSON file is valid, e.g. by using a validator like on http://jsonlint.com/):
- `metadata`: Either the actual XML string on a single line or a URL to the metadata
- `preResponseInteractions`: a list of interactions that should be executed on the login page. The interactions are executed sequentially and should cause the target IdP to send its response to the mock SP (e.g. logging in to your IdP). Each interaction is specified as follows: 
  - `interactionType`: This specifies how you wish to interact with the page. This should be `form`, `link` or `element`.
    - `form`: Allows you to look up a form on the page, fill in some of the fields and submit it
    - `link`: Allows you to look up a link (with some link-specific attributes) on the page and click it
    - `element`: Allows you to look up any HTML element on the page and click it
  - `lookupAttribute`: This should be `id`, `name`, `href` or `text`. Note that `href` and `text` can only be used when interactionType is `link`. 
    - `id`, `name`: The element you wish to interact with will be looked up by its "id" or "name" attribute
    - `href` (link only): The link (anchor) element you wish to interact with will be looked up by its "href" attribute 
    - `text` (link only):  The link (anchor) element you wish to interact with will be looked up by the entire text of the link
  - `lookupValue`: This should be the value of the attribute or text for the element you wish to interact with
  - `submitName` (form only): is the value of the "name" attribute on the submit button
  - `inputs` (form only): is a list of `name`s of the input fields on the form and the corresponding `value`s you wish to fill in 

## Creating your own test suite:

You can create your own test suite in the `saml2webssotest.idp.testsuites` package by extending the provided TestSuite class. You can use the SAML2Int test case as an example.

Each test suite must define the characteristics of its mock SP. This mock SP is then used to test the target IdP. In order to define your mock SP, you should implement the abstract methods from the TestSuite class. You need to define the Entity ID, URL and IdP metadata XML for your mock SP. Aside from these abstract methods, the TestSuite class also contains some utility methods.  

You can then create the test cases. Each test case must be created as an inner class that extends one of the TestCase interfaces that define as specific type of test case:

- `ConfigTestCase`: this type of test case can be used to test aspects of the user's configuration. You can do this by implementing the `checkConfig(IdPConfiguration)` method, which supplies the user's configuration so you can check all aspects of it.
- `MetadataTestCase`: this type of test case can be used to test the metadata of the target IdP. You can do this by implementing the `checkMetadata(Document)` method, which supplies the IdP metadata that was found so you can check all aspects of it.
- `ResponseTestCase`: this type of test case can be used to test the SAML Response XML that was sent by the target IdP. You can do this by implementing the `checkResponse(Document)` method, which supplies the SAML Response, as received by the mock SP, so you can check all aspects of it. 

Each TestCase should ultimately return a TestStatus, which is an enum of the following values: UNKNOWN, INFORMATION, OK, WARNING, ERROR, CRITICAL.
They should be used as follows:

- `INFORMATION`: This status level is used when nothing can be said about the status of the test. It allows you to return a neutral status, which might sometimes be required.
- `OK`: This status level is used when the test is successful.
- `WARNING`: This status level is used when a test failed, but the failure does not mean non-compliance with the specification. This occurs when testing the recommendations of a specification instead of its requirements.
- `ERROR`: This status level is used when a test failed and its failure indicates non-compliance with the specification.

The values `UNKNOWN` and `CRITICAL` should not be used in the test cases. UNKNOWN is a fallback status, which should never be used, and CRITICAL is used to show that the test itself failed, whenever possible (exceptions can and most likely will still be thrown) 
 