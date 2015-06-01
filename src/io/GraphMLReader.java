package io;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import model.MetroEdge;
import model.MetroVertex;
import model.OrderedEdgeListManager;
import model.Point;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import app.Settings;
import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.event.GraphEventType;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.UserData;

public class GraphMLReader {
	private Graph g;
	private Document document;

	// Constants
	static final String[] typeName = { "none", "Element", "Attr", "Text", "CDATA", "EntityRef", "Entity", "ProcInstr", "Comment", "Document", "DocType", "DocFragment", "Notation", };

	static final String DATA = "data";
	static final String DATA_KEY = "key";
	static final String DATA_X = "x";
	static final String DATA_Y = "y";
	static final String DATA_NAME = "label";
	static final String DATA_TYPE = "attr.type";
	static final String EDGE = "edge";
	static final String NODE = "node";
	static final String GRAPHML = "graphml";
	static final String GRAPH = "graph";
	static final String ID = "id";
	static final String EDGE_SOURCE = "source";
	static final String EDGE_TARGET = "target";
	static final String DEFAULT = "default";
	static final String FOR = "for";
	static final String BOOLEAN = "boolean";
	static final String LENGTH = "length";
	static final String TIME = "time";
	static final String DIJKSTRA_COUNT = "dijkstra_count";
	static final String COLOR_R = "color.r";
	static final String COLOR_G = "color.g";
	static final String COLOR_B = "color.b";

	/**
	 * Loads a GraphML file and converts it into a Graph
	 * 
	 * @param filename
	 * @return
	 */
	public Graph loadGraph(String filename) throws SAXParseException, SAXException, ParserConfigurationException, IOException {
		load(filename + ".graphml");
		if (buildGraph()) {// this call already creates the graph!
			g.addUserDatum("basename", filename, UserData.SHARED);
			g.addUserDatum("sourceFile", filename + ".graphml", UserData.SHARED);
			g.setUserDatum("origNumVertices", new Integer(g.numVertices()), UserData.SHARED);
			return g;
		} else {
			return null;
		}

	}

	public Graph loadGraphBySelectedFile(File file) throws SAXParseException, SAXException, ParserConfigurationException, IOException {
		load(file.getAbsolutePath());
		String filename = file.getName();
		String basename = filename.substring(0, filename.lastIndexOf('.'));
		if (buildGraph()) {// this call already creates the graph!
			g.addUserDatum("basename", basename, UserData.SHARED);
			g.addUserDatum("sourceFile", filename, UserData.SHARED);
			g.setUserDatum("origNumVertices", new Integer(g.numVertices()), UserData.SHARED);
			return g;
		} else {
			return null;
		}

	}

	private void load(String filename) throws SAXParseException, SAXException, ParserConfigurationException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		document = builder.parse(new File(filename));
	}

	private Point2D rotate(MetroVertex mv, double angle) {
		Point2D result = new Point2D.Double();
		Point2D point = new Point2D.Double(mv.getX(), mv.getY());
	    AffineTransform rotation = new AffineTransform();
	    double angleInRadians = (angle * Math.PI / 180);
	    rotation.rotate(angleInRadians, 0, 0);
	    rotation.transform(point, result);
		return result;
	}
	
	/**
	 * 
	 */
	private boolean buildGraph() throws FatalException {
		if (document == null)
			return false;

		NodeList nl = document.getElementsByTagName(GRAPHML);
		if (nl.getLength() != 1)
			return false;
		Node baseNode = nl.item(0); // the <graphml> node
		HashMap edgeData = new HashMap();
		HashMap nodeData = new HashMap();
		Vector lineNames = new Vector();
		Vector lineColors = new Vector();

		// get keys for the additional data
		nl = baseNode.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node current = nl.item(i);
			if ((current.getNodeType() == Node.ELEMENT_NODE) && (current.getNodeName().equalsIgnoreCase(DATA_KEY))) {
				if (current.getAttributes().getNamedItem("for").getNodeValue().equalsIgnoreCase(NODE)) {
					nodeData.put(current.getAttributes().getNamedItem("id").getNodeValue(), new KeyElement(current.getAttributes().getNamedItem("id").getNodeValue(), current.getAttributes()
							.getNamedItem("for").getNodeValue(), current.getAttributes().getNamedItem("attr.name").getNodeValue(), current.getAttributes().getNamedItem("attr.type").getNodeValue()));
				} else if (current.getAttributes().getNamedItem("for").getNodeValue().equalsIgnoreCase(EDGE)) {
					edgeData.put(current.getAttributes().getNamedItem("id").getNodeValue(), new KeyElement(current.getAttributes().getNamedItem("id").getNodeValue(), current.getAttributes()
							.getNamedItem("for").getNodeValue(), current.getAttributes().getNamedItem("attr.name").getNodeValue(), current.getAttributes().getNamedItem("attr.type").getNodeValue()));
					if (current.getAttributes().getNamedItem("attr.type").getNodeValue().equalsIgnoreCase(BOOLEAN)) {
						lineNames.add(current.getAttributes().getNamedItem("id").getNodeValue());
						int r = 0, g = 0, b = 0;
						try {
							r = Integer.parseInt(current.getAttributes().getNamedItem(COLOR_R).getNodeValue());
							g = Integer.parseInt(current.getAttributes().getNamedItem(COLOR_G).getNodeValue());
							b = Integer.parseInt(current.getAttributes().getNamedItem(COLOR_B).getNodeValue());
						} catch (Exception e) {
							System.out.println("Couldn't decode the line color");
						}
						lineColors.add(new Color(r, g, b));
					}
				}
			}
		}

		// create the graph
		Node graphNode = baseNode.getLastChild();
		while ((graphNode.getNodeType() != Node.ELEMENT_NODE) || !(graphNode.getNodeName().equals(GRAPH))) {
			graphNode = graphNode.getPreviousSibling();
			if (graphNode == null)
				return false;
		}
		// <graph> element found
		g = new UndirectedSparseGraph();
		OrderedEdgeListManager oelm = new OrderedEdgeListManager();
		g.addListener(oelm, GraphEventType.EDGE_ADDITION);
		g.addListener(oelm, GraphEventType.EDGE_REMOVAL);
		StringLabeller VertexIDs = StringLabeller.getLabeller(g);
		g.addUserDatum("lines", lineNames, UserData.SHARED);
		g.addUserDatum("colors", lineColors, UserData.SHARED);
		g.addUserDatum("edgeStatus", "", UserData.SHARED);

		// add vertices
		Node currentVertex = graphNode.getFirstChild();

		while (currentVertex != null) {
			// is it a <node> element?
			if ((currentVertex.getNodeType() == Node.ELEMENT_NODE) && currentVertex.getNodeName().equalsIgnoreCase(NODE)) {
				MetroVertex mv = (MetroVertex) g.addVertex(new MetroVertex());
				try {
					VertexIDs.setLabel(mv, currentVertex.getAttributes().getNamedItem(ID).getNodeValue());
				} catch (StringLabeller.UniqueLabelException ule) {
					throw new FatalException("Vertex ID not unique");
				}
				// get position and name
				Node dataElement = currentVertex.getFirstChild();
				boolean gotX = false;
				boolean gotY = false;
				while (dataElement != null) {
					// data node?
					if ((dataElement.getNodeType() == Node.ELEMENT_NODE) && dataElement.getNodeName().equalsIgnoreCase(DATA)) {
						if (dataElement.getAttributes().getNamedItem(DATA_KEY).getNodeValue().equalsIgnoreCase(DATA_X)) {
							mv.setX(Double.parseDouble(dataElement.getFirstChild().getNodeValue()));
							gotX = true;
						} else if (dataElement.getAttributes().getNamedItem(DATA_KEY).getNodeValue().equalsIgnoreCase(DATA_Y)) {
							mv.setY(Double.parseDouble(dataElement.getFirstChild().getNodeValue()));
							gotY = true;
						} else if (dataElement.getAttributes().getNamedItem(DATA_KEY).getNodeValue().equalsIgnoreCase(DATA_NAME)) {
							mv.setName((dataElement.getFirstChild() != null) ? dataElement.getFirstChild().getNodeValue() : "");
						}
					}
					dataElement = dataElement.getNextSibling();
				}
				if (!(gotX && gotY))
					throw new FatalException("Vertex has no position data");
				if(Settings.flipMap == true) {
					Point2D p = this.rotate(mv, (double) Settings.flipMapAngle);
					mv.setX(p.getX());
					mv.setY(p.getY() * -1);
				}
			}

			currentVertex = currentVertex.getNextSibling();
		}

		// add edges
		Node currentEdge = graphNode.getFirstChild();

		while (currentEdge != null) {
			// is it a <edge> element?
			if ((currentEdge.getNodeType() == Node.ELEMENT_NODE) && currentEdge.getNodeName().equalsIgnoreCase(EDGE)) {
				// extract source and target

				Vertex source, target;
				String sourceID, targetID;
				sourceID = currentEdge.getAttributes().getNamedItem(EDGE_SOURCE).getNodeValue();
				targetID = currentEdge.getAttributes().getNamedItem(EDGE_TARGET).getNodeValue();
				source = VertexIDs.getVertex(sourceID);
				target = VertexIDs.getVertex(targetID);
				MetroEdge e = (MetroEdge) g.addEdge(new MetroEdge(source, target));
				e.addUserDatum("lineArray", new Boolean[lineNames.size()], UserData.SHARED);
				Boolean[] lines = (Boolean[]) e.getUserDatum("lineArray");
				for (int i = 0; i < lines.length; i++) {
					lines[i] = Boolean.FALSE;
				}
				// add line labels
				Node dataElement = currentEdge.getFirstChild();
				while (dataElement != null) {
					// data node?
					if ((dataElement.getNodeType() == Node.ELEMENT_NODE) && dataElement.getNodeName().equalsIgnoreCase(DATA)) {
						String key_id = dataElement.getAttributes().getNamedItem(DATA_KEY).getNodeValue();
						KeyElement myKey = (KeyElement) edgeData.get(key_id);
						if (myKey != null) {
							if (myKey.getId().equalsIgnoreCase(LENGTH)) {
								e.addUserDatum(LENGTH, new Integer(dataElement.getFirstChild().getNodeValue()), UserData.SHARED);
								// System.out.println("added length " + e.getUserDatum(LENGTH));
							} else if (myKey.getId().equalsIgnoreCase(TIME)) {
								
								String line = "";
								if(dataElement.getAttributes().getNamedItem("line") != null) {
									line = dataElement.getAttributes().getNamedItem("line").getNodeValue();
								}
								
								int seconds = Integer.valueOf(dataElement.getFirstChild().getNodeValue());
								e.addUserDatum(TIME + "." + line, seconds, UserData.SHARED);
								e.setTime(line, seconds);
							} else if (myKey.getId().equalsIgnoreCase(DIJKSTRA_COUNT)) {
								
								String line = "";
								if(dataElement.getAttributes().getNamedItem("line") != null) {
									line = dataElement.getAttributes().getNamedItem("line").getNodeValue();
								}
								
								int dc = Integer.valueOf(dataElement.getFirstChild().getNodeValue());
								e.addUserDatum(DIJKSTRA_COUNT + "." + line, dc, UserData.SHARED);
								
								//e.setDijkstraCount(line, dc);
							} else if (myKey.getAttrType().equalsIgnoreCase(BOOLEAN)) {
								// get value
								e.addUserDatum(key_id, Boolean.valueOf(dataElement.getFirstChild().getNodeValue()), UserData.SHARED);
								lines[lineNames.indexOf(key_id)] = Boolean.valueOf(dataElement.getFirstChild().getNodeValue());
								if (lines[lineNames.indexOf(key_id)]) {
									e.increaseMultiplicity();
								}
							}
						}
					}
					dataElement = dataElement.getNextSibling();
				}

			}

			currentEdge = currentEdge.getNextSibling();
		}

		int numDel = 0;
		Set removeVertices = new HashSet<MetroVertex>();
		for (Iterator iter = g.getVertices().iterator(); iter.hasNext();) {
			MetroVertex element = (MetroVertex) iter.next();
			if (element.degree() > 8)
				System.out.println("Warning: vertex degree higher than 8 for vertex " + element.getName());
			if (element.degree() == 0) {
				removeVertices.add(element);
				numDel++;
				VertexIDs.removeLabel(VertexIDs.getLabel(element)); // setLabel(mv, currentVertex.getAttributes().getNamedItem(ID).getNodeValue());
			}
		}
		g.removeVertices(removeVertices);
		//System.out.println("Removed " + numDel + " singleton vertices.");

		return true;
	}
}
