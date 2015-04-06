package io;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import model.MetroEdge;
import model.MetroVertex;
import model.OrderedEdgeListManager;

import org.joda.time.Period;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.exceptions.ConstraintViolationException;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.event.GraphEventType;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.UserData;

public class TransportForLondonXMLReader {

	private static HashMap<String, Color> files = new HashMap<String, Color>();
	private static HashMap<String, MetroVertex> stopPoints = new HashMap<String, MetroVertex>();

	private static int periodToSeconds(Period p) {
		return p.getHours() * 3600 + p.getMinutes() * 60 + p.getSeconds();
	}
	
	public static void main(String argv[]) {
		
		files.put("tfl_1-BAK_-390106-y05", new Color(139, 69, 19));
		files.put("tfl_1-CEN_-670111-y05", new Color(255, 0, 0));
		files.put("tfl_1-CIR_-320101-y05", new Color(255, 215, 0));
		files.put("tfl_1-DIS_-1460111-y05", new Color(50, 205, 50));
		files.put("tfl_1-HAM_-320101-y05", new Color(255, 182, 193));
		files.put("tfl_1-JUB_-130111-y05", new Color(190, 190, 190));
		files.put("tfl_1-MET_-3360101-y05", new Color(199, 21, 133));
		files.put("tfl_1-NTN_-550101-y05", new Color(0, 0, 0));
		files.put("tfl_1-PIC_-530101-y05", new Color(0, 0, 128));
		files.put("tfl_1-VIC_-360101-y05", new Color(64, 160, 240));
		files.put("tfl_1-WAC_-60102-y05", new Color(0, 255, 192));
		files.put("tfl_25-DLR_-72-y05", new Color(0, 176, 144));
	  
	  
		try {

			Graph g = new UndirectedSparseGraph();
			OrderedEdgeListManager oelm = new OrderedEdgeListManager();
			g.addListener(oelm, GraphEventType.EDGE_ADDITION);
			g.addListener(oelm, GraphEventType.EDGE_REMOVAL);
			StringLabeller VertexIDs = StringLabeller.getLabeller(g);
			
			Vector lineNames = new Vector();
			Vector lineColors = new Vector();
			for (Entry<String, Color> entry : files.entrySet()) {
				lineNames.add(entry.getKey());
				lineColors.add(entry.getValue());
			}
			
			g.addUserDatum("lines", lineNames, UserData.SHARED);
			g.addUserDatum("colors", lineColors, UserData.SHARED);
			g.addUserDatum("edgeStatus", "", UserData.SHARED);
			
			int i = 0;
			for (Entry<String, Color> entry : files.entrySet()) {
				File file = new File("resources\\tfl\\" + entry.getKey() + ".xml");

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(file);
				doc.getDocumentElement().normalize();

				
				// Vertices
				NodeList list = doc.getElementsByTagName("StopPoint");
				for (int s = 0; s < list.getLength(); s++) {

					Element stopPoint = (Element) list.item(s);
					String stopType = getTextValue(stopPoint, "StopType");
					String commonName = getTextValue(stopPoint, "CommonName");
					String atcoCode = getTextValue(stopPoint, "AtcoCode");
					double easting = Double.parseDouble(getTextValue(stopPoint, "Easting"));
					double northing = Double.parseDouble(getTextValue(stopPoint, "Northing"));

					if (false == stopPoints.containsKey(atcoCode) && stopType.equals("RPL") && northing > 0 && easting > 0) {
						MetroVertex mv = (MetroVertex) VertexIDs.getVertex(commonName);
						if(mv == null) {
							mv = new MetroVertex( ((easting - 496000) / 100.0), ((northing - 168000) / 100.0) );
							mv.setName(commonName);
							mv.setLabelLength(commonName.length());
							g.addVertex(mv);
							VertexIDs.setLabel(mv, commonName);
						}
						System.out.println("Adding " + commonName + " (" + atcoCode + ")");
						stopPoints.put(atcoCode, mv);
					}
				}
				
				
				
				// Edges
				
				NodeList jpSections = doc.getElementsByTagName("JourneyPatternSections");

				for (int rs = 0; rs < jpSections.getLength(); rs++) {

					Element jpSection = (Element) jpSections.item(rs);

					NodeList timingLinks = jpSection.getElementsByTagName("JourneyPatternTimingLink");
					for (int tl = 0; tl < timingLinks.getLength(); tl++) {

						Element timingLink = (Element) timingLinks.item(tl);
						Element from = (Element) timingLink.getElementsByTagName("From").item(0);
						Element to = (Element) timingLink.getElementsByTagName("To").item(0);
						String fromRef = getTextValue(from, "StopPointRef");
						String toRef = getTextValue(to, "StopPointRef");

						if(stopPoints.containsKey(fromRef) && stopPoints.containsKey(toRef)) {
							try {
								MetroVertex source = stopPoints.get(fromRef);
								MetroVertex target = stopPoints.get(toRef);
								
								if(source.getName().equals(target.getName())) {
									System.err.println("Source equals target! (" + source.getName() + ")");
									continue;
								}
								
								MetroEdge e = null;
								for(Object o : g.getEdges()) {
									MetroEdge edge = (MetroEdge) o;
									if((edge.getFirst().equals(source) && edge.getSecond().equals(target)) || (edge.getFirst().equals(target) && edge.getSecond().equals(source))) {										
										e = edge;
										break;
									}
								}
								if(e == null) {
									e = (MetroEdge) g.addEdge(new MetroEdge(source, target));
									e.addUserDatum("lineArray", new Boolean[lineNames.size()], UserData.SHARED);
									Boolean[] lines = (Boolean[]) e.getUserDatum("lineArray");
									for (int j = 0; j < lines.length; j++) {
										lines[j] = Boolean.FALSE;
									}
								}
								
								Boolean[] lines = (Boolean[]) e.getUserDatum("lineArray");
								for (int j = 0; j < lines.length; j++) {
									if(i == j) {
										lines[j] = Boolean.TRUE;
										e.increaseMultiplicity();
										break;
									}
								}

								// Travel time
								String duration = getTextValue(timingLink, "RunTime");
								Period p = new Period(duration);
								int seconds = periodToSeconds(p);
								e.setTime("l" + i, seconds);
								
								System.out.println(source.getName() + " -> " + target.getName() + " (time: " + seconds + "s)");
								
							} catch(ConstraintViolationException e) {
								// Parallel edge
							}
						}
					}
					
				}
				
				
				i++;
				
			}			
			
			
			GraphMLWriter writer = new GraphMLWriter();
			String outfile = "resources\\graphml\\London-tfl.graphml";
			writer.writeGraphML(g, outfile, true);
			
			System.out.println("Wrote to " + outfile);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if (nl != null && nl.getLength() > 0) {
			Element el = (Element) nl.item(0);
			textVal = el.getFirstChild().getNodeValue().trim();
		}

		return textVal;
	}

}