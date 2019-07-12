import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bridgedb.DataSource;
import org.pathvisio.core.biopax.BiopaxReferenceManager;
import org.pathvisio.core.biopax.PublicationXref;
import org.pathvisio.core.data.PubMedQuery;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.GroupStyle;
import org.pathvisio.core.model.MGroup;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;

public class PathwayCreator {

	public static void main(String[] args) throws Exception {
		File dir = new File("pathways");
		File outDir = new File("pathways\\gpmlFiles\\");
		
		for(File f : dir.listFiles()) {
			if(f.getName().endsWith(".tsv")) {
				String gpml = f.getName().replace(".tsv","");
				gpml = gpml + ".gpml";
				if(!new File(outDir, gpml).exists()) {
					Pathway p = new Pathway();
					p.getMappInfo().setMapInfoName(f.getName().replace(".tsv",""));
					p.getMappInfo().setOrganism("Homo sapiens");
					String [] buffer = f.getName().split("_");
					String pmcid = buffer[0];
					addPubmed(p, pmcid);
					
					Map<String, List<Gene>> genes = new HashMap<>();
					String line;
					BufferedReader reader = new BufferedReader(new FileReader(f));
					while((line = reader.readLine()) != null) {
						String [] buffer2 = line.split("\t");
						Gene g = new Gene(buffer2[0], buffer2[1], buffer2[2], buffer2[3]);
						if(!genes.containsKey(buffer2[3])) {
							genes.put(buffer2[3], new ArrayList<Gene>());
						}
						genes.get(buffer2[3]).add(g);
					}
					reader.close();	
					
					
					int y = 65;
					int count = 0;
					int x = 65;
					for(String s : genes.keySet()) {
						if(genes.get(s).size() == 1) {
							PathwayElement pel = PathwayElement.createPathwayElement(ObjectType.DATANODE);
							pel.setDataNodeType(DataNodeType.GENEPRODUCT);
							pel.setMCenterX(x);
							pel.setMCenterY(y);
							pel.setMHeight(20);
							pel.setMWidth(80);
							pel.setMFontSize(12.0);
							pel.setTextLabel(genes.get(s).get(0).getLabel());
							pel.setElementID(genes.get(s).get(0).getId());
							pel.setDataSource(DataSource.getExistingBySystemCode(genes.get(s).get(0).getDs()));
							pel.addComment(s, "OCR");
					
							p.add(pel);
							y+=30;
							count++;
							if(count % 10 == 0) {
								x+=100;
								y = 65;
							}
						}
					}
					
					for(String s : genes.keySet()) {
						if(genes.get(s).size() > 1) {
							x+=100;
							y = 65;
							Set<PathwayElement> elements = new HashSet<>();
							{ 
								PathwayElement pel = PathwayElement.createPathwayElement(ObjectType.LABEL);
								pel.setMCenterX(x);
								pel.setMCenterY(y);
								pel.setMHeight(20);
								pel.setMWidth(80);
								pel.setMFontSize(12.0);
								pel.setTextLabel(s);
								p.add(pel);
								y+=20;
								elements.add(pel);
							}
							
							for(Gene g : genes.get(s)) {
								PathwayElement pel = PathwayElement.createPathwayElement(ObjectType.DATANODE);
								pel.setDataNodeType(DataNodeType.GENEPRODUCT);
								pel.setMCenterX(x);
								pel.setMCenterY(y);
								pel.setMHeight(20);
								pel.setMWidth(80);
								pel.setMFontSize(12.0);
								pel.setTextLabel(g.getLabel());
								pel.setElementID(g.getId());
								pel.setDataSource(DataSource.getExistingBySystemCode(g.getDs()));
								pel.addComment(s, "OCR");
								p.add(pel);
								y+=20;
								elements.add(pel);
							}
							PathwayElement group = PathwayElement.createPathwayElement(ObjectType.GROUP);
							p.add(group);
							group.setGroupStyle(GroupStyle.NONE);
							String id = group.createGroupId();

							for (PathwayElement pe : elements) {
								pe.setGroupRef(id);
							}
						}
					}

					p.writeToXml(new File(outDir, gpml), true);
				}
			}
 		}
	}
	
	private static void addPubmed(Pathway p, String pmc) throws Exception {
		String pubmed = pmc2pubmed(pmc);
		if(pubmed != null) {
			PubMedQuery pmq = new PubMedQuery(pubmed.trim());
			pmq.execute();
			PublicationXref xref = new PublicationXref();
			xref.setPubmedId(pubmed);
			xref.setTitle(pmq.getResult().getTitle());
			xref.setYear(pmq.getResult().getYear());
			xref.setSource(pmq.getResult().getSource());
			xref.setAuthors(PublicationXref.createAuthorString(pmq.getResult().getAuthors()));
			
			BiopaxReferenceManager m = p.getMappInfo().getBiopaxReferenceManager();
			m.addElementReference(xref);
			
			
		} else {
			throw new Exception("No PubMed!");
		}
	}

	private static String pmc2pubmed(String pmc) throws Exception {
		 URL url = new URL("https://www.ncbi.nlm.nih.gov/pmc/utils/idconv/v1.0/?ids=" + pmc);
	     BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

	     String inputLine;
	     while ((inputLine = in.readLine()) != null) {
	    	 Pattern pattern = Pattern.compile("pmid=\\\"([0-9]+)\\\"");
	         Matcher matcher = pattern.matcher(inputLine);
	         while (matcher.find()) {
	        	 in.close();
	        	 return matcher.group(1);
	         }
	     }
	     in.close();
	     return null;
	}
}
