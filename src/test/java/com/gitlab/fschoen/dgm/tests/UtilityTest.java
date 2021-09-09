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
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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
import com.gitlab.fschoen.dgm.SPARQLEvaluation;

/**
 * EDIT: 04.04.2020: implemented signature verification
 * 
 */

/**
 * @author Falko Schönteich
 *
 */
@SuppressWarnings("unused")
public class UtilityTest {

	private static final Logger log = LogManager.getLogger(UtilityTest.class);

	public static final String localDirectorydgFoafProfiles = "\\src\\test\\resources\\dgfoafprofiles\\";
	
	public static final String rdf4jServer = "http://192.168.178.82:8080/rdf4j-server/";
	public static final String repoName= "test";

	
	@Test
	public void clearRepoTest() {
		SPARQLEvaluation ev = new SPARQLEvaluation(rdf4jServer,repoName);
		ev.clearRepo();
		ev.shutDown();
	}
	
	@Test
	public void testIRI() {
		Repository repo = new SailRepository(new MemoryStore());
		repo.init();
		RepositoryConnection repoCon = null;			
		ValueFactory vf = null;
		try {
			repoCon = repo.getConnection();
			vf = repoCon.getValueFactory();

		} catch (RepositoryException e) {
			log.warn("Could not connect to repository.");
		}

		log.info(vf.createIRI("http://C:/Test"));
//		log.info(localDirectoryABOF);
//		log.info(new File(localDirectoryABOF+ROOTRDF));
	}
	
	@Test
	public void genericTest() {
		log.info(DatalogEvaluation.getSparqlPrefix());
		log.info(Paths.get("").toAbsolutePath().toString() + localDirectorydgFoafProfiles);
	}


	@Test
	public void createSelfSignedCertificate()
			throws CertificateException, NoSuchAlgorithmException, OperatorCreationException, IOException {
		
		log.info("Starting certification generation test.");
		log.trace("Starting key generation.");
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(4096);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		log.trace("Generate certificate");
		

		log.trace("Private Key:" + keyPair.getPrivate());
		FileOutputStream fileWrite = new FileOutputStream(new File("target/testPrivate.key"));
		fileWrite.write(keyPair.getPrivate().getEncoded());
		fileWrite.flush();
		fileWrite.close();
		
		log.trace("Public Key: "+ keyPair.getPublic());
		fileWrite = new FileOutputStream(new File("target/testPublic.key"));
		fileWrite.write(keyPair.getPublic().getEncoded());
		fileWrite.flush();
		fileWrite.close();
		
		X509Certificate cert = CertGenerator.generate(keyPair, "SHA256withRSA", "localhost", 3650, "http://test.com");

		fileWrite = new FileOutputStream(new File("target/testcertificateSS.cer"));
		log.trace(cert.getPublicKey());
		fileWrite.write(cert.getEncoded());
		fileWrite.flush();
		fileWrite.close();
		
		log.trace("Assertions");
		log.debug(cert.toString());
		assertEquals("CN=localhost", cert.getSubjectDN().getName());
		
		
		Collection<List<?>> altNames;

		altNames = cert.getSubjectAlternativeNames();

		String url = "";


		for (Iterator<List<?>> i = altNames.iterator(); i.hasNext();) {
			List<?> item = (List<?>) i.next();
			if (item.get(1).toString().startsWith("http://")||item.get(1).toString().startsWith("https://")) {
				url = item.get(1).toString();
			}
		}
		log.trace("Cert alt name: " + url );
		assertEquals("http://test.com",url);
		assertEquals(cert.getSubjectDN(), cert.getIssuerDN());
		assertEquals("SHA256withRSA", cert.getSigAlgName());
		assertEquals(3, cert.getVersion());
	}
	
	
	

}
