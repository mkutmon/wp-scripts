package org.wikipathways;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

import org.bridgedb.BridgeDb;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.Xref;
import org.bridgedb.XrefIterator;

/**
 * calculates the number of unqiue genes for reactome, kegg and wp
 * additionally checks how many of those genes are protein coding
 * and calculates the total number of unique genes in all databases
 * @author mkutmon
 */
public class Stats {

	public static void main(String[] args) throws Exception {
		File bridgedb = new File("/home/martina/Data/BridgeDb/Hs_Derby_20130701.bridge");
		Class.forName("org.bridgedb.rdb.IDMapperRdb");  
		IDMapper mapper = BridgeDb.connect("idmapper-pgdb:" + bridgedb.getAbsolutePath());
		
		Set<String> genesTotal = getTotalGenes(mapper, false);
		Set<String> genesProtein = getTotalGenes(mapper, true);
		Set<String> genesReactomeTotal = getGenesReactome(mapper, false);
		Set<String> genesReactomeProtein = getGenesReactome(mapper, true);
		Set<String> genesKEGGTotal = getGenesKEGG(mapper, false);
		Set<String> genesKEGGProtein = getGenesKEGG(mapper, true);
		Set<String> genesWPTotal = getGenesWikiPathways(mapper, false);
		Set<String> genesWPProtein = getGenesWikiPathways(mapper, true);
		
		System.out.println("Total Ensembl\t" + genesTotal.size());
		System.out.println("Total Ensembl Protein Coding\t" + genesProtein.size());
		System.out.println("Unique Reactome\t" + genesReactomeTotal.size());
		System.out.println("Unique Reactome Protein Coding\t" + genesReactomeProtein.size());
		System.out.println("Unique KEGG\t" + genesKEGGTotal.size());
		System.out.println("Unique KEGG Protein Coding\t" + genesKEGGProtein.size());
		System.out.println("Unique WP\t" + genesWPTotal.size());
		System.out.println("Unique WP Protein Coding\t" + genesWPProtein.size());
		
		Set<String> uniqueOverlap = new HashSet<String>();
		for(String s : genesReactomeTotal) {
			if(!uniqueOverlap.contains(s)) uniqueOverlap.add(s);
		}
		for(String s : genesKEGGTotal) {
			if(!uniqueOverlap.contains(s)) uniqueOverlap.add(s);
		}
		for(String s : genesWPTotal) {
			if(!uniqueOverlap.contains(s)) uniqueOverlap.add(s);
		}
		
		System.out.println("Unique all databases\t" + uniqueOverlap.size());
		
		Set<String> uniqueOverlapProtein = new HashSet<String>();
		for(String s : genesReactomeProtein) {
			if(!uniqueOverlapProtein.contains(s)) uniqueOverlapProtein.add(s);
		}
		for(String s : genesKEGGProtein) {
			if(!uniqueOverlapProtein.contains(s)) uniqueOverlapProtein.add(s);
		}
		for(String s : genesWPProtein) {
			if(!uniqueOverlapProtein.contains(s)) uniqueOverlapProtein.add(s);
		}
		
		System.out.println("Unique all databases Protein Coding\t" + uniqueOverlapProtein.size());
	}
	
	/**
	 * retrieve all Ensembl identifiers from BridgeDb
	 */
	private static Set<String> getTotalGenes(IDMapper mapper, boolean onlyProteinCoding) throws Exception {
		Set<String> uniqueGenes = new HashSet<String>();
		if(mapper instanceof XrefIterator) {
			for (Xref x : ((XrefIterator) mapper).getIterator()) {
				if(x.getDataSource().getSystemCode().equals("En")) {
					if(!onlyProteinCoding) {
						if(!uniqueGenes.contains(x.getId())) {
							uniqueGenes.add(x.getId());
						}
					} else {
						if(hasUniProtMapping(x, mapper)) {
							if(!uniqueGenes.contains(x.getId())) {
								uniqueGenes.add(x.getId());
							}
						}
					}
				}
			}
		}
		return uniqueGenes;
	}
	
	private static boolean hasUniProtMapping(Xref xref, IDMapper mapper) throws Exception {
		Set<Xref> res = mapper.mapID(xref, DataSource.getBySystemCode("S"));
		if(res.size() > 0) {
			return true;
		}
		return false;
	}

	/**
	 * download gene list from KEGG
	 * map entrez gene to ensembl
	 */
	private static Set<String> getGenesKEGG(IDMapper mapper, boolean onlyProteinCoding) throws Exception {
		URL wp = new URL("http://rest.kegg.jp/link/hsa/pathway");
		URLConnection con = wp.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		Set<String> mapped = new HashSet<String>();
		Set<String> uniqueGenes = new HashSet<String>();
		
		while ((line = reader.readLine()) != null) {
			String [] buffer = line.split("\t");
			String entrez = buffer[1].substring(4);
			if(!mapped.contains(entrez)) {
				// map id from UniProt to Ensembl
				Xref xref = new Xref(entrez, DataSource.getBySystemCode("L"));
				Set<Xref> res = mapper.mapID(xref, DataSource.getBySystemCode("En"));
				for(Xref x : res) {
					if(!onlyProteinCoding) {
						uniqueGenes.add(x.getId());
					} else {
						if(hasUniProtMapping(x, mapper)) {
							uniqueGenes.add(x.getId());
						}
					}
				}
				mapped.add(entrez);
			}
		}
		
		return uniqueGenes;
	}
	
	/**
	 * download gmt file from WikiPathways and filter
	 * all human genes
	 * map entrez gene to ensembl
	 */
	private static Set<String> getGenesWikiPathways(IDMapper mapper, boolean onlyProteinCoding) throws Exception {
		URL wp = new URL("http://pathvisio.org/data/bots/gmt/wikipathways.gmt");
		URLConnection con = wp.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String line;
		Set<String> mapped = new HashSet<String>();
		Set<String> uniqueGenes = new HashSet<String>();
		while ((line = reader.readLine()) != null) {
			String [] buffer = line.split("\t");
			if(buffer[0].contains("(Homo sapiens)")) {
				for(int i = 2; i < buffer.length; i++) {
					String entrez = buffer[i];
					if(!mapped.contains(entrez)) {
						// map id from UniProt to Ensembl
						Xref xref = new Xref(entrez, DataSource.getBySystemCode("L"));
						Set<Xref> res = mapper.mapID(xref, DataSource.getBySystemCode("En"));
						for(Xref x : res) {
							if(!onlyProteinCoding) {
								uniqueGenes.add(x.getId());
							} else {
								if(hasUniProtMapping(x, mapper)) {
									uniqueGenes.add(x.getId());
								}
							}
						}
						mapped.add(entrez);
					}
				}
			}
		}
		reader.close();		
		return uniqueGenes;
	}

	/**
	 * download uniprot id list from Reactome
	 * map uniprot ids to ensembl
	 */
	private static Set<String> getGenesReactome(IDMapper mapper, boolean onlyProteinCoding) throws Exception {
		URL react = new URL("http://www.reactome.org/download/current/uniprot_2_pathways.txt");
		URLConnection con = react.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		
		String line;
		Set<String> mapped = new HashSet<String>();
		Set<String> uniqueGenes = new HashSet<String>();
		while ((line = reader.readLine()) != null) {
			String [] buffer = line.split("\t");
			String uniprot = buffer[0];
			if(!mapped.contains(uniprot)) {
				// map id from UniProt to Ensembl
				Xref xref = new Xref(uniprot, DataSource.getBySystemCode("S"));
				Set<Xref> res = mapper.mapID(xref, DataSource.getBySystemCode("En"));
				for(Xref x : res) {
					if(!onlyProteinCoding) {
						uniqueGenes.add(x.getId());
					} else {
						if(hasUniProtMapping(x, mapper)) {
							uniqueGenes.add(x.getId());
						}
					}
				}
				mapped.add(uniprot);
			}
		}
		reader.close();
		
		return uniqueGenes;
	}

}
