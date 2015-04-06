package mip;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import model.MetroEdge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;

public class MetroCplex extends IloCplex {

	HashMap nameToVar;
	Set bendVars, lengthVars, deviateVars, intVars, newCuts;
	boolean addCut = false;
	HashMap lazyCuts;
	long start;
	int counter;

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public MetroCplex() throws IloException {
		super();
		nameToVar = null;
		bendVars = null;
		lengthVars = null;
		deviateVars = null;
		intVars = null;
		lazyCuts = new HashMap();
		newCuts = new HashSet();
		counter = 0;
		// TODO Auto-generated constructor stub
	}

	public IloConstraint addLazyConstraint(IloConstraint lazyCut, boolean cplexPool) throws IloException {

		if (!cplexPool) {
			String name = lazyCut.getName();
			if (lazyCuts.containsKey(name)) {// add this cut to the existing Vector
				((Vector) lazyCuts.get(name)).add(lazyCut);
			} else {
				Vector edgeCuts = new Vector(32);
				edgeCuts.add(lazyCut);
				lazyCuts.put(name, edgeCuts);
			}
			return lazyCut;
		} else {
			return super.addLazyConstraint((IloRange) lazyCut); // @michel: casted
		}
	}

	public void addPlanarityConstraint(Graph g, MetroEdge edge_i, MetroEdge edge_j) throws IloException {
		final int MINDIST = 1;
		final int MPOS = ((Integer) g.getUserDatum("origNumVertices")).intValue() + 1;

		// nur zum testen von srn!!!!
		// if ((edge_i.isPendant() && !edge_i.isRegular()) || (edge_j.isPendant() && !edge_j.isRegular())) {
		// return;
		// }

		StringLabeller vertexIDs = StringLabeller.getLabeller(g);

		String edge_i_id = "{" + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getSecond()) + "}";
		String edge_j_id = "{" + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getSecond()) + "}";
		// create binary vars for the eight possibilities of the relative position of the edegs
		IloNumVar w_i_j, w_j_i, s_i_j, s_j_i, sw_i_j, sw_j_i, nw_i_j, nw_j_i;
		w_i_j = boolVar("w(" + edge_i_id + "," + edge_j_id + ")");
		w_j_i = boolVar("w(" + edge_j_id + "," + edge_i_id + ")");
		s_i_j = boolVar("s(" + edge_i_id + "," + edge_j_id + ")");
		s_j_i = boolVar("s(" + edge_j_id + "," + edge_i_id + ")");
		sw_i_j = boolVar("sw(" + edge_i_id + "," + edge_j_id + ")");
		sw_j_i = boolVar("sw(" + edge_j_id + "," + edge_i_id + ")");
		nw_i_j = boolVar("nw(" + edge_i_id + "," + edge_j_id + ")");
		nw_j_i = boolVar("nw(" + edge_j_id + "," + edge_i_id + ")");
		nameToVar.put("w(" + edge_i_id + "," + edge_j_id + ")", w_i_j);
		nameToVar.put("w(" + edge_j_id + "," + edge_i_id + ")", w_j_i);
		nameToVar.put("s(" + edge_i_id + "," + edge_j_id + ")", s_i_j);
		nameToVar.put("s(" + edge_j_id + "," + edge_i_id + ")", s_j_i);
		nameToVar.put("sw(" + edge_i_id + "," + edge_j_id + ")", sw_i_j);
		nameToVar.put("sw(" + edge_j_id + "," + edge_i_id + ")", sw_j_i);
		nameToVar.put("nw(" + edge_i_id + "," + edge_j_id + ")", nw_i_j);
		nameToVar.put("nw(" + edge_j_id + "," + edge_i_id + ")", nw_j_i);

		IloLinearNumExpr myExpr = linearNumExpr();
		myExpr.addTerm(1, w_i_j);
		myExpr.addTerm(1, w_j_i);
		myExpr.addTerm(1, s_i_j);
		myExpr.addTerm(1, s_j_i);
		myExpr.addTerm(1, sw_i_j);
		myExpr.addTerm(1, sw_j_i);
		myExpr.addTerm(1, nw_i_j);
		myExpr.addTerm(1, nw_j_i);
		addGe(myExpr, 1);

		String edge_i_first = vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getFirst());
		String edge_i_second = vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getSecond());
		String edge_j_first = vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getFirst());
		String edge_j_second = vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getSecond());
		// case w(edge_i, edge_j) == 1
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("x(" + edge_i_first + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("x(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, w_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("x(" + edge_i_first + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("x(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, w_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("x(" + edge_i_second + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("x(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, w_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("x(" + edge_i_second + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("x(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, w_i_j);
		addLe(myExpr, MPOS - MINDIST);

		// case w(edge_j, edge_i) == 1
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("x(" + edge_i_first + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("x(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, w_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("x(" + edge_i_first + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("x(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, w_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("x(" + edge_i_second + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("x(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, w_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("x(" + edge_i_second + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("x(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, w_j_i);
		addLe(myExpr, MPOS - MINDIST);

		// case s(edge_i, edge_j) == 1
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("y(" + edge_i_first + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("y(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, s_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("y(" + edge_i_first + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("y(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, s_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("y(" + edge_i_second + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("y(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, s_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("y(" + edge_i_second + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("y(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, s_i_j);
		addLe(myExpr, MPOS - MINDIST);

		// case s(edge_j, edge_i) == 1
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("y(" + edge_i_first + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("y(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, s_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("y(" + edge_i_first + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("y(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, s_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("y(" + edge_i_second + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("y(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, s_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("y(" + edge_i_second + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("y(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, s_j_i);
		addLe(myExpr, MPOS - MINDIST);

		// case sw(edge_i, edge_j) == 1
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z1(" + edge_i_first + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z1(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, sw_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z1(" + edge_i_first + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z1(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, sw_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z1(" + edge_i_second + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z1(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, sw_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z1(" + edge_i_second + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z1(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, sw_i_j);
		addLe(myExpr, MPOS - MINDIST);

		// case sw(edge_j, edge_i) == 1
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z1(" + edge_i_first + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z1(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, sw_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z1(" + edge_i_first + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z1(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, sw_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z1(" + edge_i_second + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z1(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, sw_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z1(" + edge_i_second + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z1(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, sw_j_i);
		addLe(myExpr, MPOS - MINDIST);

		// case nw(edge_i, edge_j) == 1
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z2(" + edge_i_first + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z2(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, nw_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z2(" + edge_i_first + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z2(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, nw_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z2(" + edge_i_second + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z2(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, nw_i_j);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z2(" + edge_i_second + ")"));
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z2(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, nw_i_j);
		addLe(myExpr, MPOS - MINDIST);

		// case nw(edge_j, edge_i) == 1
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z2(" + edge_i_first + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z2(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, nw_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z2(" + edge_i_first + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z2(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, nw_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z2(" + edge_i_second + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z2(" + edge_j_first + ")"));
		myExpr.addTerm(MPOS, nw_j_i);
		addLe(myExpr, MPOS - MINDIST);
		myExpr = linearNumExpr();
		myExpr.addTerm(-1, (IloNumVar) nameToVar.get("z2(" + edge_i_second + ")"));
		myExpr.addTerm(1, (IloNumVar) nameToVar.get("z2(" + edge_j_second + ")"));
		myExpr.addTerm(MPOS, nw_j_i);
		addLe(myExpr, MPOS - MINDIST);

	}

	public HashMap getNameToVar() {
		return nameToVar;
	}

	public void setNameToVar(HashMap nameToVar) {
		this.nameToVar = nameToVar;
	}

	public double getValue(String name) throws UnknownObjectException, IloException {
		double val = getValue((IloNumVar) nameToVar.get(name));
		val = (Math.round(val * 10000)) / 10000.0;
		return val;
	}

	public Set getBendVars() {
		return bendVars;
	}

	public void setBendVars(Set bendVars) {
		this.bendVars = bendVars;
	}

	public Set getDeviateVars() {
		return deviateVars;
	}

	public void setDeviateVars(Set deviateVars) {
		this.deviateVars = deviateVars;
	}

	public Set getLengthVars() {
		return lengthVars;
	}

	public void setLengthVars(Set lengthVars) {
		this.lengthVars = lengthVars;
	}

	public double getBendCosts() throws UnknownObjectException, IloException {
		double costs = 0.0;
		for (Iterator iter = bendVars.iterator(); iter.hasNext();) {
			IloNumVar bendVar = (IloNumVar) iter.next();
			costs += (Math.round(10000 * getValue(bendVar))) / 10000.0;
		}
		return costs;
	}

	public double getLengthCosts() throws UnknownObjectException, IloException {
		double costs = 0.0;
		for (Iterator iter = lengthVars.iterator(); iter.hasNext();) {
			IloNumVar lengthVar = (IloNumVar) iter.next();
			costs += (Math.round(10000 * getValue(lengthVar))) / 10000.0;
		}
		return costs;
	}

	public double getDeviateCosts() throws UnknownObjectException, IloException {
		double costs = 0.0;
		for (Iterator iter = deviateVars.iterator(); iter.hasNext();) {
			IloNumVar deviateVar = (IloNumVar) iter.next();
			costs += (Math.round(10000 * getValue(deviateVar))) / 10000.0;
		}
		return costs;
	}

	public Set getIntVars() {
		return intVars;
	}

	public void setIntVars(Set intVars) {
		this.intVars = intVars;
	}

	public boolean isAddCut() {
		return addCut;
	}

	public void setAddCut(boolean addCut) {
		this.addCut = addCut;
	}

	public Set getNewCuts() {
		return newCuts;
	}

	public void setNewCuts(Set newCuts) {
		this.newCuts = newCuts;
	}

	public HashMap getLazyCuts() {
		return lazyCuts;
	}

	public int getCutCounter() {
		return counter;
	}

	public void increaseCutCounter(int counter) {
		this.counter += counter;
	}
}
