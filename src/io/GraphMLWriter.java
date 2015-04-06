package io;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import model.MetroEdge;
import model.MetroVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.utils.UserData;

public class GraphMLWriter {

	static final int REGULAR = 0;
	static final int TICK = 1;
	static final int DUMMY = 2;
	static final int LABEL = 3;

	// int vertexCounter=0;

	
	public String escapeXml(String s) {
	    return s.replaceAll("&", "&amp;").replaceAll(">", "&gt;").replaceAll("<", "&lt;").replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
	}

	private void writeVertex(MetroVertex v, StringLabeller vertexIDs, OutputStreamWriter out) throws IOException {
		String shape = "circle";
		double width = 15.0;
		double height = 15.0;
		out.write("<node id=\"" + this.escapeXml(vertexIDs.getLabel(v)) + "\">\n");
		out.write("\t<data key=\"x\">" + v.getX() + "</data>\n");
		out.write("\t<data key=\"y\">" + v.getY() + "</data>\n");
		out.write("\t<data key=\"label\">" + this.escapeXml(v.getName()) + "</data>\n");
		if (v.isDummy() || v.isJoint()) {
			out.write("\t<data key=\"dummy\">true</data>\n");
			shape = "circle";
			width = 1.0;
			height = 1.0;
			if (v.getName().equals(""))
				v.setName(vertexIDs.getLabel(v));
		} else if (v.isLabelVertex()) {
			out.write("\t<data key=\"labelVertex\">true</data>\n");
			shape = "circle";
			width = 1.0;
			height = 1.0;
		} else if (v.degree() != 2 || v.isIntersection()) {
			out.write("\t<data key=\"regular\">true</data>\n");
		} else {
			out.write("\t<data key=\"tick\">true</data>\n");
			shape = "rectangle";
			width = 10.0;
			height = 2.0;
		}
		//out.write("<data key=\"d0\">\n" + "<y:ShapeNode >\n" + "<y:Geometry  x=\"" + (30 * v.getX() - width / 2) + "\" y=\"" + (-30 * v.getY() - height / 2) + "\" width=\"" + width + "\" height=\""
		//		+ height + "\"/>\n" + "<y:Fill color=\"#FFFFFF\"  transparent=\"false\"/>\n" + "<y:BorderStyle type=\"line\" width=\"4.0\" color=\"#000000\" />\n" + "<y:Shape type=\"" + shape
		//		+ "\"/>\n" + "<y:NodeLabel>" + this.escapeXml(v.getName()) + "</y:NodeLabel>\n" + "</y:ShapeNode>\n" + "</data>\n");
		out.write("</node>\n");
		// vertexCounter++;
	}

	private void writeVertex(String id, double xPos, double yPos, String label, int type, OutputStreamWriter out) throws IOException {
		String shape = "circle";
		double width = 15.0;
		double height = 15.0;
		out.write("<node id=\"" + id + "\">\n");
		out.write("\t<data key=\"x\">" + xPos + "</data>\n");
		out.write("\t<data key=\"y\">" + yPos + "</data>\n");
		out.write("\t<data key=\"label\">" + this.escapeXml(label) + "</data>\n");
		if (type == DUMMY) {
			out.write("\t<data key=\"dummy\">true</data>\n");
		} else if (type == LABEL) {
			out.write("\t<data key=\"labelVertex\">true</data>\n");
		} else if (type == REGULAR) {
			out.write("\t<data key=\"regular\">true</data>\n");
		} else if (type == TICK) {
			out.write("\t<data key=\"tick\">true</data>\n");
		}
		if (type == TICK) {
			shape = "rectangle";
			width = 10.0;
			height = 2.0;
		} else if (type == DUMMY || type == LABEL) {
			shape = "circle";
			width = 1.0;
			height = 1.0;
		}
		//out.write("<data key=\"d0\">\n" + "<y:ShapeNode >\n" + "<y:Geometry  x=\"" + (30 * xPos - width / 2) + "\" y=\"" + (-30 * yPos - height / 2) + "\" width=\"" + width + "\" height=\"" + height
		//		+ "\"/>\n" + "<y:Fill color=\"#FFFFFF\"  transparent=\"false\"/>\n" + "<y:BorderStyle type=\"line\" width=\"4.0\" color=\"#000000\" />\n" + "<y:Shape type=\"" + shape + "\"/>\n"
		//		+ "<y:NodeLabel>" + this.escapeXml(label) + "</y:NodeLabel>\n" + "</y:ShapeNode>\n" + "</data>\n");
		out.write("</node>\n");
		// vertexCounter++;
	}

	private void writeEdge(String id, String source, String target, Boolean[] lines, OutputStreamWriter out) throws IOException {
		writeEdge(id, source, target, lines, out, "#000000", new int[]{});
	}

	private void writeEdge(String id, String source, String target, Boolean[] lines, OutputStreamWriter out, String yColor, int[] times) throws IOException {
		out.write("<edge id=\"" + this.escapeXml(id) + "\" source=\"" + this.escapeXml(source) + "\" target=\"" + this.escapeXml(target) + "\">\n");
		if (lines != null) {
			for (int i = 0; i < lines.length; i++) {
				if (lines[i].booleanValue()) {// belongs to line i
					out.write("\t<data key=\"l" + i + "\">true</data>\n");
				}
			}
		}
		if (times != null) {
			for (int i = 0; i < times.length; i++) {
				if (times[i] > 0) {
					out.write("\t<data key=\"time\" line=\"l" + i + "\">" + times[i] + "</data>\n");
				}
			}
		}
		//out.write("\t<data key=\"weight\">" + weight + "</data>\n");
		//out.write("\t<data key=\"dijkstra_count\">" + dijkstraCount + "</data>\n");
		//out.write("<data key=\"d1\">\n" + "<y:PolyLineEdge >\n" + "<y:LineStyle type=\"line\" width=\"4.0\" color=\"" + yColor + "\" />\n" + "<y:Arrows source=\"none\" target=\"none\"/>\n"
		//		+ "</y:PolyLineEdge>\n" + "</data>\n");
		out.write("</edge>\n");
	}

	public void writeGraphML(Graph g, String filename, boolean labeled) throws IOException {
		if (!labeled) {
			writeGraphML(g, filename);
		} else {
			writeGraphMLLabeled(g, filename);
		}
	}

	/**
	 * @param g
	 * @param filename
	 * @throws IOException
	 */
	public void writeGraphML(Graph g, String filename) throws IOException {
		final String preamble = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"\n"
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns\n"
				+ "http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\" xmlns:y=\"http://www.yworks.com/xml/graphml\">\n";
		final String xCoordString = "<key id=\"x\" for=\"node\" attr.name=\"x_coordinate\" attr.type=\"double\"/>\n";
		final String yCoordString = "<key id=\"y\" for=\"node\" attr.name=\"y_coordinate\" attr.type=\"double\"/>\n";
		final String labelString = "<key id=\"label\" for=\"node\" attr.name=\"station_name\" attr.type=\"string\"/>\n";
		final String timeString = "<key id=\"time\" for=\"edge\" attr.name=\"edge_time\" attr.type=\"integer\"/>";
		final String regularVertex = "<key id=\"regular\" for=\"node\" attr.name=\"regular_vertex\" attr.type=\"boolean\"/>\n";
		final String dummyVertex = "<key id=\"dummy\" for=\"node\" attr.name=\"dummy_vertex\" attr.type=\"boolean\"/>\n";
		final String labelVertex = "<key id=\"labelVertex\" for=\"node\" attr.name=\"label_vertex\" attr.type=\"boolean\"/>\n";
		final String tickVertex = "<key id=\"tick\" for=\"node\" attr.name=\"tick_vertex\" attr.type=\"boolean\"/>\n";
		final String yFilesNode = "<key id=\"d0\" for=\"node\" yfiles.type=\"nodegraphics\"/>\n";
		final String yFilesEdge = "<key id=\"d1\" for=\"edge\" yfiles.type=\"edgegraphics\"/>\n";

		StringLabeller vertexIDs = StringLabeller.getLabeller(g);
		int tickCounter = 0;

		OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(filename)), "UTF8");

		out.write(preamble);
		out.write(xCoordString);
		out.write(yCoordString);
		out.write(labelString);
		out.write(timeString);
		out.write(regularVertex);
		out.write(dummyVertex);
		out.write(labelVertex);
		out.write(tickVertex);
		out.write(yFilesNode);
		out.write(yFilesEdge);

		// create keys for the metro lines
		Vector colors = (Vector) g.getUserDatum("colors");
		for (int i = 0; i < colors.size(); i++) {
			Color col = (Color) colors.get(i);
			out.write("<key id=\"l" + i + "\" for=\"edge\" attr.name=\"l" + i + "\" attr.type=\"boolean\" color.r=\"" + col.getRed() + "\" color.g=\"" + col.getGreen() + "\" color.b=\""
					+ col.getBlue() + "\">\n" + "\t<default>FALSE</default>\n" + "</key>\n");
		}

		// start graph
		out.write("<graph id=\"G\" edgedefault=\"undirected\">\n");

		// create vertices
		/*
    	 * 
    	 */
		for (Iterator iter = g.getVertices().iterator(); iter.hasNext();) {
			MetroVertex mv = (MetroVertex) iter.next();
			if (mv.degree() == 2 && !mv.isIntersection()) {
				MetroEdge firstEdge, secondEdge;
				Iterator edges = mv.getIncidentEdges().iterator();
				firstEdge = (MetroEdge) edges.next();
				secondEdge = (MetroEdge) edges.next();
				if ((firstEdge.getNumVertices() >= 1) || (secondEdge.getNumVertices() >= 1))
					continue; // this vertex will be added later!
			}
			writeVertex(mv, vertexIDs, out);
		}

		// create edges
		int edgeCounter = 0;
		HashSet visitedEdges = new HashSet();
		for (Iterator iter = g.getEdges().iterator(); iter.hasNext();) {
			MetroEdge e = (MetroEdge) iter.next();
			if (!visitedEdges.contains(e)) {
				MetroVertex eFirst = (MetroVertex) e.getEndpoints().getFirst();
				MetroVertex eSecond = (MetroVertex) e.getEndpoints().getSecond();
				Boolean[] eLines = (Boolean[]) e.getUserDatum("lineArray");
				String yColor = "#808080";
				if (eLines != null) {
					for (int i = 0; i < eLines.length; i++) {
						if (eLines[i].booleanValue()) {
							yColor = "#" + Integer.toHexString(((Color) colors.get(i)).getRGB()).substring(2);
						}
					}
				}
				
				int[] times = new int[eLines.length];
				if (eLines != null) {
					for (int i = 0; i < eLines.length; i++) {
						times[i] = e.getTime("l" + i);
					}
				}
				e.addUserDatum("times", times, UserData.SHARED);
				
				// if (e.isIncident(vertexIDs.getVertex("n15"))) {
				// System.out.println("Kante zwischen " + eFirst.getName() + eSecond.getName() + " hat numVert " + e.getNumVertices());
				// }

				if (e.getNumVertices() >= 1) { // need to insert tick vertices

					if (eFirst.degree() == 2 && eSecond.degree() == 2 && !eFirst.isIntersection() && !eSecond.isIntersection()) { // middle part of 3-link path
						List myNames = new LinkedList(e.getMyVertexNames());
						myNames.add(0, eFirst.getName());// new first element
						myNames.add(eSecond.getName());// append to the end
						Iterator inciEdge = eFirst.getIncidentEdges().iterator();
						MetroEdge sourceEdge = (MetroEdge) inciEdge.next();
						if (sourceEdge == e)
							sourceEdge = (MetroEdge) inciEdge.next();
						inciEdge = eSecond.getIncidentEdges().iterator();
						MetroEdge targetEdge = (MetroEdge) inciEdge.next();
						if (targetEdge == e)
							targetEdge = (MetroEdge) inciEdge.next();
						MetroVertex sourceEnd = (MetroVertex) sourceEdge.getOpposite(eFirst);
						MetroVertex targetEnd = (MetroVertex) targetEdge.getOpposite(eSecond);
						int ticks = e.getNumVertices() + 2;
						double len1, len2, len3; // the lengths of the three links
						len1 = sourceEnd.distance(eFirst);
						len2 = eFirst.distance(eSecond);
						len3 = eSecond.distance(targetEnd);
						double unit = (len1 + len2 + len3) / (ticks + 1); // distance between adjacent ticks

						// ticks on first edge
						int ticksOnEdge = (int) (len1 / unit);
						double xDir = ((eFirst.getX() - sourceEnd.getX()) > 0) ? 1 : (((eFirst.getX() - sourceEnd.getX()) < 0) ? -1 : 0);
						double yDir = ((eFirst.getY() - sourceEnd.getY()) > 0) ? 1 : (((eFirst.getY() - sourceEnd.getY()) < 0) ? -1 : 0);
						String sID = vertexIDs.getLabel(sourceEnd);
						String tID = "tck" + tickCounter;
						for (int i = 1; i <= ticksOnEdge; i++) {
							writeVertex(tID, sourceEnd.getX() + i * unit * xDir, sourceEnd.getY() + i * unit * yDir, (String) ((myNames != null) ? myNames.get(i - 1) : ""), TICK, out);
							writeEdge("e" + edgeCounter, sID, tID, (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							tickCounter++;
							edgeCounter++;
							sID = tID;
							tID = "tck" + tickCounter;
						}
						// connect to bend if necessary
						if (len1 % unit != 0) { // tick not on bend
							writeVertex(vertexIDs.getLabel(eFirst), eFirst.getX(), eFirst.getY(), "", DUMMY, out);
							writeEdge("e" + edgeCounter, sID, vertexIDs.getLabel(eFirst), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							edgeCounter++;
							sID = vertexIDs.getLabel(eFirst);
						}
						int ticksDone = ticksOnEdge;

						// ticks on second edge
						ticksOnEdge = (int) ((len2 + (len1 % unit)) / unit);
						double offset = -(len1 % unit);
						xDir = ((eSecond.getX() - eFirst.getX()) > 0) ? 1 : (((eSecond.getX() - eFirst.getX()) < 0) ? -1 : 0);
						yDir = ((eSecond.getY() - eFirst.getY()) > 0) ? 1 : (((eSecond.getY() - eFirst.getY()) < 0) ? -1 : 0);
						for (int i = 1; i <= ticksOnEdge; i++) {
							writeVertex(tID, eFirst.getX() + (i * unit + offset) * xDir, eFirst.getY() + (i * unit + offset) * yDir,
									(String) ((myNames != null) ? myNames.get(ticksDone + i - 1) : ""), TICK, out);
							writeEdge("e" + edgeCounter, sID, tID, (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							tickCounter++;
							edgeCounter++;
							sID = tID;
							tID = "tck" + tickCounter;
						}
						// connect to bend if necessary
						if ((len2 + (len1 % unit)) % unit != 0) { // tick not on bend
							writeVertex(vertexIDs.getLabel(eSecond), eSecond.getX(), eSecond.getY(), "", DUMMY, out);
							writeEdge("e" + edgeCounter, sID, vertexIDs.getLabel(eSecond), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							edgeCounter++;
							sID = vertexIDs.getLabel(eSecond);
						}
						ticksDone += ticksOnEdge;

						// ticks on third edge
						ticksOnEdge = ticks - ticksDone;
						offset = -((len2 + (len1 % unit)) % unit);
						xDir = ((targetEnd.getX() - eSecond.getX()) > 0) ? 1 : (((targetEnd.getX() - eSecond.getX()) < 0) ? -1 : 0);
						yDir = ((targetEnd.getY() - eSecond.getY()) > 0) ? 1 : (((targetEnd.getY() - eSecond.getY()) < 0) ? -1 : 0);
						for (int i = 1; i <= ticksOnEdge; i++) {
							writeVertex(tID, eSecond.getX() + (i * unit + offset) * xDir, eSecond.getY() + (i * unit + offset) * yDir, (String) ((myNames != null) ? myNames.get(ticksDone + i - 1)
									: ""), TICK, out);
							writeEdge("e" + edgeCounter, sID, tID, (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							tickCounter++;
							edgeCounter++;
							sID = tID;
							tID = "tck" + tickCounter;
						}
						// final edge
						writeEdge("e" + edgeCounter, sID, vertexIDs.getLabel(targetEnd), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
						edgeCounter++;

						visitedEdges.add(e);
						visitedEdges.add(sourceEdge);
						visitedEdges.add(targetEdge);

					} else if ((eFirst.degree() == 1 && eSecond.degree() == 2 && !eSecond.isIntersection()) || (eFirst.degree() == 2 && eSecond.degree() == 1 && !eFirst.isIntersection())) {
						// 2-link pendant path
						MetroVertex linkVertex = (eFirst.degree() == 2) ? eFirst : eSecond;
						Iterator inciEdge = linkVertex.getIncidentEdges().iterator();// has 2 elements!
						MetroEdge otherEdge = (MetroEdge) inciEdge.next();
						if (otherEdge == e)
							otherEdge = (MetroEdge) inciEdge.next();
						MetroVertex startVertex, endVertex;
						if (linkVertex == eFirst) {
							startVertex = (MetroVertex) otherEdge.getOpposite(linkVertex);
							endVertex = eSecond;
						} else {
							startVertex = eFirst;
							endVertex = (MetroVertex) otherEdge.getOpposite(linkVertex);
						}

						List myNames = new LinkedList(e.getMyVertexNames());
						// keep the order?
						if (linkVertex == eFirst) {
							myNames.add(0, linkVertex.getName());// new first element
						} else {
							myNames.add(linkVertex.getName());
						}

						int ticks = e.getNumVertices() + 1;
						double len1, len2; // the lengths of the two links
						len1 = startVertex.distance(linkVertex);
						len2 = linkVertex.distance(endVertex);
						double unit = (len1 + len2) / (ticks + 1); // distance between adjacent ticks

						// ticks on first edge
						int ticksOnEdge = (int) (len1 / unit);
						double xDir = ((linkVertex.getX() - startVertex.getX()) > 0) ? 1 : (((linkVertex.getX() - startVertex.getX()) < 0) ? -1 : 0);
						double yDir = ((linkVertex.getY() - startVertex.getY()) > 0) ? 1 : (((linkVertex.getY() - startVertex.getY()) < 0) ? -1 : 0);
						String sID = vertexIDs.getLabel(startVertex);
						String tID = "tck" + tickCounter;
						for (int i = 1; i <= ticksOnEdge; i++) {
							writeVertex(tID, startVertex.getX() + i * unit * xDir, startVertex.getY() + i * unit * yDir, (String) ((myNames != null) ? myNames.get(i - 1) : ""), TICK, out);
							writeEdge("e" + edgeCounter, sID, tID, (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							tickCounter++;
							edgeCounter++;
							sID = tID;
							tID = "tck" + tickCounter;
						}
						// connect to bend if necessary
						if (len1 % unit != 0) { // tick not on bend
							writeVertex(vertexIDs.getLabel(linkVertex), linkVertex.getX(), linkVertex.getY(), "", DUMMY, out);
							writeEdge("e" + edgeCounter, sID, vertexIDs.getLabel(linkVertex), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							edgeCounter++;
							sID = vertexIDs.getLabel(linkVertex);
						}
						int ticksDone = ticksOnEdge;

						// ticks on second edge
						ticksOnEdge = ticks - ticksDone;
						double offset = -(len1 % unit);
						xDir = ((endVertex.getX() - linkVertex.getX()) > 0) ? 1 : (((endVertex.getX() - linkVertex.getX()) < 0) ? -1 : 0);
						yDir = ((endVertex.getY() - linkVertex.getY()) > 0) ? 1 : (((endVertex.getY() - linkVertex.getY()) < 0) ? -1 : 0);
						for (int i = 1; i <= ticksOnEdge; i++) {
							writeVertex(tID, linkVertex.getX() + (i * unit + offset) * xDir, linkVertex.getY() + (i * unit + offset) * yDir,
									(String) ((myNames != null) ? myNames.get(ticksDone + i - 1) : ""), TICK, out);
							writeEdge("e" + edgeCounter, sID, tID, (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							tickCounter++;
							edgeCounter++;
							sID = tID;
							tID = "tck" + tickCounter;
						}
						// final edge
						writeEdge("e" + edgeCounter, sID, vertexIDs.getLabel(endVertex), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
						edgeCounter++;

						visitedEdges.add(e);
						visitedEdges.add(otherEdge);

					} else { // insert ticks only on this edge
						int ticks = e.getNumVertices();
						double len1 = eFirst.distance(eSecond);
						double unit = len1 / (ticks + 1); // distance between adjacent ticks

						// ticks on first edge
						double xDir = ((eSecond.getX() - eFirst.getX()) > 0) ? 1 : (((eSecond.getX() - eFirst.getX()) < 0) ? -1 : 0);
						double yDir = ((eSecond.getY() - eFirst.getY()) > 0) ? 1 : (((eSecond.getY() - eFirst.getY()) < 0) ? -1 : 0);
						String sID = vertexIDs.getLabel(eFirst);
						String tID = "tck" + tickCounter;
						for (int i = 1; i <= ticks; i++) {
							writeVertex(tID, eFirst.getX() + i * unit * xDir, eFirst.getY() + i * unit * yDir, (String) ((e.getMyVertexNames() != null) ? e.getMyVertexNames().get(i - 1) : ""), TICK,
									out);
							writeEdge("e" + edgeCounter, sID, tID, (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							tickCounter++;
							edgeCounter++;
							sID = tID;
							tID = "tck" + tickCounter;
						}
						// final edge
						writeEdge("e" + edgeCounter, sID, vertexIDs.getLabel(eSecond), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
						edgeCounter++;
						visitedEdges.add(e);
					}
				} else {
					if ((eFirst.degree() == 2 && eSecond.isIntersection() && !eFirst.isIntersection())) {
						Iterator nextIt = eFirst.getIncidentEdges().iterator();
						MetroEdge nextEdge = (MetroEdge) nextIt.next();
						if (nextEdge == e)
							nextEdge = (MetroEdge) nextIt.next();
						if (nextEdge.getNumVertices() >= 1)
							continue;
					}
					if ((eFirst.isIntersection() && eSecond.degree() == 2 && !eSecond.isIntersection())) {
						Iterator nextIt = eSecond.getIncidentEdges().iterator();
						MetroEdge nextEdge = (MetroEdge) nextIt.next();
						if (nextEdge == e)
							nextEdge = (MetroEdge) nextIt.next();
						if (nextEdge.getNumVertices() >= 1)
							continue;
					}
					writeEdge("e" + edgeCounter, vertexIDs.getLabel(eFirst), vertexIDs.getLabel(eSecond), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
					edgeCounter++;
					visitedEdges.add(e);
				}
			}

			// String sID = vertexIDs.getLabel((MetroVertex) me.getEndpoints().getFirst());
			// String tID = vertexIDs.getLabel((MetroVertex) me.getEndpoints().getSecond());
			// out.write("<edge id=\"e" + edgeCounter + "\" source=\"" + sID + "\" target=\"" + tID + "\">\n");
			// Boolean[] lines = (Boolean[])me.getUserDatum("lineArray");
			// for (int i = 0; i < lines.length; i++) {
			// if (lines[i].booleanValue()) {//belongs to line i
			// out.write("\t<data key=\"l" + i + "\">true</data>\n");
			// }
			// }
			// out.write("</edge>\n");
			// edgeCounter++;
		}

		out.write("</graph>\n</graphml>\n");
		out.flush();
		out.close();

	}

	public void writeGraphMLLabeled(Graph g, String filename) throws IOException {
		final String preamble = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"\n"
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns\n"
				+ "http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\" xmlns:y=\"http://www.yworks.com/xml/graphml\">\n";
		final String xCoordString = "<key id=\"x\" for=\"node\" attr.name=\"x_coordinate\" attr.type=\"double\"/>\n";
		final String yCoordString = "<key id=\"y\" for=\"node\" attr.name=\"y_coordinate\" attr.type=\"double\"/>\n";
		final String labelString = "<key id=\"label\" for=\"node\" attr.name=\"station_name\" attr.type=\"string\"/>\n";
		final String timeString = "<key id=\"time\" for=\"edge\" attr.name=\"edge_time\" attr.type=\"integer\"/>";
		final String regularVertex = "<key id=\"regular\" for=\"node\" attr.name=\"regular_vertex\" attr.type=\"boolean\"/>\n";
		final String dummyVertex = "<key id=\"dummy\" for=\"node\" attr.name=\"dummy_vertex\" attr.type=\"boolean\"/>\n";
		final String labelVertex = "<key id=\"labelVertex\" for=\"node\" attr.name=\"label_vertex\" attr.type=\"boolean\"/>\n";
		final String tickVertex = "<key id=\"tick\" for=\"node\" attr.name=\"tick_vertex\" attr.type=\"boolean\"/>\n";
		final String edgeWeight = "<key id=\"weight\" for=\"edge\" attr.name=\"edge_weight\" attr.type=\"integer\"/>\n";
		final String dijkstraCount = "<key id=\"dijkstra_count\" for=\"edge\" attr.name=\"edge_weight\" attr.type=\"integer\"/>\n";
		// final String yFilesNode = "<key id=\"d0\" for=\"node\" yfiles.type=\"nodegraphics\"/>\n";
		// final String yFilesEdge = "<key id=\"d1\" for=\"edge\" yfiles.type=\"edgegraphics\"/>\n";

		StringLabeller vertexIDs = StringLabeller.getLabeller(g);
		int tickCounter = 0;

		OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(filename)), "UTF8");

		out.write(preamble);
		out.write(xCoordString);
		out.write(yCoordString);
		out.write(labelString);
		out.write(timeString);
		out.write(regularVertex);
		out.write(dummyVertex);
		out.write(labelVertex);
		out.write(tickVertex);
		out.write(edgeWeight);
		out.write(dijkstraCount);
		// out.write(yFilesNode);
		// out.write(yFilesEdge);

		// create keys for the metro lines
		Vector colors = (Vector) g.getUserDatum("colors");
		for (int i = 0; i < colors.size(); i++) {
			Color col = (Color) colors.get(i);
			out.write("<key id=\"l" + i + "\" for=\"edge\" attr.name=\"l" + i + "\" attr.type=\"boolean\" color.r=\"" + col.getRed() + "\" color.g=\"" + col.getGreen() + "\" color.b=\""
					+ col.getBlue() + "\">\n" + "\t<default>FALSE</default>\n" + "</key>\n");
		}

		// start graph
		out.write("<graph id=\"G\" edgedefault=\"undirected\">\n");

		// create vertices
		/*
    	 * 
    	 */
		for (Iterator iter = g.getVertices().iterator(); iter.hasNext();) {
			MetroVertex mv = (MetroVertex) iter.next();
			/*
			 * if (mv.degree()==2) { MetroEdge firstEdge, secondEdge; Iterator edges = mv.getIncidentEdges().iterator(); firstEdge = (MetroEdge) edges.next(); secondEdge = (MetroEdge) edges.next(); if
			 * ((firstEdge.getNumVertices() >= 1) || (secondEdge.getNumVertices() >= 1)) continue; //this vertex will be added later! }
			 */
			writeVertex(mv, vertexIDs, out);
		}

		// create edges
		int edgeCounter = 0;
		HashSet visitedEdges = new HashSet();
		for (Iterator iter = g.getEdges().iterator(); iter.hasNext();) {
			MetroEdge e = (MetroEdge) iter.next();
			if (!visitedEdges.contains(e)) {
				MetroVertex eFirst = (MetroVertex) e.getEndpoints().getFirst();
				MetroVertex eSecond = (MetroVertex) e.getEndpoints().getSecond();
				Boolean[] eLines = (Boolean[]) e.getUserDatum("lineArray");
				String yColor = "#808080";
				if (eLines != null) {
					for (int i = 0; i < eLines.length; i++) {
						if (eLines[i].booleanValue()) {
							yColor = "#" + Integer.toHexString(((Color) colors.get(i)).getRGB()).substring(2);
						}
					}
				}
				
				int[] times = new int[eLines.length];
				if (eLines != null) {
					for (int i = 0; i < eLines.length; i++) {
						times[i] = e.getTime("l" + i);
					}
				}
				
				if(e.getUserDatum("times") != null) {
					e.removeUserDatum("times");
				}
				e.addUserDatum("times", times, UserData.SHARED);
				
				// if (e.isIncident(vertexIDs.getVertex("n15"))) {
				// System.out.println("Kante zwischen " + eFirst.getName() + eSecond.getName() + " hat numVert " + e.getNumVertices());
				// }

				if (e.getNumVertices() >= 1) { // need to insert tick vertices

					if (e.isLabelMainDummy()) { // middle part of 3-link path

						int length = e.getNumVertices() + 1;
						double firstX = eFirst.getX();
						double firstY = eFirst.getY();
						double secondX = eSecond.getX();
						double secondY = eSecond.getY();
						double firstSecondX = (secondX - firstX) / (length - 1);
						double firstSecondY = (secondY - firstY) / (length - 1);
						if (e.isPendant()) {
							firstSecondX = (secondX - firstX) / (length - 1.5);
							firstSecondY = (secondY - firstY) / (length - 1.5);
						}

						// ticks on first edge
						String sID = vertexIDs.getLabel(eFirst);
						String tID = "tck" + tickCounter;
						for (int i = 1; i <= length - 1; i++) {
							writeVertex(tID, eFirst.getX() + (i - 0.5) * firstSecondX, eFirst.getY() + (i - 0.5) * firstSecondY,
									(String) ((e.getMyVertexNames() != null) ? e.getMyVertexNames().get(i - 1) : ""), TICK, out);
							writeEdge("e" + edgeCounter, sID, tID, (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							tickCounter++;
							edgeCounter++;
							sID = tID;
							tID = "tck" + tickCounter;
						}
						writeEdge("e" + edgeCounter, sID, vertexIDs.getLabel(eSecond), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));

						visitedEdges.add(e);

					} else if ((eFirst.degree() == 1 && eSecond.degree() == 2) || (eFirst.degree() == 2 && eSecond.degree() == 1)) {
						// 2-link pendant path
						MetroVertex linkVertex = (eFirst.degree() == 2) ? eFirst : eSecond;
						Iterator inciEdge = linkVertex.getIncidentEdges().iterator();// has 2 elements!
						MetroEdge otherEdge = (MetroEdge) inciEdge.next();
						if (otherEdge == e)
							otherEdge = (MetroEdge) inciEdge.next();
						MetroVertex startVertex, endVertex;
						if (linkVertex == eFirst) {
							startVertex = (MetroVertex) otherEdge.getOpposite(linkVertex);
							endVertex = eSecond;
						} else {
							startVertex = eFirst;
							endVertex = (MetroVertex) otherEdge.getOpposite(linkVertex);
						}

						List myNames = new LinkedList(e.getMyVertexNames());
						// keep the order?
						if (linkVertex == eFirst) {
							myNames.add(0, linkVertex.getName());// new first element
						} else {
							myNames.add(linkVertex.getName());
						}

						int ticks = e.getNumVertices() + 1;
						double len1, len2; // the lengths of the two links
						len1 = startVertex.distance(linkVertex);
						len2 = linkVertex.distance(endVertex);
						double unit = (len1 + len2) / (ticks + 1); // distance between adjacent ticks

						// ticks on first edge
						int ticksOnEdge = (int) (len1 / unit);
						double xDir = ((linkVertex.getX() - startVertex.getX()) > 0) ? 1 : (((linkVertex.getX() - startVertex.getX()) < 0) ? -1 : 0);
						double yDir = ((linkVertex.getY() - startVertex.getY()) > 0) ? 1 : (((linkVertex.getY() - startVertex.getY()) < 0) ? -1 : 0);
						String sID = vertexIDs.getLabel(startVertex);
						String tID = "tck" + tickCounter;
						for (int i = 1; i <= ticksOnEdge; i++) {
							writeVertex(tID, startVertex.getX() + i * unit * xDir, startVertex.getY() + i * unit * yDir, (String) ((myNames != null) ? myNames.get(i - 1) : ""), TICK, out);
							writeEdge("e" + edgeCounter, sID, tID, (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							tickCounter++;
							edgeCounter++;
							sID = tID;
							tID = "tck" + tickCounter;
						}
						// connect to bend if necessary
						if (len1 % unit != 0) { // tick not on bend
							writeVertex(vertexIDs.getLabel(linkVertex), linkVertex.getX(), linkVertex.getY(), "", DUMMY, out);
							writeEdge("e" + edgeCounter, sID, vertexIDs.getLabel(linkVertex), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							edgeCounter++;
							sID = vertexIDs.getLabel(linkVertex);
						}
						int ticksDone = ticksOnEdge;

						// ticks on second edge
						ticksOnEdge = ticks - ticksDone;
						double offset = -(len1 % unit);
						xDir = ((endVertex.getX() - linkVertex.getX()) > 0) ? 1 : (((endVertex.getX() - linkVertex.getX()) < 0) ? -1 : 0);
						yDir = ((endVertex.getY() - linkVertex.getY()) > 0) ? 1 : (((endVertex.getY() - linkVertex.getY()) < 0) ? -1 : 0);
						for (int i = 1; i <= ticksOnEdge; i++) {
							writeVertex(tID, linkVertex.getX() + (i * unit + offset) * xDir, linkVertex.getY() + (i * unit + offset) * yDir,
									(String) ((myNames != null) ? myNames.get(ticksDone + i - 1) : ""), TICK, out);
							writeEdge("e" + edgeCounter, sID, tID, (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							tickCounter++;
							edgeCounter++;
							sID = tID;
							tID = "tck" + tickCounter;
						}
						// final edge
						writeEdge("e" + edgeCounter, sID, vertexIDs.getLabel(endVertex), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
						edgeCounter++;

						visitedEdges.add(e);
						visitedEdges.add(otherEdge);

					} else { // insert ticks only on this edge
						int ticks = e.getNumVertices();
						if (e.isLabelIntersectionMainDummy())
							ticks = 0;
						double len1 = eFirst.distance(eSecond);
						double unit = len1 / (ticks + 1); // distance between adjacent ticks

						// ticks on first edge
						double xDir = ((eSecond.getX() - eFirst.getX()) > 0) ? 1 : (((eSecond.getX() - eFirst.getX()) < 0) ? -1 : 0);
						double yDir = ((eSecond.getY() - eFirst.getY()) > 0) ? 1 : (((eSecond.getY() - eFirst.getY()) < 0) ? -1 : 0);
						String sID = vertexIDs.getLabel(eFirst);
						String tID = "tck" + tickCounter;
						for (int i = 1; i <= ticks; i++) {
							writeVertex(tID, eFirst.getX() + i * unit * xDir, eFirst.getY() + i * unit * yDir, (String) ((e.getMyVertexNames() != null) ? e.getMyVertexNames().get(i - 1) : ""), TICK,
									out);
							writeEdge("e" + edgeCounter, sID, tID, (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
							tickCounter++;
							edgeCounter++;
							sID = tID;
							tID = "tck" + tickCounter;
						}
						// final edge
						writeEdge("e" + edgeCounter, sID, vertexIDs.getLabel(eSecond), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
						edgeCounter++;
						visitedEdges.add(e);
					}
				} else {
					writeEdge("e" + edgeCounter, vertexIDs.getLabel(eFirst), vertexIDs.getLabel(eSecond), (Boolean[]) e.getUserDatum("lineArray"), out, yColor, (int[]) e.getUserDatum("times"));
					edgeCounter++;
					visitedEdges.add(e);
				}
			}

			// String sID = vertexIDs.getLabel((MetroVertex) me.getEndpoints().getFirst());
			// String tID = vertexIDs.getLabel((MetroVertex) me.getEndpoints().getSecond());
			// out.write("<edge id=\"e" + edgeCounter + "\" source=\"" + sID + "\" target=\"" + tID + "\">\n");
			// Boolean[] lines = (Boolean[])me.getUserDatum("lineArray");
			// for (int i = 0; i < lines.length; i++) {
			// if (lines[i].booleanValue()) {//belongs to line i
			// out.write("\t<data key=\"l" + i + "\">true</data>\n");
			// }
			// }
			// out.write("</edge>\n");
			// edgeCounter++;
		}

		out.write("</graph>\n</graphml>\n");
		out.flush();
		out.close();

	}
}
