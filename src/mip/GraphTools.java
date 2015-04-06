package mip;

import ilog.concert.IloException;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import mip.comparator.HorizontalOrderComparator;
import model.Face;
import model.MetroEdge;
import model.MetroPath;
import model.MetroVertex;
import model.Point;
import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.StringLabeller.UniqueLabelException;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserData;

public class GraphTools {

	static int getMPrime(Graph g) {
		int counter = 0;
		for (Iterator iter = g.getEdges().iterator(); iter.hasNext();) {
			MetroEdge e = (MetroEdge) iter.next();

			Boolean[] lines = (Boolean[]) e.getUserDatum("lineArray");
			if (lines != null)
				for (int i = 0; i < lines.length; i++) {
					if (lines[i].booleanValue()) {
						counter++;
					}
				}
		}
		return counter;
	}

	/**
	 * This method inserts a single dummy vertex on all edges that directly connect two intersections in order to allow more flexibility for their layout
	 * 
	 * @param g
	 *            our graph
	 */
	static void insertBends(Graph g) {
		/*
		int bendVertexCounter = 0;
		StringLabeller vertexIDs = StringLabeller.getLabeller(g);

		MetroEdge[] edges = (MetroEdge[]) g.getEdges().toArray(new MetroEdge[0]);
		for (int i = 0; i < edges.length; i++) {
			MetroEdge myEdge = edges[i];
			MetroVertex first, second;
			first = myEdge.getFirst();
			second = myEdge.getSecond();
			if (first.isIntersection() && second.isIntersection()) {
				MetroVertex newVertex = (MetroVertex) g.addVertex(new MetroVertex((first.getX() + second.getX()) / 2, (first.getY() + second.getY()) / 2));
				newVertex.setJoint(true);
				try {
					vertexIDs.setLabel(newVertex, "b" + (bendVertexCounter++));
				} catch (UniqueLabelException ule) {
					// this should never happen cause we increase jointVertexCounter each time
					ule.printStackTrace();
				}
				MetroEdge e1, e2;
				e1 = (MetroEdge) g.addEdge(new MetroEdge(first, newVertex));
				e2 = (MetroEdge) g.addEdge(new MetroEdge(second, newVertex));

				// e1.setLeftFace(myEdge.getLeftFace());
				// e1.setRightFace(myEdge.getRightFace());
				// e2.setLeftFace(myEdge.getLeftFace());
				// e2.setRightFace(myEdge.getRightFace());
				e1.setLength(0.5 * myEdge.getLength());
				e2.setLength(0.5 * myEdge.getLength());
				e1.setNumVertices(0);
				e2.setNumVertices(0);
				e1.setMultiplicity(myEdge.getMultiplicity());
				e2.setMultiplicity(myEdge.getMultiplicity());

				Iterator userData = myEdge.getUserDatumKeyIterator();
				while (userData.hasNext()) {
					Object key = userData.next();
					Object value = myEdge.getUserDatum(key);
					e1.setUserDatum(key, value, UserData.SHARED);
					e2.setUserDatum(key, value, UserData.SHARED);
				}
				e1.setBendEdge(true);
				e2.setBendEdge(true);
				e1.setMainEdge(e1);
				e2.setMainEdge(e2);
				// g.addEdge(e1);
				// g.addEdge(e2);

				first.replaceEdgeInList(myEdge, e1);
				second.replaceEdgeInList(myEdge, e2);

				g.removeEdge(myEdge);

			}
		}*/
	}

	/**
	 * Reduces the number of nodes in the graph by contracting all nodes that have degree 2 and both neighbors also have degree 2 (as long as no parallel edges would result from this) This allows for
	 * more bends than removing all degree-2 nodes
	 * 
	 * @param g
	 *            the Graph on which to perform the contraction
	 */
	static void contractMostDegTwoVertices(Graph g, boolean contractPendant) {

		g.setUserDatum("edgeStatus", "cmdtv", UserData.SHARED);
		// careful! Array may contain references to removed vertices...
		MetroVertex[] vertices = (MetroVertex[]) g.getVertices().toArray(new MetroVertex[0]);
		for (int i = 0; i < vertices.length; i++) {
			MetroVertex v = vertices[i];
			if (v.degree() == 2 && !v.isIntersection()) {
				Iterator nb = v.getNeighbors().iterator();
				MetroVertex first = (MetroVertex) nb.next();
				MetroVertex second = (MetroVertex) nb.next();
				if (!first.isIntersection() && !second.isIntersection() && !(first.isNeighborOf(second))) {
					// can remove v
					MetroEdge toFirst = (MetroEdge) v.findEdge(first);
					MetroEdge toSecond = (MetroEdge) v.findEdge(second);
					int firstLength = toFirst.containsUserDatumKey("length") ? ((Integer) toFirst.getUserDatum("length")).intValue() : 1;
					int secondLength = toSecond.containsUserDatumKey("length") ? ((Integer) toSecond.getUserDatum("length")).intValue() : 1;
					MetroEdge e = (MetroEdge) g.addEdge(new MetroEdge(first, second));

					LinkedList mergedNamesList = new LinkedList();
					List firstNamesList = toFirst.getMyVertexNames();
					if (firstNamesList != null) {
						if (toFirst.getEndpoints().getFirst() != first) {// need to invert the list
							ListIterator it = firstNamesList.listIterator(firstNamesList.size());// end of the list
							while (it.hasPrevious()) {
								String label = (String) it.previous();
								mergedNamesList.add(label);
							}
						} else {
							mergedNamesList.addAll(firstNamesList);
						}
					}
					mergedNamesList.add(v.getName());
					List secondNamesList = toSecond.getMyVertexNames();
					if (secondNamesList != null) {
						if (toSecond.getEndpoints().getFirst() != v) {// need to invert the list
							ListIterator it = secondNamesList.listIterator(secondNamesList.size());// end of the list
							while (it.hasPrevious()) {
								String label = (String) it.previous();
								mergedNamesList.add(label);
							}
						} else {
							mergedNamesList.addAll(secondNamesList);
						}
					}
					e.setMyVertexNames(mergedNamesList);

					Iterator userData = toFirst.getUserDatumKeyIterator();
					while (userData.hasNext()) {
						Object key = userData.next();
						Object value = toFirst.getUserDatum(key);
						e.setUserDatum(key, value, UserData.SHARED);
					}
					userData = toSecond.getUserDatumKeyIterator();
					while (userData.hasNext()) {
						Object key = userData.next();
						Object value = toSecond.getUserDatum(key);
						e.setUserDatum(key, value, UserData.SHARED);
					}
					Boolean[] linesToFirst = (Boolean[]) toFirst.getUserDatum("lineArray");
					Boolean[] linesToSecond = (Boolean[]) toSecond.getUserDatum("lineArray");
					for (int j = 0; j < linesToFirst.length; j++) {
						linesToFirst[j] = Boolean.valueOf(linesToFirst[j].booleanValue() || linesToSecond[j].booleanValue());
					}
					e.setUserDatum("lineArray", linesToFirst, UserData.SHARED);
					e.setPendant(toFirst.isPendant());
					e.setUserDatum("length", new Integer(firstLength + secondLength), UserData.SHARED);
					e.setLength(firstLength + secondLength);
					e.setNumVertices(toFirst.getNumVertices() + toSecond.getNumVertices() + 1);
					e.setMultiplicity(toFirst.getMultiplicity());
					g.removeVertex(v);
				}
			}
		}
		if (contractPendant) {
			vertices = (MetroVertex[]) g.getVertices().toArray(new MetroVertex[0]);
			for (int i = 0; i < vertices.length; i++) {
				MetroVertex v = vertices[i];

				if (v.degree() == 2 && !v.isIntersection()) {
					Iterator nb = v.getNeighbors().iterator();
					MetroVertex first = (MetroVertex) nb.next();
					MetroVertex second = (MetroVertex) nb.next();
					if ((((MetroEdge) first.findEdge(v)).isPendant()) && (((MetroEdge) second.findEdge(v)).isPendant())) { // deg 2 vertex on pendant edge
						MetroEdge toFirst = (MetroEdge) v.findEdge(first);
						MetroEdge toSecond = (MetroEdge) v.findEdge(second);
						int firstLength = toFirst.containsUserDatumKey("length") ? ((Integer) toFirst.getUserDatum("length")).intValue() : 1;
						int secondLength = toSecond.containsUserDatumKey("length") ? ((Integer) toSecond.getUserDatum("length")).intValue() : 1;
						MetroEdge e = (MetroEdge) g.addEdge(new MetroEdge(first, second));
						Iterator userData = toFirst.getUserDatumKeyIterator();
						while (userData.hasNext()) {
							Object key = userData.next();
							Object value = toFirst.getUserDatum(key);
							e.setUserDatum(key, value, UserData.SHARED);
						}
						userData = toSecond.getUserDatumKeyIterator();
						while (userData.hasNext()) {
							Object key = userData.next();
							Object value = toSecond.getUserDatum(key);
							e.setUserDatum(key, value, UserData.SHARED);
						}
						Boolean[] linesToFirst = (Boolean[]) toFirst.getUserDatum("lineArray");
						Boolean[] linesToSecond = (Boolean[]) toSecond.getUserDatum("lineArray");
						for (int j = 0; j < linesToFirst.length; j++) {
							linesToFirst[j] = Boolean.valueOf(linesToFirst[j].booleanValue() || linesToSecond[j].booleanValue());
						}
						e.setUserDatum("lineArray", linesToFirst, UserData.SHARED);
						e.setUserDatum("length", new Integer(firstLength + secondLength), UserData.SHARED);
						e.setNumVertices(toFirst.getNumVertices() + toSecond.getNumVertices() + 1);
						e.setMultiplicity(toFirst.getMultiplicity());
						e.setPendant(true);
						// System.out.println("contracted a node on a pendant edge: " + e.getNumVertices() + " " + e.getUserDatum("length"));
						// System.out.println("from: " + toFirst.getNumVertices() + " " + toFirst.getUserDatum("length"));
						// System.out.println("and " + toSecond.getNumVertices() + " " + toSecond.getUserDatum("length"));

						LinkedList mergedNamesList = new LinkedList();
						List firstNamesList = toFirst.getMyVertexNames();
						if (firstNamesList != null) {
							if (toFirst.getEndpoints().getFirst() != first) {// need to invert the list
								ListIterator it = firstNamesList.listIterator(firstNamesList.size());// end of the list
								while (it.hasPrevious()) {
									String label = (String) it.previous();
									mergedNamesList.add(label);
								}
							} else {
								mergedNamesList.addAll(firstNamesList);
							}
						}
						mergedNamesList.add(v.getName());
						List secondNamesList = toSecond.getMyVertexNames();
						if (secondNamesList != null) {
							if (toSecond.getEndpoints().getFirst() != v) {// need to invert the list
								ListIterator it = secondNamesList.listIterator(secondNamesList.size());// end of the list
								while (it.hasPrevious()) {
									String label = (String) it.previous();
									mergedNamesList.add(label);
								}
							} else {
								mergedNamesList.addAll(secondNamesList);
							}
						}
						e.setMyVertexNames(mergedNamesList);

						g.removeVertex(v);
					}
				}
			}
		}
	}

	/**
	 * Reduces the number of nodes in the graph by contracting all nodes that have degree 2 (as long as no parallel edges would result from this)
	 * 
	 * @param g
	 *            the Graph on which to perform the contraction
	 */
	static void contractDegTwoVertices(Graph g) {
		g.setUserDatum("edgeStatus", "cdtv", UserData.SHARED);
		// careful! Array may contain references to removed vertices...
		MetroVertex[] vertices = (MetroVertex[]) g.getVertices().toArray(new MetroVertex[0]);
		for (int i = 0; i < vertices.length; i++) {
			MetroVertex v = vertices[i];
			if (v.degree() == 2 && !v.isIntersection()) {
				Iterator nb = v.getNeighbors().iterator();
				MetroVertex first = (MetroVertex) nb.next();
				MetroVertex second = (MetroVertex) nb.next();
				if (!(first.isNeighborOf(second))) {
					// can remove v
					MetroEdge toFirst = (MetroEdge) v.findEdge(first);
					MetroEdge toSecond = (MetroEdge) v.findEdge(second);
					int firstLength = toFirst.containsUserDatumKey("length") ? ((Integer) toFirst.getUserDatum("length")).intValue() : 1;
					int secondLength = toSecond.containsUserDatumKey("length") ? ((Integer) toSecond.getUserDatum("length")).intValue() : 1;
					MetroEdge e = (MetroEdge) g.addEdge(new MetroEdge(first, second));
					int firstIndex = first.getIndexOfEdge(toFirst); // has to be done after the new edge is added, otherwise the index is no longer valid
					int secondIndex = second.getIndexOfEdge(toSecond);

					/* update the station name list of this edge */
					LinkedList mergedNamesList = new LinkedList();
					LinkedList mergedLengthsList = new LinkedList();
					List firstNamesList = toFirst.getMyVertexNames();
					List firstLengthsList = toFirst.getMyVertexLengths();
					if (firstNamesList != null) {
						if (toFirst.getEndpoints().getFirst() != first) {// need to invert the list
							ListIterator it = firstNamesList.listIterator(firstNamesList.size());// end of the list
							while (it.hasPrevious()) {
								String label = (String) it.previous();
								mergedNamesList.add(label);
							}
						} else {
							mergedNamesList.addAll(firstNamesList);
						}
					}
					if (firstLengthsList != null) {
						if (toFirst.getEndpoints().getFirst() != first) {// need to invert the list
							ListIterator it = firstLengthsList.listIterator(firstLengthsList.size());// end of the list
							while (it.hasPrevious()) {
								Double labelLength = (Double) it.previous();
								mergedLengthsList.add(labelLength);
							}
						} else {
							mergedLengthsList.addAll(firstLengthsList);
						}
					}
					mergedNamesList.add(v.getName());
					mergedLengthsList.add(new Double(v.getLabelLength()));
					List secondNamesList = toSecond.getMyVertexNames();
					List secondLengthsList = toSecond.getMyVertexLengths();
					if (secondNamesList != null) {
						if (toSecond.getEndpoints().getFirst() != v) {// need to invert the list
							ListIterator it = secondNamesList.listIterator(secondNamesList.size());// end of the list
							while (it.hasPrevious()) {
								String label = (String) it.previous();
								mergedNamesList.add(label);
							}
						} else {
							mergedNamesList.addAll(secondNamesList);
						}
					}
					if (secondLengthsList != null) {
						if (toSecond.getEndpoints().getFirst() != v) {// need to invert the list
							ListIterator it = secondLengthsList.listIterator(secondLengthsList.size());// end of the list
							while (it.hasPrevious()) {
								Double labelLength = (Double) it.previous();
								mergedLengthsList.add(labelLength);
							}
						} else {
							mergedLengthsList.addAll(secondLengthsList);
						}
					}
					e.setMyVertexNames(mergedNamesList);
					e.setMyVertexLengths(mergedLengthsList);
					e.setMultiplicity(toFirst.getMultiplicity());
					e.setNumVertices(toFirst.getNumVertices() + toSecond.getNumVertices() + 1);
					e.setCurrentLabelWidth(Math.max(Math.max(toFirst.getCurrentLabelWidth(), toSecond.getCurrentLabelWidth()), v.getLabelLength() + 0.45));

					Iterator userData = toFirst.getUserDatumKeyIterator();
					while (userData.hasNext()) {
						Object key = userData.next();
						Object value = toFirst.getUserDatum(key);
						e.setUserDatum(key, value, UserData.SHARED);
						// System.out.println("Set " + key + " to " + value);
					}
					userData = toSecond.getUserDatumKeyIterator();
					while (userData.hasNext()) {
						Object key = userData.next();
						Object value = toSecond.getUserDatum(key);
						e.setUserDatum(key, value, UserData.SHARED);
						// System.out.println("Set " + key + " to " + value);
					}
					Boolean[] linesToFirst = (Boolean[]) toFirst.getUserDatum("lineArray");
					Boolean[] linesToSecond = (Boolean[]) toSecond.getUserDatum("lineArray");
					for (int j = 0; j < linesToFirst.length; j++) {
						linesToFirst[j] = Boolean.valueOf(linesToFirst[j].booleanValue() || linesToSecond[j].booleanValue());
					}
					e.setUserDatum("lineArray", linesToFirst, UserData.SHARED);
					e.setUserDatum("length", new Integer(firstLength + secondLength), UserData.SHARED);
					// System.out.println("Set length to " + e.getUserDatum("length") + "\n*********");
					g.removeVertex(v);
					if (firstIndex < 0)
						System.out.println("problem! with edge (" + first.getName() + "<>" + v.getName() + "<>" + second.getName() + ")");
					first.setEdgeIndex(e, firstIndex); // adjust the position in the circular order
					second.setEdgeIndex(e, secondIndex);
				}
			}
		}
	}

	static void writeEPS(Graph g, String filename) throws IOException {
		writeEPS(g, filename, true, false, false);
	}

	public static void writeEPS(Graph g, String filename, boolean leftOut, boolean labelled, boolean boxes) throws IOException {
		final int LARGERSIDE = 480;
		final int MARGIN = 10;
		final int RADIUS = 3;
		final int LINEWIDTH = 2;
		boolean overlapLabels = false;
		boolean labelboxes = boxes;
		boolean classic = false; // draws sharp bends (old version) - otherwise soft bends
		boolean debug = false; // draws all vertices and their id
		double labelLeftOffsetX = 0.4;
		double labelRightOffsetX = 0.5;
		double labelHorOffsetY = -0.15;
		double fontScale = 0.75;
		double tickToLabelX = 0;
		double tickToLabelY = -0.15;
		double boxSpacing = 0.2;
		double boxSpacingX = boxSpacing;
		double boxSpacingY = boxSpacing;
		double boxHeight = 0.75 * fontScale + boxSpacingX + boxSpacingY;

		String prolog = "%!PS-Adobe-3.0 EPSF-3.0\n%%BoundingBox: ";
		// get x and y ranges
		double xMin, xMax, yMin, yMax;
		int llx = 0, lly = 0, urx, ury;
		double scale;
		Iterator it = g.getVertices().iterator();
		MetroVertex mv = (MetroVertex) it.next();
		xMin = xMax = mv.getX();
		yMin = yMax = mv.getY();
		while (it.hasNext()) {
			mv = (MetroVertex) it.next();
			if (mv.getX() < xMin)
				xMin = mv.getX();
			if (mv.getX() > xMax)
				xMax = mv.getX();
			if (mv.getY() < yMin)
				yMin = mv.getY();
			if (mv.getY() > yMax)
				yMax = mv.getY();
		}
		// special cases
		if (xMax == xMin)
			xMax += 1.0;
		if (yMax == yMin)
			yMax += 1.0;
		// calculate bounding box
		if (xMax - xMin > yMax - yMin) {
			urx = LARGERSIDE + 2 * MARGIN;
			ury = (int) Math.ceil(((yMax - yMin) / (xMax - xMin)) * LARGERSIDE) + 2 * MARGIN;
			scale = (LARGERSIDE) / (xMax - xMin);
		} else {
			ury = LARGERSIDE + 2 * MARGIN;
			urx = (int) Math.ceil(((xMax - xMin) / (yMax - yMin)) * LARGERSIDE) + 2 * MARGIN;
			scale = (LARGERSIDE) / (yMax - yMin);
		}
		prolog = prolog.concat(llx + " " + lly + " " + urx + " " + ury);
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(filename)));
		out.write(prolog);
		out.newLine();
		out.newLine();
		out.write("%file created on " + (DateFormat.getDateTimeInstance()).format(new Date()));
		out.newLine();
		out.write("%by metromap.GraphTools.writeEPS()");
		out.newLine();
		out.write("%source network is read from file " + (String) g.getUserDatum("sourceFile"));
		out.newLine();
		out.write("%solution is read from file " + (String) g.getUserDatum("solutionFile"));
		out.newLine();
		out.write("%the total edge length is " + (Double) g.getUserDatum("edgeLength"));
		out.newLine();
		out.write("%the basic cost of sector deviations is " + (Double) g.getUserDatum("sectorDeviation"));
		out.newLine();
		out.write("%the basic bend cost along the lines is " + (Double) g.getUserDatum("bendCost"));
		out.newLine();
		out.write("%the value of the objective function is " + (Double) g.getUserDatum("objective"));
		out.newLine();
		out.write("%the optimality gap is " + (Double) g.getUserDatum("gap"));
		out.newLine();
		out.write("%the weights are: length " + (Integer) g.getUserDatum("lengthFac") + "; sector deviation " + (Integer) g.getUserDatum("deviationFac") + "; bends "
				+ (Integer) g.getUserDatum("bendFac"));
		out.newLine();
		out.write("%the minimum edge length is " + (Integer) g.getUserDatum("minEdgeLength"));
		out.newLine();
		out.write("%the elapsed wall clock time is " + (Double) g.getUserDatum("elapsedTime"));
		out.newLine();
		out.write("%the number of constraints in this model is " + (Integer) g.getUserDatum("numConstraints"));
		out.newLine();
		out.write("%the number of additional cuts for this solution is " + (Integer) g.getUserDatum("addedCuts"));
		out.newLine();
		out.write("%the number of variables in this model is " + (Integer) g.getUserDatum("numVars"));
		out.newLine();
		out.write("%among them " + (Integer) g.getUserDatum("numBinVars") + " binary variables and " + (Integer) g.getUserDatum("numIntVars") + " integer variables");
		out.newLine();

		// out.write("/Helvetica findfont 0.5 scalefont setfont"); out.newLine();
		// 08-07-01: this can deal with Umlauts
		/*
		 * out.write("/Helvetica findfont\n" + "dup length dict begin {1 index /FID ne {def} {pop pop} ifelse} forall\n" + "/Encoding ISOLatin1Encoding def currentdict end\n" +
		 * "/ISOLatin1 exch definefont dup\n" + fontScale + " scalefont setfont"); out.newLine();
		 */

		// allows normal and bold font
		out.write("/FSD {findfont exch scalefont def} bind def\n" + "/Umlaut {dup length dict begin {1 index /FID ne {def} {pop pop} ifelse} forall\n"
				+ "/Encoding ISOLatin1Encoding def currentdict end\n" + "/ISOLatin1 exch definefont dup} def\n" + "/F1 0.75 /Helvetica FSD\n" + "/F2 0.75 /Helvetica-Bold FSD\n" + "F1 Umlaut setfont");
		out.newLine();

		// how to draw stations
		// BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		// System.out.println("how do you want to draw non-interchange stations:");
		// System.out.println("\t[1]\tsame as interchange stations");
		// System.out.println("\t[2]\tblack tick-marks");
		// System.out.println("\t[3]\tdo not draw those stations");
		// char stationType = in.readLine().charAt(0);
		char stationType = '2';

		if (stationType == '3')
			leftOut = false;
		// in = new BufferedReader(new InputStreamReader(System.in));
		// System.out.println("do you want to include label boxes [y|n]?");
		// char c = in.readLine().charAt(0);
		// char c = 'y';
		// if (c=='y') labelboxes = true;

		out.write("/Radius " + RADIUS + " def");
		out.newLine();
		out.write("/Margin " + MARGIN + " def");
		out.newLine();
		out.write("/Linewidth " + LINEWIDTH + " def");
		out.newLine();
		out.write("/Baselength Linewidth " + scale + " div def");
		out.newLine();
		out.write("/Scale " + scale + " def");
		out.newLine();
		out.write("/Radius Radius " + scale + " div def");
		out.newLine();
		out.write("Linewidth " + scale + " div setlinewidth");
		out.newLine();
		out.write("/Stationradius Baselength 4 div 5 mul def");
		out.newLine();
		out.write("/Bendradius Baselength 3 mul def");
		out.newLine();
		out.write("/TickToLabelX " + tickToLabelX + " def");
		out.newLine();
		out.write("/TickToLabelY " + tickToLabelY + " def");
		out.newLine();
		out.write("/BoxSpacingX " + boxSpacingX + " def");
		out.newLine();
		out.write("/BoxSpacingY " + boxSpacingY + " def");
		out.newLine();
		out.write("/BoxHeight " + boxHeight + " def");
		out.newLine();
		out.write("1 setlinecap");
		out.newLine();
		// now let's draw the graph within the box
		out.write("Margin Margin translate");
		out.newLine();
		out.write("Scale Scale scale");
		out.newLine();
		out.write((-xMin) + " " + (-yMin) + " translate");
		out.newLine();
		// now we can use the original coordinates and everything will be inside the bb
		Color[] colors = (Color[]) ((Vector) g.getUserDatum("colors")).toArray(new Color[0]);

		if (classic) {// sharp bends; otherwise soft bends
			it = g.getEdges().iterator();
			while (it.hasNext()) {
				MetroEdge e = (MetroEdge) it.next();
				if (!labelboxes && (e.isLabelParallelDummy() || e.isLabelDirectionDummy() || e.isLabelIntersectionConnectDummy() || e.isLabelIntersectionMainDummy()))
					continue; // draw labelboxes?
				MetroVertex first = (MetroVertex) e.getEndpoints().getFirst();
				MetroVertex second = (MetroVertex) e.getEndpoints().getSecond();

				Boolean[] lines = (Boolean[]) e.getUserDatum("lineArray");
				Vector myColors = new Vector();

				if (lines != null)
					for (int i = 0; i < lines.length; i++) {
						if (lines[i].booleanValue()) {
							myColors.add(colors[i]);
						}
					}

				// no associated line? add default color black
				if (myColors.isEmpty()) {
					myColors.add(Color.LIGHT_GRAY);
				}

				int countLines = myColors.size();
				/*
				 * try { color = (Color)((Vector)g.getUserDatum("colors")).elementAt(myLine); } catch (ArrayIndexOutOfBoundsException aie) { System.out.println("An edge seems to belong to no line"); }
				 */
				if (countLines != 1) {
					// set linewidths to 3/4 x, 2/3 x, 1/2 x for 2, 3, 4+ lines
					if (countLines == 2) {
						out.write("gsave currentlinewidth 4 div 3 mul setlinewidth");
						out.newLine();
					} else if (countLines == 3) {
						out.write("gsave currentlinewidth 3 div 2 mul setlinewidth");
						out.newLine();
					} else {
						out.write("gsave currentlinewidth 2 div setlinewidth");
						out.newLine();
					}
					double dirX = second.getX() - first.getX();
					double dirY = second.getY() - first.getY();
					double firstX = first.getX();
					double firstY = first.getY();
					double secondX = second.getX();
					double secondY = second.getY();

					if (dirX == 0) {
						for (int i = 0; i < countLines; i++) {
							float[] rgb = ((Color) myColors.elementAt(i)).getRGBColorComponents(null);
							out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
							out.newLine();
							out.write(firstX + " " + firstY + " moveto");
							out.newLine();
							out.write((i - (countLines - 1) / 2.0) + " currentlinewidth mul 0 rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						}
					} else if (dirY == 0) {
						for (int i = 0; i < countLines; i++) {
							float[] rgb = ((Color) myColors.elementAt(i)).getRGBColorComponents(null);
							out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
							out.newLine();
							out.write(firstX + " " + firstY + " moveto");
							out.newLine();
							out.write("0 " + (i - (countLines - 1) / 2.0) + " currentlinewidth mul rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						}
					} else if (dirX * dirY > 0) {
						for (int i = 0; i < countLines; i++) {
							float[] rgb = ((Color) myColors.elementAt(i)).getRGBColorComponents(null);
							out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
							out.newLine();
							out.write(firstX + " " + firstY + " moveto");
							out.newLine();
							out.write((i - (countLines - 1) / 2.0) + " currentlinewidth mul .5 sqrt mul -1 mul dup -1 mul rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						}
					} else {
						for (int i = 0; i < countLines; i++) {
							float[] rgb = ((Color) myColors.elementAt(i)).getRGBColorComponents(null);
							out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
							out.newLine();
							out.write(firstX + " " + firstY + " moveto");
							out.newLine();
							out.write((i - (countLines - 1) / 2.0) + " currentlinewidth mul .5 sqrt mul dup rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						}
					}
					out.write("grestore");
					out.newLine();
					if (e.isRegular() && ((first.isTerminus() && first.degree() == 1) || (second.isTerminus() && second.degree() == 1))) {
						float[] rgb = ((Color) myColors.get(0)).getRGBColorComponents(null);
						out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
						out.newLine();
						MetroVertex endVertex = (first.isTerminus() && first.degree() == 1) ? first : second;
						out.write(endVertex.getX() + " " + endVertex.getY() + " moveto");
						out.newLine();
						if (dirX == 0) {
							out.write("Radius 0 rmoveto");
							out.newLine();
							out.write("-2 Radius mul 0 rlineto");
							out.newLine();
						} else if (dirY == 0) {
							out.write("0 Radius rmoveto");
							out.newLine();
							out.write("0 -2 Radius mul rlineto");
							out.newLine();
						} else if (dirX * dirY > 0) {
							out.write(".5 sqrt Radius mul dup -1 mul rmoveto");
							out.newLine();
							out.write(".5 sqrt Radius mul -2 mul dup -1 mul rlineto");
							out.newLine();
						} else {
							out.write(".5 sqrt Radius mul dup rmoveto");
							out.newLine();
							out.write(".5 sqrt Radius mul -2 mul dup rlineto");
							out.newLine();
						}
						out.write("stroke");
						out.newLine();
					}

				} else { // just one line
					float[] rgb = ((Color) myColors.get(0)).getRGBColorComponents(null);
					out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
					out.newLine();
					out.write(first.getX() + " " + first.getY() + " moveto");
					out.newLine();
					// out.write("currentpoint Radius 0 360 arc"); out.newLine();
					// out.write("-1 Radius mul 0 rmoveto"); out.newLine();
					out.write(second.getX() + " " + second.getY() + " lineto");
					out.newLine();
					// out.write("currentpoint Radius 0 360 arc"); out.newLine();
					// out.write("-1 Radius mul 0 rmoveto"); out.newLine();

					if (e.isRegular() && ((first.isTerminus() && first.degree() == 1) || (second.isTerminus() && second.degree() == 1))) {
						MetroVertex endVertex = (first.isTerminus() && first.degree() == 1) ? first : second;
						double dirX = first.getX() - second.getX();
						double dirY = first.getY() - second.getY();
						out.write(endVertex.getX() + " " + endVertex.getY() + " moveto");
						out.newLine();
						if (dirX == 0) {
							out.write("Radius 0 rmoveto");
							out.newLine();
							out.write("-2 Radius mul 0 rlineto");
							out.newLine();
						} else if (dirY == 0) {
							out.write("0 Radius rmoveto");
							out.newLine();
							out.write("0 -2 Radius mul rlineto");
							out.newLine();
						} else if (dirX * dirY > 0) {
							out.write(".5 sqrt Radius mul dup -1 mul rmoveto");
							out.newLine();
							out.write(".5 sqrt Radius mul -2 mul dup -1 mul rlineto");
							out.newLine();
						} else {
							out.write(".5 sqrt Radius mul dup rmoveto");
							out.newLine();
							out.write(".5 sqrt Radius mul -2 mul dup rlineto");
							out.newLine();
						}
					}

					out.write("stroke");
					out.newLine();
				}

			} // end while of drawing the network
		} else {// experimental version trying to use soft bends.
			Set lineSet = GraphTools.getMetroLines(g);
			/*
			 * each multiedge of width k has two available slots for the next line depending on the desired direction these slots are kept in two HashMaps actual drawing takes place along the metro
			 * lines.
			 */
			HashMap<MetroEdge, Integer> originalDirection = new HashMap<MetroEdge, Integer>(g.numEdges());
			HashMap<MetroEdge, Integer> inverseDirection = new HashMap<MetroEdge, Integer>(g.numEdges());
			for (Iterator iter = lineSet.iterator(); iter.hasNext();) {// loop over lines
				MetroPath currentLine = (MetroPath) iter.next();
				if (currentLine.size() == 1) {// a single edge
					MetroEdge myEdge = (MetroEdge) currentLine.getFirst();
					MetroVertex first = (MetroVertex) myEdge.getEndpoints().getFirst();
					MetroVertex second = (MetroVertex) myEdge.getEndpoints().getSecond();

					if (myEdge.getMultiplicity() > 1) {

						// set correct line width
						if (myEdge.getMultiplicity() == 2) {
							out.write("gsave Baselength 0.7 mul setlinewidth");
							out.newLine();
						} else if (myEdge.getMultiplicity() == 3) {
							out.write("gsave Baselength 0.6 mul setlinewidth");
							out.newLine();
						} else {
							out.write("gsave Baselength 0.45 mul setlinewidth");
							out.newLine();
						}
						// find free slot
						Integer slot = originalDirection.get(myEdge);
						if (slot == null) {
							originalDirection.put(myEdge, new Integer(1));
							slot = new Integer(0);
						} else {
							originalDirection.put(myEdge, new Integer(slot.intValue() + 1));
						}
						// retrieve this line's color
						float[] rgb = (colors[currentLine.getColor()]).getRGBColorComponents(null);
						out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
						out.newLine();

						// draw the edge in its slot (slot numbering starts to the left of the oriented edge)
						double dirX = second.getX() - first.getX();
						double dirY = second.getY() - first.getY();

						if (dirX == 0 && dirY > 0) {// upward vertical
							out.write(first.getX() + " " + first.getY() + " moveto");
							out.newLine();
							out.write((slot.intValue() - (myEdge.getMultiplicity() - 1) / 2.0) + " currentlinewidth mul 0 rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						} else if (dirY == 0 && dirX > 0) {// rightward horizontal
							out.write(first.getX() + " " + first.getY() + " moveto");
							out.newLine();
							out.write("0 " + ((myEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) + " currentlinewidth mul rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						} else if (dirX * dirY > 0 && dirX > 0) {// upward /
							out.write(first.getX() + " " + first.getY() + " moveto");
							out.newLine();
							out.write((slot.intValue() - (myEdge.getMultiplicity() - 1) / 2.0) + " currentlinewidth mul 2 sqrt div" + ((myEdge.getMultiplicity() - 1) / 2.0 - slot.intValue())
									+ " currentlinewidth mul 2 sqrt div rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						} else if (dirX * dirY < 0 && dirX > 0) {// downward \
							out.write(first.getX() + " " + first.getY() + " moveto");
							out.newLine();
							out.write(((myEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) + " currentlinewidth mul 2 sqrt div" + ((myEdge.getMultiplicity() - 1) / 2.0 - slot.intValue())
									+ " currentlinewidth mul 2 sqrt div rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						} else if (dirX == 0 && dirY < 0) {// downward vertical
							out.write(first.getX() + " " + first.getY() + " moveto");
							out.newLine();
							out.write(((myEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) + " currentlinewidth mul 0 rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						} else if (dirY == 0 && dirX < 0) {// leftward horizontal
							out.write(first.getX() + " " + first.getY() + " moveto");
							out.newLine();
							out.write("0 " + (slot.intValue() - (myEdge.getMultiplicity() - 1) / 2.0) + " currentlinewidth mul rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						} else if (dirX * dirY > 0 && dirX < 0) {// downward /
							out.write(first.getX() + " " + first.getY() + " moveto");
							out.newLine();
							out.write(((myEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) + " currentlinewidth mul 2 sqrt div" + (slot.intValue() - (myEdge.getMultiplicity() - 1) / 2.0)
									+ " currentlinewidth mul 2 sqrt div rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						} else if (dirX * dirY < 0 && dirX < 0) {// upward \
							out.write(first.getX() + " " + first.getY() + " moveto");
							out.newLine();
							out.write((slot.intValue() - (myEdge.getMultiplicity() - 1) / 2.0) + " currentlinewidth mul 2 sqrt div" + (slot.intValue() - (myEdge.getMultiplicity() - 1) / 2.0)
									+ " currentlinewidth mul 2 sqrt div rmoveto");
							out.newLine();
							out.write(dirX + " " + dirY + " rlineto stroke");
							out.newLine();
						}
						out.write("grestore");
						out.newLine();

					} else { // simple edge
						// retrieve this line's color
						float[] rgb = (colors[currentLine.getColor()]).getRGBColorComponents(null);
						out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
						out.newLine();
						// draw the edge in the only slot
						out.write(first.getX() + " " + first.getY() + " moveto");
						out.newLine();
						out.write(second.getX() + " " + second.getY() + " lineto");
						out.newLine();
						out.write("stroke");
						out.newLine();
					}

				} else {// longer path -- need to use arct operator
					Iterator iterator = currentLine.iterator();
					MetroEdge firstEdge = (MetroEdge) iterator.next();
					MetroEdge previousEdge = firstEdge;
					MetroEdge currentEdge = (MetroEdge) currentLine.get(1);
					MetroVertex first = (MetroVertex) firstEdge.getEndpoints().getFirst();
					MetroVertex second = (MetroVertex) firstEdge.getEndpoints().getSecond();
					Point p1 = null, p2 = null, p3 = null, p4 = null; // actual coordinates for two adjacent multiedges
					double linewidth = 2.0 / scale;
					double currentlinewidth = linewidth;
					if (first.isIncident(currentEdge)) { // turn around if necessary
						second = first;
						first = (MetroVertex) firstEdge.getEndpoints().getSecond();
					}

					float[] rgb = (colors[currentLine.getColor()]).getRGBColorComponents(null);
					out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
					out.newLine();
					out.write(first.getX() + " " + first.getY() + " moveto");
					out.newLine();
					// now we are at the beginning of the path
					if (firstEdge.getMultiplicity() == 1) {
						out.write("Baselength setlinewidth");
						out.newLine();
					}

					if (firstEdge.getMultiplicity() > 1) {
						// set correct line width
						if (firstEdge.getMultiplicity() == 2) {
							out.write("Baselength 0.7 mul setlinewidth");
							out.newLine();
							currentlinewidth = 0.75 * linewidth;
						} else if (firstEdge.getMultiplicity() == 3) {
							out.write("Baselength 0.6 mul setlinewidth");
							out.newLine();
							currentlinewidth = 2.0 / 3.0 * linewidth;
						} else {
							out.write("Baselength 0.45 mul setlinewidth");
							out.newLine();
							currentlinewidth = 0.5 * linewidth;
						}
						// find free slot
						Integer slot;
						if (firstEdge.getEndpoints().getFirst() == first) { // original direction
							slot = originalDirection.get(firstEdge);
							if (slot == null) {
								originalDirection.put(firstEdge, new Integer(1));
								slot = new Integer(0);
							} else {
								originalDirection.put(firstEdge, new Integer(slot.intValue() + 1));
							}
						} else { // inverse direction
							slot = inverseDirection.get(firstEdge);
							if (slot == null) {
								inverseDirection.put(firstEdge, new Integer(1));
								slot = new Integer(0);
							} else {
								inverseDirection.put(firstEdge, new Integer(slot.intValue() + 1));
							}
						}

						// move to the slot starting point (slot numbering starts to the left of the oriented edge)
						double dirX = second.getX() - first.getX();
						double dirY = second.getY() - first.getY();

						if (dirX == 0 && dirY > 0) {// upward vertical
							p1 = new Point(first.getX() + (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth, first.getY());
							p2 = new Point(second.getX() + (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth, second.getY());
							out.write((slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) + " currentlinewidth mul 0 rmoveto");
							out.newLine();
							// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
						} else if (dirY == 0 && dirX > 0) {// rightward horizontal
							p1 = new Point(first.getX(), first.getY() + ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth);
							p2 = new Point(second.getX(), second.getY() + ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth);
							out.write("0 " + ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) + " currentlinewidth mul rmoveto");
							out.newLine();
							// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
						} else if (dirX * dirY > 0 && dirX > 0) {// upward /
							p1 = new Point(first.getX() + (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2), first.getY()
									+ ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2));
							p2 = new Point(second.getX() + (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2), second.getY()
									+ ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2));
							out.write((slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) + " currentlinewidth mul 2 sqrt div " + ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue())
									+ " currentlinewidth mul 2 sqrt div rmoveto");
							out.newLine();
							// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
						} else if (dirX * dirY < 0 && dirX > 0) {// downward \
							p1 = new Point(first.getX() + ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2), first.getY()
									+ ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2));
							p2 = new Point(second.getX() + ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2), second.getY()
									+ ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2));
							out.write(((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) + " currentlinewidth mul 2 sqrt div " + ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue())
									+ " currentlinewidth mul 2 sqrt div rmoveto");
							out.newLine();
							// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
						} else if (dirX == 0 && dirY < 0) {// downward vertical
							p1 = new Point(first.getX() + ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth, first.getY());
							p2 = new Point(second.getX() + ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth, second.getY());
							out.write(((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) + " currentlinewidth mul 0 rmoveto");
							out.newLine();
							// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
						} else if (dirY == 0 && dirX < 0) {// leftward horizontal
							p1 = new Point(first.getX(), first.getY() + (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth);
							p2 = new Point(second.getX(), second.getY() + (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth);
							out.write("0 " + (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) + " currentlinewidth mul rmoveto");
							out.newLine();
							// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
						} else if (dirX * dirY > 0 && dirX < 0) {// downward /
							p1 = new Point(first.getX() + ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2), first.getY()
									+ (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2));
							p2 = new Point(second.getX() + ((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2), second.getY()
									+ (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2));
							out.write(((firstEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) + " currentlinewidth mul 2 sqrt div " + (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0)
									+ " currentlinewidth mul 2 sqrt div rmoveto");
							out.newLine();
							// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
						} else if (dirX * dirY < 0 && dirX < 0) {// upward \
							p1 = new Point(first.getX() + (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2), first.getY()
									+ (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2));
							p2 = new Point(second.getX() + (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2), second.getY()
									+ (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2));
							out.write((slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0) + " currentlinewidth mul 2 sqrt div " + (slot.intValue() - (firstEdge.getMultiplicity() - 1) / 2.0)
									+ " currentlinewidth mul 2 sqrt div rmoveto");
							out.newLine();
							// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
						}
					} else {
						p1 = new Point(first.getX(), first.getY());
						p2 = new Point(second.getX(), second.getY());
					}
					currentEdge = firstEdge;

					for (; iterator.hasNext();) {
						// next edge
						previousEdge = currentEdge;
						currentEdge = (MetroEdge) iterator.next();

						first = second;
						second = (MetroVertex) currentEdge.getOpposite(first);

						// find next points
						currentlinewidth = linewidth;
						Integer slot = new Integer(0);
						if (currentEdge.getMultiplicity() > 1) {
							// set correct line width
							if (currentEdge.getMultiplicity() == 2) {
								currentlinewidth = 0.75 * linewidth;
							} else if (currentEdge.getMultiplicity() == 3) {
								currentlinewidth = 2.0 / 3.0 * linewidth;
							} else {
								currentlinewidth = 0.5 * linewidth;
							}
							// find free slot
							if (currentEdge.getEndpoints().getFirst() == first) { // original direction
								slot = originalDirection.get(currentEdge);
								if (slot == null) {
									originalDirection.put(currentEdge, new Integer(1));
									slot = new Integer(0);
								} else {
									originalDirection.put(currentEdge, new Integer(slot.intValue() + 1));
								}
							} else { // inverse direction
								slot = inverseDirection.get(currentEdge);
								if (slot == null) {
									inverseDirection.put(currentEdge, new Integer(1));
									slot = new Integer(0);
								} else {
									inverseDirection.put(currentEdge, new Integer(slot.intValue() + 1));
								}
							}

							// find the slot starting point (slot numbering starts to the left of the oriented edge)
							double dirX = second.getX() - first.getX();
							double dirY = second.getY() - first.getY();

							if (dirX == 0 && dirY > 0) {// upward vertical
								p3 = new Point(first.getX() + (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth, first.getY());
								p4 = new Point(second.getX() + (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth, second.getY());
								// out.write((slot.intValue() - (currentEdge.getMultiplicity()-1)/2.0) + " currentlinewidth mul 0 rmoveto");out.newLine();
								// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
							} else if (dirY == 0 && dirX > 0) {// rightward horizontal
								p3 = new Point(first.getX(), first.getY() + ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth);
								p4 = new Point(second.getX(), second.getY() + ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth);
								// out.write("0 " + ((currentEdge.getMultiplicity()-1)/2.0 - slot.intValue()) + " currentlinewidth mul rmoveto");out.newLine();
								// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
							} else if (dirX * dirY > 0 && dirX > 0) {// upward /
								p3 = new Point(first.getX() + (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2), first.getY()
										+ ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2));
								p4 = new Point(second.getX() + (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2), second.getY()
										+ ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2));
								// out.write((slot.intValue() - (currentEdge.getMultiplicity()-1)/2.0) + " currentlinewidth mul 2 sqrt div" + ((currentEdge.getMultiplicity()-1)/2.0 - slot.intValue())
								// + " currentlinewidth mul 2 sqrt div rmoveto");out.newLine();
								// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
							} else if (dirX * dirY < 0 && dirX > 0) {// downward \
								p3 = new Point(first.getX() + ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2), first.getY()
										+ ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2));
								p4 = new Point(second.getX() + ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2), second.getY()
										+ ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2));
								// out.write(((currentEdge.getMultiplicity()-1)/2.0 - slot.intValue()) + " currentlinewidth mul 2 sqrt div" + ((currentEdge.getMultiplicity()-1)/2.0 - slot.intValue())
								// + " currentlinewidth mul 2 sqrt div rmoveto");out.newLine();
								// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
							} else if (dirX == 0 && dirY < 0) {// downward vertical
								p3 = new Point(first.getX() + ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth, first.getY());
								p4 = new Point(second.getX() + ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth, second.getY());
								// out.write(((currentEdge.getMultiplicity()-1)/2.0 - slot.intValue()) + " currentlinewidth mul 0 rmoveto");out.newLine();
								// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
							} else if (dirY == 0 && dirX < 0) {// leftward horizontal
								p3 = new Point(first.getX(), first.getY() + (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth);
								p4 = new Point(second.getX(), second.getY() + (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth);
								// out.write("0 " + (slot.intValue() - (currentEdge.getMultiplicity()-1)/2.0) + " currentlinewidth mul rmoveto");out.newLine();
								// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
							} else if (dirX * dirY > 0 && dirX < 0) {// downward /
								p3 = new Point(first.getX() + ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2), first.getY()
										+ (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2));
								p4 = new Point(second.getX() + ((currentEdge.getMultiplicity() - 1) / 2.0 - slot.intValue()) * currentlinewidth / Math.sqrt(2), second.getY()
										+ (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2));
								// out.write(((currentEdge.getMultiplicity()-1)/2.0 - slot.intValue()) + " currentlinewidth mul 2 sqrt div" + (slot.intValue() - (currentEdge.getMultiplicity()-1)/2.0)
								// + " currentlinewidth mul 2 sqrt div rmoveto");out.newLine();
								// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
							} else if (dirX * dirY < 0 && dirX < 0) {// upward \
								p3 = new Point(first.getX() + (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2), first.getY()
										+ (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2));
								p4 = new Point(second.getX() + (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2), second.getY()
										+ (slot.intValue() - (currentEdge.getMultiplicity() - 1) / 2.0) * currentlinewidth / Math.sqrt(2));
								// out.write((slot.intValue() - (currentEdge.getMultiplicity()-1)/2.0) + " currentlinewidth mul 2 sqrt div" + (slot.intValue() - (currentEdge.getMultiplicity()-1)/2.0)
								// + " currentlinewidth mul 2 sqrt div rmoveto");out.newLine();
								// out.write(dirX + " " + dirY + " rlineto stroke");out.newLine();
							}
						} else {
							p3 = new Point(first.getX(), first.getY());
							p4 = new Point(second.getX(), second.getY());
						}

						if (first.isIntersection()) {// just a straight line, no bend
							out.write(p2.getX() + " " + p2.getY() + " lineto");
							out.newLine();// connect to the target point
							out.write(p3.getX() + " " + p3.getY() + " moveto");
							out.newLine();// move to correct next point

						} else {
							// do we make a turn?
							if ((previousEdge.getDirection() - currentEdge.getDirection()) % 4 != 0) {// turn
								Point intersection = findIntersection(p1, p2, p3, p4);
								if (intersection == null) {
									System.out.println("something's wrong: " + p1 + p2 + p3 + p4);
									System.out.println("directions: " + previousEdge.getDirection() + " " + currentEdge.getDirection());
								}
								int turn = getTurnDirection(p1, intersection, p4);
								int mySlot = 0;
								if (turn == -1) {// left turn; use slot directly
									mySlot = slot;
								} else if (turn == 1) {
									mySlot = currentEdge.getMultiplicity() - slot;
								}
								out.write(intersection.getX() + " " + intersection.getY() + " " + p4.getX() + " " + p4.getY() + " Bendradius currentlinewidth " + mySlot + " mul add arct");
								out.newLine();
							} else {// no turn
								out.write(p2.getX() + " " + p2.getY() + " lineto");
								out.newLine();
							}
							/*
							 * this we had before reading the discrete direction varibles from cplex; had some strange behaviour for parallel segments. //find intersection point Point intersection =
							 * findIntersection(p1, p2, p3, p4); if (intersection != null) {//we have a bend here int turn = getTurnDirection(p1, intersection, p4); int mySlot = 0; if (turn==-1)
							 * {//left turn; use slot directly mySlot = slot; } else if (turn==1) { mySlot = currentEdge.getMultiplicity() - slot; } out.write("currentpoint " + intersection.getX() +
							 * " " + intersection.getY() + " moveto .66 Radius mul 0 rmoveto -1.32 Radius mul 0 rlineto moveto "); out.write(intersection.getX() + " " + intersection.getY() + " " +
							 * p4.getX() + " " + p4.getY() + " Bendradius currentlinewidth " + mySlot + " mul add arct");out.newLine(); } else {//lines are parallel and no intersection => p2==p3
							 * out.write(p2.getX() + " " + p2.getY() + " lineto");out.newLine(); }
							 */
						}
						// draw it but keep currentpath alive
						out.write("currentpoint stroke moveto");
						out.newLine();

						// shift points
						p1 = p3;
						p2 = p4;
						// set next linewidth
						if (currentEdge.getMultiplicity() == 1) {
							out.write("Baselength setlinewidth");
							out.newLine();
						} else if (currentEdge.getMultiplicity() == 2) {
							out.write("Baselength 0.7 mul setlinewidth");
							out.newLine();
						} else if (currentEdge.getMultiplicity() == 3) {
							out.write("Baselength 0.6 mul setlinewidth");
							out.newLine();
						} else {
							out.write("Baselength 0.45 mul setlinewidth");
							out.newLine();
						}

					}
					// last edge
					first = second;
					second = (MetroVertex) currentEdge.getOpposite(first);

					// we just need a lineto to the last point
					out.write(p2.getX() + " " + p2.getY() + " lineto");
					out.newLine();
					out.write("stroke");
					out.newLine();
				}
			}// end drawing network

			if (labelboxes) {// don't forget to draw the boxes
				it = g.getEdges().iterator();
				while (it.hasNext()) {
					MetroEdge e = (MetroEdge) it.next();
					if (!(e.isLabelParallelDummy() || e.isLabelDirectionDummy() || e.isLabelIntersectionConnectDummy() || e.isLabelIntersectionMainDummy()))
						continue; // draw labelboxes?
					MetroVertex first = (MetroVertex) e.getEndpoints().getFirst();
					MetroVertex second = (MetroVertex) e.getEndpoints().getSecond();

					float[] rgb = Color.LIGHT_GRAY.getRGBColorComponents(null);
					out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
					out.newLine();
					out.write(first.getX() + " " + first.getY() + " moveto");
					out.newLine();
					// out.write("currentpoint Radius 0 360 arc"); out.newLine();
					// out.write("-1 Radius mul 0 rmoveto"); out.newLine();
					out.write(second.getX() + " " + second.getY() + " lineto");
					out.newLine();
					// out.write("currentpoint Radius 0 360 arc"); out.newLine();
					// out.write("-1 Radius mul 0 rmoveto"); out.newLine();

					out.write("stroke");
					out.newLine();
				}
			}
		}

		/*
		 * Draw ticks and circles
		 */
		out.write("newpath");
		out.newLine();
		out.write("0 0 0 setrgbcolor");
		out.newLine();
		out.write("0 setlinecap");
		out.newLine();
		out.write("currentlinewidth 2 div setlinewidth");
		out.newLine(); // thinner circles and ticks; width = 0.5 linewidth
		it = g.getVertices().iterator();
		while (it.hasNext()) {
			MetroVertex v = (MetroVertex) it.next();
			if (!(v.isDummy() || v.isLabelVertex() || v.isJoint())) {
				if (v.degree() > 2 || v.isIntersection() || stationType == '1') { // draw circle
					out.write(v.getX() + " " + v.getY() + " moveto");
					out.newLine();
					out.write("currentpoint Stationradius 0 rmoveto");
					out.newLine();
					out.write("Stationradius 0 360 arc");
					out.newLine();
					if (overlapLabels) {
						out.write(v.getX() + " TickToLabelX 1.5 mul add " + v.getY() + " moveto");
						out.newLine();
						out.write("(" + v.getName() + ") show");
						out.newLine();
					}
					// out.write("gsave stroke grestore");
					// out.write("gsave 255 255 255 setrgbcolor fill grestore"); out.newLine();
				} else if (v.isTerminus() && v.degree() == 1 && !labelled) { // draw thicker terminus tick (unlabeled only)
					// draws a black tick in termini

					MetroVertex neighbor = (MetroVertex) v.getNeighbors().iterator().next();
					double dirX = v.getX() - neighbor.getX();
					double dirY = v.getY() - neighbor.getY();
					out.write(v.getX() + " " + v.getY() + " moveto");
					out.newLine();
					if (dirX == 0) {
						out.write("Radius 0 rmoveto");
						out.newLine();
						out.write("-2 Radius mul 0 rlineto");
						out.newLine();
					} else if (dirY == 0) {
						out.write("0 Radius rmoveto");
						out.newLine();
						out.write("0 -2 Radius mul rlineto");
						out.newLine();
					} else if (dirX * dirY > 0) {
						out.write(".5 sqrt Radius mul dup -1 mul rmoveto");
						out.newLine();
						out.write(".5 sqrt Radius mul -2 mul dup -1 mul rlineto");
						out.newLine();
					} else {
						out.write(".5 sqrt Radius mul dup rmoveto");
						out.newLine();
						out.write(".5 sqrt Radius mul -2 mul dup rlineto");
						out.newLine();
					}
					if (overlapLabels) {
						out.write(v.getX() + " TickToLabelX 1.5 mul add " + v.getY() + " moveto");
						out.newLine();
						out.write("(" + v.getName() + ") show");
						out.newLine();
					}
					out.write("gsave currentlinewidth 2 mul setlinewidth stroke grestore");
					out.newLine();
				} else if (!v.isTerminus() && stationType != '3' && !((String) g.getUserDatum("edgeStatus")).equals("cmdtv")) {
					Iterator neighbors = v.getNeighbors().iterator(); // is of size 2
					MetroVertex first = (MetroVertex) neighbors.next();
					MetroVertex second = (MetroVertex) neighbors.next();
					double dirX, dirY;
					if (first.degree() != 2) {
						dirX = (second.getX() - v.getX());
						dirY = (second.getY() - v.getY());
					} else {
						dirX = (first.getX() - v.getX());
						dirY = (first.getY() - v.getY());
					}
					out.write(v.getX() + " " + v.getY() + " moveto");
					out.newLine();
					if (dirX == 0) {
						out.write(".66 Radius mul 0 rmoveto");
						out.newLine();
						out.write("-1.32 Radius mul 0 rlineto");
						out.newLine();
					} else if (dirY == 0) {
						out.write("0 .66 Radius mul rmoveto");
						out.newLine();
						out.write("0 -1.32 Radius mul rlineto");
						out.newLine();
					} else if (dirX * dirY > 0) {
						out.write(".5 sqrt .66 Radius mul mul dup -1 mul rmoveto");
						out.newLine();
						out.write(".5 sqrt .66 Radius mul mul -2 mul dup -1 mul rlineto");
						out.newLine();
					} else {
						out.write(".5 sqrt .66 Radius mul mul dup rmoveto");
						out.newLine();
						out.write(".5 sqrt .66 Radius mul mul -2 mul dup rlineto");
						out.newLine();
					}
					if (overlapLabels) {
						out.write(v.getX() + " TickToLabelX 1.5 mul add " + v.getY() + " moveto");
						out.newLine();
						out.write("(" + v.getName() + ") show");
						out.newLine();
					}
				}
			}
		}
		out.write("gsave 255 255 255 setrgbcolor fill grestore stroke");
		out.newLine();

		if (((String) g.getUserDatum("edgeStatus")).equals("cmdtv") && stationType == '2') { // draw ticks on contracted edges with 3 segments
			it = g.getEdges().iterator();
			HashSet visitedEdges = new HashSet();
			while (it.hasNext()) {
				MetroEdge e1 = (MetroEdge) it.next();
				if (!visitedEdges.contains(e1)) {
					MetroEdge e2, e3;
					MetroVertex e1First = (MetroVertex) e1.getEndpoints().getFirst();
					MetroVertex e1Second = (MetroVertex) e1.getEndpoints().getSecond();
					if ((e1First.isIntersection() && e1Second.degree() == 2 && !e1Second.isIntersection() && !e1Second.isJoint())
							|| (e1Second.isIntersection() && e1First.degree() == 2 && !e1First.isIntersection() && !e1First.isJoint())) { // found edge able to serve as starting edge on a 2 or 3 edge
																																			// path
						MetroVertex connect12 = (e1First.degree() == 2 && !e1First.isIntersection()) ? e1First : e1Second;
						MetroVertex start = (MetroVertex) e1.getOpposite(connect12);
						Iterator nextEdge = connect12.getIncidentEdges().iterator();
						e2 = (MetroEdge) nextEdge.next();
						if (e1 == e2)
							e2 = (MetroEdge) nextEdge.next();
						if (e2.getOpposite(connect12).degree() == 2 && !((MetroVertex) e2.getOpposite(connect12)).isIntersection()) { // e2 is the middle edge, so get e3
							MetroVertex connect23 = (MetroVertex) e2.getOpposite(connect12);
							nextEdge = connect23.getIncidentEdges().iterator();
							e3 = (MetroEdge) nextEdge.next();
							if (e2 == e3)
								e3 = (MetroEdge) nextEdge.next();
							MetroVertex end = (MetroVertex) e3.getOpposite(connect23);
							// get minimum length of this path
							int minLength;
							if (e1.getUserDatum("length") != null) {
								minLength = ((Integer) e1.getUserDatum("length")).intValue();
							} else {
								minLength = 1;
							}
							if (e2.getUserDatum("length") != null) {
								minLength += ((Integer) e2.getUserDatum("length")).intValue();
							} else {
								minLength++;
							}
							if (e3.getUserDatum("length") != null) {
								minLength += ((Integer) e3.getUserDatum("length")).intValue();
							} else {
								minLength++;
							}
							// get real length
							double e1Length = Math.max(Math.abs(connect12.getX() - start.getX()), Math.abs(connect12.getY() - start.getY()));
							double e2Length = Math.max(Math.abs(connect23.getX() - connect12.getX()), Math.abs(connect23.getY() - connect12.getY()));
							double e3Length = Math.max(Math.abs(end.getX() - connect23.getX()), Math.abs(end.getY() - connect23.getY()));

							double unit = (e1Length + e2Length + e3Length) / minLength;

							// start on e1
							int verticesToDraw1 = (int) (e1Length / unit);
							double dirX = (connect12.getX() - start.getX()) / e1Length;
							double dirY = (connect12.getY() - start.getY()) / e1Length;
							for (int i = 1; i <= verticesToDraw1; i++) {
								out.write((start.getX() + i * unit * dirX) + " " + (start.getY() + i * unit * dirY) + " moveto");
								out.newLine();
								if (dirX == 0) {
									out.write(".66 Radius mul 0 rmoveto");
									out.newLine();
									out.write("-1.32 Radius mul 0 rlineto");
									out.newLine();
								} else if (dirY == 0) {
									out.write("0 .66 Radius mul rmoveto");
									out.newLine();
									out.write("0 -1.32 Radius mul rlineto");
									out.newLine();
								} else if (dirX * dirY > 0) {
									out.write(".5 sqrt .66 Radius mul mul dup -1 mul rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup -1 mul rlineto");
									out.newLine();
								} else {
									out.write(".5 sqrt .66 Radius mul mul dup rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup rlineto");
									out.newLine();
								}
							}
							double remainder = e1Length % unit;
							int verticesToDraw2 = (int) ((e2Length + remainder) / unit);
							dirX = (connect23.getX() - connect12.getX()) / e2Length;
							dirY = (connect23.getY() - connect12.getY()) / e2Length;
							for (int i = 1; i <= verticesToDraw2; i++) {
								out.write((connect12.getX() + (i * unit - remainder) * dirX) + " " + (connect12.getY() + (i * unit - remainder) * dirY) + " moveto");
								out.newLine();
								if (dirX == 0) {
									out.write(".66 Radius mul 0 rmoveto");
									out.newLine();
									out.write("-1.32 Radius mul 0 rlineto");
									out.newLine();
								} else if (dirY == 0) {
									out.write("0 .66 Radius mul rmoveto");
									out.newLine();
									out.write("0 -1.32 Radius mul rlineto");
									out.newLine();
								} else if (dirX * dirY > 0) {
									out.write(".5 sqrt .66 Radius mul mul dup -1 mul rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup -1 mul rlineto");
									out.newLine();
								} else {
									out.write(".5 sqrt .66 Radius mul mul dup rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup rlineto");
									out.newLine();
								}
							}
							remainder = (e2Length + remainder) % unit;
							int verticesToDraw3 = (minLength - 1) - verticesToDraw1 - verticesToDraw2; // rounding errors forced me to calculate it this way for the last edge
							dirX = (end.getX() - connect23.getX()) / e3Length;
							dirY = (end.getY() - connect23.getY()) / e3Length;
							for (int i = 1; i <= verticesToDraw3; i++) {
								out.write((connect23.getX() + (i * unit - remainder) * dirX) + " " + (connect23.getY() + (i * unit - remainder) * dirY) + " moveto");
								out.newLine();
								if (dirX == 0) {
									out.write(".66 Radius mul 0 rmoveto");
									out.newLine();
									out.write("-1.32 Radius mul 0 rlineto");
									out.newLine();
								} else if (dirY == 0) {
									out.write("0 .66 Radius mul rmoveto");
									out.newLine();
									out.write("0 -1.32 Radius mul rlineto");
									out.newLine();
								} else if (dirX * dirY > 0) {
									out.write(".5 sqrt .66 Radius mul mul dup -1 mul rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup -1 mul rlineto");
									out.newLine();
								} else {
									out.write(".5 sqrt .66 Radius mul mul dup rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup rlineto");
									out.newLine();
								}
							}

							visitedEdges.add(e1);
							visitedEdges.add(e2);
							visitedEdges.add(e3);
						} else { // only two edges between intersections
							MetroVertex end = (MetroVertex) e2.getOpposite(connect12);
							// get minimum length of this path
							int minLength;
							if (e1.getUserDatum("length") != null) {
								minLength = ((Integer) e1.getUserDatum("length")).intValue();
							} else {
								minLength = 1;
							}
							if (e2.getUserDatum("length") != null) {
								minLength += ((Integer) e2.getUserDatum("length")).intValue();
							} else {
								minLength++;
							}
							// get real length
							double e1Length = Math.max(Math.abs(connect12.getX() - start.getX()), Math.abs(connect12.getY() - start.getY()));
							double e2Length = Math.max(Math.abs(end.getX() - connect12.getX()), Math.abs(end.getY() - connect12.getY()));

							double unit = (e1Length + e2Length) / minLength;

							// start on e1
							int verticesToDraw = (int) (e1Length / unit);
							double dirX = (connect12.getX() - start.getX()) / e1Length;
							double dirY = (connect12.getY() - start.getY()) / e1Length;
							for (int i = 1; i <= verticesToDraw; i++) {
								out.write((start.getX() + i * unit * dirX) + " " + (start.getY() + i * unit * dirY) + " moveto");
								out.newLine();
								if (dirX == 0) {
									out.write(".66 Radius mul 0 rmoveto");
									out.newLine();
									out.write("-1.32 Radius mul 0 rlineto");
									out.newLine();
								} else if (dirY == 0) {
									out.write("0 .66 Radius mul rmoveto");
									out.newLine();
									out.write("0 -1.32 Radius mul rlineto");
									out.newLine();
								} else if (dirX * dirY > 0) {
									out.write(".5 sqrt .66 Radius mul mul dup -1 mul rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup -1 mul rlineto");
									out.newLine();
								} else {
									out.write(".5 sqrt .66 Radius mul mul dup rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup rlineto");
									out.newLine();
								}
							}
							double remainder = e1Length % unit;
							verticesToDraw = (minLength - 1) - verticesToDraw; // rounding errors forced me to calculate it this way...
							dirX = (end.getX() - connect12.getX()) / e2Length;
							dirY = (end.getY() - connect12.getY()) / e2Length;
							for (int i = 1; i <= verticesToDraw; i++) {
								out.write((connect12.getX() + (i * unit - remainder) * dirX) + " " + (connect12.getY() + (i * unit - remainder) * dirY) + " moveto");
								out.newLine();
								if (dirX == 0) {
									out.write(".66 Radius mul 0 rmoveto");
									out.newLine();
									out.write("-1.32 Radius mul 0 rlineto");
									out.newLine();
								} else if (dirY == 0) {
									out.write("0 .66 Radius mul rmoveto");
									out.newLine();
									out.write("0 -1.32 Radius mul rlineto");
									out.newLine();
								} else if (dirX * dirY > 0) {
									out.write(".5 sqrt .66 Radius mul mul dup -1 mul rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup -1 mul rlineto");
									out.newLine();
								} else {
									out.write(".5 sqrt .66 Radius mul mul dup rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup rlineto");
									out.newLine();
								}
							}

							visitedEdges.add(e1);
							visitedEdges.add(e2);
						}
					} else if (e1.isPendant()) { // found single pendant edge
						// check if e1 has a neighbor that covers e1
						if ((e1First.isTerminus() && e1Second.isIntersection()) || (e1Second.isTerminus() && e1First.isIntersection())) {

							int length = e1.containsUserDatumKey("length") ? ((Integer) e1.getUserDatum("length")).intValue() : 1;
							if (length > 1) {
								double firstX = e1First.getX();
								double firstY = e1First.getY();
								double secondX = e1Second.getX();
								double secondY = e1Second.getY();
								double firstSecondX = (secondX - firstX) / length;
								double firstSecondY = (secondY - firstY) / length;
								if (stationType == '1') {
									// creates circles
									for (int i = 1; i < length; i++) {
										out.write((firstX + i * firstSecondX) + " " + (firstY + i * firstSecondY) + " moveto");
										out.newLine();
										out.write("currentpoint Stationradius 0 rmoveto");
										out.newLine();
										out.write("Stationradius 0 360 arc");
										out.newLine();
									}
								} else if (stationType == '2') {
									// creates tickmarks
									for (int i = 1; i < length; i++) {
										out.write((firstX + i * firstSecondX) + " " + (firstY + i * firstSecondY) + " moveto");
										out.newLine();
										if (firstSecondX == 0) {
											out.write(".66 Radius mul 0 rmoveto");
											out.newLine();
											out.write("-1.32 Radius mul 0 rlineto");
											out.newLine();
										} else if (firstSecondY == 0) {
											out.write("0 .66 Radius mul rmoveto");
											out.newLine();
											out.write("0 -1.32 Radius mul rlineto");
											out.newLine();
										} else if (firstSecondX * firstSecondY > 0) {
											out.write(".5 sqrt .66 Radius mul mul dup -1 mul rmoveto");
											out.newLine();
											out.write(".5 sqrt .66 Radius mul mul -2 mul dup -1 mul rlineto");
											out.newLine();
										} else {
											out.write(".5 sqrt .66 Radius mul mul dup rmoveto");
											out.newLine();
											out.write(".5 sqrt .66 Radius mul mul -2 mul dup rlineto");
											out.newLine();
										}
									}
								}
							}
						}

					}
				}
			}
			out.write("stroke");
			out.newLine();
		} else if (leftOut) {
			if (!labelled) {
				// draw left out vertices
				it = g.getEdges().iterator();
				while (it.hasNext()) {
					Edge e = (Edge) it.next();
					int length = e.containsUserDatumKey("length") ? ((Integer) e.getUserDatum("length")).intValue() : 1;
					if (length > 1) {
						MetroVertex first = (MetroVertex) e.getEndpoints().getFirst();
						MetroVertex second = (MetroVertex) e.getEndpoints().getSecond();
						double firstX = first.getX();
						double firstY = first.getY();
						double secondX = second.getX();
						double secondY = second.getY();
						double firstSecondX = (secondX - firstX) / length;
						double firstSecondY = (secondY - firstY) / length;
						if (stationType == '1') {
							// creates circles
							for (int i = 1; i < length; i++) {
								out.write((firstX + i * firstSecondX) + " " + (firstY + i * firstSecondY) + " moveto");
								out.newLine();
								out.write("currentpoint Stationradius 0 rmoveto");
								out.newLine();
								out.write("Stationradius 0 360 arc");
								out.newLine();
							}
						} else if (stationType == '2') {
							// creates tickmarks
							for (int i = 1; i < length; i++) {
								out.write((firstX + i * firstSecondX) + " " + (firstY + i * firstSecondY) + " moveto");
								out.newLine();
								if (firstSecondX == 0) {
									out.write(".66 Radius mul 0 rmoveto");
									out.newLine();
									out.write("-1.32 Radius mul 0 rlineto");
									out.newLine();
								} else if (firstSecondY == 0) {
									out.write("0 .66 Radius mul rmoveto");
									out.newLine();
									out.write("0 -1.32 Radius mul rlineto");
									out.newLine();
								} else if (firstSecondX * firstSecondY > 0) {
									out.write(".5 sqrt .66 Radius mul mul dup -1 mul rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup -1 mul rlineto");
									out.newLine();
								} else {
									out.write(".5 sqrt .66 Radius mul mul dup rmoveto");
									out.newLine();
									out.write(".5 sqrt .66 Radius mul mul -2 mul dup rlineto");
									out.newLine();
								}
							}
						}
					}
				}
				out.write("stroke");
				out.newLine();
			} else { // have a labeled drawing

				for (Iterator iter = g.getEdges().iterator(); iter.hasNext();) {
					MetroEdge edge = (MetroEdge) iter.next();
					if (!(edge.isLabelMainDummy() || edge.isLabelIntersectionMainDummy() || edge.isSkippedLabelEdge()))
						continue; // nothing to do for non-main edges

					if (edge.isLabelIntersectionMainDummy()) { // we have a label modeled as a single edge
						MetroVertex first = (MetroVertex) edge.getEndpoints().getFirst();
						MetroVertex second = (MetroVertex) edge.getEndpoints().getSecond();
						String label = (String) edge.getMyVertexNames().get(0);
						boolean isSingleEdgeLabel = true;

						// draw terminus boxes
						MetroVertex innerLabelVertex = (first.degree() == 1) ? second : first; // find the network vertex
						MetroVertex networkVertex = innerLabelVertex;
						MetroVertex outerLabelVertex = (MetroVertex) edge.getOpposite(innerLabelVertex);
						MetroEdge connectorEdge = edge;
						if (innerLabelVertex.isLabelVertex() && innerLabelVertex.degree() == 2) {// have a label as a pair of edges
							isSingleEdgeLabel = false;
							Iterator<MetroEdge> edgeIt = innerLabelVertex.getIncidentEdges().iterator();
							connectorEdge = edgeIt.next();
							if (connectorEdge == edge) {
								connectorEdge = edgeIt.next();
							}
							// now connectorEdge is the LableIntersectionConnectEdge
							networkVertex = (MetroVertex) connectorEdge.getOpposite(innerLabelVertex);
							// now networkVertex is the station of G
						}
						if (networkVertex.isTerminus()) {// draw a box around it

							// draw tick if necessary (example: wien)
							if (networkVertex.degree() == 2) {
								Iterator<MetroEdge> eit = networkVertex.getIncidentEdges().iterator();
								MetroEdge otherEdge = eit.next();
								if (otherEdge == connectorEdge)
									otherEdge = eit.next();

								if (otherEdge.getNumVertices() == 0) {// pendant edge with only one station
									double firstSecondX = networkVertex.getX() - ((MetroVertex) otherEdge.getOpposite(networkVertex)).getX();
									double firstSecondY = networkVertex.getY() - ((MetroVertex) otherEdge.getOpposite(networkVertex)).getY();
									out.write(networkVertex.getX() + " " + networkVertex.getY() + " moveto");
									out.newLine();
									if (firstSecondX == 0) {
										out.write(".8 Radius mul 0 rmoveto");
										out.newLine();
										out.write("-1.6 Radius mul 0 rlineto");
										out.newLine();
									} else if (firstSecondY == 0) {
										out.write("0 .8 Radius mul rmoveto");
										out.newLine();
										out.write("0 -1.6 Radius mul rlineto");
										out.newLine();
									} else if (firstSecondX * firstSecondY > 0) {
										out.write(".5 sqrt .8 Radius mul mul dup -1 mul rmoveto");
										out.newLine();
										out.write(".5 sqrt .8 Radius mul mul -2 mul dup -1 mul rlineto");
										out.newLine();
									} else {
										out.write(".5 sqrt .8 Radius mul mul dup rmoveto");
										out.newLine();
										out.write(".5 sqrt .8 Radius mul mul -2 mul dup rlineto");
										out.newLine();
									}
									out.write("gsave currentlinewidth 2 mul setlinewidth stroke grestore");
									out.newLine();
								}
							}

							if (false) {
								Color myColor = networkVertex.getColor();
								float[] rgb = myColor.getRGBColorComponents(null);

								out.write("gsave");
								out.newLine();
								out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
								out.newLine();
								out.write(innerLabelVertex.getX() + " " + innerLabelVertex.getY() + " moveto");
								out.newLine();

								if (innerLabelVertex.getY() == outerLabelVertex.getY()) {// horizontal label
									if ((outerLabelVertex.getX() - innerLabelVertex.getX()) > 0) { // to the right
										if (isSingleEdgeLabel) {
											out.write("TickToLabelX TickToLabelY 1.5 mul BoxSpacingY sub rmoveto");
											out.newLine();
										} else {
											out.write("-1 BoxSpacingX mul TickToLabelY 1.5 mul BoxSpacingY sub rmoveto");
											out.newLine();
										}
										out.write("0 BoxHeight rlineto");
										out.newLine();
										out.write(networkVertex.getLabelLength() + " BoxSpacingX 2 mul add 0 rlineto");
										out.newLine();
										out.write("0 -1 BoxHeight mul rlineto");
										out.newLine();
										out.write("closepath stroke grestore");
										out.newLine();

									} else { // to the left
										if (isSingleEdgeLabel) {
											out.write("-1 TickToLabelX mul TickToLabelY 1.5 mul BoxSpacingY sub rmoveto");
											out.newLine();
										} else {
											out.write("BoxSpacingX TickToLabelY 1.5 mul BoxSpacingY sub rmoveto");
											out.newLine();
										}
										out.write("0 BoxHeight rlineto");
										out.newLine();
										out.write((-networkVertex.getLabelLength()) + " BoxSpacingX -2 mul add 0 rlineto");
										out.newLine();
										out.write("0 -1 BoxHeight mul rlineto");
										out.newLine();
										out.write("closepath stroke grestore");
										out.newLine();
									}
								} else {// diagonal label
									if ((outerLabelVertex.getX() - innerLabelVertex.getX()) > 0) { // to the upperright
										if (isSingleEdgeLabel) {
											out.write("45 rotate TickToLabelX TickToLabelY 1.5 mul BoxSpacingY sub rmoveto ");
											out.newLine();
										} else {
											out.write("45 rotate -1 BoxSpacingX mul TickToLabelY 1.5 mul BoxSpacingY sub rmoveto ");
											out.newLine();
										}
										out.write("0 BoxHeight rlineto");
										out.newLine();
										out.write(networkVertex.getLabelLength() + " BoxSpacingX 2 mul add 0 rlineto");
										out.newLine();
										out.write("0 -1 BoxHeight mul rlineto");
										out.newLine();
										out.write("closepath stroke grestore");
										out.newLine();

									} else { // to the lowerleft
										if (isSingleEdgeLabel) {
											out.write("45 rotate -1 TickToLabelX mul TickToLabelY 1.5 mul BoxSpacingY sub rmoveto ");
											out.newLine();
										} else {
											out.write("45 rotate BoxSpacingX TickToLabelY 1.5 mul  BoxSpacingY sub rmoveto ");
											out.newLine();
										}
										// out.write("-0.8 -0.4 rmoveto");out.newLine();
										out.write("0 BoxHeight rlineto");
										out.newLine();
										out.write((-networkVertex.getLabelLength()) + " BoxSpacingX -2 mul add 0 rlineto");
										out.newLine();
										out.write("0 -1 BoxHeight mul rlineto");
										out.newLine();
										out.write("closepath stroke grestore");
										out.newLine();
										// out.write("0 -0.45 rmoveto 45 rotate"); out.newLine();
										// out.write(-((Double)edge.getMyVertexLengths().get(i-1)).doubleValue() + " 0 rmoveto"); out.newLine();
										// out.write("(" + edge.getMyVertexNames().get(i-1) + ") show -45 rotate"); out.newLine();
									}
								}
							}

						}

						double firstX = first.getX();
						double firstY = first.getY();
						double secondX = second.getX();
						double secondY = second.getY();
						double firstSecondX = (secondX - firstX) / 2;
						double firstSecondY = (secondY - firstY) / 2;

						if (networkVertex.isTerminus()) {
							out.write("F2 Umlaut setfont");
							out.newLine();
						}

						out.write(firstX + " " + firstY + " moveto");
						out.newLine();

						if (networkVertex.getY() < innerLabelVertex.getY()) { // baseline
							if (innerLabelVertex.getY() == outerLabelVertex.getY()) { // hor
								if (innerLabelVertex.getX() > outerLabelVertex.getX()) { // left
									out.write((-edge.getLength()) + " 0 rmoveto");
									out.newLine();
								}
								out.write("(" + label + ") show");
								out.newLine();
							} else { // diag
								out.write("45 rotate");
								out.newLine();
								out.write("(" + label + ") show -45 rotate");
								out.newLine();
							}
						} else if (networkVertex.getY() == innerLabelVertex.getY()) { // centerline
							if (innerLabelVertex.getX() > outerLabelVertex.getX()) { // left
								out.write((-edge.getLength()) + " 0 rmoveto");
								out.newLine();
							}
							out.write("0 TickToLabelY 1.5 mul rmoveto");
							out.newLine();
							out.write("(" + label + ") show");
							out.newLine();
						} else { // topline
							if (innerLabelVertex.getY() == outerLabelVertex.getY()) { // hor
								if (innerLabelVertex.getX() > outerLabelVertex.getX()) { // left
									out.write((-edge.getLength()) + " 0 rmoveto");
									out.newLine();
								}
								out.write("0 TickToLabelY 3 mul rmoveto");
								out.newLine();
								out.write("(" + label + ") show");
								out.newLine();
							} else { // diag
								out.write("45 rotate");
								out.newLine();
								out.write((-edge.getLength()) + " 0 rmoveto");
								out.newLine();
								out.write("0 TickToLabelY 3 mul rmoveto");
								out.newLine();
								out.write("(" + label + ") show -45 rotate");
								out.newLine();
							}
						}

						if (networkVertex.isTerminus()) {
							out.write("F1 Umlaut setfont");
							out.newLine();
						}

						/*
						 * old version for labels modeled as edges 2009-12-10 if (firstSecondY == 0) {//horizontal label if ((secondX - firstX) > 0) { //to the right if (isSingleEdgeLabel) {
						 * out.write("TickToLabelX TickToLabelY 1.5 mul rmoveto"); out.newLine(); if (networkVertex.isTerminus()) out.write("BoxSpacingX 0 rmoveto ");
						 * 
						 * } else { out.write("0 TickToLabelY 1.5 mul rmoveto"); out.newLine(); } out.write("(" + label + ") show"); out.newLine(); } else { //to the left if (isSingleEdgeLabel) {
						 * out.write("-1 TickToLabelX mul " + edge.getLength() + " sub TickToLabelY 1.5 mul rmoveto"); out.newLine(); if (networkVertex.isTerminus())
						 * out.write("-1 BoxSpacingX mul 0 rmoveto "); } else { out.write((-edge.getLength()) + " BoxSpacingX add TickToLabelY 1.5 mul rmoveto"); out.newLine(); } out.write("(" + label
						 * + ") show"); out.newLine(); } } else {//diagonal label if ((secondX - firstX) > 0) { //to the upperright // out.write("0.25 0.25 rmoveto 45 rotate"); out.newLine(); if
						 * (isSingleEdgeLabel) { out.write("45 rotate TickToLabelX TickToLabelY 1.5 mul rmoveto"); out.newLine(); if (networkVertex.isTerminus()) out.write("BoxSpacingX 0 rmoveto ");
						 * 
						 * } else { out.write("45 rotate 0 TickToLabelY 1.5 mul rmoveto"); out.newLine(); } out.write("(" + label + ") show -45 rotate"); out.newLine(); } else { //to the lowerleft //
						 * out.write("0 -0.45 rmoveto 45 rotate"); out.newLine(); // out.write((-edge.getLength()+0.4) + " 0 rmoveto"); out.newLine(); if (isSingleEdgeLabel) {
						 * out.write("45 rotate -1 TickToLabelX mul " + edge.getLength() + " sub TickToLabelY 1.5 mul rmoveto"); out.newLine(); if (networkVertex.isTerminus())
						 * out.write("-1 BoxSpacing mul 0 rmoveto ");
						 * 
						 * } else { out.write("45 rotate " + (-edge.getLength()) + " BoxSpacingX add TickToLabelY 1.5 mul rmoveto"); out.newLine(); } out.write("(" + label + ") show -45 rotate");
						 * out.newLine(); } }
						 */

					} else if (edge.isSkippedLabelEdge()) { // edges that are not labeled (e.g. in the outer face)
						// draw ticks on these edges at least!
						MetroVertex first = (MetroVertex) edge.getEndpoints().getFirst();
						MetroVertex second = (MetroVertex) edge.getEndpoints().getSecond();
						int length = edge.getNumVertices() + 1;
						double firstX = first.getX();
						double firstY = first.getY();
						double secondX = second.getX();
						double secondY = second.getY();
						double firstSecondX = (secondX - firstX) / (length);
						double firstSecondY = (secondY - firstY) / (length);

						// creates tickmarks
						for (int i = 1; i < length; i++) {
							out.write((firstX + i * firstSecondX) + " " + (firstY + i * firstSecondY) + " moveto");
							out.newLine();
							if (firstSecondX == 0) {
								out.write(".66 Radius mul 0 rmoveto");
								out.newLine();
								out.write("-1.32 Radius mul 0 rlineto");
								out.newLine();
							} else if (firstSecondY == 0) {
								out.write("0 .66 Radius mul rmoveto");
								out.newLine();
								out.write("0 -1.32 Radius mul rlineto");
								out.newLine();
							} else if (firstSecondX * firstSecondY > 0) {
								out.write(".5 sqrt .66 Radius mul mul dup -1 mul rmoveto");
								out.newLine();
								out.write(".5 sqrt .66 Radius mul mul -2 mul dup -1 mul rlineto");
								out.newLine();
							} else {
								out.write(".5 sqrt .66 Radius mul mul dup rmoveto");
								out.newLine();
								out.write(".5 sqrt .66 Radius mul mul -2 mul dup rlineto");
								out.newLine();
							}

						}
						out.write("stroke");
						out.newLine();
					} else if (edge.getNumVertices() == 1) {
						// label is modelled as an edge
						MetroVertex first = (MetroVertex) edge.getEndpoints().getFirst();
						MetroVertex second = (MetroVertex) edge.getEndpoints().getSecond();
						MetroVertex labelEnd = null;
						String label = (String) edge.getMyVertexNames().get(0);
						Iterator findLabelEnd = second.getIncidentEdges().iterator();
						while (findLabelEnd.hasNext()) {
							MetroEdge labelEdge = (MetroEdge) findLabelEnd.next();
							if (labelEdge.isLabelDirectionDummy()) {
								labelEnd = (MetroVertex) labelEdge.getOpposite(second);
								break;
							}
						}
						double firstX = first.getX();
						double firstY = first.getY();
						double secondX = second.getX();
						double secondY = second.getY();
						double firstSecondX = (secondX - firstX) / 2;
						double firstSecondY = (secondY - firstY) / 2;
						out.write(secondX + " " + secondY + " moveto");
						out.newLine();
						if (firstSecondX == 0) {
							out.write(".66 Radius mul 0 rmoveto");
							out.newLine();
							out.write("-1.32 Radius mul 0 rlineto");
							out.newLine();
						} else if (firstSecondY == 0) {
							out.write("0 .66 Radius mul rmoveto");
							out.newLine();
							out.write("0 -1.32 Radius mul rlineto");
							out.newLine();
						} else if (firstSecondX * firstSecondY > 0) {
							out.write(".5 sqrt .66 Radius mul mul dup -1 mul rmoveto");
							out.newLine();
							out.write(".5 sqrt .66 Radius mul mul -2 mul dup -1 mul rlineto");
							out.newLine();
						} else {
							out.write(".5 sqrt .66 Radius mul mul dup rmoveto");
							out.newLine();
							out.write(".5 sqrt .66 Radius mul mul -2 mul dup rlineto");
							out.newLine();
						}
						out.write(secondX + " " + secondY + " moveto");
						out.newLine();
						if (firstSecondX == 0) { // horizontal label
							if ((labelEnd.getX() - secondX) > 0) { // to the right
								out.write("Radius 0 rmoveto");
								out.newLine();
								out.write("TickToLabelX TickToLabelY 1.5 mul rmoveto");
								out.newLine();
								out.write("(" + label + ") show");
								out.newLine();
							} else { // to the left
								out.write("-1 Radius mul 0 rmoveto");
								out.newLine();
								out.write("-1 TickToLabelX mul " + ((Double) edge.getMyVertexLengths().get(0)).doubleValue() + " sub TickToLabelY 1.5 mul rmoveto");
								out.newLine();
								out.write("(" + label + ") show");
								out.newLine();
							}
						} else if (firstSecondY == 0) { // diagonal label
							if ((labelEnd.getX() - secondX) > 0) { // to the upperright
								out.write("0 Radius rmoveto");
								out.newLine();
								out.write("45 rotate");
								out.newLine();
								out.write("(" + label + ") show -45 rotate");
								out.newLine();
								// out.write("0.25 0.35 rmoveto 45 rotate"); out.newLine();
								// out.write("(" + label + ") show -45 rotate"); out.newLine();
							} else { // to the lowerleft
								out.write("0 -1 Radius mul rmoveto");
								out.newLine();
								out.write("45 rotate -1 TickToLabelX mul " + ((Double) edge.getMyVertexLengths().get(0)).doubleValue() + " sub TickToLabelY 3 mul rmoveto");
								out.newLine();
								out.write("(" + label + ") show -45 rotate");
								out.newLine();
								// out.write("0 -0.45 rmoveto 45 rotate"); out.newLine();
								// out.write(-((Double)edge.getMyVertexLengths().get(0)).doubleValue() + " 0 rmoveto"); out.newLine();
								// out.write("(" + label + ") show -45 rotate"); out.newLine();
							}
						} else if (firstSecondX * firstSecondY > 0) {
							if ((labelEnd.getX() - secondX) > 0) { // to the right
								out.write(".5 sqrt Radius mul dup -1 mul rmoveto");
								out.newLine();
								out.write("TickToLabelX TickToLabelY 3 mul rmoveto");
								out.newLine();
								out.write("(" + label + ") show");
								out.newLine();
							} else { // to the left
								out.write(".5 sqrt -1 Radius mul mul dup -1 mul rmoveto");
								out.newLine();
								out.write("-1 TickToLabelX mul " + ((Double) edge.getMyVertexLengths().get(0)).doubleValue() + " sub TickToLabelY 0 mul rmoveto");
								out.newLine();
								out.write("(" + label + ") show");
								out.newLine();
							}
						} else {
							if ((labelEnd.getX() - secondX) > 0) { // to the right
								out.write(".5 sqrt Radius mul dup rmoveto");
								out.newLine();
								out.write("TickToLabelX TickToLabelY 0 mul rmoveto");
								out.newLine();
								out.write("(" + label + ") show");
								out.newLine();
							} else { // to the left
								out.write(".5 sqrt -1 Radius mul mul dup rmoveto");
								out.newLine();
								out.write("-1 TickToLabelX mul " + ((Double) edge.getMyVertexLengths().get(0)).doubleValue() + " sub TickToLabelY 3 mul rmoveto");
								out.newLine();
								out.write("(" + label + ") show");
								out.newLine();
							}
						}
						out.write("stroke");
						out.newLine();
					} else if (edge.getNumVertices() > 1) {
						// label is modelled as parallelogram
						MetroVertex first = (MetroVertex) edge.getEndpoints().getFirst();
						MetroVertex second = (MetroVertex) edge.getEndpoints().getSecond();
						int length = edge.getNumVertices() + 1;
						double firstX = first.getX();
						double firstY = first.getY();
						double secondX = second.getX();
						double secondY = second.getY();
						double firstSecondX = (secondX - firstX) / (length - 1);
						double firstSecondY = (secondY - firstY) / (length - 1);
						if (edge.isPendant() && (first.degree() == 2 || second.degree() == 2)) { // deg==2 is true for only for final pendant edge (count label box edges)
							firstSecondX = (secondX - firstX) / (length - 1.5);
							firstSecondY = (secondY - firstY) / (length - 1.5);
						}
						MetroVertex labelEnd = null;
						Iterator findLabelEnd = second.getIncidentEdges().iterator();
						while (findLabelEnd.hasNext()) {
							MetroEdge labelEdge = (MetroEdge) findLabelEnd.next();
							if (labelEdge.isLabelDirectionDummy()) {
								labelEnd = (MetroVertex) labelEdge.getOpposite(second);
								break;
							}
						}

						// draw box around terminus and a thicker tick
						if (edge.isPendant() && second.isTerminus()) {
							out.write("stroke");
							out.newLine();
							out.write(secondX + " " + secondY + " moveto");
							out.newLine();
							if (firstSecondX == 0) {
								out.write(".8 Radius mul 0 rmoveto");
								out.newLine();
								out.write("-1.6 Radius mul 0 rlineto");
								out.newLine();
							} else if (firstSecondY == 0) {
								out.write("0 .8 Radius mul rmoveto");
								out.newLine();
								out.write("0 -1.6 Radius mul rlineto");
								out.newLine();
							} else if (firstSecondX * firstSecondY > 0) {
								out.write(".5 sqrt .8 Radius mul mul dup -1 mul rmoveto");
								out.newLine();
								out.write(".5 sqrt .8 Radius mul mul -2 mul dup -1 mul rlineto");
								out.newLine();
							} else {
								out.write(".5 sqrt .8 Radius mul mul dup rmoveto");
								out.newLine();
								out.write(".5 sqrt .8 Radius mul mul -2 mul dup rlineto");
								out.newLine();
							}
							out.write("gsave currentlinewidth 2 mul setlinewidth stroke grestore");
							out.newLine();

							if (false) { // don't want termini boxed
								float[] rgb = second.getColor().getRGBColorComponents(null);

								out.write("gsave");
								out.newLine();
								out.write(rgb[0] + " " + rgb[1] + " " + rgb[2] + " setrgbcolor");
								out.newLine();
								out.write(secondX + " " + secondY + " moveto");
								out.newLine();
								if (firstSecondY != 0) {// horizontal label
									if ((labelEnd.getX() - secondX) > 0) { // to the right
										out.write("TickToLabelX TickToLabelY BoxSpacingY sub rmoveto");
										out.newLine();
										out.write("0 BoxHeight rlineto");
										out.newLine();
										out.write(((Double) edge.getMyVertexLengths().get(length - 2)).doubleValue() + " BoxSpacingX 2 mul add 0 rlineto");
										out.newLine();
										out.write("0 -1 BoxHeight mul rlineto");
										out.newLine();
										out.write("closepath stroke grestore");
										out.newLine();
									} else { // to the left
										out.write("-1 TickToLabelX mul TickToLabelY BoxSpacingY sub rmoveto");
										out.newLine();
										out.write("0 BoxHeight rlineto");
										out.newLine();
										out.write((-((Double) edge.getMyVertexLengths().get(length - 2)).doubleValue()) + " BoxSpacingX -2 mul add 0 rlineto");
										out.newLine();
										out.write("0 -1 BoxHeight mul rlineto");
										out.newLine();
										out.write("closepath stroke grestore");
										out.newLine();
									}
								} else {// diagonal label
									if ((labelEnd.getX() - secondX) > 0) { // to the upperright
										out.write("45 rotate TickToLabelX TickToLabelY BoxSpacingY sub rmoveto");
										out.newLine();
										out.write("0 BoxHeight rlineto");
										out.newLine();
										out.write(((Double) edge.getMyVertexLengths().get(length - 2)).doubleValue() + " BoxSpacingX 2 mul add 0 rlineto");
										out.newLine();
										out.write("0 -1 BoxHeight mul rlineto");
										out.newLine();
										out.write("closepath stroke grestore");
										out.newLine();
									} else { // to the lowerleft
										out.write("45 rotate -1 TickToLabelX mul TickToLabelY BoxSpacingY sub rmoveto");
										out.newLine();
										out.write("0 BoxHeight rlineto");
										out.newLine();
										out.write((-((Double) edge.getMyVertexLengths().get(length - 2)).doubleValue()) + " BoxSpacingX -2 mul add 0 rlineto");
										out.newLine();
										out.write("0 -1 BoxHeight mul rlineto");
										out.newLine();
										out.write("closepath stroke grestore");
										out.newLine();

										// out.write("0 -0.45 rmoveto 45 rotate"); out.newLine();
										// out.write(-((Double)edge.getMyVertexLengths().get(i-1)).doubleValue() + " 0 rmoveto"); out.newLine();
										// out.write("(" + edge.getMyVertexNames().get(i-1) + ") show -45 rotate"); out.newLine();
									}
								}
							}
						}

						// creates tickmarks
						for (int i = 1; i < length; i++) {
							out.write((firstX + (i - 0.5) * firstSecondX) + " " + (firstY + (i - 0.5) * firstSecondY) + " moveto");
							out.newLine();
							if (firstSecondX == 0) {
								out.write(".66 Radius mul 0 rmoveto");
								out.newLine();
								out.write("-1.32 Radius mul 0 rlineto");
								out.newLine();
							} else if (firstSecondY == 0) {
								out.write("0 .66 Radius mul rmoveto");
								out.newLine();
								out.write("0 -1.32 Radius mul rlineto");
								out.newLine();
							} else if (firstSecondX * firstSecondY > 0) {
								out.write(".5 sqrt .66 Radius mul mul dup -1 mul rmoveto");
								out.newLine();
								out.write(".5 sqrt .66 Radius mul mul -2 mul dup -1 mul rlineto");
								out.newLine();
							} else {
								out.write(".5 sqrt .66 Radius mul mul dup rmoveto");
								out.newLine();
								out.write(".5 sqrt .66 Radius mul mul -2 mul dup rlineto");
								out.newLine();
							}
							out.write((firstX + (i - 0.5) * firstSecondX) + " " + (firstY + (i - 0.5) * firstSecondY) + " moveto");
							out.newLine();
							if (firstSecondX == 0) { // horizontal label
								if ((labelEnd.getX() - secondX) > 0) { // to the right
									out.write("Radius 0 rmoveto");
									out.newLine();
									out.write("TickToLabelX TickToLabelY 1.5 mul rmoveto");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F2 Umlaut setfont ");
									out.write("(" + edge.getMyVertexNames().get(i - 1) + ") show");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F1 Umlaut setfont ");
								} else { // to the left
									out.write("-1 Radius mul 0 rmoveto");
									out.newLine();
									out.write("-1 TickToLabelX mul " + ((Double) edge.getMyVertexLengths().get(i - 1)).doubleValue() + " sub TickToLabelY 1.5 mul rmoveto");
									out.newLine();
									if (edge.isPendant() && i == length - 1) {
										out.write("F2 Umlaut setfont ");
										// out.write("-0.05 " + ((Double)edge.getMyVertexLengths().get(i-1)).doubleValue() + " mul 0 rmoveto");
									}
									out.write("(" + edge.getMyVertexNames().get(i - 1) + ") show");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F1 Umlaut setfont\n");
								}
							} else if (firstSecondY == 0) { // diagonal label
								if ((labelEnd.getX() - secondX) > 0) { // to the upperright
									out.write("0 Radius rmoveto");
									out.newLine();
									out.write("45 rotate TickToLabelX TickToLabelY 0 mul rmoveto");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F2 Umlaut setfont ");
									out.write("(" + edge.getMyVertexNames().get(i - 1) + ") show -45 rotate");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F1 Umlaut setfont ");
								} else { // to the lowerleft
									out.write("0 -1 Radius mul rmoveto");
									out.newLine();
									out.write("45 rotate -1 TickToLabelX mul " + ((Double) edge.getMyVertexLengths().get(i - 1)).doubleValue() + " sub TickToLabelY 3 mul rmoveto");
									out.newLine();
									if (edge.isPendant() && i == length - 1) {
										out.write("F2 Umlaut setfont ");
										// out.write("-0.05 " + ((Double)edge.getMyVertexLengths().get(i-1)).doubleValue() + " mul 0 rmoveto");
									}
									out.write("(" + edge.getMyVertexNames().get(i - 1) + ") show -45 rotate");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F1 Umlaut setfont\n");
								}
							} else if (firstSecondX * firstSecondY > 0) {
								if ((labelEnd.getX() - secondX) > 0) { // to the right
									out.write(".5 sqrt Radius mul dup -1 mul rmoveto");
									out.newLine();
									out.write("TickToLabelX TickToLabelY 3 mul rmoveto");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F2 Umlaut setfont ");
									out.write("(" + edge.getMyVertexNames().get(i - 1) + ") show");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F1 Umlaut setfont\n");
								} else { // to the left
									out.write(".5 sqrt -1 Radius mul mul dup -1 mul rmoveto");
									out.newLine();
									out.write("-1 TickToLabelX mul " + ((Double) edge.getMyVertexLengths().get(i - 1)).doubleValue() + " sub TickToLabelY 0 mul rmoveto");
									out.newLine();
									if (edge.isPendant() && i == length - 1) {
										out.write("F2 Umlaut setfont ");
										// out.write("-0.05 " + ((Double)edge.getMyVertexLengths().get(i-1)).doubleValue() + " mul 0 rmoveto");
									}
									out.write("(" + edge.getMyVertexNames().get(i - 1) + ") show");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F1 Umlaut setfont\n");
								}
							} else {
								if ((labelEnd.getX() - secondX) > 0) { // to the right
									out.write(".5 sqrt Radius mul dup rmoveto");
									out.newLine();
									out.write("TickToLabelX TickToLabelY 0 mul rmoveto");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F2 Umlaut setfont ");
									out.write("(" + edge.getMyVertexNames().get(i - 1) + ") show");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F1 Umlaut setfont\n");
								} else { // to the left
									out.write(".5 sqrt -1 Radius mul mul dup rmoveto");
									out.newLine();
									out.write("-1 TickToLabelX mul " + ((Double) edge.getMyVertexLengths().get(i - 1)).doubleValue() + " sub TickToLabelY 3 mul rmoveto");
									out.newLine();
									if (edge.isPendant() && i == length - 1) {
										out.write("F2 Umlaut setfont ");
										// out.write("-0.05 " + ((Double)edge.getMyVertexLengths().get(i-1)).doubleValue() + " mul 0 rmoveto");
									}
									out.write("(" + edge.getMyVertexNames().get(i - 1) + ") show");
									out.newLine();
									if (edge.isPendant() && i == length - 1)
										out.write("F1 Umlaut setfont\n");
								}
							}

							/*
							 * if (firstSecondY != 0) {//horizontal label if ((labelEnd.getX() - secondX) > 0) { //to the right out.write("TickToLabelX TickToLabelY rmoveto"); out.newLine(); if
							 * (edge.isPendant() && i==length-1) out.write("BoxSpacingX 0 rmoveto "); out.write("(" + edge.getMyVertexNames().get(i-1) + ") show"); out.newLine(); } else { //to the
							 * left out.write("-1 TickToLabelX mul " + ((Double)edge.getMyVertexLengths().get(i-1)).doubleValue() + " sub TickToLabelY rmoveto"); out.newLine(); if (edge.isPendant() &&
							 * i==length-1) out.write("-1 BoxSpacingX mul 0 rmoveto "); out.write("(" + edge.getMyVertexNames().get(i-1) + ") show"); out.newLine(); } } else {//diagonal label if
							 * ((labelEnd.getX() - secondX) > 0) { //to the upperright out.write("45 rotate TickToLabelX TickToLabelY rmoveto"); out.newLine(); if (edge.isPendant() && i==length-1)
							 * out.write("BoxSpacingX 0 rmoveto "); out.write("(" + edge.getMyVertexNames().get(i-1) + ") show -45 rotate"); out.newLine(); } else { //to the lowerleft
							 * out.write("45 rotate -1 TickToLabelX mul " + ((Double)edge.getMyVertexLengths().get(i-1)).doubleValue() + " sub TickToLabelY rmoveto"); out.newLine();
							 * //out.write((-((Double)edge.getMyVertexLengths().get(i-1)).doubleValue() - ((i==length-1)?0.2:0)) + " 0 rmoveto"); out.newLine(); if (edge.isPendant() && i==length-1)
							 * out.write("-1 BoxSpacingX mul 0 rmoveto "); out.write("(" + edge.getMyVertexNames().get(i-1) + ") show -45 rotate"); out.newLine(); } }
							 */
						}

						out.write("stroke");
						out.newLine();
					}

				}
				out.write("stroke");
				out.newLine();

			}

		}
		out.flush();

		if (debug) {// draw all nodes
			out.write(1 + " " + 0 + " " + 0 + " setrgbcolor");
			out.newLine();
			it = g.getVertices().iterator();
			StringLabeller vertexIDs = StringLabeller.getLabeller(g);
			while (it.hasNext()) {
				MetroVertex myV = (MetroVertex) it.next();
				out.write(myV.getX() + " " + myV.getY() + " moveto");
				out.newLine();
				out.write("currentpoint Stationradius 3 div 0 rmoveto");
				out.newLine();
				out.write("Stationradius 3 div 0 360 arc");
				out.newLine();
				out.write("Stationradius 0 rmoveto (" + vertexIDs.getLabel(myV) + ") show");
				out.newLine();
				out.write("stroke");
				out.newLine();
			}
		}

		out.close();
	}

	static void addLabelLengths(Graph g) throws IOException {
		String basename = (String) g.getUserDatum("basename");
		StringLabeller vertexIDs = StringLabeller.getLabeller(g);
		int counter = 0;
		final double labelScaleFactor = 0.75;

		File llFile = new File(basename + ".ll");

		if (!llFile.exists()) {
			llFile.createNewFile();
		}

		BufferedReader in = new BufferedReader(new FileReader(llFile));
		// System.out.println("opened file " + basename + ".ll");
		String currentLine = in.readLine();
		while (currentLine != null) {
			String[] tokens = currentLine.split("[\\s]"); // should result in sth like {n1, 3.1415}
			if (tokens.length == 2) {
				MetroVertex v = (MetroVertex) vertexIDs.getVertex(tokens[0]);
				if (v != null) {
					v.setLabelLength(labelScaleFactor * Double.parseDouble(tokens[1]));
					counter++;
				}
			}
			currentLine = in.readLine();
		}
		in.close();
		System.out.println("succeeded in reading " + counter + " label lengths");
	}

	static void changeCoordinates(Graph g, MetroCplex myCplex) throws UnknownObjectException, IloException {
		StringLabeller vertexIDs = StringLabeller.getLabeller(g);
		for (Iterator iter = g.getVertices().iterator(); iter.hasNext();) {
			MetroVertex mv = (MetroVertex) iter.next();
			mv.setX(myCplex.getValue("x(" + vertexIDs.getLabel(mv) + ")"));
			mv.setY(myCplex.getValue("y(" + vertexIDs.getLabel(mv) + ")"));
		}
		for (Iterator iterator = g.getEdges().iterator(); iterator.hasNext();) {
			MetroEdge me = (MetroEdge) iterator.next();
			if (me.isRegular() || me.isLabelConnectDummy() || me.isLabelMainDummy()) {
				me.setDirection((int) myCplex.getValue("dir(" + vertexIDs.getLabel((MetroVertex) me.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((MetroVertex) me.getEndpoints().getSecond())
						+ ")"));
			}
		}
	}

	static void changeCoordinates(Graph g, String filename) throws IOException {
		double edgeLength = 0;
		double sectorDeviation = 0;
		double bendCost = 0;

		StringLabeller vertexIDs = StringLabeller.getLabeller(g);
		// reset all coordinates to (0,0) as the solution only contains non-zero entries!
		Iterator it = g.getVertices().iterator();
		while (it.hasNext()) {
			MetroVertex v = (MetroVertex) it.next();
			v.setX(0.0);
			v.setY(0.0);
		}
		BufferedReader in = new BufferedReader(new FileReader(new File(filename)));
		String currentLine = in.readLine();
		while (currentLine != null) {
			currentLine = currentLine.replaceAll("[\" ,]", ""); // remove unwanted symbols in the string. should be like x(ni)2.0000 now
			String[] tokens = currentLine.split("[\\(\\)]"); // should result in sth like {x, n1, 1.0}
			if (tokens[0].equals("x")) {
				MetroVertex v = (MetroVertex) vertexIDs.getVertex(tokens[1]);
				v.setX(Double.parseDouble(tokens[2].trim()));
			} else if (tokens[0].equals("y")) {
				MetroVertex v = (MetroVertex) vertexIDs.getVertex(tokens[1]);
				v.setY(Double.parseDouble(tokens[2].trim()));
			} else if (tokens[0].equals("D")) {
				edgeLength += Double.parseDouble(tokens[tokens.length - 1].trim()); // access the last token; necessary to improve robustness for truncated variable names
			} else if (tokens[0].startsWith("sel_d")) {
				sectorDeviation += Double.parseDouble(tokens[tokens.length - 1].trim());
			} else if (tokens[0].equals("dev")) {
				bendCost += Double.parseDouble(tokens[tokens.length - 1].trim());
			}
			currentLine = in.readLine();
		}
		in.close();
		g.setUserDatum("solutionFile", filename, UserData.SHARED);
		g.setUserDatum("edgeLength", new Double(edgeLength), UserData.SHARED);
		g.setUserDatum("sectorDeviation", new Double(sectorDeviation), UserData.SHARED);
		g.setUserDatum("bendCost", new Double(bendCost), UserData.SHARED);
	}

	/*
	 * computes the set of all metro lines as a linked list of edges. nodes at which one line branches are considered as separating nodes, i.e. the line is split up. Required, e.g., to compute the
	 * bend cost along lines.
	 */
	public static Set getMetroLines(Graph g) {
		Set metroLines = new HashSet();
		int numLines = ((Vector) g.getUserDatum("lines")).size();
		Set[] pathSets = new HashSet[numLines];
		for (int i = 0; i < pathSets.length; i++) {
			pathSets[i] = new HashSet();
		}
		
		//System.out.println("g.getEdges(): " + g.getEdges());

		// forall edges insert edge e into its corresponding path sets
		Iterator it = g.getEdges().iterator();
		while (it.hasNext()) {
			MetroEdge e = (MetroEdge) it.next();
			if (e.isLabelDirectionDummy() || e.isLabelParallelDummy() || e.isLabelIntersectionConnectDummy() || e.isLabelIntersectionMainDummy())
				continue; // label edges do not belong to any lines
			Boolean[] lines = (Boolean[]) e.getUserDatum("lineArray");
			for (int i = 0; i < lines.length; i++) {
				if (lines[i].booleanValue()) {
					pathSets[i].add(e);
				}
			}
		}

		Vector _lines = (Vector) g.getUserDatum("lines");
		

		//System.out.println("setting color for " + pathSets.length + " pathSets");
		for (int i = 0; i < pathSets.length; i++) {
			while (!pathSets[i].isEmpty()) {
				it = pathSets[i].iterator();
				MetroPath edgePath = new MetroPath();
				edgePath.setColor(i);
				edgePath.setName((String) _lines.get(i));
				Edge e = (Edge) it.next();
				pathSets[i].remove(e);
				Vertex first = (Vertex) e.getEndpoints().getFirst();
				Vertex second = (Vertex) e.getEndpoints().getSecond();
				edgePath.add(e);
				//System.out.println("edgePath.add(e): " + e);
				// put all edges connected to "second" to the right of e, all edges connected to "first" to the left of e
				int currentDegree = 0;
				Edge currentEdge = e;
				Vertex currentVertex = second;
				Edge nextEdge = null;
				Iterator incidentEdges = currentVertex.getIncidentEdges().iterator();
				while (incidentEdges.hasNext()) {
					MetroEdge myEdge = (MetroEdge) incidentEdges.next();
					if (myEdge.isLabelDirectionDummy() || myEdge.isLabelParallelDummy() || myEdge.isLabelIntersectionConnectDummy() || myEdge.isLabelIntersectionMainDummy())
						continue; // these edges don't belong to lines
					if (((Boolean[]) myEdge.getUserDatum("lineArray"))[i].booleanValue()) {
						currentDegree++;
						if ((myEdge != currentEdge) && pathSets[i].contains(myEdge)) { // found the next edge of this path (in case the degree is 2)
							nextEdge = myEdge;
						}
					}
				}
				while ((currentDegree == 2) && (nextEdge != null)) {
					currentEdge = nextEdge;
					edgePath.addLast(currentEdge); // add vertices to the right of this list
					pathSets[i].remove(currentEdge);
					currentVertex = currentEdge.getOpposite(currentVertex);
					currentDegree = 0;
					nextEdge = null;
					incidentEdges = currentVertex.getIncidentEdges().iterator();
					while (incidentEdges.hasNext()) {
						MetroEdge myEdge = (MetroEdge) incidentEdges.next();
						if (myEdge.isLabelDirectionDummy() || myEdge.isLabelParallelDummy() || myEdge.isLabelIntersectionConnectDummy() || myEdge.isLabelIntersectionMainDummy())
							continue; // these edges don't belong to lines
						if (((Boolean[]) myEdge.getUserDatum("lineArray"))[i].booleanValue()) {
							currentDegree++;
							if ((myEdge != currentEdge) && pathSets[i].contains(myEdge)) { // found the next edge of this path (in case the degree is 2)
								nextEdge = myEdge;
							}// endif
						}// endif
					}// endwhile
				}// endwhile -- done extending e to the right
				currentDegree = 0;
				currentEdge = e;
				currentVertex = first;
				nextEdge = null;
				incidentEdges = currentVertex.getIncidentEdges().iterator();
				while (incidentEdges.hasNext()) {
					MetroEdge myEdge = (MetroEdge) incidentEdges.next();
					if (myEdge.isLabelDirectionDummy() || myEdge.isLabelParallelDummy() || myEdge.isLabelIntersectionConnectDummy() || myEdge.isLabelIntersectionMainDummy())
						continue; // these edges don't belong to lines
					if (((Boolean[]) myEdge.getUserDatum("lineArray"))[i].booleanValue()) {
						currentDegree++;
						if ((myEdge != currentEdge) && pathSets[i].contains(myEdge)) { // found the next edge of this path (in case the degree is 2)
							nextEdge = myEdge;
						}
					}
				}
				while ((currentDegree == 2) && (nextEdge != null)) {
					currentEdge = nextEdge;
					edgePath.addFirst(currentEdge); // add vertices to the left of this list
					pathSets[i].remove(currentEdge);
					currentVertex = currentEdge.getOpposite(currentVertex);
					currentDegree = 0;
					nextEdge = null;
					incidentEdges = currentVertex.getIncidentEdges().iterator();
					while (incidentEdges.hasNext()) {
						MetroEdge myEdge = (MetroEdge) incidentEdges.next();
						if (myEdge.isLabelDirectionDummy() || myEdge.isLabelParallelDummy() || myEdge.isLabelIntersectionConnectDummy() || myEdge.isLabelIntersectionMainDummy())
							continue; // these edges don't belong to lines
						if (((Boolean[]) myEdge.getUserDatum("lineArray"))[i].booleanValue()) {
							currentDegree++;
							if ((myEdge != currentEdge) && pathSets[i].contains(myEdge)) { // found the next edge of this path (in case the degree is 2)
								nextEdge = myEdge;
							}// endif
						}// endif
					}// endwhile
				}// endwhile -- done extending e to the left
				metroLines.add(edgePath);
			}// endwhile -- done with this line
		}// endfor -- done with all lines
		
		return metroLines;
	}

	static void printMetroLine(List l, Graph g) {
		ListIterator it = l.listIterator();
		StringLabeller ids = StringLabeller.getLabeller(g);
		while (it.hasNext()) {
			Edge e = (Edge) it.next();
			Vertex first = (Vertex) e.getEndpoints().getFirst();
			Vertex second = (Vertex) e.getEndpoints().getSecond();
			String id1 = ids.getLabel(first);
			String id2 = ids.getLabel(second);
			System.out.print("{" + id1 + "," + id2 + "}-");
		}
		System.out.println("");
	}

	static void computePendantEdges(Graph g) {
		Iterator it = g.getVertices().iterator();
		while (it.hasNext()) {
			MetroVertex v = (MetroVertex) it.next();
			if (v.degree() == 1) {// end vertex of a pendant edge path
				// v.setTerminus(true);
				MetroEdge currentEdge = (MetroEdge) v.getIncidentEdges().iterator().next();
				currentEdge.setPendant(true);
				MetroVertex currentVertex = (MetroVertex) currentEdge.getOpposite(v);
				while (currentVertex.degree() == 2) { // continue pendant path
					currentEdge = (MetroEdge) currentVertex.getNextEdgeFromList(currentEdge); // get the other edge incident to currentVertex
					currentVertex = (MetroVertex) currentEdge.getOpposite(currentVertex); // get the next vertex along the path
					currentEdge.setPendant(true);
				}
				if (currentVertex.degree() == 3) {// special case that two pendant paths merge (like East London line)
					Iterator<MetroEdge> it2 = currentVertex.getIncidentEdges().iterator();
					MetroEdge e1 = it2.next();
					MetroEdge e2 = it2.next();
					MetroEdge e3 = it2.next();
					MetroEdge enp;
					int pendant = (e1.isPendant() ? 1 : 0) + (e2.isPendant() ? 1 : 0) + (e3.isPendant() ? 1 : 0);
					if (pendant == 2) {
						if (!e1.isPendant()) {
							enp = e1;
						} else if (!e2.isPendant()) {
							enp = e2;
						} else {
							enp = e3;
						}
						currentVertex = (MetroVertex) enp.getOpposite(currentVertex);
						enp.setPendant(true);
						currentEdge = enp;
						while (currentVertex.degree() == 2) { // continue pendant path
							currentEdge = (MetroEdge) currentVertex.getNextEdgeFromList(currentEdge); // get the other edge incident to currentVertex
							currentVertex = (MetroVertex) currentEdge.getOpposite(currentVertex); // get the next vertex along the path
							currentEdge.setPendant(true);
						}
					}
				} // endif
			}
		}
	}

	/*
	 * checks whether the polyline (v1,v2,v3) turns left(-1), straight(0) or right(1) at v2
	 */
	static int getTurnDirection(MetroVertex v1, MetroVertex v2, MetroVertex v3) {
		// get Vector v1v2
		double v1v2_x = v2.getX() - v1.getX();
		double v1v2_y = v2.getY() - v1.getY();
		// get the normal vector for v1v2 that points to the left
		double n_x = -v1v2_y;
		double n_y = v1v2_x;
		// get Vector v2v3
		double v2v3_x = v3.getX() - v2.getX();
		double v2v3_y = v3.getY() - v2.getY();
		// calc scalar product <n,v2v3>
		double indicator = n_x * v2v3_x + n_y * v2v3_y;
		if (indicator < 0) {
			return 1;
		} else if (indicator > 0) {
			return -1;
		} else {
			return 0;
		}
	}

	/*
	 * checks whether the polyline (v1,v2,v3) turns left(-1), straight(0) or right(1) at v2
	 */
	public static int getTurnDirection(Point v1, Point v2, Point v3) {
		// get Vector v1v2
		double v1v2_x = v2.getX() - v1.getX();
		double v1v2_y = v2.getY() - v1.getY();
		// get the normal vector for v1v2 that points to the left
		double n_x = -v1v2_y;
		double n_y = v1v2_x;
		// get Vector v2v3
		double v2v3_x = v3.getX() - v2.getX();
		double v2v3_y = v3.getY() - v2.getY();
		// calc scalar product <n,v2v3>
		double indicator = n_x * v2v3_x + n_y * v2v3_y;
		if (indicator < 0) {
			return 1;
		} else if (indicator > 0) {
			return -1;
		} else {
			return 0;
		}
	}

	/*
	 * computes the intersection of two lines given by two points
	 */
	public static Point findIntersection(Point p1, Point p2, Point p3, Point p4) {
		Point intersection = new Point();
		// line1: a1 x + b1 y + c1 = 0
		double a1, b1, c1;
		if (p1.getX() == p2.getX()) {// vertical line
			a1 = 1;
			b1 = 0;
			c1 = -p1.getX();
		} else {
			a1 = (p2.getY() - p1.getY()) / (p2.getX() - p1.getX());
			b1 = -1;
			c1 = p1.getY() - p1.getX() * a1;
		}
		// line2: a2 x + b2 y + c2 = 0
		double a2, b2, c2;
		if (p3.getX() == p4.getX()) {
			a2 = 1;
			b2 = 0;
			c2 = -p3.getX();
		} else {
			a2 = (p4.getY() - p3.getY()) / (p4.getX() - p3.getX());
			b2 = -1;
			c2 = p3.getY() - p3.getX() * a2;
		}

		if ((a1 * b2 - a2 * b1) == 0) {// lines are parallel
			intersection = null;
		} else {
			intersection.setX((b1 * c2 - c1 * b2) / (a1 * b2 - a2 * b1));
			intersection.setY((c1 * a2 - a1 * c2) / (a1 * b2 - a2 * b1));
		}
		return intersection;
	}

	static Set addConvexHullEdges(Graph g) {
		Set dummyEdges = new HashSet();
		// compute lexicographical order of all vertices
		MetroVertex[] vertices = (MetroVertex[]) g.getVertices().toArray(new MetroVertex[0]);
		Arrays.sort(vertices, new HorizontalOrderComparator());
		LinkedList upper, lower;
		// compute upper hull (cf. de Berg et al. "Computational Geometry" p. 6)
		upper = new LinkedList();
		upper.add(vertices[0]);
		upper.add(vertices[1]); // fill list with the two leftmost vertices
		for (int i = 2; i < vertices.length; i++) {
			upper.add(vertices[i]);
			while ((upper.size() > 2)
					&& (GraphTools.getTurnDirection((MetroVertex) upper.get(upper.size() - 3), (MetroVertex) upper.get(upper.size() - 2), (MetroVertex) upper.get(upper.size() - 1)) != 1)) { // not a
																																																// right
																																																// turn
				upper.remove(upper.size() - 2); // remove second last vertex
			}
		}
		// compute lower hull
		lower = new LinkedList();
		lower.add(vertices[vertices.length - 1]);
		lower.add(vertices[vertices.length - 2]); // fill list with the two rightmost vertices
		for (int i = vertices.length - 3; i >= 0; i--) {
			lower.add(vertices[i]);
			while ((lower.size() > 2)
					&& (GraphTools.getTurnDirection((MetroVertex) lower.get(lower.size() - 3), (MetroVertex) lower.get(lower.size() - 2), (MetroVertex) lower.get(lower.size() - 1)) != 1)) { // not a
																																																// right
																																																// turn
				lower.remove(lower.size() - 2); // remove second last vertex
			}
		}
		// remove first and last vertex of lower (no duplicates wanted)
		lower.removeFirst();
		lower.removeLast();
		// append lower to upper
		upper.addAll(lower);
		// now iterate through upper and create the edges as dummies
		Boolean[] dummyLines = new Boolean[((Vector) g.getUserDatum("lines")).size()];
		for (int i = 0; i < dummyLines.length; i++) {
			dummyLines[i] = Boolean.FALSE;
		}
		Iterator it = upper.iterator();
		MetroVertex nextVertex;
		MetroVertex currentVertex = (MetroVertex) it.next();
		while (it.hasNext()) {
			nextVertex = (MetroVertex) it.next();
			if (!currentVertex.isNeighborOf(nextVertex)) {
				MetroEdge myEdge = (MetroEdge) g.addEdge(new MetroEdge(currentVertex, nextVertex));
				myEdge.setDummyType(MetroEdge.CONVEX_HULL_DUMMY);
				myEdge.addUserDatum("lineArray", dummyLines, UserData.SHARED);
				dummyEdges.add(myEdge);
			}
			currentVertex = nextVertex;
		}
		// close the hull with the final edge
		nextVertex = (MetroVertex) upper.getFirst();
		if (!currentVertex.isNeighborOf(nextVertex)) {
			MetroEdge myEdge = (MetroEdge) g.addEdge(new MetroEdge(currentVertex, nextVertex));
			myEdge.setDummyType(MetroEdge.CONVEX_HULL_DUMMY);
			myEdge.addUserDatum("lineArray", dummyLines, UserData.SHARED);
			dummyEdges.add(myEdge);
		}
		return dummyEdges;
	}

	static void clearFaces(Graph g) {
		for (Iterator it = g.getEdges().iterator(); it.hasNext();) {
			MetroEdge e = (MetroEdge) it.next();
			e.setLeftFace(null);
			e.setRightFace(null);
		}
	}

	static void computeFaces(Graph g) {
		List faceList = new LinkedList();
		g.setUserDatum("faceList", faceList, UserData.SHARED);
		Iterator it = g.getEdges().iterator();
		while (it.hasNext()) {
			MetroEdge e = (MetroEdge) it.next();
			if (e.getLeftFace() == null) { // found an edge with no leftFace; create the face and add all its edges
				Face myFace = new Face();
				faceList.add(myFace);
				MetroEdge currentEdge = e;
				myFace.appendEdge(currentEdge);
				currentEdge.setLeftFace(myFace);
				MetroVertex currentVertex = (MetroVertex) e.getEndpoints().getSecond();
				MetroEdge nextEdge = (MetroEdge) currentVertex.getPreviousEdgeFromList(currentEdge);
				while ((nextEdge != e) || (currentVertex == nextEdge.getEndpoints().getSecond())) { // second part needed in case this edge sticks into the face
					currentEdge = nextEdge;
					currentVertex = (MetroVertex) currentEdge.getOpposite(currentVertex);
					myFace.appendEdge(currentEdge); // add this edge to the face
					if (currentVertex == currentEdge.getEndpoints().getFirst()) {
						currentEdge.setRightFace(myFace); // remember the face inside the edge
					} else {
						currentEdge.setLeftFace(myFace);
					}
					nextEdge = (MetroEdge) currentVertex.getPreviousEdgeFromList(currentEdge);
				}
			}
			if (e.getRightFace() == null) {// found an edge with no rightFace; create the face and add all its edges
				Face myFace = new Face();
				faceList.add(myFace);
				MetroEdge currentEdge = e;
				myFace.appendEdge(currentEdge);
				currentEdge.setRightFace(myFace);
				MetroVertex currentVertex = (MetroVertex) e.getEndpoints().getSecond();
				MetroEdge nextEdge = (MetroEdge) currentVertex.getNextEdgeFromList(currentEdge);
				while ((nextEdge != e) || (currentVertex == nextEdge.getEndpoints().getSecond())) { // second part needed in case this edge sticks into the face
					currentEdge = nextEdge;
					currentVertex = (MetroVertex) currentEdge.getOpposite(currentVertex);
					myFace.appendEdge(currentEdge); // add this edge to the face
					if (currentVertex == currentEdge.getEndpoints().getFirst()) {
						currentEdge.setLeftFace(myFace); // remember the face inside the edge
					} else {
						currentEdge.setRightFace(myFace);
					}
					nextEdge = (MetroEdge) currentVertex.getNextEdgeFromList(currentEdge);
				}
			}
		}
		// now all faces should be found...
		// System.out.println("Eulers equality evaluates to: " + (g.numEdges()-g.numVertices()+2 == faceList.size()));
	}

	static void insertDummyVertices_Dep(Graph g) throws FatalException {
		MetroVertex[] vertices = (MetroVertex[]) g.getVertices().toArray(new MetroVertex[0]);
		HorizontalOrderComparator myHOC = new HorizontalOrderComparator();
		Arrays.sort(vertices, myHOC);
		HashSet currentEdges = new HashSet();
		HashSet currentEdgesBuffer = new HashSet();
		StringLabeller vertexIDs = StringLabeller.getLabeller(g);
		HashSet removeEdges = new HashSet();
		HashSet addEdges = new HashSet();
		int loop = 0, dummyCounter = 0;

		for (int i = 0; i < vertices.length; i++) {
			// System.out.println("i: " + i);
			MetroVertex[] neighbors = (MetroVertex[]) vertices[i].getNeighbors().toArray(new MetroVertex[0]);
			Arrays.sort(neighbors, myHOC); // now edges ending at vertices[i] appear before those starting at this vertex
			currentEdgesBuffer.clear();
			for (int j = 0; j < neighbors.length; j++) {
				// System.out.println("j: " + j);
				if (myHOC.compare(neighbors[j], vertices[i]) < 0) {
					currentEdges.remove(vertices[i].findEdge(neighbors[j])); // reached the end of this edge
				} else if (myHOC.compare(neighbors[j], vertices[i]) > 0) {
					Edge myEdge = vertices[i].findEdge(neighbors[j]);
					// now check for intersections before adding the edge
					Iterator it = currentEdges.iterator();
					while (it.hasNext()) {
						loop++;
						Edge crossingEdge = (Edge) it.next();
						MetroVertex mv0 = (MetroVertex) crossingEdge.getEndpoints().getFirst();
						MetroVertex mv1 = (MetroVertex) crossingEdge.getEndpoints().getSecond();
						if (!crossingEdge.isIncident(vertices[i]) && !crossingEdge.isIncident(neighbors[j])) {
							if (Line2D.linesIntersect(mv0.getX(), mv0.getY(), mv1.getX(), mv1.getY(), vertices[i].getX(), vertices[i].getY(), neighbors[j].getX(), neighbors[j].getY())) {
								// found intersection, adding dummy vertex...
								// vectors a: mv0 -> mv1, b: vertices[i] -> neighbors[j], c: mv0 -> vertices[i]
								// a~ = (ay, -ax) is orthogonal to a
								double ax, ay, bx, by, cx, cy, lambda;
								ax = mv1.getX() - mv0.getX();
								ay = mv1.getY() - mv0.getY();
								bx = neighbors[j].getX() - vertices[i].getX();
								by = neighbors[j].getY() - vertices[i].getY();
								cx = vertices[i].getX() - mv0.getX();
								cy = vertices[i].getY() - mv0.getY();
								if (ay * bx - ax * by != 0) {
									lambda = (ax * cy - ay * cx) / (ay * bx - ax * by);
								} else {// this case only appears when a and b are parallel
									lambda = 0.5;
								}
								double px, py; // intersection point
								px = vertices[i].getX() + lambda * bx;
								py = vertices[i].getY() + lambda * by;
								MetroVertex mv = new MetroVertex();
								mv.setX(px);
								mv.setY(py);
								mv.setDummy(true);
								g.addVertex(mv);
								try {
									vertexIDs.setLabel(mv, "d" + (dummyCounter++));
								} catch (UniqueLabelException e) {
									// this should never happen cause we increase dummyCounter each time
									e.printStackTrace();
								}
								System.out.println("Found crossing at position (" + px + "," + py + ")");
								System.out.println("between edges (" + mv0.getName() + mv1.getName() + ") and (" + vertices[i].getName() + neighbors[j].getName() + ")");

								// connect the vertex
								Iterator keys = crossingEdge.getUserDatumKeyIterator();
								Edge e1 = new MetroEdge(mv0, mv);
								Edge e2 = new MetroEdge(mv, mv1);
								while (keys.hasNext()) {
									Object key = keys.next();
									e1.addUserDatum(key, crossingEdge.getUserDatum(key), UserData.SHARED);
									e2.addUserDatum(key, crossingEdge.getUserDatum(key), UserData.SHARED);
								}
								addEdges.add(e1);
								addEdges.add(e2);

								keys = myEdge.getUserDatumKeyIterator();
								e1 = new MetroEdge(vertices[i], mv);
								e2 = new MetroEdge(mv, neighbors[j]);
								while (keys.hasNext()) {
									Object key = keys.next();
									e1.addUserDatum(key, myEdge.getUserDatum(key), UserData.SHARED);
									e2.addUserDatum(key, myEdge.getUserDatum(key), UserData.SHARED);
								}
								addEdges.add(e1);
								addEdges.add(e2);
								removeEdges.add(myEdge);
								removeEdges.add(crossingEdge);

							}// endif
						}// endif
					} // endwhile
						// now add the edge to currentEdges...
					currentEdgesBuffer.add(myEdge);
				}// end elseif
			}// endfor
			currentEdges.addAll(currentEdgesBuffer);
		} // endfor
			// now modify the graph

		Iterator it = addEdges.iterator();
		while (it.hasNext()) {
			Edge e = (Edge) it.next();
			g.addEdge(e);
		}
		it = removeEdges.iterator();
		while (it.hasNext()) {
			Edge e = (Edge) it.next();
			g.removeEdge(e);
		}
		System.out.println("checked " + loop + " edge pairs for crossings");
	}

	public static Set intersectingEdges(Graph g) {
		Set intersectingEdges = new HashSet();
		LinkedList vertices = new LinkedList(g.getVertices());
		HorizontalOrderComparator myHOC = new HorizontalOrderComparator();
		Collections.sort(vertices, myHOC); // O(n * log n)
		HashSet currentEdges = new HashSet();
		// HashSet currentEdgesBuffer = new HashSet();
		// StringLabeller vertexIDs = StringLabeller.getLabeller(g);
		// int loop=0, dummyCounter=0;

		for (int i = 0; i < vertices.size(); i++) {
			// System.out.println("i: " + i);
			MetroVertex currentVertex = (MetroVertex) vertices.get(i);
			MetroVertex[] neighbors = (MetroVertex[]) currentVertex.getNeighbors().toArray(new MetroVertex[0]);
			Arrays.sort(neighbors, myHOC); // now edges ending at vertices[i] appear before those starting at this vertex
			for (int j = 0; j < neighbors.length; j++) {
				// System.out.println("j: " + j);
				MetroVertex currentNeighbor = neighbors[j];
				if (myHOC.compare(currentNeighbor, currentVertex) < 0) {
					currentEdges.remove(currentVertex.findEdge(currentNeighbor)); // reached the end of this edge
				} else if (myHOC.compare(currentNeighbor, currentVertex) > 0) {
					Edge myEdge = currentVertex.findEdge(currentNeighbor);

					// now check for intersections before adding the edge
					// currentEdgesBuffer.clear();
					Iterator it = currentEdges.iterator();
					while (it.hasNext()) {
						Edge crossingEdge = (Edge) it.next();
						MetroVertex mv0 = (MetroVertex) crossingEdge.getEndpoints().getFirst();
						MetroVertex mv1 = (MetroVertex) crossingEdge.getEndpoints().getSecond();
						if (!crossingEdge.isIncident(currentVertex) && !crossingEdge.isIncident(currentNeighbor)) {
							if (Line2D.linesIntersect(mv0.getX(), mv0.getY(), mv1.getX(), mv1.getY(), currentVertex.getX(), currentVertex.getY(), currentNeighbor.getX(), currentNeighbor.getY())) {
								intersectingEdges.add(new Pair(crossingEdge, myEdge));
							}// endif
						}// endif
					} // endwhile
						// now add the edges to currentEdges...
					currentEdges.add(myEdge);
				}// end elseif
			}// endfor
		} // endfor
		System.out.println("Found " + intersectingEdges.size() + " edge crossings");
		return intersectingEdges;
	}

	static void insertDummyVertices(Graph g) throws FatalException {
		LinkedList vertices = new LinkedList(g.getVertices());
		HorizontalOrderComparator myHOC = new HorizontalOrderComparator();
		Collections.sort(vertices, myHOC); // O(n * log n)
		HashSet currentEdges = new HashSet();
		HashSet currentEdgesBuffer = new HashSet();
		StringLabeller vertexIDs = StringLabeller.getLabeller(g);
		int loop = 0, dummyCounter = 0;

		for (int i = 0; i < vertices.size(); i++) {
			// System.out.println("i: " + i);
			MetroVertex currentVertex = (MetroVertex) vertices.get(i);
			MetroVertex[] neighbors = (MetroVertex[]) currentVertex.getNeighbors().toArray(new MetroVertex[0]);
			Arrays.sort(neighbors, myHOC); // now edges ending at vertices[i] appear before those starting at this vertex
			for (int j = 0; j < neighbors.length; j++) {
				// System.out.println("j: " + j);
				MetroVertex currentNeighbor = neighbors[j];
				if (myHOC.compare(currentNeighbor, currentVertex) < 0) {
					currentEdges.remove(currentVertex.findEdge(currentNeighbor)); // reached the end of this edge
				} else if (myHOC.compare(currentNeighbor, currentVertex) > 0) {
					MetroEdge myEdge = (MetroEdge) currentVertex.findEdge(currentNeighbor);

					// now check for intersections before adding the edge
					currentEdgesBuffer.clear();
					Iterator it = currentEdges.iterator();
					while (it.hasNext()) {
						loop++;
						MetroEdge crossingEdge = (MetroEdge) it.next();
						MetroVertex mv0 = (MetroVertex) crossingEdge.getEndpoints().getFirst();
						MetroVertex mv1 = (MetroVertex) crossingEdge.getEndpoints().getSecond();
						if (!crossingEdge.isIncident(currentVertex) && !crossingEdge.isIncident(currentNeighbor)) {
							if (Line2D.linesIntersect(mv0.getX(), mv0.getY(), mv1.getX(), mv1.getY(), currentVertex.getX(), currentVertex.getY(), currentNeighbor.getX(), currentNeighbor.getY())) {
								// found intersection, adding dummy vertex...
								// vectors a: mv0 -> mv1, b: vertices[i] -> neighbors[j], c: mv0 -> vertices[i]
								// a~ = (ay, -ax) is orthogonal to a
								double ax, ay, bx, by, cx, cy, lambda;
								ax = mv1.getX() - mv0.getX();
								ay = mv1.getY() - mv0.getY();
								bx = currentNeighbor.getX() - currentVertex.getX();
								by = currentNeighbor.getY() - currentVertex.getY();
								cx = currentVertex.getX() - mv0.getX();
								cy = currentVertex.getY() - mv0.getY();
								if (ay * bx - ax * by != 0) {
									lambda = (ax * cy - ay * cx) / (ay * bx - ax * by);
								} else {// this case only appears when a and b are parallel
									lambda = 0.5;
								}
								double px, py; // intersection point
								px = currentVertex.getX() + lambda * bx;
								py = currentVertex.getY() + lambda * by;
								MetroVertex mv = new MetroVertex();
								mv.setX(px);
								mv.setY(py);
								mv.setDummy(true);
								mv.setIntersection(true);
								g.addVertex(mv);
								try {
									vertexIDs.setLabel(mv, "d" + (dummyCounter++));
									mv.setName(vertexIDs.getLabel(mv));
								} catch (UniqueLabelException e) {
									// this should never happen cause we increase dummyCounter each time
									e.printStackTrace();
								}
								// insert mv at the right position of list vertices; will be to the right of the current position
								int pos = Collections.binarySearch(vertices, mv, myHOC);
								if (pos < 0) { // else mv is already in the list --> impossible cause it's a new vertex!
									pos = -pos - 1;
									vertices.add(pos, mv);
								}

								System.out.println("Found crossing at position (" + px + "," + py + ")");
								System.out.println("between edges (" + mv0.getName() + "<>" + mv1.getName() + ") and (" + currentVertex.getName() + "<>" + currentNeighbor.getName() + ")");

								// connect the vertex
								Iterator keys = crossingEdge.getUserDatumKeyIterator();
								MetroEdge e1 = new MetroEdge(mv0, mv);
								MetroEdge e2 = new MetroEdge(mv, mv1);
								while (keys.hasNext()) {
									Object key = keys.next();
									e1.addUserDatum(key, crossingEdge.getUserDatum(key), UserData.SHARED);
									e2.addUserDatum(key, crossingEdge.getUserDatum(key), UserData.SHARED);
								}
								e1.setMultiplicity(crossingEdge.getMultiplicity());
								e2.setMultiplicity(crossingEdge.getMultiplicity());
								g.addEdge(e1);
								g.addEdge(e2);
								g.removeEdge(crossingEdge);
								it.remove(); // remove crossingEdge from currentEdges -- will be replaced with its new left part
								crossingEdge = (myHOC.compare(mv0, mv1) < 0) ? e1 : e2; // the left of e1, e2
								currentEdgesBuffer.add(crossingEdge);// will be added once all currentEdges are done

								keys = myEdge.getUserDatumKeyIterator();
								e1 = new MetroEdge(currentVertex, mv);
								e2 = new MetroEdge(mv, currentNeighbor);
								while (keys.hasNext()) {
									Object key = keys.next();
									e1.addUserDatum(key, myEdge.getUserDatum(key), UserData.SHARED);
									e2.addUserDatum(key, myEdge.getUserDatum(key), UserData.SHARED);
								}
								e1.setMultiplicity(myEdge.getMultiplicity());
								e2.setMultiplicity(myEdge.getMultiplicity());
								g.addEdge(e1);
								g.addEdge(e2);
								g.removeEdge(myEdge);
								myEdge = e1; // currentVertex is left of currentNeighbor by construction
								currentNeighbor = mv;

							}// endif
						}// endif
					} // endwhile
						// now add the edges to currentEdges...
					currentEdges.add(myEdge);
					currentEdges.addAll(currentEdgesBuffer);
				}// end elseif
			}// endfor
		} // endfor

		System.out.println("checked " + loop + " edge pairs for crossings");
	}

	static Graph createLabelGraph(Graph g) {
		return createLabelGraph(g, false);
	}

	// modifies the graph directly and returns it
	// boolean all determines whether labels in the external face are considered
	static Graph createLabelGraph(Graph g, boolean all) {
		boolean labelWithBends = false;
		boolean oneEdgeLabels = false; // intersection labels with a bend -> new mode
		boolean allinternal = (all || false); // do we label internal intersections?
		boolean pendantJoint = false; // allow a bend at the inserted label vertex on a pendant edge to the terminus
		boolean increaseLabels = false; // for larger maps like Sydney
		Graph labelGraph = g;
		System.out.println("old graph is " + g);
		System.out.println("new graph is " + labelGraph);

		StringLabeller vertexIDs = StringLabeller.getLabeller(labelGraph);
		int labelVertexCounter = 0;

		MetroEdge[] edges = (MetroEdge[]) labelGraph.getEdges().toArray(new MetroEdge[0]);
		for (int i = 0; i < edges.length; i++) {
			MetroEdge currentEdge = edges[i];

			// if we have a pendant edge but do not want to consider labels in the outer face then skip extending the edge
			if (!all && (currentEdge.isPendant() || currentEdge.getLeftFace() == currentEdge.getRightFace())) {
				currentEdge.setSkippedLabelEdge(true);
			} else if (currentEdge.isPendant() && currentEdge.getNumVertices() >= 1
					&& (((MetroVertex) currentEdge.getEndpoints().getFirst()).degree() == 1 || ((MetroVertex) currentEdge.getEndpoints().getSecond()).degree() == 1)) {
				// create the new label face

				// 08-06-30: getting rid of label_connect_dummy edges (marked //##)
				MetroVertex first, second, start, end;
				first = (MetroVertex) currentEdge.getEndpoints().getFirst();
				second = (MetroVertex) currentEdge.getEndpoints().getSecond();
				end = (first.degree() == 1) ? first : second;
				start = (first.degree() == 1) ? second : first;
				MetroVertex u1 = new MetroVertex(start.getX() + (end.getX() - start.getX()) / 3, start.getY() + (end.getY() - start.getY()) / 3);
				MetroVertex u2 = end;
				MetroVertex u3 = new MetroVertex(u2.getX() + 30, u2.getY());
				MetroVertex u4 = new MetroVertex(u1.getX() + 30, u1.getY());
				u1.setLabelVertex(true);
				u1.setJoint(pendantJoint);
				u3.setLabelVertex(true);
				u4.setLabelVertex(true);
				labelGraph.addVertex(u1);
				labelGraph.addVertex(u3);
				labelGraph.addVertex(u4);
				try {
					vertexIDs.setLabel(u1, "l" + (labelVertexCounter++));
					vertexIDs.setLabel(u3, "l" + (labelVertexCounter++));
					vertexIDs.setLabel(u4, "l" + (labelVertexCounter++));
				} catch (UniqueLabelException e) {
					// this should never happen cause we increase dummyCounter each time
					e.printStackTrace();
				}
				MetroEdge e1 = new MetroEdge(start, u1);
				MetroEdge e2 = new MetroEdge(u1, end);
				MetroEdge e4 = new MetroEdge(end, u3);
				MetroEdge e5 = new MetroEdge(u4, u3);
				MetroEdge e6 = new MetroEdge(u1, u4);
				e1.setDummyType(MetroEdge.LABEL_CONNECT_DUMMY);
				e2.setDummyType(MetroEdge.LABEL_MAIN_DUMMY);
				e4.setDummyType(MetroEdge.LABEL_DIRECTION_DUMMY);
				e5.setDummyType(MetroEdge.LABEL_PARALLEL_DUMMY);
				e6.setDummyType(MetroEdge.LABEL_DIRECTION_DUMMY);
				e1.setMainEdge(e2);
				e2.setMainEdge(e2);
				e4.setMainEdge(e2);
				e5.setMainEdge(e2);
				e6.setMainEdge(e2);
				e1.setLeftFace(currentEdge.getLeftFace());
				e1.setRightFace(currentEdge.getRightFace());
				e2.setLeftFace(currentEdge.getLeftFace());
				e2.setRightFace(currentEdge.getRightFace());
				e4.setLeftFace(currentEdge.getLeftFace());
				e4.setRightFace(currentEdge.getRightFace());
				e5.setLeftFace(currentEdge.getLeftFace());
				e5.setRightFace(currentEdge.getRightFace());
				e6.setLeftFace(currentEdge.getLeftFace());
				e6.setRightFace(currentEdge.getRightFace());

				e1.setLength(0.5);
				e2.setLength(currentEdge.getNumVertices() + 0.5);
				e5.setLength(currentEdge.getNumVertices() + 0.5);
				// e2 has to receive the attributes from currentEdge
				List names = currentEdge.getMyVertexNames();
				List lengths = currentEdge.getMyVertexLengths();
				if (first.degree() == 1) { // reverse the list and append
					List tempNames = new LinkedList();
					List tempLengths = new LinkedList();
					ListIterator it = names.listIterator(names.size());// end of the list
					while (it.hasPrevious()) {
						String label = (String) it.previous();
						tempNames.add(label);
					}
					it = lengths.listIterator(lengths.size());// end of the list
					while (it.hasPrevious()) {
						Double length = (Double) it.previous();
						tempLengths.add(length);
					}
					names = tempNames;
					lengths = tempLengths;
					names.add(end.getName());
					lengths.add(new Double(end.getLabelLength()));
				} else {
					names.add(end.getName());
					lengths.add(new Double(end.getLabelLength()));
				}
				e2.setMyVertexNames(names);
				e2.setMyVertexLengths(lengths);
				e2.setNumVertices(currentEdge.getNumVertices() + 1);
				e2.setCurrentLabelWidth(Math.max(currentEdge.getCurrentLabelWidth(), end.getLabelLength()) + 0.45 + 0.2);// add 0.2 for terminus!
				// to indicate that this label is managed differently now set length to 0.
				e4.setLength(e2.getCurrentLabelWidth());
				e6.setLength(e2.getCurrentLabelWidth());
				end.setLabelLength(0);
				e2.setPendant(currentEdge.isPendant());
				Iterator userData = currentEdge.getUserDatumKeyIterator();
				while (userData.hasNext()) {
					Object key = userData.next();
					Object value = currentEdge.getUserDatum(key);
					e1.setUserDatum(key, value, UserData.SHARED);
					e2.setUserDatum(key, value, UserData.SHARED);
				}
				e1.setMultiplicity(currentEdge.getMultiplicity());
				e2.setMultiplicity(currentEdge.getMultiplicity());
				e1.removeUserDatum("length");
				labelGraph.addEdge(e1);
				labelGraph.addEdge(e2);
				labelGraph.addEdge(e4);
				labelGraph.addEdge(e5);
				labelGraph.addEdge(e6);
				start.replaceEdgeInList(currentEdge, e1);
				end.replaceEdgeInList(currentEdge, e2);
				labelGraph.removeEdge(currentEdge);
			} else if (currentEdge.getNumVertices() >= 2) {

				// create the new label face
				MetroVertex first, second;

				first = (MetroVertex) currentEdge.getEndpoints().getFirst();
				second = (MetroVertex) currentEdge.getEndpoints().getSecond();

				MetroVertex u1 = new MetroVertex(first.getX() + (second.getX() - first.getX()) / 3, first.getY() + (second.getY() - first.getY()) / 3);
				MetroVertex u2 = new MetroVertex(first.getX() + 2 * (second.getX() - first.getX()) / 3, first.getY() + 2 * (second.getY() - first.getY()) / 3);
				MetroVertex u3 = new MetroVertex(u2.getX() + 30, u2.getY());
				MetroVertex u4 = new MetroVertex(u1.getX() + 30, u1.getY());

				u1.setLabelVertex(true);
				u2.setLabelVertex(true);
				u3.setLabelVertex(true);
				u4.setLabelVertex(true);
				labelGraph.addVertex(u1);
				labelGraph.addVertex(u2);
				labelGraph.addVertex(u3);
				labelGraph.addVertex(u4);
				try {
					vertexIDs.setLabel(u1, "l" + (labelVertexCounter++));
					vertexIDs.setLabel(u2, "l" + (labelVertexCounter++));
					vertexIDs.setLabel(u3, "l" + (labelVertexCounter++));
					vertexIDs.setLabel(u4, "l" + (labelVertexCounter++));
				} catch (UniqueLabelException e) {
					// this should never happen cause we increase dummyCounter each time
					e.printStackTrace();
				}
				MetroEdge e1, e2, e3, e4, e5, e6, e7 = null, e8 = null;

				// two more vertices for edges with bends
				if (labelWithBends) {
					MetroVertex u5 = new MetroVertex(first.getX() + (second.getX() - first.getX()) / 5, first.getY() + (second.getY() - first.getY()) / 5);
					MetroVertex u6 = new MetroVertex(first.getX() + 4 * (second.getX() - first.getX()) / 5, first.getY() + 4 * (second.getY() - first.getY()) / 5);

					labelGraph.addVertex(u5);
					labelGraph.addVertex(u6);
					try {
						vertexIDs.setLabel(u5, "l" + (labelVertexCounter++));
						vertexIDs.setLabel(u6, "l" + (labelVertexCounter++));
					} catch (UniqueLabelException e) {
						// this should never happen cause we increase dummyCounter each time
						e.printStackTrace();
					}
					e1 = new MetroEdge(u5, u1);
					e2 = new MetroEdge(u1, u2);
					e3 = new MetroEdge(u2, u6);
					e4 = new MetroEdge(u2, u3);
					e5 = new MetroEdge(u4, u3);
					e6 = new MetroEdge(u1, u4);
					e7 = new MetroEdge(first, u5);
					e8 = new MetroEdge(u6, second);

				} else {
					e1 = new MetroEdge(first, u1);
					e2 = new MetroEdge(u1, u2);
					e3 = new MetroEdge(u2, second);
					e4 = new MetroEdge(u2, u3);
					e5 = new MetroEdge(u4, u3);
					e6 = new MetroEdge(u1, u4);
				}
				e1.setDummyType(MetroEdge.LABEL_CONNECT_DUMMY);
				e2.setDummyType(MetroEdge.LABEL_MAIN_DUMMY);
				e3.setDummyType(MetroEdge.LABEL_CONNECT_DUMMY);
				e4.setDummyType(MetroEdge.LABEL_DIRECTION_DUMMY);
				e5.setDummyType(MetroEdge.LABEL_PARALLEL_DUMMY);
				e6.setDummyType(MetroEdge.LABEL_DIRECTION_DUMMY);
				e1.setMainEdge(e2);
				e2.setMainEdge(e2);
				e3.setMainEdge(e2);
				e4.setMainEdge(e2);
				e5.setMainEdge(e2);
				e6.setMainEdge(e2);
				e1.setLeftFace(currentEdge.getLeftFace());
				e1.setRightFace(currentEdge.getRightFace());
				e2.setLeftFace(currentEdge.getLeftFace());
				e2.setRightFace(currentEdge.getRightFace());
				e3.setLeftFace(currentEdge.getLeftFace());
				e3.setRightFace(currentEdge.getRightFace());
				e4.setLeftFace(currentEdge.getLeftFace());
				e4.setRightFace(currentEdge.getRightFace());
				e5.setLeftFace(currentEdge.getLeftFace());
				e5.setRightFace(currentEdge.getRightFace());
				e6.setLeftFace(currentEdge.getLeftFace());
				e6.setRightFace(currentEdge.getRightFace());

				e1.setLength(0.5);
				e3.setLength(0.5);
				e4.setLength(currentEdge.getCurrentLabelWidth());
				e6.setLength(currentEdge.getCurrentLabelWidth());
				e2.setLength(currentEdge.getNumVertices());
				e5.setLength(currentEdge.getNumVertices());
				// e2 has to receive the attributes from currentEdge
				e2.setCurrentLabelWidth(currentEdge.getCurrentLabelWidth());
				e2.setMyVertexNames(currentEdge.getMyVertexNames());
				e2.setMyVertexLengths(currentEdge.getMyVertexLengths());
				e2.setNumVertices(currentEdge.getNumVertices());
				e2.setPendant(currentEdge.isPendant());
				Iterator userData = currentEdge.getUserDatumKeyIterator();
				while (userData.hasNext()) {
					Object key = userData.next();
					Object value = currentEdge.getUserDatum(key);
					e1.setUserDatum(key, value, UserData.SHARED);
					e2.setUserDatum(key, value, UserData.SHARED);
					e3.setUserDatum(key, value, UserData.SHARED);
				}
				e1.setMultiplicity(currentEdge.getMultiplicity());
				e2.setMultiplicity(currentEdge.getMultiplicity());
				e3.setMultiplicity(currentEdge.getMultiplicity());

				e1.removeUserDatum("length");
				e3.removeUserDatum("length");
				labelGraph.addEdge(e1);
				labelGraph.addEdge(e2);
				labelGraph.addEdge(e3);
				labelGraph.addEdge(e4);
				labelGraph.addEdge(e5);
				labelGraph.addEdge(e6);

				if (labelWithBends) {
					e7.setLeftFace(currentEdge.getLeftFace());
					e7.setRightFace(currentEdge.getRightFace());
					e8.setLeftFace(currentEdge.getLeftFace());
					e8.setLeftFace(currentEdge.getRightFace());
					e7.setLength(0.5);
					e8.setLength(0.5);
					e7.setMultiplicity(currentEdge.getMultiplicity());
					e8.setMultiplicity(currentEdge.getMultiplicity());
					e7.setDummyType(MetroEdge.LABEL_CONNECT_DUMMY);
					e8.setDummyType(MetroEdge.LABEL_CONNECT_DUMMY);
					System.out.println(e7.isLabelConnectDummy() + " " + e8.isLabelConnectDummy());
					userData = currentEdge.getUserDatumKeyIterator();
					while (userData.hasNext()) {
						Object key = userData.next();
						Object value = currentEdge.getUserDatum(key);
						e7.setUserDatum(key, value, UserData.SHARED);
						e8.setUserDatum(key, value, UserData.SHARED);
					}
					// e7.setUserDatum("length", new Integer(1), UserData.SHARED);
					// e8.setUserDatum("length", new Integer(1), UserData.SHARED);
					first.replaceEdgeInList(currentEdge, e7);
					second.replaceEdgeInList(currentEdge, e8);
				} else {
					first.replaceEdgeInList(currentEdge, e1);
					second.replaceEdgeInList(currentEdge, e3);
				}
				labelGraph.removeEdge(currentEdge);

			} else if (currentEdge.getNumVertices() == 1) {
				// model this as one edge
				MetroVertex first, second;
				first = (MetroVertex) currentEdge.getEndpoints().getFirst();
				second = (MetroVertex) currentEdge.getEndpoints().getSecond();

				MetroVertex u1 = new MetroVertex(first.getX() + (second.getX() - first.getX()) / 2, first.getY() + (second.getY() - first.getY()) / 2);
				MetroVertex u2 = new MetroVertex(u1.getX() + 30, u1.getY());
				u1.setLabelVertex(true);
				u2.setLabelVertex(true);
				labelGraph.addVertex(u1);
				labelGraph.addVertex(u2);
				try {
					vertexIDs.setLabel(u1, "l" + (labelVertexCounter++));
					vertexIDs.setLabel(u2, "l" + (labelVertexCounter++));
				} catch (UniqueLabelException e) {
					// this should never happen cause we increase dummyCounter each time
					e.printStackTrace();
				}
				MetroEdge e1 = new MetroEdge(first, u1);
				MetroEdge e2 = new MetroEdge(u1, second);
				MetroEdge e3 = new MetroEdge(u1, u2);
				e1.setDummyType(MetroEdge.LABEL_MAIN_DUMMY);
				e2.setDummyType(MetroEdge.LABEL_CONNECT_DUMMY);
				e3.setDummyType(MetroEdge.LABEL_DIRECTION_DUMMY);
				e1.setMainEdge(e1);
				e2.setMainEdge(e1);
				e3.setMainEdge(e1);
				e1.setLeftFace(currentEdge.getLeftFace());
				e1.setRightFace(currentEdge.getRightFace());
				e2.setLeftFace(currentEdge.getLeftFace());
				e2.setRightFace(currentEdge.getRightFace());
				e3.setLeftFace(currentEdge.getLeftFace());
				e3.setRightFace(currentEdge.getRightFace());

				e1.setLength(1.0);
				e2.setLength(1.0);
				e3.setLength(currentEdge.getCurrentLabelWidth());
				e1.setCurrentLabelWidth(currentEdge.getCurrentLabelWidth());
				e1.setMyVertexNames(currentEdge.getMyVertexNames());
				e1.setMyVertexLengths(currentEdge.getMyVertexLengths());
				e1.setNumVertices(currentEdge.getNumVertices());
				e1.setPendant(currentEdge.isPendant());
				Iterator userData = currentEdge.getUserDatumKeyIterator();
				while (userData.hasNext()) {
					Object key = userData.next();
					Object value = currentEdge.getUserDatum(key);
					e1.setUserDatum(key, value, UserData.SHARED);
					e2.setUserDatum(key, value, UserData.SHARED);
				}
				e1.removeUserDatum("length");
				e2.removeUserDatum("length");
				e1.setMultiplicity(currentEdge.getMultiplicity());
				e2.setMultiplicity(currentEdge.getMultiplicity());
				labelGraph.addEdge(e1);
				labelGraph.addEdge(e2);
				labelGraph.addEdge(e3);
				first.replaceEdgeInList(currentEdge, e1);
				second.replaceEdgeInList(currentEdge, e2);
				labelGraph.removeEdge(currentEdge);

			} else {
				// do nothing here

			}

		}

		MetroVertex[] vertices = (MetroVertex[]) labelGraph.getVertices().toArray(new MetroVertex[0]);

		for (int i = 0; i < vertices.length; i++) {
			MetroVertex v = vertices[i];
			if (v.isLabelVertex() || v.isDummy()) {
				continue;
			} else if (v.getLabelLength() > 0 && (v.degree() > 1 || all) && allinternal) {
				if (oneEdgeLabels) {
					// add a label edge
					MetroVertex u1 = new MetroVertex();
					// MetroVertex u2 = new MetroVertex();
					u1.setLabelVertex(true);
					// u2.setLabelVertex(true);
					u1.setLabelIntersectionVertex(true);
					// u2.setLabelIntersectionVertex(true);
					labelGraph.addVertex(u1);
					// labelGraph.addVertex(u2);
					try {
						vertexIDs.setLabel(u1, "l" + (labelVertexCounter++));
						// vertexIDs.setLabel(u2, "l"+(labelVertexCounter++));
					} catch (UniqueLabelException e) {
						// this should never happen cause we increase dummyCounter each time
						e.printStackTrace();
					}
					MetroEdge e1 = new MetroEdge(v, u1);
					// MetroEdge e2 = new MetroEdge(u1, u2);
					e1.setDummyType(MetroEdge.LABEL_INTERSECTION_MAIN);
					// e2.setDummyType(MetroEdge.LABEL_INTERSECTION_MAIN);
					e1.setMainEdge(e1);
					// e2.setMainEdge(e2);

					// a bit longer for termini to accomodate the color box
					e1.setLength(v.getLabelLength() + 0.5 + (v.isTerminus() ? 0.4 : 0));
					// e2.setLength(v.getLabelLength());
					e1.setCurrentLabelWidth(v.getLabelLength() + 0.5 + (v.isTerminus() ? 0.4 : 0));
					LinkedList name = new LinkedList();
					name.add(v.getName());
					e1.setMyVertexNames(name);
					// e2.setMyVertexLengths(currentEdge.getMyVertexLengths());
					e1.setNumVertices(1);
					e1.setPendant(true);
					labelGraph.addEdge(e1);
					// labelGraph.addEdge(e2);
					// first.replaceEdgeInList(currentEdge, e1);
					// second.replaceEdgeInList(currentEdge, e2);
					// labelGraph.removeEdge(currentEdge);
				} else {
					MetroVertex u1 = new MetroVertex();
					MetroVertex u2 = new MetroVertex();
					u1.setLabelVertex(true);
					u2.setLabelVertex(true);
					u1.setLabelIntersectionVertex(true);
					u2.setLabelIntersectionVertex(true);
					labelGraph.addVertex(u1);
					labelGraph.addVertex(u2);
					try {
						vertexIDs.setLabel(u1, "l" + (labelVertexCounter++));
						vertexIDs.setLabel(u2, "l" + (labelVertexCounter++));
					} catch (UniqueLabelException e) {
						// this should never happen cause we increase dummyCounter each time
						e.printStackTrace();
					}
					MetroEdge e1 = new MetroEdge(v, u1);
					MetroEdge e2 = new MetroEdge(u1, u2);
					e1.setDummyType(MetroEdge.LABEL_INTERSECTION_CONNECT);
					e2.setDummyType(MetroEdge.LABEL_INTERSECTION_MAIN);
					e1.setMainEdge(e2);
					e2.setMainEdge(e2);
					e1.findFaceCandidates();
					e2.setFaceCandidates(e1.getFaceCandidates());

					// a bit longer for termini to accomodate the color box
					// e1.setLength(0.7+(v.isTerminus()?0.2:0));
					e1.setLength(0.7);
					e2.setLength(v.getLabelLength());
					e2.setCurrentLabelWidth(v.getLabelLength());
					LinkedList name = new LinkedList();
					name.add(v.getName());
					e2.setMyVertexNames(name);
					// e2.setMyVertexLengths(currentEdge.getMyVertexLengths());
					e2.setNumVertices(1);
					e1.setPendant(true);
					e2.setPendant(true);
					labelGraph.addEdge(e1);
					labelGraph.addEdge(e2);
					// first.replaceEdgeInList(currentEdge, e1);
					// second.replaceEdgeInList(currentEdge, e2);
					// labelGraph.removeEdge(currentEdge);
				}
			}
		}

		/*
		 * this part increases the label lengths for use in large maps like sydney...
		 */
		if (increaseLabels) {
			for (Iterator<MetroEdge> labelEdges = g.getEdges().iterator(); labelEdges.hasNext();) {
				MetroEdge edge = labelEdges.next();
				if (/* edge.isLabelDirectionDummy() || edge.isLabelIntersectionMainDummy() || */edge.isLabelIntersectionConnectDummy()) {
					edge.setLength(1.5 * edge.getLength());
				}
			}
		}

		return labelGraph;
	}

	static boolean canShareFace(MetroEdge e1, MetroEdge e2) {
		if (e1.isLabelIntersectionMainDummy()) {
			if (e2.isLabelIntersectionMainDummy()) { // both edges are LAbelIntersectionMain
				MetroVertex intersection1 = (MetroVertex) e1.getEndpoints().getFirst();
				if (intersection1.degree() == 1) {
					intersection1 = (MetroVertex) e1.getOpposite(intersection1);
				}
				Set incEdges = intersection1.getIncidentEdges();
				HashSet faceSet1 = new HashSet();
				for (Iterator iter = incEdges.iterator(); iter.hasNext();) {
					MetroEdge myEdge = (MetroEdge) iter.next();
					faceSet1.add(myEdge.getLeftFace());
					faceSet1.add(myEdge.getRightFace());
				}
				MetroVertex intersection2 = (MetroVertex) e2.getEndpoints().getFirst();
				if (intersection2.degree() == 1) {
					intersection2 = (MetroVertex) e2.getOpposite(intersection2);
				}
				incEdges = intersection2.getIncidentEdges();
				// HashSet faceSet2 = new HashSet();
				for (Iterator iter = incEdges.iterator(); iter.hasNext();) {
					MetroEdge myEdge = (MetroEdge) iter.next();
					if (faceSet1.contains(myEdge.getLeftFace()) && myEdge.getLeftFace() != null)
						return true;
					if (faceSet1.contains(myEdge.getRightFace()) && myEdge.getRightFace() != null)
						return true;
				}
				return false;
			} else {
				MetroVertex intersection = (MetroVertex) e1.getEndpoints().getFirst();
				if (intersection.degree() == 1) {
					intersection = (MetroVertex) e1.getOpposite(intersection);
				}
				Set incEdges = intersection.getIncidentEdges();
				Face f1 = e2.getLeftFace();
				Face f2 = e2.getRightFace();
				HashSet faceSet = new HashSet();
				for (Iterator iter = incEdges.iterator(); iter.hasNext();) {
					MetroEdge myEdge = (MetroEdge) iter.next();
					faceSet.add(myEdge.getLeftFace());
					faceSet.add(myEdge.getRightFace());
				}
				if (faceSet.contains(f1) || faceSet.contains(f2)) {
					return true;
				} else {
					return false;
				}
			}
		} else {
			if (e2.isLabelIntersectionMainDummy()) {
				MetroVertex intersection = (MetroVertex) e2.getEndpoints().getFirst();
				if (intersection.degree() == 1) {
					intersection = (MetroVertex) e2.getOpposite(intersection);
				}
				Set incEdges = intersection.getIncidentEdges();
				Face f1 = e1.getLeftFace();
				Face f2 = e1.getRightFace();
				HashSet faceSet = new HashSet();
				for (Iterator iter = incEdges.iterator(); iter.hasNext();) {
					MetroEdge myEdge = (MetroEdge) iter.next();
					faceSet.add(myEdge.getLeftFace());
					faceSet.add(myEdge.getRightFace());
				}
				if (faceSet.contains(f1) || faceSet.contains(f2)) {
					return true;
				} else {
					return false;
				}
			} else { // no edge is LabelIntersectionMain
				return (e1.getLeftFace() == e2.getLeftFace() || e1.getLeftFace() == e2.getRightFace() || e1.getRightFace() == e2.getLeftFace() || e1.getRightFace() == e2.getRightFace());
			}
		}
	}

	static void addSectorInfo(Graph g) {
		StringLabeller vertexIDs = StringLabeller.getLabeller(g);

		for (Iterator it = g.getEdges().iterator(); it.hasNext();) {
			MetroEdge e = (MetroEdge) it.next();
			if (e.isRegular() || e.isLabelMainDummy()) {// need sector only for those
				MetroVertex first = (MetroVertex) e.getEndpoints().getFirst();
				MetroVertex second = (MetroVertex) e.getEndpoints().getSecond();

				// compute sector s_first(second)
				double first_x = first.getX();
				double first_y = first.getY();
				double second_x = second.getX();
				double second_y = second.getY();
				// move origin to first vertex
				second_x = second_x - first_x;
				second_y = second_y - first_y;
				if ((Math.cos(3 * Math.PI / 8) * second_x + Math.sin(3 * Math.PI / 8) * second_y) >= 0) {
					if ((Math.cos(15 * Math.PI / 8) * second_x + Math.sin(15 * Math.PI / 8) * second_y) >= 0) {
						if ((Math.cos(13 * Math.PI / 8) * second_x + Math.sin(13 * Math.PI / 8) * second_y) >= 0) {
							e.addUserDatum("sector1to2", new Integer(0), UserData.SHARED);
						} else {
							e.addUserDatum("sector1to2", new Integer(1), UserData.SHARED);
						}
					} else {
						if ((Math.cos(Math.PI / 8) * second_x + Math.sin(Math.PI / 8) * second_y) >= 0) {
							e.addUserDatum("sector1to2", new Integer(2), UserData.SHARED);
						} else {
							e.addUserDatum("sector1to2", new Integer(3), UserData.SHARED);
						}
					}
				} else {
					if ((Math.cos(7 * Math.PI / 8) * second_x + Math.sin(7 * Math.PI / 8) * second_y) >= 0) {
						if ((Math.cos(5 * Math.PI / 8) * second_x + Math.sin(5 * Math.PI / 8) * second_y) >= 0) {
							e.addUserDatum("sector1to2", new Integer(4), UserData.SHARED);
						} else {
							e.addUserDatum("sector1to2", new Integer(5), UserData.SHARED);
						}
					} else {
						if ((Math.cos(9 * Math.PI / 8) * second_x + Math.sin(9 * Math.PI / 8) * second_y) >= 0) {
							e.addUserDatum("sector1to2", new Integer(6), UserData.SHARED);
						} else {
							e.addUserDatum("sector1to2", new Integer(7), UserData.SHARED);
						}
					}
				}
				// System.out.println("Sector " + id_first + "," + id_second + ": " + sector);
				int opposite = (((Integer) e.getUserDatum("sector1to2")).intValue() + 4) % 8;
				e.addUserDatum("sector2to1", new Integer(opposite), UserData.SHARED);
			}
		}
	}

	// this method is called to insert two joints for each regular parallelogram-labeled edge
	// 2008-07-25: now also for edges where only a single vertex has been contracted (nice for sydney circular quay)
	static void insertJoints(Graph g) {
		int jointVertexCounter = 0;
		StringLabeller vertexIDs = StringLabeller.getLabeller(g);

		MetroEdge[] edges = (MetroEdge[]) g.getEdges().toArray(new MetroEdge[0]);
		for (int i = 0; i < edges.length; i++) {
			MetroEdge e = edges[i];

			// happens if we merged this edge beforehand when processing its counterpart
			if (e.getGraph() != g)
				continue;

			if (e.getNumVertices() >= 1 && (!e.isPendant() || (e.isPendant() && ((MetroVertex) e.getEndpoints().getFirst()).degree() > 1 && ((MetroVertex) e.getEndpoints().getSecond()).degree() > 1))) {
				MetroVertex first = (MetroVertex) e.getEndpoints().getFirst();
				MetroVertex second = (MetroVertex) e.getEndpoints().getSecond();

				// do we have the case of a non-contracted deg-2 vertex due to parallel edges? then fix it
				boolean mergeTwoEdges = false;
				MetroEdge other = null;
				MetroVertex third = null;
				List names = null;
				List lengths = null;
				if (first.degree() != 2 && second.degree() == 2 && !second.isIntersection()) {
					mergeTwoEdges = true;
					names = e.getMyVertexNames();
					lengths = e.getMyVertexLengths();
					Iterator iter = second.getIncidentEdges().iterator();
					other = (MetroEdge) iter.next();
					if (other == e)
						other = (MetroEdge) iter.next();
					third = (MetroVertex) other.getOpposite(second);
				} else if (second.degree() != 2 && first.degree() == 2 && !first.isIntersection()) {
					mergeTwoEdges = true;
					// switch first and second
					first = second;
					second = (MetroVertex) e.getOpposite(first);
					names = e.getInvertedNames();
					lengths = e.getInvertedLengths();
					Iterator iter = second.getIncidentEdges().iterator();
					other = (MetroEdge) iter.next();
					if (other == e)
						other = (MetroEdge) iter.next();
					third = (MetroVertex) other.getOpposite(second);
				}

				MetroVertex u1 = new MetroVertex(first.getX() + (second.getX() - first.getX()) / 3, first.getY() + (second.getY() - first.getY()) / 3);
				MetroVertex u2 = new MetroVertex(first.getX() + 2 * (second.getX() - first.getX()) / 3, first.getY() + 2 * (second.getY() - first.getY()) / 3);
				u1.setJoint(true);
				u2.setJoint(true);
				g.addVertex(u1);
				g.addVertex(u2);
				try {
					vertexIDs.setLabel(u1, "j" + (jointVertexCounter++));
					vertexIDs.setLabel(u2, "j" + (jointVertexCounter++));
				} catch (UniqueLabelException ule) {
					// this should never happen cause we increase jointVertexCounter each time
					ule.printStackTrace();
				}

				if (mergeTwoEdges) {
					u2.setX(third.getX() + (second.getX() - third.getX()) / 3);
					u2.setY(third.getY() + (second.getY() - third.getY()) / 3);
					names.add(second.getName());
					lengths.add(new Double(second.getLabelLength()));
					e.setCurrentLabelWidth(Math.max(e.getCurrentLabelWidth(), second.getLabelLength()));
					// 08-06-30: bugfix - 'other' might be an empty edge, so check for numVertices
					if (second == other.getEndpoints().getFirst()) {
						if (other.getNumVertices() > 0) {
							names.addAll(other.getMyVertexNames());
							lengths.addAll(other.getMyVertexLengths());
						}

					} else {
						if (other.getNumVertices() > 0) {
							names.addAll(other.getInvertedNames());
							lengths.addAll(other.getInvertedLengths());
						}

					}
					e.setCurrentLabelWidth(Math.max(e.getCurrentLabelWidth(), other.getCurrentLabelWidth()));
					e.setNumVertices(e.getNumVertices() + other.getNumVertices() + 1);
					e.setMyVertexLengths(lengths);
					e.setMyVertexNames(names);
					e.setUserDatum("length", new Integer(e.getNumVertices()), UserData.SHARED);
				}

				MetroEdge e1 = new MetroEdge(first, u1);
				MetroEdge e2 = new MetroEdge(u1, u2);
				MetroEdge e3 = mergeTwoEdges ? new MetroEdge(u2, third) : new MetroEdge(u2, second);
				e1.setLeftFace(e.getLeftFace());
				e1.setRightFace(e.getRightFace());
				e2.setLeftFace(e.getLeftFace());
				e2.setRightFace(e.getRightFace());
				e3.setLeftFace(e.getLeftFace());
				e3.setRightFace(e.getRightFace());
				e1.setLength(1);
				e3.setLength(1);
				e2.setLength(e.getLength());
				e2.setNumVertices(e.getNumVertices());
				e2.setMyVertexLengths(e.getMyVertexLengths());
				e2.setMyVertexNames(e.getMyVertexNames());
				e2.setCurrentLabelWidth(e.getCurrentLabelWidth());
				e1.setNumVertices(0);
				e3.setNumVertices(0);
				e1.setMultiplicity(e.getMultiplicity());
				e2.setMultiplicity(e.getMultiplicity());
				e3.setMultiplicity(e.getMultiplicity());

				Iterator userData = e.getUserDatumKeyIterator();
				while (userData.hasNext()) {
					Object key = userData.next();
					Object value = e.getUserDatum(key);
					e1.setUserDatum(key, value, UserData.SHARED);
					e2.setUserDatum(key, value, UserData.SHARED);
					e3.setUserDatum(key, value, UserData.SHARED);
				}
				e1.setUserDatum("length", new Integer(1), UserData.SHARED);
				e3.setUserDatum("length", new Integer(1), UserData.SHARED);
				e1.setLabelJointEdge(true);
				e3.setLabelJointEdge(true);
				e1.setMainEdge(e1);
				e3.setMainEdge(e3);
				g.addEdge(e1);
				g.addEdge(e2);
				g.addEdge(e3);

				first.replaceEdgeInList(e, e1);
				if (mergeTwoEdges) {
					third.replaceEdgeInList(e, e3);
				} else {
					second.replaceEdgeInList(e, e3);
				}
				g.removeEdge(e);
				if (mergeTwoEdges) {
					g.removeEdge(other);
					g.removeVertex(second);
				}
			}
			// tried inserting joints on pendant edges but decided to skip it and make the additional node added in createLabelGraph() a joint
			// update: simpler to insert a joint!
			else if (e.isPendant()) {
				MetroVertex first = (MetroVertex) e.getEndpoints().getFirst();
				MetroVertex second = (MetroVertex) e.getEndpoints().getSecond();
				MetroVertex end = (first.degree() == 1) ? first : second;
				MetroVertex start = (first.degree() == 1) ? second : first;
				MetroVertex u1 = new MetroVertex(start.getX() + (end.getX() - start.getX()) / 3, start.getY() + (end.getY() - start.getY()) / 3);
				u1.setJoint(true);
				g.addVertex(u1);
				try {
					vertexIDs.setLabel(u1, "j" + (jointVertexCounter++));
				} catch (UniqueLabelException ule) {
					// this should never happen cause we increase jointVertexCounter each time
					ule.printStackTrace();
				}

				MetroEdge e1 = new MetroEdge(start, u1);
				MetroEdge e2 = new MetroEdge(u1, end);
				e1.setLeftFace(e.getLeftFace()); // for a pendant edge left face = right face ans we do not need to care about the direction
				e1.setRightFace(e.getRightFace());
				e2.setLeftFace(e.getLeftFace());
				e2.setRightFace(e.getRightFace());
				e1.setLength(0.5);
				e2.setLength(e.getLength());
				e2.setNumVertices(e.getNumVertices());
				if (end == second || e.getMyVertexLengths() == null) {
					e2.setMyVertexLengths(e.getMyVertexLengths());
					e2.setMyVertexNames(e.getMyVertexNames());
				} else { // need to invert the lists
					LinkedList templen = new LinkedList();
					ListIterator<Double> it = e.getMyVertexLengths().listIterator(e.getMyVertexLengths().size());// end of the list
					while (it.hasPrevious()) {
						Double len = it.previous();
						templen.add(len);
					}
					e2.setMyVertexLengths(templen);
					LinkedList tempnam = new LinkedList();
					ListIterator<String> it2 = e.getMyVertexNames().listIterator(e.getMyVertexNames().size());// end of the list
					while (it2.hasPrevious()) {
						String label = it2.previous();
						tempnam.add(label);
					}
					e2.setMyVertexNames(tempnam);

				}
				e2.setCurrentLabelWidth(e.getCurrentLabelWidth());
				e1.setNumVertices(0);
				e1.setMultiplicity(e.getMultiplicity());
				e2.setMultiplicity(e.getMultiplicity());

				Iterator userData = e.getUserDatumKeyIterator();
				while (userData.hasNext()) {
					Object key = userData.next();
					Object value = e.getUserDatum(key);
					e1.setUserDatum(key, value, UserData.SHARED);
					e2.setUserDatum(key, value, UserData.SHARED);
				}
				e1.removeUserDatum("length");
				e1.setLabelJointEdge(true);
				e1.setMainEdge(e1);
				e1.setPendant(true);
				e2.setPendant(true);
				g.addEdge(e1);
				g.addEdge(e2);

				start.replaceEdgeInList(e, e1);
				end.replaceEdgeInList(e, e2);
				g.removeEdge(e);
			}
		}
	}

	static void setIntersectionStatus(Graph g) {
		for (Iterator iter = g.getVertices().iterator(); iter.hasNext();) {
			MetroVertex mv = (MetroVertex) iter.next();
			if (mv.degree() >= 3) {
				mv.setIntersection(true);
				System.out.println("found intersection at " + mv.getName());
			} else if (mv.degree() == 2) {
				MetroEdge first, second;
				Iterator edges = mv.getIncidentEdges().iterator();
				first = (MetroEdge) edges.next();
				second = (MetroEdge) edges.next();
				Boolean[] lines1, lines2;
				lines1 = (Boolean[]) first.getUserDatum("lineArray");
				lines2 = (Boolean[]) second.getUserDatum("lineArray");
				if (!Arrays.equals(lines1, lines2)) {
					mv.setIntersection(true);
					mv.setTerminus(true);
					System.out.println("found intersection and terminus at " + mv.getName());
				}
			}
		}
	}

	static void setTerminusStatus(Graph g) {
		for (Iterator iter = g.getVertices().iterator(); iter.hasNext();) {
			MetroVertex mv = (MetroVertex) iter.next();
			if (mv.degree() == 1) {
				mv.setTerminus(true);

				Color[] colors = (Color[]) ((Vector) g.getUserDatum("colors")).toArray(new Color[0]);
				MetroEdge edge = (MetroEdge) mv.getIncidentEdges().iterator().next();
				Boolean[] lines = (Boolean[]) edge.getUserDatum("lineArray");
				if (lines != null)
					for (int i = 0; i < lines.length; i++) {
						if (lines[i].booleanValue()) {
							mv.setColor(colors[i]);
						}
					}

				System.out.println("found terminus at " + mv.getName());
			} else if (mv.degree() >= 2) {
				Iterator it1 = mv.getIncidentEdges().iterator();
				MetroEdge edge = (MetroEdge) it1.next();
				Boolean[] lines = (Boolean[]) edge.getUserDatum("lineArray");
				int[] degree = new int[lines.length];
				for (int i = 0; i < degree.length; i++) {
					degree[i] = 0;
				}

				// get the degree in each line
				it1 = mv.getIncidentEdges().iterator();
				while (it1.hasNext()) {
					edge = (MetroEdge) it1.next();
					lines = (Boolean[]) edge.getUserDatum("lineArray");
					for (int i = 0; i < lines.length; i++) {
						degree[i] += lines[i].booleanValue() ? 1 : 0;
					}
				}

				for (int i = 0; i < degree.length; i++) {
					if (degree[i] == 1) {
						Color[] colors = (Color[]) ((Vector) g.getUserDatum("colors")).toArray(new Color[0]);
						mv.setTerminus(true);
						System.out.println("found terminus at " + mv.getName());
						mv.setColor(colors[i]);
						break;
					}
				}
			}
		}
	}

}
