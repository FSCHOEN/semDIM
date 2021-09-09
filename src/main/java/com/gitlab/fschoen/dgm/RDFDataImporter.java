package com.gitlab.fschoen.dgm;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;



/**
 * @author Falko Schönteich
 * 
 */
public class RDFDataImporter {

	private static final Logger log = LogManager.getLogger(RDFDataImporter.class);
	private String dataPath;
	private String context="";
	private String localDirectory="";
	
	
	public ImporterType importerType;
	public enum ImporterType { WEB, LOCAL, OTHER}
	
	public RDFDataImporter(String dataPath) {
		this.dataPath = dataPath;
		this.context=dataPath;
		this.importerType=determineImporterType();
	}
	
	public RDFDataImporter(String dataPath, String context) {
		this.dataPath=dataPath;
		this.context=context;
		this.importerType=determineImporterType();
	}
	
	private ImporterType determineImporterType() {
		if(dataPath.matches("^(http|https|ftp)://.*$")) {
				return ImporterType.WEB;
		} else if(dataPath.matches("^[a-zA-Z]:[/\\\\].*$")){
			return ImporterType.LOCAL;
		} else
			return ImporterType.OTHER;
			
		}	

	public boolean importProfile(RepositoryConnection repoCon) {
		try {
			
			ValueFactory valueFactory = repoCon.getValueFactory();
			IRI contextIRI = valueFactory.createIRI(context);	
			switch(importerType){
			case WEB:
				log.trace("RDF WEB Import: "+dataPath);
				repoCon.add(new URL(dataPath), null, Rio.getWriterFormatForFileName(dataPath).get(), contextIRI);			
				return true;
			case LOCAL:
				log.trace("RDF Local Import: "+dataPath);
				repoCon.add(new File(dataPath), null, Rio.getWriterFormatForFileName(dataPath).get(), contextIRI);
				return true;
			case OTHER:
				log.error("Could not import profile: " +dataPath);
				return false;
			}			
				
		} catch (MalformedURLException e) {
			log.error("MalformedURL");
			return false;
		} catch (RDFParseException e) {
			log.warn("Could not parse RDF-Data at " + dataPath	+ e.getMessage());
			return false;
		} catch (IOException e) {
			log.warn("IOException: " + dataPath + " "+ localDirectory);
			e.printStackTrace();
			return false;
		} catch (RepositoryException e) {
			log.warn("Could not connect to repository.");
			return false;
		}
		return false;
	}
	
	public boolean checkImportProfile(RepositoryConnection repoCon) {
		try {
			
			
			ValueFactory valueFactory = repoCon.getValueFactory();
			IRI profileIRI = valueFactory.createIRI(dataPath.toString());
			IRI temporaryProfileIRI = valueFactory.createIRI(dataPath.toString()+"__temporary__");
			
			log.info("Local Direct:" + localDirectory);
			if(!localDirectory.equals("")) {				
				File localFile=new File(localDirectory+dataPath.toString().substring(dataPath.toString().lastIndexOf("/")+1,dataPath.toString().length()));
				log.trace("Looking for local file "+ localFile);
				repoCon.add(localFile, null, Rio.getWriterFormatForFileName(localFile.toString()).get(), temporaryProfileIRI);
				
			} else {
				log.trace("Importing from remote location " + profileIRI);
//				repoCon.add(dataPath, null, Rio.getWriterFormatForFileName(dataPath.toString()).get(), temporaryProfileIRI);
				log.debug("Temporaryly added " + profileIRI + " to Repository");
								
			}
			
			if(!checkNamespaceValidity(temporaryProfileIRI)) {
				log.warn("Profile is not valid: " + temporaryProfileIRI );
				return false;
			}
			else
			if(!localDirectory.equals("")) {				
				File localFile=new File(localDirectory+dataPath.toString().substring(dataPath.toString().lastIndexOf("/")+1,dataPath.toString().length()));
				log.trace("Looking for local file "+ localFile);
				repoCon.add(localFile, null, Rio.getWriterFormatForFileName(localFile.toString()).get(), profileIRI);
				return true;
			} else {
				log.trace("Importing from remote location " + profileIRI);
//				repoCon.add(dataPath, null, Rio.getWriterFormatForFileName(dataPath.toString()).get(), profileIRI);
				log.debug("Added " + profileIRI + " to Repository");
				return true;				
			}
				
		} catch (MalformedURLException e) {
			log.error("MalformedURL");
			return false;
		} catch (RDFParseException e) {
			log.warn("Could not parse RDF-Data at " + dataPath	+ e.getMessage());
			return false;
		} catch (IOException e) {
			log.warn("IOException: " + dataPath);
			return false;
		} catch (RepositoryException e) {
			log.warn("Could not connect to repository.");
			return false;
		}
	}
	
	public boolean checkNamespaceValidity(IRI profileIRI) {
		return true;
	}

}
