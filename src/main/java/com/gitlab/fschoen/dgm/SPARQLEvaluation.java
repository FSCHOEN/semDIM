package com.gitlab.fschoen.dgm;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * 
 * @author Falko Schönteich
 * 
 */
@SuppressWarnings("deprecation")
public class SPARQLEvaluation {
	
	public String repoMode= "REMOTE"; // "REMOTE" or "LOCAL"
	public String rdf4jServer = "http://192.168.178.82:8080/rdf4j-server/";
	public String repoName= "test";
	
	public final static String DGFOAF = "http://userpages.uni-koblenz.de/~schwagereit/dgfoaf/0.1/dgfoaf.owl#";
	public final static String DGM = "http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgm.owl#";
	public final static String TRANSFERSPERMISSIONSTO= DGM+"transfersPermissionsTo";
	public final static String INHERITSPERMISSIONSFROM=DGM+"inheritsPermissionsFrom";
	public final static String LOCALimport = "http://import.localhost/";
	public final static String LOCALtrusted = "http://trusted.localhost/";
	public final static String FOAF = org.eclipse.rdf4j.model.vocabulary.FOAF.NAMESPACE;
	public final static String RDF = org.eclipse.rdf4j.model.vocabulary.RDF.NAMESPACE;
	public final static String RDFS = org.eclipse.rdf4j.model.vocabulary.RDFS.NAMESPACE;
	public final static String OWL=org.eclipse.rdf4j.model.vocabulary.OWL.NAMESPACE;
	public final static String SAMEAS=OWL+"sameAs";
	public final static String MEMBER=FOAF+"member";

	public final static String SPARQLprefixes=
			"PREFIX owl: <http://www.w3.org/2002/07/owl#> \r\n" + 
			"PREFIX localimport: <http://localimport.localhost/>";
	
	
	
	public static String getSparqlPrefix() {
		return "PREFIX foaf:<" + FOAF + "> \n" 
				+ "PREFIX rdf:<" + RDF + "> \n"
				+ "PREFIX rdfs:<" + RDFS + "> \n" + "PREFIX dgfoaf:<" + DGFOAF
				+ "> \n";
	}

	private static final Logger log = LogManager.getLogger(SPARQLEvaluation.class);

	private Repository repo;
	private RepositoryConnection repoCon;
	public ValueFactory vf;
	
//	private Set<String> failedToRetrieve;

//	private String prefixes = getSparqlPrefix();

	public SPARQLEvaluation() {

		initialize();
	}
	
	public SPARQLEvaluation(String rdf4jServer, String repoName) {
		this.rdf4jServer=rdf4jServer;
		this.repoName=repoName;
		initialize();
	}
	
	public SPARQLEvaluation(Boolean localRepo) {
		if(localRepo)
			this.repoMode="LOCAL";		
		initialize();
	}


	public void initialize() {

		if(repoMode.equals("REMOTE")) {			
			log.trace("Initializing Remote Repo: " + repoName + " @ " + rdf4jServer);
			repo = new HTTPRepository(rdf4jServer,repoName);
		} else {
		repo = new SailRepository(new MemoryStore());
		repo.init();
		repoCon = null;			
		}

//		failedToRetrieve = new HashSet<String>();

		try {
			repoCon = repo.getConnection();
			vf = repoCon.getValueFactory();

		} catch (RepositoryException e) {
			log.warn("Could not connect to repository.");
		}
	}
	
	public void shutDown() {
		log.trace("Shutting down Repo");
		if(repoMode.equals("REMOTE")) repo.shutDown();
	}
	
	public void clearRepo() {
		repoCon.clear();
		
	}


	public boolean addToRepository(String tripleFile) {
		return addToRepository(tripleFile, tripleFile);
	}
	
	public boolean addToRepository(String tripleFile,String context) {
			RDFDataImporter imp= new RDFDataImporter(tripleFile,context);
			return imp.importProfile(repoCon);
		
	}
	


	public List<Statement> checkOwlSameAsPersons(String signer,String contextToCheck) {
		log.trace("checkOwlSameAsPersons("+signer +", " + contextToCheck+")");
		IRI signerIRI=vf.createIRI(signer);
		IRI contextIRI=null;
		if(!contextToCheck.equals(""))
			contextIRI=vf.createIRI(contextToCheck);
		RepositoryResult<Statement> resultStatements= repoCon.getStatements(signerIRI, vf.createIRI(SAMEAS), null,contextIRI);
		for(Statement st:resultStatements) {
			IRI personBIRI=(IRI) st.getObject();
			repoCon.add(vf.createStatement(signerIRI,vf.createIRI(TRANSFERSPERMISSIONSTO),personBIRI),vf.createIRI(LOCALtrusted));
			repoCon.add(vf.createStatement(personBIRI,vf.createIRI(INHERITSPERMISSIONSFROM),signerIRI),vf.createIRI(LOCALtrusted));
		}
		
		RepositoryResult<Statement> resultStatements2= repoCon.getStatements(null, vf.createIRI(SAMEAS), signerIRI,contextIRI);
		for(Statement st:resultStatements2) {
			IRI personBIRI=(IRI) st.getSubject();
			repoCon.add(vf.createStatement(personBIRI,vf.createIRI(TRANSFERSPERMISSIONSTO),signerIRI),vf.createIRI(LOCALtrusted));
			repoCon.add(vf.createStatement(signerIRI,vf.createIRI(INHERITSPERMISSIONSFROM),personBIRI),vf.createIRI(LOCALtrusted));
		}
		
		List<Statement> result=resultStatements.asList();
		resultStatements2.asList().addAll(result);
		return result;
	}
	
	public void checkMembers(String signer,String contextToCheck) {
		log.trace("checkMembers("+signer +", " + contextToCheck+")");
		TupleQueryResult tqr=getLegitMembershipAssignments(signer, contextToCheck);
		
		for(BindingSet bind:tqr) 
			repoCon.add(vf.createStatement(
					(IRI) bind.getValue("Group"),vf.createIRI(MEMBER),bind.getValue("Member"),vf.createIRI(LOCALtrusted)
					));		
		
	}
	
	public void checkSameGroups(String signer,String contextToCheck) {
		log.trace("checkMembers("+signer +", " + contextToCheck+")");
		TupleQueryResult tqr=getLegitGroupPermissionTransfers(signer, contextToCheck);
		
		for(BindingSet bind:tqr) 
			repoCon.add(vf.createStatement(
					(IRI) bind.getValue("Group1"),vf.createIRI(TRANSFERSPERMISSIONSTO),bind.getValue("Group2"),vf.createIRI(LOCALtrusted)
					));		
		
	}
	
	public void checkNamespaceOwnerships(String signer,String contextToCheck) {
		log.trace("checkNamespaceOwnersips("+signer +", " + contextToCheck+")");
		TupleQueryResult tqr=getLegitNamespaceDefinitions(signer, contextToCheck);
		
		for(BindingSet bind:tqr) 
			repoCon.add(vf.createStatement(
					(IRI) bind.getValue("s"), (IRI) bind.getValue("p"),bind.getValue("o"),vf.createIRI(LOCALtrusted)
					));		
		
	}
	
	public List<Statement> chechOwlSameAsGroups(String signer, String contextToCheck){
		IRI signerIRI=vf.createIRI(signer);
		IRI contextIRI=null;
		if(!contextToCheck.equals(""))
			contextIRI=vf.createIRI(contextToCheck);
		RepositoryResult<Statement> resultStatements= repoCon.getStatements(signerIRI, vf.createIRI(SAMEAS), null,contextIRI);
		for(Statement st:resultStatements) {
			IRI personBIRI=(IRI) st.getObject();
			repoCon.add(vf.createStatement(signerIRI,vf.createIRI(TRANSFERSPERMISSIONSTO),personBIRI),vf.createIRI(LOCALtrusted));
			repoCon.add(vf.createStatement(personBIRI,vf.createIRI(INHERITSPERMISSIONSFROM),signerIRI),vf.createIRI(LOCALtrusted));
		}
		
		RepositoryResult<Statement> resultStatements2= repoCon.getStatements(null, vf.createIRI(SAMEAS), signerIRI,contextIRI);
		for(Statement st:resultStatements2) {
			IRI personBIRI=(IRI) st.getSubject();
			repoCon.add(vf.createStatement(personBIRI,vf.createIRI(TRANSFERSPERMISSIONSTO),signerIRI),vf.createIRI(LOCALtrusted));
			repoCon.add(vf.createStatement(signerIRI,vf.createIRI(INHERITSPERMISSIONSFROM),personBIRI),vf.createIRI(LOCALtrusted));
		}
		
		List<Statement> result=resultStatements.asList();
		resultStatements2.asList().addAll(result);
		return result;
		
	}
	
	public TupleQueryResult getIllegitStatements(String signer, String contextToCheck){
//		IRI signerIRI=vf.createIRI(signer);
//		IRI contextIRI=null;
//		if(!contextToCheck.equals(""))
//			contextIRI=vf.createIRI(contextToCheck);	
		
		String query="PREFIX owl: <http://www.w3.org/2002/07/owl#> \r\n" + 
				"PREFIX import: <http://import.localhost/>\r\n" + 
				"\r\n" + 
				"SELECT DISTINCT ?p1 ?p2 WHERE {\r\n" + 
				"GRAPH <" + contextToCheck + "> \r\n" + 
				"  {?p1 owl:sameAs ?p2}  \r\n" + 
				"  filter(?p1!= <" + signer + "> && ?p2!= <" + signer + ">)\r\n" + 
				"}";

		TupleQuery tQuery=repoCon.prepareTupleQuery(QueryLanguage.SPARQL,query);
		TupleQueryResult resultBindingsets= tQuery.evaluate();
		
		return resultBindingsets;		
	}
	
	public TupleQueryResult getLegitMembershipAssignments(String signer, String contextToCheck){
		
		
		String query = "PREFIX import: <http://import.localhost/>\r\n" + 
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\r\n" + 
				"PREFIX trusted: <http://trusted.localhost/>\r\n" + 
				"PREFIX dgm: <http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgm.owl#>\r\n" + 
				"SELECT Distinct ?Group ?Member WHERE {\r\n" + 
				"  GRAPH <" + contextToCheck + "> \r\n" + 
				"  { ?Group foaf:member ?Member }\r\n" + 
				"  GRAPH trusted: \r\n" + 
				"  { <" + signer + "> ^foaf:member* / dgm:administrate ?Group }\r\n" + 
				"}";

		TupleQuery tQuery=repoCon.prepareTupleQuery(QueryLanguage.SPARQL,query);
		TupleQueryResult resultBindingsets= tQuery.evaluate();
		
		return resultBindingsets;		
	}
	
	// needs revision
	public TupleQueryResult getLegitGroupPermissionTransfers(String signer, String contextToCheck){
		
		
		String query = "PREFIX import: <http://import.localhost/>\r\n" + 
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\r\n" + 
				"PREFIX trusted: <http://trusted.localhost/>\r\n" + 
				"PREFIX dgm: <http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgm.owl#>\r\n" + 
				"SELECT Distinct ?Group1 ?Group2 WHERE {\r\n" + 
				"  GRAPH <" + contextToCheck + "> \r\n" + 
				"  { ?Group1 owl:sameAs ?Group2 } UNION { ?Group2 owl:sameAs ?Group1 }\r\n" + 
				"  GRAPH trusted: \r\n" + 
				"  { <" + signer + "> ^foaf:member* / dgm:administrate ?Group1 }\r\n" + 
				"}";

		TupleQuery tQuery=repoCon.prepareTupleQuery(QueryLanguage.SPARQL,query);
		TupleQueryResult resultBindingsets= tQuery.evaluate();
		
		return resultBindingsets;		
	}
	
	public TupleQueryResult getLegitNamespaceDefinitions(String signer, String contextToCheck){
		
		
		String query = "PREFIX import: <http://import.localhost/>\r\n" + 
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\r\n" + 
				"PREFIX trusted: <http://trusted.localhost/>\r\n" + 
				"PREFIX a: <http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/abfo/a/>\r\n" + 
				"PREFIX dgm: <http://gitlab.com/FSCHOEN/RDF-Test-Data/raw/master/dgm.owl#>\r\n" + 
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" + 
				"SELECT Distinct ?s ?p ?o WHERE {\r\n" + 
				"  GRAPH <" + contextToCheck + ">\r\n" + 
				"  { \r\n" + 
				"    {VALUES ?p { rdf:type foaf:member dgm:transfersPermissionsTo dgm:administratedBy } ?s ?p ?o .}\r\n" + 
				"  }\r\n" + 
				"  GRAPH trusted: \r\n" + 
				"  { <" + signer + "> dgm:ownsNamespace ?Namespace }\r\n" + 
				"  FILTER regex(str(?s), str(?Namespace))\r\n" + 
				"}";

		TupleQuery tQuery=repoCon.prepareTupleQuery(QueryLanguage.SPARQL,query);
		TupleQueryResult resultBindingsets= tQuery.evaluate();
		
		return resultBindingsets;		
	}
	
	

	public boolean checkInheritsPermission(String personA, String personB) {
		Statement state=vf.createStatement(vf.createIRI(personA), vf.createIRI(INHERITSPERMISSIONSFROM), vf.createIRI(personB));
		return repoCon.hasStatement(state, false, vf.createIRI(LOCALtrusted));
	}

}
