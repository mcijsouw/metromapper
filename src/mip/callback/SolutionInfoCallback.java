package mip.callback;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.MIPInfoCallback;
import io.GraphMLReader;
import io.GraphMLWriter;

import java.util.HashMap;
import java.util.Iterator;

import mip.GraphTools;
import mip.MetroCplex;
import model.MetroEdge;
import model.MetroVertex;
import model.TransferGraph;
import app.Settings;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.utils.UserData;

import app.MetroMapper;

public class SolutionInfoCallback extends MIPInfoCallback {

	Graph myGraph;
	StringLabeller vertexIDs;
	MetroCplex cplex;
	HashMap nameToVar;
	GraphMLWriter myWriter;
	int graphmlLoop;
	boolean labeled;
	String baseFile;
	double bestSolution = Double.MAX_VALUE;
	MetroMapper app;

	public SolutionInfoCallback(Graph g, MetroCplex myCplex, GraphMLWriter writer, String file, boolean isLabeled, MetroMapper app) {
		myGraph = g;
		vertexIDs = StringLabeller.getLabeller(myGraph);
		cplex = myCplex;
		nameToVar = cplex.getNameToVar();
		myWriter = writer;
		graphmlLoop = 0;
		baseFile = file;
		labeled = isLabeled;
		this.app = app;
	}

	@Override
	protected void main() throws IloException {

		// we have a new integer solution, so let's check it
		if (hasIncumbent() && (bestSolution > getIncumbentObjValue())) {
			bestSolution = getIncumbentObjValue();
			for (Iterator<MetroVertex> iter = myGraph.getVertices().iterator(); iter.hasNext();) {
				MetroVertex mv = iter.next();
				double x = getIncumbentValue((IloNumVar) nameToVar.get("x(" + vertexIDs.getLabel(mv) + ")"));
				double y = getIncumbentValue((IloNumVar) nameToVar.get("y(" + vertexIDs.getLabel(mv) + ")"));
				x = (Math.round(x * 10000)) / 10000.0;
				y = (Math.round(y * 10000)) / 10000.0;
				mv.setX(x);
				mv.setY(y);
			}
			for (Iterator iterator = myGraph.getEdges().iterator(); iterator.hasNext();) {
				MetroEdge me = (MetroEdge) iterator.next();
				if (me.isRegular() || me.isLabelConnectDummy() || me.isLabelMainDummy()) {
					me.setDirection((int) Math.round(getIncumbentValue((IloNumVar) nameToVar.get("dir(" + vertexIDs.getLabel((MetroVertex) me.getEndpoints().getFirst()) + ","
							+ vertexIDs.getLabel((MetroVertex) me.getEndpoints().getSecond()) + ")"))));
				}
			}

			try {

				// myGraph.setUserDatum("edgeLength",Double.valueOf(cplex.getLengthCosts()),UserData.SHARED);
				// myGraph.setUserDatum("sectorDeviation",Double.valueOf(cplex.getDeviateCosts()),UserData.SHARED);
				// myGraph.setUserDatum("bendCost",Double.valueOf(cplex.getBendCosts()),UserData.SHARED);
				myGraph.setUserDatum("objective", Double.valueOf((Math.round(10000 * getIncumbentObjValue()) / 10000.0)), UserData.SHARED);
				myGraph.setUserDatum("gap", Double.valueOf(1 - ((Math.round(10000 * getBestObjValue()) / 10000.0) / (Math.round(10000 * getIncumbentObjValue()) / 10000.0))), UserData.SHARED);
				myGraph.setUserDatum("elapsedTime", Double.valueOf((System.currentTimeMillis() - cplex.getStart()) / 1000.), UserData.SHARED);
				myGraph.setUserDatum("numConstraints", Integer.valueOf(getNrows()), UserData.SHARED);
				myGraph.setUserDatum("numVars", Integer.valueOf(getNcols()), UserData.SHARED);
				myGraph.setUserDatum("numBinVars", Integer.valueOf(cplex.getNbinVars()), UserData.SHARED);
				myGraph.setUserDatum("numIntVars", Integer.valueOf(cplex.getNintVars()), UserData.SHARED);
				myGraph.setUserDatum("addedCuts", Integer.valueOf(cplex.getCutCounter()), UserData.SHARED);

				String outFile = baseFile + "-" + ((graphmlLoop < 10) ? "0" : "") + graphmlLoop;
				graphmlLoop++;
				myWriter.writeGraphML(myGraph, outFile + ".graphml", labeled);
				System.out.println("Wrote solution to file " + outFile + ".graphml");
				
				// Load new graph
				this.app.initializeMap(outFile);
				
			} catch (Exception e) {
				System.out.println("unable to write file. Exception: " + e);
				e.printStackTrace();
			}

		}
	}

}
