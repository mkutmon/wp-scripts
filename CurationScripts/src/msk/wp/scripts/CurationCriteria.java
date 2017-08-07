package msk.wp.scripts;


import java.net.URL;

import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.Comment;
import org.pathvisio.wikipathways.webservice.WSCurationTag;
import org.pathvisio.wikipathways.webservice.WSPathway;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;
import org.wikipathways.client.WikiPathwaysClient;

public class CurationCriteria {

	public static void main(String[] args) throws Exception {
		WikiPathwaysClient client = new WikiPathwaysClient(new URL("http://webservice.wikipathways.org"));
		System.out.println("[INFO:] Get analysis collection pathways");
		WSCurationTag [] tags = client.getCurationTagsByName("Curation:AnalysisCollection");
//		WSCurationTag [] tags = client.getCurationTagsByName("Curation:Tutorial");
		
		String output = "";
		for(WSCurationTag tag : tags) {
			WSPathwayInfo info = tag.getPathway();
			System.out.println("[INFO:] Parse " + info.getId());
			WSPathway pwy = client.getPathway(info.getId());
			Pathway pathway = WikiPathwaysClient.toPathway(pwy);
			
			// check description
			boolean description = false;
			for(Comment c : pathway.getMappInfo().getComments()) {
				if(c.getSource() != null && c.getSource().equals("WikiPathways-description")) {
					if(!c.getComment().isEmpty()) {
						description = true;
					}
				}
			}
			
			int countTotal = 0;
			int countMissing = 0;
			for(PathwayElement e : pathway.getDataObjects()) {
				if(e.getObjectType().equals(ObjectType.DATANODE)) {
					if(e.getDataNodeType().equals("GeneProduct") || e.getDataNodeType().equals("Protein") || e.getDataNodeType().equals("Metabolite")) {
						countTotal++;
						if(e.getXref().getId().isEmpty()) {
							countMissing++;
						}
					}
				}
			}
			boolean annotated = false;
			if(countTotal > 0) {
				double perc = (countMissing/countTotal)*100.0;
				if(perc < 20.0) {
					annotated = true;
				}
			}
			output = output + "\n" + info.getId() + "\t" + description + "\t" + countTotal + "\t" + countMissing + "\t" + annotated;
		}

		System.out.println(output);
	}

}
