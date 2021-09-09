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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.gitlab.fschoen.dgm.CertGenerator;
import com.gitlab.fschoen.dgm.SPARQLEvaluation;

import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.SignatureVerifier;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.main.RSAKeyPair;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusReader;

import org.eclipse.rdf4j.repository.Repository;

import org.eclipse.rdf4j.repository.*;

import org.eclipse.rdf4j.model.IRI;

/**
 * EDIT: 21.04.2020: worked on admin checks
 * 
 */

/**
 * @author Falko Schönteich
 *
 */

@SuppressWarnings("unused")

public class EvaluationTestABOF {

	private static final Logger log = LogManager.getLogger(EvaluationTestABOF.class);

	public static final String localDirectoryABOF = "\\src\\test\\resources\\abfo\\";
	public static final String ROOTRDF ="root.n3";
	public static final String ROOTRDF_A ="root-a.n3";
	public final static String LOCALimport = "http://import.localhost/";
	public final static String LOCALtrusted = "http://trusted.localhost/";
	
	public static final String rdf4jServer = "http://192.168.178.82:8080/rdf4j-server/";
	public static final String repoName= "test";
	
	
	public static final String root_a = "http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/abfo/a/root-a";

	public static final String a_1 = "http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/abfo/a/a-1";
	public static final String b_1 = "http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/abfo/b/b-1";
	
	public static final String a_2 = "http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/abfo/a/a-2";
	
	public static final String f_1 = "http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/abfo/f/f-1";

	public static final String o_1 = "http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/abfo/o/o-1";
	
	public String getLastPart(String fullPath) {
		String result= fullPath.substring(fullPath.toString().lastIndexOf("\\")+1, fullPath.toString().length());
		return result.substring(result.toString().lastIndexOf("/")+1, result.toString().length());
	}

	
	public String getFullPathToLocalFile(String fileName, String localDir) {
		return Paths.get("").toAbsolutePath().toString() 
				+ localDir
				+ fileName;
	}
	
	public String getTestedGraph(String signer,String localDir) {
		return getFullPathToLocalFile(
				getLastPart(signer)
				+ "_abfo_signed.trig",
				localDir);
	}
	
	public String getPublicKey(String signer, String localDir) {
		return getFullPathToLocalFile(
				getLastPart(signer)
				+ "_public.key",
				localDir);

	}
	
	public String getPrivateKey(String signer, String localDir) {
		return Paths.get("").toAbsolutePath().toString() + localDir+signer+"_private.key";
	}
	
	public GraphCollection validateSignedGraph(String signer, String testedFilePath, String publicKeyFilePath) {
		GraphCollection gc = null;
		PublicKey publicKey = null;
		log.trace("Validating " + testedFilePath + " with " + publicKeyFilePath);
		try {
			gc = TriGPlusReader.readFile(testedFilePath, true);			
		} catch (Exception e) {
			log.error("Graph file could not be loaded!");
			e.printStackTrace();
		}
		try {	
			publicKey = RSAKeyPair.loadPublicKey(publicKeyFilePath);
		} catch (Exception e) {
			log.error("Public Key could not be loaded!");
			e.printStackTrace();
		}
		try {
			if(SignatureVerifier.verify(gc, publicKey))	{
				log.info("Signature valid by " + getLastPart(signer));
			} else {
				log.error("Signature INVALID! Public key wrong or "+ getLastPart(signer) + " not signer.");
			}
		} catch (Exception e) {
			log.error("Verification error.");
			e.printStackTrace();
		}
		
		return gc;
	}
	

	@Test
	public void evaluate_a1_signed() {
		String signer=a_1;
		String testedGraph=getTestedGraph(signer, localDirectoryABOF);

		///// Signature validation of graph /////
		GraphCollection gc=validateSignedGraph(
				signer, testedGraph, getPublicKey(signer, localDirectoryABOF) );		
		if(gc==null) assertTrue(false);
		
		///// Initializing Evaluation /////
		log.trace("Creating SPARQL Evaluation");
		SPARQLEvaluation ev = new SPARQLEvaluation(rdf4jServer,repoName);
		ev.clearRepo();
		
		///// Adding ROOTRDF to Evaluation as LOCALtrusted /////
		log.trace("Adding " + ROOTRDF);
		ev.addToRepository(getFullPathToLocalFile(ROOTRDF,localDirectoryABOF),LOCALtrusted);
		
		
		///// Adding ROOTRDF to Evaluation as LOCALtrusted /////
		log.trace("Adding " + ROOTRDF_A);
		ev.addToRepository(getFullPathToLocalFile(ROOTRDF_A,localDirectoryABOF),LOCALimport+ROOTRDF_A);
		
		ev.checkNamespaceOwnerships(root_a, LOCALimport+ROOTRDF_A);
		
		///// Adding graph to be tested/inferred on to Evaluation, but as LOCALimport /////
		log.trace("Adding " + getLastPart(testedGraph));
		assertTrue(ev.addToRepository(testedGraph,LOCALimport+getLastPart(testedGraph)));
	
		ev.checkOwlSameAsPersons(signer, LOCALimport+getLastPart(testedGraph));
		ev.checkMembers(signer, LOCALimport+getLastPart(testedGraph));
		
		///// Get illegit Statements/////
		TupleQueryResult tqr=ev.getIllegitStatements(signer, LOCALimport+getLastPart(testedGraph));
		for(BindingSet bind:tqr) 
			log.debug("Illegit binding: "+ bind);

		///// Get legit Membersips/////
		tqr=ev.getLegitMembershipAssignments(signer, LOCALimport+getLastPart(testedGraph));
		for(BindingSet bind:tqr) 
			log.debug("Legit Membership: "+ bind);
	
		
		
		///// Same as checks/////
		String personA = a_1;
		String personB = b_1;		
		
		assertTrue(testInheritsPermission(personB, personA));
		
		
		
		ev.shutDown();
	}
	
	
	public boolean testInheritsPermission(String personA, String personB) {
		
		log.info("Starting inhert-test " + personA +" <- " + personB);
		
		SPARQLEvaluation ev = new SPARQLEvaluation();
		
		boolean testSuccessful = ev.checkInheritsPermission(personA, personB);
		String resultMessage = personA + (testSuccessful ? " <- " : " <-/- ") +  personB;
		if (testSuccessful)
			log.info(resultMessage);
		else
			log.error(resultMessage);
		
		ev.shutDown();
		return testSuccessful; 
	}
}
