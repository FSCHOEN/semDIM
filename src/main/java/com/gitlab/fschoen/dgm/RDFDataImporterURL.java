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
public class RDFDataImporterURL {

	private static final Logger log = LogManager.getLogger(RDFDataImporterURL.class);
	private URL dataPath=null;
	private String localDirectory="";
	
	public RDFDataImporterURL(URL url) {
		this.dataPath = url;
	}
	
	public RDFDataImporterURL(URL url,String localDirectory) {
		this.dataPath = url;
		this.localDirectory=localDirectory;
	}


	public RDFDataImporterURL(String tripleFilePath, String localDirectory2) {
		if(tripleFilePath.matches("^(http|https|ftp)://.*$"))
			try {
				this.dataPath=new URL(tripleFilePath);
			} catch (MalformedURLException e) {
				log.error("Malformed URL");
				e.printStackTrace();
			}
			this.localDirectory=localDirectory2;
	}

	public boolean importProfile(RepositoryConnection repoCon) {
		try {
			IRI dataIRI;
			
			ValueFactory valueFactory = repoCon.getValueFactory();
			dataIRI = valueFactory.createIRI(dataPath.toString());			
			
			if(!localDirectory.equals("")) {				
				File localFile=new File(localDirectory+dataPath.toString().substring(dataPath.toString().lastIndexOf("/")+1,dataPath.toString().length()));
				log.trace("Looking for local file "+ localFile);
				repoCon.add(localFile, null, Rio.getWriterFormatForFileName(localFile.toString()).get(), dataIRI);
				log.debug("Added " + dataIRI + " from local directory to repository");
				return true;
			} else {
				log.trace("Importing from remote location " + dataIRI);
				repoCon.add(dataPath, null, Rio.getWriterFormatForFileName(dataPath.toString()).get(), dataIRI);
				log.debug("Added " + dataIRI + " from remote location to repository");
				return true;				
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
				repoCon.add(dataPath, null, Rio.getWriterFormatForFileName(dataPath.toString()).get(), temporaryProfileIRI);
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
				repoCon.add(dataPath, null, Rio.getWriterFormatForFileName(dataPath.toString()).get(), profileIRI);
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
