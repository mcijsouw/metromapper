package io;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import model.MetroEdge;

import org.xml.sax.SAXException;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.decorators.StringLabeller;

public class SydneyTravelTimes {

	private ArrayList<SydneyTriple> triples = new ArrayList<SydneyTriple>();

	public SydneyTravelTimes() {

		this.triples.add(new SydneyTriple("Berowra", "Mt Kuring-gai", 4));
		this.triples.add(new SydneyTriple("Mt Kuring-gai", "Mt Colah", 3));
		this.triples.add(new SydneyTriple("Mt Colah", "Asquith", 3));
		this.triples.add(new SydneyTriple("Asquith", "Hornsby", 3));
		this.triples.add(new SydneyTriple("Hornsby", "Waitara", 3));
		this.triples.add(new SydneyTriple("Waitara", "Wahroonga", 2));
		this.triples.add(new SydneyTriple("Wahroonga", "Warrawee", 2));
		this.triples.add(new SydneyTriple("Warrawee", "Turramurra", 2));
		this.triples.add(new SydneyTriple("Turramurra", "Pymble", 3));
		this.triples.add(new SydneyTriple("Pymble", "Gordon", 2));
		this.triples.add(new SydneyTriple("Gordon", "Killara", 2));
		this.triples.add(new SydneyTriple("Killara", "Lindfield", 2));
		this.triples.add(new SydneyTriple("Lindfield", "Roseville", 3));
		this.triples.add(new SydneyTriple("Roseville", "Chatswood", 3));
		this.triples.add(new SydneyTriple("Chatswood", "Artarmon", 2));
		this.triples.add(new SydneyTriple("Artarmon", "St Leonards", 2));
		this.triples.add(new SydneyTriple("St Leonards", "Wollstonecraft", 3));
		this.triples.add(new SydneyTriple("Wollstonecraft", "Waverton", 2));
		this.triples.add(new SydneyTriple("Waverton", "North Sydney", 4));
		this.triples.add(new SydneyTriple("North Sydney", "Milsons Point", 2));
		this.triples.add(new SydneyTriple("Milsons Point", "Wynyard", 4));
		this.triples.add(new SydneyTriple("Wynyard", "Town Hall", 4));
		this.triples.add(new SydneyTriple("Town Hall", "Central", 4));
		this.triples.add(new SydneyTriple("Central", "Redfern", 2));
		this.triples.add(new SydneyTriple("Redfern", "Burwood", 9));
		this.triples.add(new SydneyTriple("Burwood", "Strathfield", 3));
		this.triples.add(new SydneyTriple("Strathfield", "Lidcombe", 5));
		this.triples.add(new SydneyTriple("Lidcombe", "Auburn", 3));
		this.triples.add(new SydneyTriple("Auburn", "Clyde", 3));
		this.triples.add(new SydneyTriple("Clyde", "Granville", 2));
		this.triples.add(new SydneyTriple("Granville", "Harris Park", 3));
		this.triples.add(new SydneyTriple("Harris Park", "Parramatta ", 2));
		this.triples.add(new SydneyTriple("Hornsby", "Normanhurst", 2));
		this.triples.add(new SydneyTriple("Normanhurst", "Thornleigh", 3));
		this.triples.add(new SydneyTriple("Thornleigh", "Pennant Hills", 2));
		this.triples.add(new SydneyTriple("Pennant Hills", "Beecroft", 3));
		this.triples.add(new SydneyTriple("Beecroft", "Cheltenham", 2));
		this.triples.add(new SydneyTriple("Cheltenham", "Epping", 3));
		this.triples.add(new SydneyTriple("Epping", "Eastwood", 3));
		this.triples.add(new SydneyTriple("Eastwood", "Denistone", 2));
		this.triples.add(new SydneyTriple("Denistone", "West Ryde", 2));
		this.triples.add(new SydneyTriple("West Ryde", "Meadowbank", 2));
		this.triples.add(new SydneyTriple("Meadowbank", "Rhodes", 3));
		this.triples.add(new SydneyTriple("Rhodes", "Concord West", 2));
		this.triples.add(new SydneyTriple("Concord West", "North Strathfield", 2));
		this.triples.add(new SydneyTriple("North Strathfield", "Strathfield", 6));
		this.triples.add(new SydneyTriple("Strathfield", "Burwood", 2));
		this.triples.add(new SydneyTriple("Burwood", "Redfern", 9));
		this.triples.add(new SydneyTriple("Redfern", "Central", 4));
		this.triples.add(new SydneyTriple("Central", "Town Hall", 3));
		this.triples.add(new SydneyTriple("Town Hall", "Wynyard", 3));
		this.triples.add(new SydneyTriple("Wynyard", "Milsons Point", 3));
		this.triples.add(new SydneyTriple("Milsons Point", "North Sydney", 4));
		this.triples.add(new SydneyTriple("North Sydney", "Waverton", 2));
		this.triples.add(new SydneyTriple("Waverton", "Wollstonecraft", 2));
		this.triples.add(new SydneyTriple("Wollstonecraft", "St Leonards", 3));
		this.triples.add(new SydneyTriple("St Leonards", "Artarmon", 2));
		this.triples.add(new SydneyTriple("Artarmon", "Chatswood ", 4));
		this.triples.add(new SydneyTriple("Emu Plains", "Penrith", 4));
		this.triples.add(new SydneyTriple("Penrith", "Kingswood", 3));
		this.triples.add(new SydneyTriple("Kingswood", "Werrington", 4));
		this.triples.add(new SydneyTriple("Werrington", "St Marys", 3));
		this.triples.add(new SydneyTriple("St Marys", "Mt Druitt", 4));
		this.triples.add(new SydneyTriple("Mt Druitt", "Rooty Hill", 3));
		this.triples.add(new SydneyTriple("Rooty Hill", "Doonside", 3));
		this.triples.add(new SydneyTriple("Doonside", "Blacktown", 3));
		this.triples.add(new SydneyTriple("Richmond", "East Richmond", 2));
		this.triples.add(new SydneyTriple("East Richmond", "Clarendon", 4));
		this.triples.add(new SydneyTriple("Clarendon", "Windsor", 3));
		this.triples.add(new SydneyTriple("Windsor", "Mulgrave", 3));
		this.triples.add(new SydneyTriple("Mulgrave", "Vineyard", 3));
		this.triples.add(new SydneyTriple("Vineyard", "Riverstone", 8));
		this.triples.add(new SydneyTriple("Riverstone", "Schofields", 4));
		this.triples.add(new SydneyTriple("Schofields", "Quakers Hill", 3));
		this.triples.add(new SydneyTriple("Quakers Hill", "Marayong", 3));
		this.triples.add(new SydneyTriple("Marayong", "Blacktown", 5));
		this.triples.add(new SydneyTriple("Blacktown", "Seven Hills", 4));
		this.triples.add(new SydneyTriple("Seven Hills", "Toongabbie", 3));
		this.triples.add(new SydneyTriple("Toongabbie", "Pendle Hill", 2));
		this.triples.add(new SydneyTriple("Pendle Hill", "Wentworthville", 3));
		this.triples.add(new SydneyTriple("Wentworthville", "Westmead", 3));
		this.triples.add(new SydneyTriple("Westmead", "Parramatta", 4));
		this.triples.add(new SydneyTriple("Parramatta", "Harris Park", 1));
		this.triples.add(new SydneyTriple("Harris Park", "Granville", 3));
		this.triples.add(new SydneyTriple("Granville", "Clyde", 2));
		this.triples.add(new SydneyTriple("Clyde", "Auburn", 2));
		this.triples.add(new SydneyTriple("Auburn", "Lidcombe", 3));
		this.triples.add(new SydneyTriple("Lidcombe", "Strathfield", 7));
		this.triples.add(new SydneyTriple("Strathfield", "Burwood", 2));
		this.triples.add(new SydneyTriple("Burwood", "Redfern", 9));
		this.triples.add(new SydneyTriple("Redfern", "Central", 4));
		this.triples.add(new SydneyTriple("Central", "Town Hall", 3));
		this.triples.add(new SydneyTriple("Town Hall", "Wynyard", 3));
		this.triples.add(new SydneyTriple("Wynyard", "Milsons Point", 3));
		this.triples.add(new SydneyTriple("Milsons Point", "North Sydney", 3));
		this.triples.add(new SydneyTriple("North Sydney", "Waverton", 2));
		this.triples.add(new SydneyTriple("Waverton", "Wollstonecraft", 2));
		this.triples.add(new SydneyTriple("Wollstonecraft", "St Leonards", 3));
		this.triples.add(new SydneyTriple("St Leonards", "Artarmon", 2));
		this.triples.add(new SydneyTriple("Artarmon", "Chatswood ", 4));
		this.triples.add(new SydneyTriple("Macarthur", "Campbelltown", 4));
		this.triples.add(new SydneyTriple("Campbelltown", "Leumeah", 2));
		this.triples.add(new SydneyTriple("Leumeah", "Minto", 4));
		this.triples.add(new SydneyTriple("Minto", "Ingleburn", 3));
		this.triples.add(new SydneyTriple("Ingleburn", "Macquarie Fields", 3));
		this.triples.add(new SydneyTriple("Macquarie Fields", "Glenfield", 3));
		this.triples.add(new SydneyTriple("Glenfield", "Holsworthy", 5));
		this.triples.add(new SydneyTriple("Holsworthy", "East Hills", 3));
		this.triples.add(new SydneyTriple("East Hills", "Panania", 2));
		this.triples.add(new SydneyTriple("Panania", "Revesby", 3));
		this.triples.add(new SydneyTriple("Revesby", "Padstow", 3));
		this.triples.add(new SydneyTriple("Padstow", "Riverwood", 2));
		this.triples.add(new SydneyTriple("Riverwood", "Narwee", 3));
		this.triples.add(new SydneyTriple("Narwee", "Beverly Hills", 2));
		this.triples.add(new SydneyTriple("Beverly Hills", "Kingsgrove", 2));
		this.triples.add(new SydneyTriple("Kingsgrove", "Bexley North", 3));
		this.triples.add(new SydneyTriple("Bexley North", "Bardwell Park", 2));
		this.triples.add(new SydneyTriple("Bardwell Park", "Turrella", 2));
		this.triples.add(new SydneyTriple("Turrella", "Wolli Creek", 2));
		this.triples.add(new SydneyTriple("Wolli Creek", "International Airport", 2));
		this.triples.add(new SydneyTriple("International Airport", "Domestic Airport", 3));
		this.triples.add(new SydneyTriple("Domestic Airport", "Mascot", 2));
		this.triples.add(new SydneyTriple("Mascot", "Green Square", 3));
		this.triples.add(new SydneyTriple("Green Square", "Sydenham", 2));
		this.triples.add(new SydneyTriple("Sydenham", "Redfern", 6));
		this.triples.add(new SydneyTriple("Redfern", "Central", 4));
		this.triples.add(new SydneyTriple("Central", "Museum", 2));
		this.triples.add(new SydneyTriple("Museum", "St James", 2));
		this.triples.add(new SydneyTriple("St James", "Circular Quay", 4));
		this.triples.add(new SydneyTriple("Circular Quay", "Wynyard", 2));
		this.triples.add(new SydneyTriple("Wynyard", "Town Hall ", 2));
		this.triples.add(new SydneyTriple("Glenfield", "Casula", 3));
		this.triples.add(new SydneyTriple("Casula", "Liverpool", 5));
		this.triples.add(new SydneyTriple("Liverpool", "Warwick Farm", 2));
		this.triples.add(new SydneyTriple("Warwick Farm", "Cabramatta", 3));
		this.triples.add(new SydneyTriple("Cabramatta", "Canley Vale", 2));
		this.triples.add(new SydneyTriple("Canley Vale", "Fairfield", 3));
		this.triples.add(new SydneyTriple("Fairfield", "Yennora", 2));
		this.triples.add(new SydneyTriple("Yennora", "Guildford", 3));
		this.triples.add(new SydneyTriple("Guildford", "Merrylands", 3));
		this.triples.add(new SydneyTriple("Merrylands", "Granville", 4));
		this.triples.add(new SydneyTriple("Granville", "Clyde", 1));
		this.triples.add(new SydneyTriple("Clyde", "Auburn", 2));
		this.triples.add(new SydneyTriple("Auburn", "Lidcombe", 3));
		this.triples.add(new SydneyTriple("Lidcombe", "Flemington", 4));
		this.triples.add(new SydneyTriple("Flemington", "Homebush", 4));
		this.triples.add(new SydneyTriple("Homebush", "Strathfield", 3));
		this.triples.add(new SydneyTriple("Strathfield", "Burwood", 2));
		this.triples.add(new SydneyTriple("Burwood", "Croydon", 2));
		this.triples.add(new SydneyTriple("Croydon", "Ashfield", 2));
		this.triples.add(new SydneyTriple("Ashfield", "Summer Hill", 2));
		this.triples.add(new SydneyTriple("Summer Hill", "Lewisham", 2));
		this.triples.add(new SydneyTriple("Lewisham", "Petersham", 2));
		this.triples.add(new SydneyTriple("Petersham", "Stanmore", 2));
		this.triples.add(new SydneyTriple("Stanmore", "Newtown", 2));
		this.triples.add(new SydneyTriple("Newtown", "Macdonaldtown", 2));
		this.triples.add(new SydneyTriple("Macdonaldtown", "Redfern", 2));
		this.triples.add(new SydneyTriple("Redfern", "Central", 4));
		this.triples.add(new SydneyTriple("Central", "Town Hall", 3));
		this.triples.add(new SydneyTriple("Town Hall", "Wynyard", 3));
		this.triples.add(new SydneyTriple("Wynyard", "Circular Quay", 3));
		this.triples.add(new SydneyTriple("Circular Quay", "St James", 3));
		this.triples.add(new SydneyTriple("St James", "Museum ", 2));
		this.triples.add(new SydneyTriple("Liverpool", "Warwick Farm", 3));
		this.triples.add(new SydneyTriple("Warwick Farm", "Cabramatta", 3));
		this.triples.add(new SydneyTriple("Cabramatta", "Carramar", 3));
		this.triples.add(new SydneyTriple("Carramar", "Villawood", 2));
		this.triples.add(new SydneyTriple("Villawood", "Leightonfield", 2));
		this.triples.add(new SydneyTriple("Leightonfield", "Chester Hill", 2));
		this.triples.add(new SydneyTriple("Chester Hill", "Sefton", 3));
		this.triples.add(new SydneyTriple("Birrong", "Sefton", 4));
		this.triples.add(new SydneyTriple("Lidcombe", "Berala", 3));
		this.triples.add(new SydneyTriple("Berala", "Regents Park", 4));
		this.triples.add(new SydneyTriple("Regents Park", "Birrong", 4));
		this.triples.add(new SydneyTriple("Birrong", "Yagoona", 2));
		this.triples.add(new SydneyTriple("Yagoona", "Bankstown", 4));
		this.triples.add(new SydneyTriple("Bankstown", "Punchbowl", 3));
		this.triples.add(new SydneyTriple("Punchbowl", "Wiley Park", 2));
		this.triples.add(new SydneyTriple("Wiley Park", "Lakemba", 2));
		this.triples.add(new SydneyTriple("Lakemba", "Belmore", 2));
		this.triples.add(new SydneyTriple("Belmore", "Campsie", 2));
		this.triples.add(new SydneyTriple("Campsie", "Canterbury", 3));
		this.triples.add(new SydneyTriple("Canterbury", "Hurlstone Park", 2));
		this.triples.add(new SydneyTriple("Hurlstone Park", "Dulwich Hill", 2));
		this.triples.add(new SydneyTriple("Dulwich Hill", "Marrickville", 3));
		this.triples.add(new SydneyTriple("Marrickville", "Sydenham", 4));
		this.triples.add(new SydneyTriple("Sydenham", "St Peters", 2));
		this.triples.add(new SydneyTriple("St Peters", "Erskineville", 2));
		this.triples.add(new SydneyTriple("Redfern", "Erskineville", 3));
		this.triples.add(new SydneyTriple("Waterfall", "Heathcote", 5));
		this.triples.add(new SydneyTriple("Heathcote", "Engadine", 3));
		this.triples.add(new SydneyTriple("Engadine", "Loftus", 4));
		this.triples.add(new SydneyTriple("Cronulla", "Woolooware", 2));
		this.triples.add(new SydneyTriple("Woolooware", "Caringbah", 3));
		this.triples.add(new SydneyTriple("Caringbah", "Miranda", 2));
		this.triples.add(new SydneyTriple("Miranda", "Gymea", 3));
		this.triples.add(new SydneyTriple("Gymea", "Kirrawee", 2));
		this.triples.add(new SydneyTriple("Kirrawee", "Sutherland", 4));
		this.triples.add(new SydneyTriple("Sutherland", "Jannali", 2));
		this.triples.add(new SydneyTriple("Jannali", "Como", 3));
		this.triples.add(new SydneyTriple("Como", "Oatley", 4));
		this.triples.add(new SydneyTriple("Oatley", "Mortdale", 2));
		this.triples.add(new SydneyTriple("Mortdale", "Penshurst", 2));
		this.triples.add(new SydneyTriple("Penshurst", "Hurstville", 3));
		this.triples.add(new SydneyTriple("Hurstville", "Allawah", 3));
		this.triples.add(new SydneyTriple("Allawah", "Carlton", 2));
		this.triples.add(new SydneyTriple("Carlton", "Kogarah", 2));
		this.triples.add(new SydneyTriple("Kogarah", "Rockdale", 2));
		this.triples.add(new SydneyTriple("Rockdale", "Banksia", 2));
		this.triples.add(new SydneyTriple("Banksia", "Arncliffe", 2));
		this.triples.add(new SydneyTriple("Arncliffe", "Wolli Creek", 3));
		this.triples.add(new SydneyTriple("Wolli Creek", "Tempe", 1));
		this.triples.add(new SydneyTriple("Tempe", "Sydenham", 3));
		this.triples.add(new SydneyTriple("Sydenham", "Redfern", 6));
		this.triples.add(new SydneyTriple("Redfern", "Central", 3));
		this.triples.add(new SydneyTriple("Central", "Town Hall", 3));
		this.triples.add(new SydneyTriple("Central", "Green Square", 6));
		this.triples.add(new SydneyTriple("Town Hall", "Martin Place", 2));
		this.triples.add(new SydneyTriple("Martin Place", "Kings Cross", 2));
		this.triples.add(new SydneyTriple("Kings Cross", "Edgecliff", 3));
		this.triples.add(new SydneyTriple("Edgecliff", "Bondi Junction", 3));
		this.triples.add(new SydneyTriple("Campbelltown", "Leumeah", 2));
		this.triples.add(new SydneyTriple("Leumeah", "Minto", 4));
		this.triples.add(new SydneyTriple("Minto", "Ingleburn", 3));
		this.triples.add(new SydneyTriple("Ingleburn", "Macquarie Fields", 3));
		this.triples.add(new SydneyTriple("Macquarie Fields", "Glenfield", 3));
		this.triples.add(new SydneyTriple("Glenfield", "Casula", 3));
		this.triples.add(new SydneyTriple("Casula", "Liverpool", 5));
		this.triples.add(new SydneyTriple("Liverpool", "Warwick Farm", 2));
		this.triples.add(new SydneyTriple("Warwick Farm", "Cabramatta", 3));
		this.triples.add(new SydneyTriple("Cabramatta", "Canley Vale", 2));
		this.triples.add(new SydneyTriple("Canley Vale", "Fairfield", 3));
		this.triples.add(new SydneyTriple("Fairfield", "Yennora", 2));
		this.triples.add(new SydneyTriple("Yennora", "Guildford", 3));
		this.triples.add(new SydneyTriple("Guildford", "Merrylands", 3));
		this.triples.add(new SydneyTriple("Merrylands", "Harris Park", 4));
		this.triples.add(new SydneyTriple("Harris Park", "Parramatta", 3));
		this.triples.add(new SydneyTriple("Parramatta", "Westmead", 3));
		this.triples.add(new SydneyTriple("Westmead", "Wentworthville", 2));
		this.triples.add(new SydneyTriple("Wentworthville", "Pendle Hill", 2));
		this.triples.add(new SydneyTriple("Pendle Hill", "Toongabbie", 3));
		this.triples.add(new SydneyTriple("Toongabbie", "Seven Hills", 3));
		this.triples.add(new SydneyTriple("Seven Hills", "Blacktown", 5));
		this.triples.add(new SydneyTriple("Blacktown", "Marayong", 3));
		this.triples.add(new SydneyTriple("Marayong", "Quakers Hill", 3));
		this.triples.add(new SydneyTriple("Quakers Hill", "Schofields ", 3));
		this.triples.add(new SydneyTriple("Carlingford", "Telopea", 2));
		this.triples.add(new SydneyTriple("Telopea", "Dundas", 3));
		this.triples.add(new SydneyTriple("Dundas", "Rydalmere", 2));
		this.triples.add(new SydneyTriple("Rydalmere", "Camellia", 1));
		this.triples.add(new SydneyTriple("Camellia", "Rosehill", 2));
		this.triples.add(new SydneyTriple("Rosehill", "Clyde", 3));
		this.triples.add(new SydneyTriple("Clyde", "Auburn", 2));
		this.triples.add(new SydneyTriple("Auburn", "Lidcombe", 3));
		this.triples.add(new SydneyTriple("Lidcombe", "Strathfield", 7));
		this.triples.add(new SydneyTriple("Strathfield", "Burwood", 2));
		this.triples.add(new SydneyTriple("Burwood", "Redfern", 9));
		this.triples.add(new SydneyTriple("Redfern", "Central", 4));
		this.triples.add(new SydneyTriple("Central", "Town Hall", 3));
		this.triples.add(new SydneyTriple("Town Hall", "Wynyard", 3));
		this.triples.add(new SydneyTriple("Wynyard", "Milsons Point", 3));
		this.triples.add(new SydneyTriple("Milsons Point", "North Sydney ", 3));
		this.triples.add(new SydneyTriple("Lidcombe", "Olympic Park", 5));
		
		try {
			this.process();
		} catch (SAXException | ParserConfigurationException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean process() throws SAXException, ParserConfigurationException, IOException {
			
		GraphMLReader reader = new GraphMLReader();
		Graph g = reader.loadGraph("resources/graphml/Sydney");
		
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
					for(SydneyTriple t : this.triples) {
						if( (t.from.equals(edge.getFirst().getName()) && t.to.equals(edge.getSecond().getName())) || (t.to.equals(edge.getFirst().getName()) && t.from.equals(edge.getSecond().getName())) ) {
							time = t.time * 60;
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
		
		SydneyTravelTimes vtt = new SydneyTravelTimes();
		
	}

}

class SydneyTriple {
	
	public String from;
	public String to;
	public int time;
	
	public SydneyTriple(String from, String to, int time) {
		this.from = from;
		this.to = to;
		this.time = time;
	}
	
	public String toString() {
		return "{" + this.from + "," + this.to + "," + this.time + "}";
	}
	
}