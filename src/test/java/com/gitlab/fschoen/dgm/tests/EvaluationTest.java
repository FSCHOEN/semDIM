package com.gitlab.fschoen.dgm.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.gitlab.fschoen.dgm.CertGenerator;
import com.gitlab.fschoen.dgm.DatalogEvaluation;

/**
 * EDIT: 05.04.2020: reworked Test structre
 * 
 */

/**
 * @author Falko Schönteich
 *
 */
@SuppressWarnings("unused")
public class EvaluationTest {

	private static final Logger log = LogManager.getLogger(EvaluationTest.class);

	public static final String localDirectorydgFoafProfiles = "\\src\\test\\resources\\dgfoafprofiles\\";
	
	// Happy Group; Admins: Adam, Alice; Members: Charly, Ronald
	public static final String happyGroupAdam="http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgfoafprofiles/dgfoafadam.n3#HappyGroup";
	public static final String happyGroupAmanda="http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgfoafprofiles/dgfoafamanda.n3#HappyGroup";
	public static final String charly ="http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgfoafprofiles/dgfoafcharly.n3#me";	
	public static final String barnie ="http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgfoafprofiles/dgfoafbarnie.n3#me";	
	public static final String samebarnie ="http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgfoafprofiles/alternativeDomain.n3#SameBarnie";
	public static final String larissa ="http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgfoafprofiles/dgfoaflarissa.n3#me";	
	public static final String samelarissa ="http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgfoafprofiles/alternativeDomain.n3#SameLarissa";
	public static final String samelarissa2 ="http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgfoafprofiles/alternativeDomain.n3#SameLarissa2";
	public static final String otherlarissa ="http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgfoafprofiles/otherLarissa.n3#me";
	public static final String penny ="http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgfoafprofiles/penny.n3#me";	

	public boolean testMembership(String group, String person) {
		return testMembership(group, person, true);
	}
	
	public boolean testMembership(String group, String person, boolean successfulIfMember) {
		
		String fileName=person.toString().substring(person.toString().lastIndexOf("/")+1,person.toString().length());
		log.info("Starting evaluateMembership " + fileName);
		
		DatalogEvaluation ev = new DatalogEvaluation();
		
		boolean testSuccessful = ev.evaluateMembership(group, person,
				Paths.get("").toAbsolutePath().toString() + localDirectorydgFoafProfiles);
		if(!successfulIfMember)
			testSuccessful=!testSuccessful;
		String resultMessage = person + (testSuccessful ? " is " : " is not ") + "a member of " + group;
		if (testSuccessful)
			log.info(resultMessage);
		else
			log.error(resultMessage);
		
		ev.shutDown();
		return testSuccessful; 
	}	
	
	public boolean testMembershipTwoGroups(String groupA, String groupB, String person) {
		return testMembershipTwoGroups(groupA, groupB, person, true);
	}
	
	public boolean testMembershipTwoGroups(String groupA, String groupB, String person, boolean successfulIfMember) {
		
		DatalogEvaluation ev = new DatalogEvaluation();


		boolean testSuccessfulA = ev.evaluateMembership(groupA, person,
				Paths.get("").toAbsolutePath().toString() + localDirectorydgFoafProfiles);
		if(!successfulIfMember)
			testSuccessfulA=!testSuccessfulA;
		String resultMessage = person + (!testSuccessfulA ? " is " : " is not ") + "a member of " + groupA;
		if (testSuccessfulA)
			log.info(resultMessage);
		else
			log.error(resultMessage);


		boolean testSuccessfulB = ev.evaluateMembership(groupB, person,
				Paths.get("").toAbsolutePath().toString() + localDirectorydgFoafProfiles);
		if(!successfulIfMember)
			testSuccessfulB=!testSuccessfulB;
		resultMessage = person + (!testSuccessfulB ? " is " : " is not ") + "a member of " + groupB;

		if (testSuccessfulB)
			log.info(resultMessage);
		else
			log.error(resultMessage);

		ev.shutDown();
		return(testSuccessfulA && testSuccessfulB);
	}	

	@Test
	public void evaluateMembershipCharly() {
		String group = happyGroupAdam;
		String person =charly;		
		assertTrue(testMembership(group, person));
	}
	
	@Test
	public void evaluateMembershipBarny() {
		String group = happyGroupAdam;
		String person =barnie;		
		assertTrue(testMembership(group, person));
	}
	
	@Test
	public void evaluateMembershipSameBarnie() {
		String group = happyGroupAdam;
		String person =samebarnie;		
		assertTrue(testMembership(group, person));
	}
	
	@Test
	public void evaluateMembershipPenny() {
		String group = happyGroupAdam;
		String person =penny;		
		assertTrue(testMembership(group, person,false));
	}
	
	@Test
	public void evaluateMembershipLarissa() {
		String groupA = happyGroupAdam;
		String groupB = happyGroupAmanda;
		String person =larissa;		
		assertTrue(testMembershipTwoGroups(groupA, groupB, person, false));
	}
	

	@Test
	public void evaluateMembershipSameLarissa() {
		String groupA = happyGroupAdam;
		String groupB = happyGroupAmanda;
		String person =samelarissa;		
		assertTrue(testMembershipTwoGroups(groupA, groupB, person, false));
	}
	

	@Test
	public void evaluateMembershipSameLarissa2() {
		String groupA = happyGroupAdam;
		String groupB = happyGroupAmanda;
		String person =samelarissa2;		
		assertTrue(testMembershipTwoGroups(groupA, groupB, person, false));
	}
	
	@Test
	public void evaluateMembershipOtherLarissa() {
		String groupA = happyGroupAdam;
		String groupB = happyGroupAmanda;
		String person =otherlarissa;		
		assertTrue(testMembershipTwoGroups(groupA, groupB, person, true));
	}
	



	@Test
	public void evaluateSameBarnie() {

		DatalogEvaluation ev = new DatalogEvaluation();
		
		String persona = barnie;
		String personb = samebarnie;
		
		boolean testSuccessful = ev.evaluateSameAs(persona, personb,
				Paths.get("").toAbsolutePath().toString() + localDirectorydgFoafProfiles);
		String resultMessage = persona + (testSuccessful ? " is " : " is not ") + "the same as " + personb;
		if (testSuccessful)
			log.info(resultMessage);
		else
			log.error(resultMessage);

		ev.shutDown();
		assertTrue(testSuccessful);
	}

	

}
