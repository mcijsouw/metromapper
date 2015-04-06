package mip;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Goal;
import io.GraphMLWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import model.MetroEdge;
import model.MetroVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.utils.Pair;

public class PlanarityGoal extends Goal {

	Graph myGraph;
	StringLabeller vertexIDs;
	MetroCplex cplex;
	HashMap nameToVar, lazyCuts;
	GraphMLWriter myWriter;
	static int graphmlLoop;
	String baseFile;

	PlanarityGoal(Graph g, MetroCplex myCplex, GraphMLWriter writer, String file) {
		myGraph = g;
		vertexIDs = StringLabeller.getLabeller(myGraph);
		cplex = myCplex;
		nameToVar = cplex.getNameToVar();
		this.lazyCuts = myCplex.getLazyCuts();
		myWriter = writer;
		graphmlLoop = 0;
		baseFile = file;
	}

	public Vector addPlanarityCuts(MetroEdge edge_i, MetroEdge edge_j) {
		// Set cuts = cplex.getNewCuts();
		// System.out.println("has " + cuts.size() + " cuts");
		// nur zum testen von srn!!!!
		// if ((edge_i.isPendant() && !edge_i.isRegular()) || (edge_j.isPendant() && !edge_j.isRegular())) {
		// return;
		// }

		String edge_i_id = "{" + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getSecond()) + "}";
		String edge_j_id = "{" + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getSecond()) + "}";
		if (lazyCuts.containsKey(edge_i_id + "," + edge_j_id)) {
			Vector v = (Vector) lazyCuts.get(edge_i_id + "," + edge_j_id);
			// lazyCuts.remove(edge_i_id + "," + edge_j_id);
			System.out.println("added cuts for " + edge_i_id + edge_j_id);
			System.out.println("has " + v.size() + " cuts");
			return v;
		} else if (lazyCuts.containsKey(edge_j_id + "," + edge_i_id)) {
			Vector v = (Vector) lazyCuts.get(edge_j_id + "," + edge_i_id);
			// lazyCuts.remove(edge_j_id + "," + edge_i_id);
			System.out.println("added cuts for " + edge_j_id + edge_i_id);
			System.out.println("has " + v.size() + " cuts");
			return v;
		}
		return null;
		// cplex.setNewCuts(cuts);
	}

	public Goal execute(IloCplex cpl) throws IloException {

		if (!isIntegerFeasible()) {
			// System.out.println("executed goal but infeasible");
			return cpl.and(cpl.branchAsCplex(), this);
		}

		for (Iterator iter = myGraph.getVertices().iterator(); iter.hasNext();) {
			MetroVertex mv = (MetroVertex) iter.next();
			double x = getValue((IloNumVar) nameToVar.get("x(" + vertexIDs.getLabel(mv) + ")"));
			double y = getValue((IloNumVar) nameToVar.get("y(" + vertexIDs.getLabel(mv) + ")"));
			x = (Math.round(x * 10000)) / 10000.0;
			y = (Math.round(y * 10000)) / 10000.0;
			mv.setX(x);
			mv.setY(y);
		}

		// TODO: output of intermediate solution?!?
		System.out.println("found new solution");
		Set intersectingEdges = GraphTools.intersectingEdges(myGraph);

		if (!intersectingEdges.isEmpty()) {
			try {
				myWriter.writeGraphML(myGraph, baseFile + "_np" + ((graphmlLoop < 10) ? "0" : "") + graphmlLoop + ".graphml");
				System.out.println("Wrote solution to file " + baseFile + "_np" + ((graphmlLoop < 10) ? "0" : "") + (graphmlLoop++) + ".graphml");
			} catch (IOException e) {
				System.out.println("unable to write file. Exception: " + e);
			}
			Vector cuts = new Vector();
			for (Iterator iter = intersectingEdges.iterator(); iter.hasNext();) {
				Pair pair = (Pair) iter.next();
				MetroEdge edge1 = (MetroEdge) pair.getFirst();
				MetroEdge edge2 = (MetroEdge) pair.getSecond();
				// TODO: add lazy constraint here!
				Vector v = addPlanarityCuts(edge1, edge2);
				if (v != null)
					cuts.addAll(v);

				System.out.println("Edges " + ((MetroVertex) edge1.getEndpoints().getFirst()).getName() + " " + ((MetroVertex) edge1.getEndpoints().getSecond()).getName() + " and "
						+ ((MetroVertex) edge2.getEndpoints().getFirst()).getName() + " " + ((MetroVertex) edge2.getEndpoints().getSecond()).getName() + " intersect.");

			}

			// Set cuts = cplex.getNewCuts();
			IloCplex.Goal goal = this;
			System.out.println("Adding " + cuts.size() + " cuts...");
			goal = cpl.and(cpl.globalCutGoal((IloRange[]) cuts.toArray(new IloRange[0])), this);

			return goal;

		}
		try {
			myWriter.writeGraphML(myGraph, baseFile + ((graphmlLoop < 10) ? "0" : "") + graphmlLoop + ".graphml");
			System.out.println("Wrote solution to file " + baseFile + ((graphmlLoop < 10) ? "0" : "") + (graphmlLoop++) + ".graphml");
		} catch (IOException e) {
			System.out.println("unable to write file. Exception: " + e);
		}
		return null;
	}

}
