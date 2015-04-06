package mip.callback;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.IncumbentCallback;
import io.GraphMLWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import mip.GraphTools;
import mip.MetroCplex;
import model.MetroEdge;
import model.MetroVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserData;

public class PlanarityCallback extends IncumbentCallback {

	Graph myGraph;
	StringLabeller vertexIDs;
	MetroCplex cplex;
	HashMap nameToVar;
	GraphMLWriter myWriter;
	int graphmlLoop;
	boolean ignore;
	boolean labeled;
	String baseFile;

	public PlanarityCallback(Graph g, MetroCplex myCplex, GraphMLWriter writer, String file, boolean isLabeled, boolean ignoreCrossings) {
		myGraph = g;
		vertexIDs = StringLabeller.getLabeller(myGraph);
		cplex = myCplex;
		nameToVar = cplex.getNameToVar();
		myWriter = writer;
		graphmlLoop = 0;
		baseFile = file;
		ignore = ignoreCrossings;
		labeled = isLabeled;
	}

	public boolean addPlanarityCuts(MetroEdge edge_i, MetroEdge edge_j) {
		boolean foundcut = false;
		Set cuts = cplex.getNewCuts();
		// System.out.println("has " + cuts.size() + " cuts");
		// nur zum testen von srn!!!!
		// if ((edge_i.isPendant() && !edge_i.isRegular()) || (edge_j.isPendant() && !edge_j.isRegular())) {
		// return;
		// }

		String edge_i_id = "{" + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getSecond()) + "}";
		String edge_j_id = "{" + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getSecond()) + "}";
		// System.out.println("checking " + edge_j_id + edge_i_id);

		if (cplex.getLazyCuts().containsKey(edge_i_id + "," + edge_j_id)) {
			cuts.addAll((Vector) cplex.getLazyCuts().get(edge_i_id + "," + edge_j_id));
			// System.out.println("added cuts for " + edge_i_id + edge_j_id);
			foundcut = true;
			// System.out.println("has " + cuts.size() + " cuts");
		} else if (cplex.getLazyCuts().containsKey(edge_j_id + "," + edge_i_id)) {
			cuts.addAll((Vector) cplex.getLazyCuts().get(edge_j_id + "," + edge_i_id));
			// System.out.println("added cuts for " + edge_j_id + edge_i_id);
			foundcut = true;
			// System.out.println("has " + cuts.size() + " cuts");
		} else {
			System.out.println("no lazy cut for " + edge_j_id + edge_i_id + " found.");
		}

		// cplex.setNewCuts(cuts);
		return foundcut;
	}

	protected void main() throws IloException {
		// we have a new integer solution, so let's check it
		for (Iterator iter = myGraph.getVertices().iterator(); iter.hasNext();) {
			MetroVertex mv = (MetroVertex) iter.next();
			double x = getValue((IloNumVar) nameToVar.get("x(" + vertexIDs.getLabel(mv) + ")"));
			double y = getValue((IloNumVar) nameToVar.get("y(" + vertexIDs.getLabel(mv) + ")"));
			x = (Math.round(x * 10000)) / 10000.0;
			y = (Math.round(y * 10000)) / 10000.0;
			mv.setX(x);
			mv.setY(y);
		}
		for (Iterator iterator = myGraph.getEdges().iterator(); iterator.hasNext();) {
			MetroEdge me = (MetroEdge) iterator.next();
			if (me.isRegular() || me.isLabelConnectDummy() || me.isLabelMainDummy()) {
				me.setDirection((int) Math.round(getValue((IloNumVar) nameToVar.get("dir(" + vertexIDs.getLabel((MetroVertex) me.getEndpoints().getFirst()) + ","
						+ vertexIDs.getLabel((MetroVertex) me.getEndpoints().getSecond()) + ")"))));
			}
		}

		// TODO: output of intermediate solution?!?
		System.out.println("found new solution");
		// BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		// char c=' ';
		// try {
		// c = in.readLine().charAt(0);
		// } catch (IOException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		//
		// if (c == 'y') {
		// try {
		// myWriter.writeGraphML(myGraph, baseFile + ((graphmlLoop<10)?"0":"") + graphmlLoop + ".graphml");
		// GraphTools.writeEPS(myGraph, baseFile + ((graphmlLoop<10)?"0":"") + graphmlLoop + ".eps", true, true);
		// System.out.println("Wrote solution to file " + baseFile + ((graphmlLoop<10)?"0":"") + (graphmlLoop++) + ".graphml");
		// } catch (Exception e) {
		// System.out.println("unable to write file. Exception: " + e);
		// e.printStackTrace();
		// }
		// }
		Set intersectingEdges = GraphTools.intersectingEdges(myGraph);
		// System.out.println("Do you really want to add the planarity constraints?");
		// BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		// char c=' ';
		// try {
		// c = in.readLine().charAt(0);
		// } catch (IOException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		if (!intersectingEdges.isEmpty() && !ignore /* && c=='y' */) {
			boolean improvement = false;
			for (Iterator iter = intersectingEdges.iterator(); iter.hasNext();) {
				Pair pair = (Pair) iter.next();
				MetroEdge edge1 = (MetroEdge) pair.getFirst();
				MetroEdge edge2 = (MetroEdge) pair.getSecond();
				// add lazy constraint here!
				improvement = addPlanarityCuts(edge1, edge2) || improvement;

				// System.out.println("Edges " + ((MetroVertex) edge1.getEndpoints().getFirst()).getName() + " "
				// + ((MetroVertex) edge1.getEndpoints().getSecond()).getName() + " and "
				// + ((MetroVertex) edge2.getEndpoints().getFirst()).getName() + " "
				// + ((MetroVertex) edge2.getEndpoints().getSecond()).getName() + " intersect.");

			}
			if (improvement) {
				cplex.setAddCut(true);
				reject();

			} else {
				System.out.println("Lazy cuts for intersecting edges not found. Continuing.");
				try {

					// myGraph.setUserDatum("edgeLength",Double.valueOf(cplex.getLengthCosts()),UserData.SHARED);
					// myGraph.setUserDatum("sectorDeviation",Double.valueOf(cplex.getDeviateCosts()),UserData.SHARED);
					// myGraph.setUserDatum("bendCost",Double.valueOf(cplex.getBendCosts()),UserData.SHARED);
					myGraph.setUserDatum("objective", Double.valueOf((Math.round(10000 * getObjValue()) / 10000.0)), UserData.SHARED);
					myGraph.setUserDatum("gap", Double.valueOf(1 - ((Math.round(10000 * getBestObjValue()) / 10000.0) / (Math.round(10000 * getObjValue()) / 10000.0))), UserData.SHARED);
					myGraph.setUserDatum("elapsedTime", Double.valueOf((System.currentTimeMillis() - cplex.getStart()) / 1000.), UserData.SHARED);
					myGraph.setUserDatum("numConstraints", Integer.valueOf(getNrows()), UserData.SHARED);
					myGraph.setUserDatum("numVars", Integer.valueOf(getNcols()), UserData.SHARED);
					myGraph.setUserDatum("numBinVars", Integer.valueOf(cplex.getNbinVars()), UserData.SHARED);
					myGraph.setUserDatum("numIntVars", Integer.valueOf(cplex.getNintVars()), UserData.SHARED);
					myGraph.setUserDatum("addedCuts", Integer.valueOf(cplex.getCutCounter()), UserData.SHARED);

					myWriter.writeGraphML(myGraph, baseFile + ((graphmlLoop < 10) ? "0" : "") + graphmlLoop + ".graphml", labeled);
					GraphTools.writeEPS(myGraph, baseFile + ((graphmlLoop < 10) ? "0" : "") + graphmlLoop + "box.eps", true, labeled, true);
					GraphTools.writeEPS(myGraph, baseFile + ((graphmlLoop < 10) ? "0" : "") + graphmlLoop + ".eps", true, labeled, false);
					System.out.println("Wrote solution to file " + baseFile + ((graphmlLoop < 10) ? "0" : "") + (graphmlLoop++) + ".graphml");
				} catch (Exception e) {
					System.out.println("unable to write file. Exception: " + e);
					e.printStackTrace();
				}
			}
		} else {
			try {

				// myGraph.setUserDatum("edgeLength",Double.valueOf(cplex.getLengthCosts()),UserData.SHARED);
				// myGraph.setUserDatum("sectorDeviation",Double.valueOf(cplex.getDeviateCosts()),UserData.SHARED);
				// myGraph.setUserDatum("bendCost",Double.valueOf(cplex.getBendCosts()),UserData.SHARED);
				myGraph.setUserDatum("objective", Double.valueOf((Math.round(10000 * getObjValue()) / 10000.0)), UserData.SHARED);
				myGraph.setUserDatum("gap", Double.valueOf(1 - ((Math.round(10000 * getBestObjValue()) / 10000.0) / (Math.round(10000 * getObjValue()) / 10000.0))), UserData.SHARED);
				myGraph.setUserDatum("elapsedTime", Double.valueOf((System.currentTimeMillis() - cplex.getStart()) / 1000.), UserData.SHARED);
				myGraph.setUserDatum("numConstraints", Integer.valueOf(getNrows()), UserData.SHARED);
				myGraph.setUserDatum("numVars", Integer.valueOf(getNcols()), UserData.SHARED);
				myGraph.setUserDatum("numBinVars", Integer.valueOf(cplex.getNbinVars()), UserData.SHARED);
				myGraph.setUserDatum("numIntVars", Integer.valueOf(cplex.getNintVars()), UserData.SHARED);
				myGraph.setUserDatum("addedCuts", Integer.valueOf(cplex.getCutCounter()), UserData.SHARED);

				myWriter.writeGraphML(myGraph, baseFile + ((graphmlLoop < 10) ? "0" : "") + graphmlLoop + ".graphml", labeled);
				GraphTools.writeEPS(myGraph, baseFile + ((graphmlLoop < 10) ? "0" : "") + graphmlLoop + "box.eps", true, labeled, true);
				GraphTools.writeEPS(myGraph, baseFile + ((graphmlLoop < 10) ? "0" : "") + graphmlLoop + ".eps", true, labeled, false);
				System.out.println("Wrote solution to file " + baseFile + ((graphmlLoop < 10) ? "0" : "") + (graphmlLoop++) + ".graphml");
			} catch (Exception e) {
				System.out.println("unable to write file. Exception: " + e);
				e.printStackTrace();
			}
		}
	}

}
