package io;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import model.MetroEdge;

import org.xml.sax.SAXException;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.decorators.StringLabeller;

public class ViennaTravelTimes {

	private ArrayList<Triple> triples = new ArrayList<Triple>();

	public ViennaTravelTimes() {

		this.triples.add(new Triple("Leopoldau", "Groﬂfeldsiedlung", 120));
		this.triples.add(new Triple("Groﬂfeldsiedlung", "Aderklaaer Straﬂe", 60));
		this.triples.add(new Triple("Aderklaaer Straﬂe", "Rennbahnweg", 60));
		this.triples.add(new Triple("Rennbahnweg", "Kagraner Platz", 120));
		this.triples.add(new Triple("Kagraner Platz", "Kagran", 120));
		this.triples.add(new Triple("Kagran", "Alte Donau", 60));
		this.triples.add(new Triple("Alte Donau", "VIC Kaiserm¸hlen", 120));
		this.triples.add(new Triple("VIC Kaiserm¸hlen", "Donauinsel", 60));
		this.triples.add(new Triple("Donauinsel", "Vorgartenstraﬂe", 120));
		this.triples.add(new Triple("Vorgartenstraﬂe", "Praterstern", 60));
		this.triples.add(new Triple("Praterstern", "Nestroyplatz", 60));
		this.triples.add(new Triple("Nestroyplatz", "Schwedenplatz", 60));
		this.triples.add(new Triple("Schwedenplatz", "Stephansplatz", 60));
		this.triples.add(new Triple("Stephansplatz", "Karlsplatz", 120));
		this.triples.add(new Triple("Karlsplatz", "Taubstummengasse", 120));
		this.triples.add(new Triple("Taubstummengasse", "S¸dtiroler Platz", 60));
		this.triples.add(new Triple("S¸dtiroler Platz", "Keplerplatz", 120));
		this.triples.add(new Triple("Keplerplatz", "Reumannplatz", 60));
		this.triples.add(new Triple("Aspernstraﬂe", "Donauspital", 60));
		this.triples.add(new Triple("Donauspital", "Hardeggasse", 120));
		this.triples.add(new Triple("Hardeggasse", "Stadlau Bahnhst", 60));
		this.triples.add(new Triple("Stadlau Bahnhst", "Donaustadtbr¸cke", 60));
		this.triples.add(new Triple("Donaustadtbr¸cke", "Donaumarina", 120));
		this.triples.add(new Triple("Donaumarina", "Stadion", 60));
		this.triples.add(new Triple("Stadion", "Trabrennstraﬂe", 120));
		this.triples.add(new Triple("Trabrennstraﬂe", "Messe", 60));
		this.triples.add(new Triple("Messe", "Praterstern", 120));
		this.triples.add(new Triple("Praterstern", "Taborstraﬂe", 120));
		this.triples.add(new Triple("Taborstraﬂe", "Schottenring", 60));
		this.triples.add(new Triple("Schottenring", "Schottentor", 60));
		this.triples.add(new Triple("Schottentor", "Rathaus", 120));
		this.triples.add(new Triple("Rathaus", "Volkstheater", 120));
		this.triples.add(new Triple("Volkstheater", "Museumsquartier", 60));
		this.triples.add(new Triple("Museumsquartier", "Karlsplatz", 120));
		this.triples.add(new Triple("Ottakring", "Kendlerstraﬂe", 60));
		this.triples.add(new Triple("Kendlerstraﬂe", "H¸tteldorfer Straﬂe", 60));
		this.triples.add(new Triple("H¸tteldorfer Straﬂe", "Johnstraﬂe", 60));
		this.triples.add(new Triple("Johnstraﬂe", "Schweglerstraﬂe", 120));
		this.triples.add(new Triple("Schweglerstraﬂe", "Westbahnhof", 60));
		this.triples.add(new Triple("Westbahnhof", "Zieglergasse", 60));
		this.triples.add(new Triple("Zieglergasse", "Neubaugasse", 60));
		this.triples.add(new Triple("Neubaugasse", "Volkstheater", 120));
		this.triples.add(new Triple("Volkstheater", "Herrengasse", 60));
		this.triples.add(new Triple("Herrengasse", "Stephansplatz", 120));
		this.triples.add(new Triple("Stephansplatz", "Stubentor", 60));
		this.triples.add(new Triple("Stubentor", "Landstraﬂe", 60));
		this.triples.add(new Triple("Landstraﬂe", "Rochusgasse", 120));
		this.triples.add(new Triple("Rochusgasse", "Kardinal-Nagel-Platz", 60));
		this.triples.add(new Triple("Kardinal-Nagel-Platz", "Schlachthausgasse", 60));
		this.triples.add(new Triple("Schlachthausgasse", "Erdberg", 60));
		this.triples.add(new Triple("Erdberg", "Gasometer", 120));
		this.triples.add(new Triple("Gasometer", "Zippererstraﬂe", 60));
		this.triples.add(new Triple("Zippererstraﬂe", "Enkplatz", 60));
		this.triples.add(new Triple("Enkplatz", "Simmering", 60));
		this.triples.add(new Triple("Heiligenstadt", "Spittelau", 120));
		this.triples.add(new Triple("Spittelau", "Friedensbr¸cke", 60));
		this.triples.add(new Triple("Friedensbr¸cke", "Roﬂauer L‰nde", 60));
		this.triples.add(new Triple("Roﬂauer L‰nde", "Schottenring", 120));
		this.triples.add(new Triple("Schottenring", "Schwedenplatz", 60));
		this.triples.add(new Triple("Schwedenplatz", "Landstraﬂe", 120));
		this.triples.add(new Triple("Landstraﬂe", "Stadtpark", 60));
		this.triples.add(new Triple("Stadtpark", "Karlsplatz", 120));
		this.triples.add(new Triple("Karlsplatz", "Kettenbr¸ckengasse", 120));
		this.triples.add(new Triple("Kettenbr¸ckengasse", "Pilgramgasse", 60));
		this.triples.add(new Triple("Pilgramgasse", "Margareteng¸rtel", 120));
		this.triples.add(new Triple("Margareteng¸rtel", "L‰ngenfeldgasse", 120));
		this.triples.add(new Triple("L‰ngenfeldgasse", "Meidling Hauptstraﬂe", 60));
		this.triples.add(new Triple("Meidling Hauptstraﬂe", "Schˆnbrunn", 60));
		this.triples.add(new Triple("Schˆnbrunn", "Hietzing", 120));
		this.triples.add(new Triple("Hietzing", "Braunschweiggasse", 60));
		this.triples.add(new Triple("Braunschweiggasse", "Unter St. Veit", 120));
		this.triples.add(new Triple("Unter St. Veit", "Ober St. Veit", 60));
		this.triples.add(new Triple("Ober St. Veit", "H¸tteldorf", 120));
		this.triples.add(new Triple("Floridsdorf", "Neue Donau", 120));
		this.triples.add(new Triple("Neue Donau", "Handelskai", 60));
		this.triples.add(new Triple("Handelskai", "Dresdner Straﬂe", 60));
		this.triples.add(new Triple("Dresdner Straﬂe", "J‰gerstraﬂe", 60));
		this.triples.add(new Triple("J‰gerstraﬂe", "Spittelau", 60));
		this.triples.add(new Triple("Spittelau", "Nuﬂdorfer Straﬂe", 120));
		this.triples.add(new Triple("Nuﬂdorfer Straﬂe", "W‰hringer Straﬂe Volksoper", 60));
		this.triples.add(new Triple("W‰hringer Straﬂe Volksoper", "Michelbeuern AKH", 120));
		this.triples.add(new Triple("Michelbeuern AKH", "Alser Straﬂe", 60));
		this.triples.add(new Triple("Alser Straﬂe", "Josefst‰dter Straﬂe", 120));
		this.triples.add(new Triple("Josefst‰dter Straﬂe", "Thaliastraﬂe", 120));
		this.triples.add(new Triple("Thaliastraﬂe", "Burggasse Stadthalle", 60));
		this.triples.add(new Triple("Burggasse Stadthalle", "Westbahnhof", 120));
		this.triples.add(new Triple("Westbahnhof", "Gumpendorfer Straﬂe", 60));
		this.triples.add(new Triple("Gumpendorfer Straﬂe", "L‰ngenfeldgasse", 120));
		this.triples.add(new Triple("L‰ngenfeldgasse", "Niederhofstraﬂe", 60));
		this.triples.add(new Triple("Niederhofstraﬂe", "Philadelphiabr¸cke", 120));
		this.triples.add(new Triple("Philadelphiabr¸cke", "Tscherttegasse", 120));
		this.triples.add(new Triple("Tscherttegasse", "Am Schˆpfwerk", 60));
		this.triples.add(new Triple("Am Schˆpfwerk", "Alterlaa", 180));
		this.triples.add(new Triple("Alterlaa", "Erlaaer Straﬂe", 120));
		this.triples.add(new Triple("Erlaaer Straﬂe", "Perfektastraﬂe", 60));
		this.triples.add(new Triple("Perfektastraﬂe", "Siebenhirten", 60));
		
		try {
			this.process();
		} catch (SAXException | ParserConfigurationException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean process() throws SAXException, ParserConfigurationException, IOException {
			
		GraphMLReader reader = new GraphMLReader();
		Graph g = reader.loadGraph("resources/graphml/Vienna");
		
		String output = "";
		
		int edgeCounter = 0;
		int error = 0;
		for(Object e : g.getEdges()) {
			MetroEdge edge = (MetroEdge) e;
			
			Boolean[] lineArray = (Boolean[]) edge.getUserDatum("lineArray");
			StringLabeller VertexIDs = StringLabeller.getLabeller(g);
			String labelFirst = VertexIDs.getLabel(edge.getFirst());
			String labelSecond = VertexIDs.getLabel(edge.getSecond());
			output += "<edge id=\"e" + edgeCounter + "\" source=\"" + labelFirst + "\" target=\"" + labelSecond + "\">\r\n";
			int i = 0;
			for(Boolean b : lineArray) {
				if(b == true) {
					int time = Integer.MAX_VALUE;
					boolean timeFound = false;
					for(Triple t : this.triples) {
						if( (t.from.equals(edge.getFirst().getName()) && t.to.equals(edge.getSecond().getName())) || (t.to.equals(edge.getFirst().getName()) && t.from.equals(edge.getSecond().getName())) ) {
							time = t.time;
							timeFound = true;
							break;
						}
					}
					if(!timeFound) {
						System.err.println("No time found for edge " + edge.getFirst().getName() + "~" + edge.getSecond().getName() + " in line l" + i);
						error++;
					} else {
						//System.out.println("Time found for edge " + edge.getFirst().getName() + "~" + edge.getSecond().getName() + ": " + time + " seconds");
					}
					output += "\t<data key=\"l" + i + "\">true</data>\r\n";
					output += "\t<data key=\"time\" line=\"l" + i + "\">" + time + "</data>\r\n";
				}
				i++;
			}
			output += "</edge>\r\n";
			edgeCounter++;
		}
		System.err.println(error + " errors");
		
		System.out.println(output);
		
		return true;
	}
	
	public static void main(String argv[]) {
		
		ViennaTravelTimes vtt = new ViennaTravelTimes();
		
	}

}

class Triple {
	
	public String from;
	public String to;
	public int time;
	
	public Triple(String from, String to, int time) {
		this.from = from;
		this.to = to;
		this.time = time;
	}
	
	public String toString() {
		return "{" + this.from + "," + this.to + "," + this.time + "}";
	}
	
}