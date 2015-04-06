package io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import model.MetroVertex;
import edu.uci.ics.jung.graph.Graph;

public class CSVCountReader extends HashMap<String, Integer[]> {
	
	private HashMap<String,String> mapping = new HashMap<String,String>();

	public CSVCountReader() {
		
		// mapping
		//     .put( graph vertex name   ,  name in passenger count file ); 
		mapping.put("St.John's Wood", "St. John's Wood");
		mapping.put("Heathrow Terminals 1-3", "Heathrow Terminals 123");
		mapping.put("Harrow & Wealdstone Station", "Harrow & Wealdstone");
		mapping.put("Watford Underground Station", "Watford");
		mapping.put("Hammersmith", "Hammersmith (Dis)");
		mapping.put("Hammersmith (Ham & City Line)", "Hammersmith (H&C)");
		mapping.put("Queen's Park (London)", "Queen's Park");
		mapping.put("Bank", "Bank & Monument");
		mapping.put("Monument", "Bank & Monument");
		mapping.put("Greenwich Station", "North Greenwich");
		mapping.put("Shepherd's Bush (Central Line)", "Shepherd's Bush (Cen)");
		mapping.put("Bromley-By-Bow", "Bromley-by-Bow");
		mapping.put("St James's Park", "St. James's Park");
		mapping.put("King's Cross St.Pancras", "King's Cross St. Pancras");
		mapping.put("Tooting Bec Station", "Tooting Bec");
		mapping.put("Paddington (H&C Line)", "Paddington");
		mapping.put("Royal Victoria", "Victoria");
		mapping.put("Kennington Station", "Kennington");
		mapping.put("Stratford High Street", "Stratford");
		mapping.put("Wood Green Station", "Wood Green");
		mapping.put("Borough Station", "Borough");
		mapping.put("Canary Wharf DLR Station", "Canary Wharf");
		mapping.put("Hampstead Station", "Hampstead");
		mapping.put("Richmond (London)", "Richmond");
		mapping.put("Brent Cross Station", "Brent Cross");
		mapping.put("London Bridge Station", "London Bridge");
		
		String csvFile = "resources/passengercount/london-entries-2012-week.csv";
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
	 
		try {
	 
			br = new BufferedReader(new FileReader(csvFile));
			while ((line = br.readLine()) != null) {
	 
				String[] row = line.split(cvsSplitBy);
				if(row.length == 97) {
					Integer[] times = new Integer[96];
					for(int i = 0; i < 96; i++) {
						times[i] = Integer.valueOf(row[i+1]);
					}
					this.put(row[0], times);
				} else {
					System.err.println("Found line with " + row.length + " columns.. (97 expected)");
				}
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void applyToGraph(Graph g) {
		int notfound = 0;
		for(Object v : g.getVertices()) {
			MetroVertex mv = (MetroVertex) v;
			String name = mv.getName();
			if(mapping.containsKey(name)) {
				name = mapping.get(name);
			}
			if(this.containsKey(name)) {
				mv.setPassengerCount(this.get(name));
			} else {
				//System.err.println("Passenger count missing for " + name);
				notfound++;
			}
		}
		if(notfound > 0) {
			//System.err.println("Passenger count missing for " + notfound + "/" + g.getVertices().size() + " vertices.");
		}
	}
	
	public static String[] mapTimes() {
		String times = "0200-0215,0215-0230,0230-0245,0245-0300,0300-0315,0315-0330,0330-0345,0345-0400,0400-0415,0415-0430,0430-0445,0445-0500,0500-0515,0515-0530,0530-0545,0545-0600,0600-0615,0615-0630,0630-0645,0645-0700,0700-0715,0715-0730,0730-0745,0745-0800,0800-0815,0815-0830,0830-0845,0845-0900,0900-0915,0915-0930,0930-0945,0945-1000,1000-1015,1015-1030,1030-1045,1045-1100,1100-1115,1115-1130,1130-1145,1145-1200,1200-1215,1215-1230,1230-1245,1245-1300,1300-1315,1315-1330,1330-1345,1345-1400,1400-1415,1415-1430,1430-1445,1445-1500,1500-1515,1515-1530,1530-1545,1545-1600,1600-1615,1615-1630,1630-1645,1645-1700,1700-1715,1715-1730,1730-1745,1745-1800,1800-1815,1815-1830,1830-1845,1845-1900,1900-1915,1915-1930,1930-1945,1945-2000,2000-2015,2015-2030,2030-2045,2045-2100,2100-2115,2115-2130,2130-2145,2145-2200,2200-2215,2215-2230,2230-2245,2245-2300,2300-2315,2315-2330,2330-2345,2345-2400,0000-0015,0015-0030,0030-0045,0045-0100,0100-0115,0115-0130,0130-0145,0145-0200";
		return times.split(",");
	}
	
}
