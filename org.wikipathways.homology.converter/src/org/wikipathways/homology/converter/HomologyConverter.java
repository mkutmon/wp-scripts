package org.wikipathways.homology.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.bridgedb.bio.Organism;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.wikipathways.webservice.WSPathway;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;
import org.wikipathways.client.WikiPathwaysClient;

public class HomologyConverter {

	private Organism sourceSpecies;
	private Organism targetSpecies;
	
	private Map<String, String> mappingHighConf;
	private Map<String, String> mappingLowConf;
	
	public static void main(String[] args) {
		try {
			HomologyConverter converter = new HomologyConverter("Homo sapiens", "Bos taurus");
			converter.readMappingFile(new File("resources/mart_export.txt"));
			converter.startConversion(new URL("http://webservice.wikipathways.org"));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}
	
	private void startConversion(URL url) throws RemoteException, ConverterException {
		int countHigh = 0;
		int countLow = 0;
		WikiPathwaysClient client = new WikiPathwaysClient(url);
		WSPathwayInfo[] pwys = client.listPathways(sourceSpecies);
		System.out.println(pwys.length);
		for(WSPathwayInfo i : pwys) {
			WSPathway p = client.getPathway(i.getId());
//			WSPathway p = client.getPathway(i.getId(), Integer.getInteger(i.getRevision()));
			System.out.println(p.getId() + "\t" + p.getRevision());
			
			Pathway pathway = WikiPathwaysClient.toPathway(p);
			for(Xref x : pathway.getDataNodeXrefs()) {
				if(x.getDataSource().equals(DataSource.getBySystemCode("En"))) {
					if(mappingHighConf.containsKey(x.getId())) {
						countHigh++;
					} else if(mappingLowConf.containsKey(x.getId())) {
						countLow++;
					}
				}
			}
		}
		System.out.println(countHigh + "\t" + countLow);
	}

	public HomologyConverter(String sourceSpeciesName, String targetSpeciesName) throws Exception {
		sourceSpecies = verifyInputOrganism(sourceSpeciesName);
		targetSpecies = verifyInputOrganism(targetSpeciesName);
		mappingHighConf = new HashMap<String, String>();
		mappingLowConf = new HashMap<String, String>();
		if(sourceSpecies != null && targetSpecies != null) {
			System.out.println("You have selected to convert pathways from " + sourceSpecies + " to " + targetSpecies);
		} else {
			throw new Exception("Invalid organisms selected.");
		}
	}
	
	public void readMappingFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		int countNoMapping = 0;
		int countHighConfMapping = 0;
		int countLowConfMapping = 0;
		while((line = reader.readLine()) != null) {
			String [] buffer = line.split(",");
			if(buffer.length > 1) {
				String source = buffer[0];
				String target = buffer[1];
				String confidence = buffer[2];
				if(confidence.equals("1")) {
					mappingHighConf.put(source, target);
					countHighConfMapping++;
				} else {
					mappingLowConf.put(source, target);
					countLowConfMapping++;
				}
			} else {
				countNoMapping++;
			}
		}
		System.out.println("\nThe mapping file contains\n" + countHighConfMapping + " high confidence mappings,\n" + countLowConfMapping + " low confidence mappings and\n" + countNoMapping + " ids without mapping.\n");
		reader.close();
	}

	private Organism verifyInputOrganism(String organism) {
		for(String name : Organism.latinNames()) {
			if(name.equals(organism)) {
				return Organism.fromLatinName(name);
			}
		}
		return null;
	}
}
