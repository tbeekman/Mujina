/*
*   Copyright 2010 James Cox <james.s.cox@gmail.com>
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

package jcox.saml.xml;

import jcox.util.IDService;
import jcox.util.TimeService;
import jcox.saml.xml.IssuerGenerator;

import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.impl.AssertionBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class AssertionGenerator {

	private final XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

	private final IssuerGenerator issuerGenerator;
	private final SubjectGenerator subjectGenerator;
	private final IDService idService;
	private final TimeService timeService;
	private final AuthnStatementGenerator authnStatementGenerator = new AuthnStatementGenerator();
	private final AttributeStatementGenerator attributeStatementGenerator = new AttributeStatementGenerator(); 
	
	
	public AssertionGenerator(String issuingEntityName, TimeService timeService, IDService idService) {
		super();
	
		this.timeService = timeService;
		this.idService = idService;
		issuerGenerator = new IssuerGenerator(issuingEntityName);
		subjectGenerator = new SubjectGenerator(timeService);
	}
	
	public  Assertion generateAssertion (UsernamePasswordAuthenticationToken authToken, String recepientAssertionConsumerURL, int validForInSeconds,  String inResponseTo, DateTime authnInstant) {
		
		
		UserDetails principal =	(UserDetails) authToken.getPrincipal();
		WebAuthenticationDetails details = (WebAuthenticationDetails) authToken.getDetails();
		
		AssertionBuilder assertionBuilder = (AssertionBuilder)builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
		Assertion assertion = assertionBuilder.buildObject();
		
		Subject subject = subjectGenerator.generateSubject(recepientAssertionConsumerURL, validForInSeconds, principal.getUsername(), inResponseTo, details.getRemoteAddress());
		
		Issuer issuer = issuerGenerator.generateIssuer();
		
		AuthnStatement authnStatement = authnStatementGenerator.generateAuthnStatement(authnInstant);
		
		assertion.setIssuer(issuer);
		assertion.getAuthnStatements().add(authnStatement);
		assertion.setSubject(subject);

		assertion.getAttributeStatements().add(attributeStatementGenerator.generateAttributeStatement(authToken.getAuthorities()));
		
		assertion.setID(idService.generateID());
		assertion.setIssueInstant(timeService.getCurrentDateTime());
		return assertion;
	}
}