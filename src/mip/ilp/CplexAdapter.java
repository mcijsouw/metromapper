package mip.ilp;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import mip.MetroCplex;
import model.MetroEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;

public class CplexAdapter {

	static boolean cplexLazyPool = false; // do we handle the constraints ourselves using controlCallback (false) or should CPLEX do it (true)?

	public static MetroCplex relaxToCplex(ILP myILP, Graph g) {
		MetroCplex myCplex = null;
		try {
			myCplex = new MetroCplex();
			HashMap nameToCplexVar = new HashMap();
			StringLabeller vertexIDs = StringLabeller.getLabeller(g);
			Set bends = new HashSet();
			Set lengths = new HashSet();
			Set deviate = new HashSet();
			Set ints = new HashSet();

			// create the Cplex variables
			for (Iterator iter = myILP.getVariables().iterator(); iter.hasNext();) {
				Variable var = (Variable) iter.next();
				String varName = myILP.getVariableNames().getName(var);
				IloNumVar cplexVar;
				if (var.isBinary()) {
					cplexVar = myCplex.numVar(0, 1, varName);
				} else if (var.isGeneral()) {
					cplexVar = myCplex.numVar(var.getLower(), var.getUpper(), varName);
				} else {
					cplexVar = myCplex.numVar(var.getLower(), var.getUpper(), varName);
				}
				nameToCplexVar.put(varName, cplexVar);
				if (varName.startsWith("D(")) {
					lengths.add(cplexVar);
				} else if (varName.startsWith("dev(")) {
					bends.add(cplexVar);
				} else if (varName.startsWith("sel_d")) {
					deviate.add(cplexVar);
				}
			}

			// trying to create all possible intersection binary variables
			Edge[] edges = (Edge[]) g.getEdges().toArray(new Edge[0]);
			for (int i = 0; i < edges.length; i++) {
				for (int j = i + 1; j < edges.length; j++) {
					// edge pair must not be incident
					MetroEdge edge_i = (MetroEdge) edges[i];
					MetroEdge edge_j = (MetroEdge) edges[j];
					if (edge_i.isIncident((Vertex) edge_j.getEndpoints().getFirst()) || edge_i.isIncident((Vertex) edge_j.getEndpoints().getSecond()))
						continue;
					String edge_i_id = "{" + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getSecond()) + "}";
					String edge_j_id = "{" + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getSecond()) + "}";
					IloNumVar w_i_j, w_j_i, s_i_j, s_j_i, sw_i_j, sw_j_i, nw_i_j, nw_j_i;
					w_i_j = myCplex.numVar(0, 1, "w(" + edge_i_id + "," + edge_j_id + ")");
					w_j_i = myCplex.numVar(0, 1, "w(" + edge_j_id + "," + edge_i_id + ")");
					s_i_j = myCplex.numVar(0, 1, "s(" + edge_i_id + "," + edge_j_id + ")");
					s_j_i = myCplex.numVar(0, 1, "s(" + edge_j_id + "," + edge_i_id + ")");
					sw_i_j = myCplex.numVar(0, 1, "sw(" + edge_i_id + "," + edge_j_id + ")");
					sw_j_i = myCplex.numVar(0, 1, "sw(" + edge_j_id + "," + edge_i_id + ")");
					nw_i_j = myCplex.numVar(0, 1, "nw(" + edge_i_id + "," + edge_j_id + ")");
					nw_j_i = myCplex.numVar(0, 1, "nw(" + edge_j_id + "," + edge_i_id + ")");
					nameToCplexVar.put("w(" + edge_i_id + "," + edge_j_id + ")", w_i_j);
					nameToCplexVar.put("w(" + edge_j_id + "," + edge_i_id + ")", w_j_i);
					nameToCplexVar.put("s(" + edge_i_id + "," + edge_j_id + ")", s_i_j);
					nameToCplexVar.put("s(" + edge_j_id + "," + edge_i_id + ")", s_j_i);
					nameToCplexVar.put("sw(" + edge_i_id + "," + edge_j_id + ")", sw_i_j);
					nameToCplexVar.put("sw(" + edge_j_id + "," + edge_i_id + ")", sw_j_i);
					nameToCplexVar.put("nw(" + edge_i_id + "," + edge_j_id + ")", nw_i_j);
					nameToCplexVar.put("nw(" + edge_j_id + "," + edge_i_id + ")", nw_j_i);

				}
			}

			myCplex.setNameToVar(nameToCplexVar);
			myCplex.setLengthVars(lengths);
			myCplex.setBendVars(bends);
			myCplex.setDeviateVars(deviate);

			// create the constraints
			for (Iterator iter = myILP.getConstraints().iterator(); iter.hasNext();) {
				Constraint con = (Constraint) iter.next();
				IloLinearNumExpr myExpr = myCplex.linearNumExpr();
				// add summands to the expression
				for (Iterator it2 = con.getSummands().iterator(); it2.hasNext();) {
					Summand term = (Summand) it2.next();
					myExpr.addTerm(term.getFactor(), (IloNumVar) nameToCplexVar.get(term.getVariableName()));
				}

				if (con.isLess()) {
					if (con.isLazy()) {
						myCplex.addLazyConstraint(myCplex.le(myExpr, con.getRhs(), con.getName()), cplexLazyPool);
					} else {
						myCplex.addLe(myExpr, con.getRhs(), con.getName());
					}
				} else if (con.isEqual()) {
					if (con.isLazy()) {
						myCplex.addLazyConstraint(myCplex.eq(myExpr, con.getRhs(), con.getName()), cplexLazyPool);
					} else {
						myCplex.addEq(myExpr, con.getRhs(), con.getName());
					}
				} else {
					if (con.isLazy()) {
						myCplex.addLazyConstraint(myCplex.ge(myExpr, con.getRhs(), con.getName()), cplexLazyPool);
					} else {
						myCplex.addGe(myExpr, con.getRhs(), con.getName());
					}
				}

			}

			// create the objective
			IloLinearNumExpr myExpr = myCplex.linearNumExpr();
			ObjectiveFunction obj = myILP.getObjFct();
			for (Iterator iter = obj.getSummands().iterator(); iter.hasNext();) {
				Summand term = (Summand) iter.next();

				// ########################
				// experimental: count bends on multiedges only once...
				if (term.getVariableName().startsWith("dev(")) {
					myExpr.remove((IloNumVar) nameToCplexVar.get(term.getVariableName()));
				}
				// ########################

				myExpr.addTerm(term.getFactor(), (IloNumVar) nameToCplexVar.get(term.getVariableName()));
			}
			if (obj.isMinimize()) {
				myCplex.addMinimize(myExpr, obj.getName());
			} else {
				myCplex.addMaximize(myExpr, obj.getName());
			}

			return myCplex;
		} catch (IloException e) {
			System.err.println("Concert exception caught: " + e);
		}
		return myCplex;
	}

	public static MetroCplex convertToCplex(ILP myILP, Graph g) {
		MetroCplex myCplex = null;
		try {
			myCplex = new MetroCplex();
			HashMap nameToCplexVar = new HashMap();
			StringLabeller vertexIDs = StringLabeller.getLabeller(g);
			Set bends = new HashSet();
			Set lengths = new HashSet();
			Set deviate = new HashSet();
			Set ints = new HashSet();

			// create the Cplex variables
			for (Iterator iter = myILP.getVariables().iterator(); iter.hasNext();) {
				Variable var = (Variable) iter.next();
				String varName = myILP.getVariableNames().getName(var);
				IloNumVar cplexVar;
				if (var.isBinary()) {
					cplexVar = myCplex.boolVar(varName);
					ints.add(cplexVar);

				} else if (var.isGeneral()) {
					cplexVar = myCplex.intVar((int) var.getLower(), (int) var.getUpper(), varName);
					ints.add(cplexVar);

				} else {
					cplexVar = myCplex.numVar(var.getLower(), var.getUpper(), varName);

				}
				nameToCplexVar.put(varName, cplexVar);
				if (varName.startsWith("D(")) {
					lengths.add(cplexVar);
				} else if (varName.startsWith("dev(")) {
					bends.add(cplexVar);
				} else if (varName.startsWith("sel_d")) {
					deviate.add(cplexVar);
				}
			}

			// trying to create all possible intersection binary variables
			Edge[] edges = (Edge[]) g.getEdges().toArray(new Edge[0]);
			for (int i = 0; i < edges.length; i++) {
				for (int j = i + 1; j < edges.length; j++) {
					// edge pair must not be incident
					MetroEdge edge_i = (MetroEdge) edges[i];
					MetroEdge edge_j = (MetroEdge) edges[j];
					if (edge_i.isIncident((Vertex) edge_j.getEndpoints().getFirst()) || edge_i.isIncident((Vertex) edge_j.getEndpoints().getSecond()))
						continue;
					String edge_i_id = "{" + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getSecond()) + "}";
					String edge_j_id = "{" + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getSecond()) + "}";
					IloNumVar w_i_j, w_j_i, s_i_j, s_j_i, sw_i_j, sw_j_i, nw_i_j, nw_j_i;
					w_i_j = myCplex.boolVar("w(" + edge_i_id + "," + edge_j_id + ")");
					w_j_i = myCplex.boolVar("w(" + edge_j_id + "," + edge_i_id + ")");
					s_i_j = myCplex.boolVar("s(" + edge_i_id + "," + edge_j_id + ")");
					s_j_i = myCplex.boolVar("s(" + edge_j_id + "," + edge_i_id + ")");
					sw_i_j = myCplex.boolVar("sw(" + edge_i_id + "," + edge_j_id + ")");
					sw_j_i = myCplex.boolVar("sw(" + edge_j_id + "," + edge_i_id + ")");
					nw_i_j = myCplex.boolVar("nw(" + edge_i_id + "," + edge_j_id + ")");
					nw_j_i = myCplex.boolVar("nw(" + edge_j_id + "," + edge_i_id + ")");
					nameToCplexVar.put("w(" + edge_i_id + "," + edge_j_id + ")", w_i_j);
					nameToCplexVar.put("w(" + edge_j_id + "," + edge_i_id + ")", w_j_i);
					nameToCplexVar.put("s(" + edge_i_id + "," + edge_j_id + ")", s_i_j);
					nameToCplexVar.put("s(" + edge_j_id + "," + edge_i_id + ")", s_j_i);
					nameToCplexVar.put("sw(" + edge_i_id + "," + edge_j_id + ")", sw_i_j);
					nameToCplexVar.put("sw(" + edge_j_id + "," + edge_i_id + ")", sw_j_i);
					nameToCplexVar.put("nw(" + edge_i_id + "," + edge_j_id + ")", nw_i_j);
					nameToCplexVar.put("nw(" + edge_j_id + "," + edge_i_id + ")", nw_j_i);

				}
			}

			myCplex.setNameToVar(nameToCplexVar);
			myCplex.setLengthVars(lengths);
			myCplex.setBendVars(bends);
			myCplex.setDeviateVars(deviate);

			// create the constraints
			for (Iterator iter = myILP.getConstraints().iterator(); iter.hasNext();) {
				Constraint con = (Constraint) iter.next();
				IloLinearNumExpr myExpr = myCplex.linearNumExpr();
				// add summands to the expression
				for (Iterator it2 = con.getSummands().iterator(); it2.hasNext();) {
					Summand term = (Summand) it2.next();
					myExpr.addTerm(term.getFactor(), (IloNumVar) nameToCplexVar.get(term.getVariableName()));
				}

				if (con.isLess()) {
					if (con.isLazy()) {
						myCplex.addLazyConstraint(myCplex.le(myExpr, con.getRhs(), con.getName()), cplexLazyPool);
					} else {
						myCplex.addLe(myExpr, con.getRhs(), con.getName());
					}
				} else if (con.isEqual()) {
					if (con.isLazy()) {
						myCplex.addLazyConstraint(myCplex.eq(myExpr, con.getRhs(), con.getName()), cplexLazyPool);
					} else {
						myCplex.addEq(myExpr, con.getRhs(), con.getName());
					}
				} else {
					if (con.isLazy()) {
						myCplex.addLazyConstraint(myCplex.ge(myExpr, con.getRhs(), con.getName()), cplexLazyPool);
					} else {
						myCplex.addGe(myExpr, con.getRhs(), con.getName());
					}
				}

			}

			// create the objective
			IloLinearNumExpr myExpr = myCplex.linearNumExpr();
			ObjectiveFunction obj = myILP.getObjFct();
			for (Iterator iter = obj.getSummands().iterator(); iter.hasNext();) {
				Summand term = (Summand) iter.next();

				// ########################
				// experimental: count bends on multiedges only once...
				if (term.getVariableName().startsWith("dev(")) {
					myExpr.remove((IloNumVar) nameToCplexVar.get(term.getVariableName()));
				}
				// ########################

				myExpr.addTerm(term.getFactor(), (IloNumVar) nameToCplexVar.get(term.getVariableName()));
			}
			if (obj.isMinimize()) {
				myCplex.addMinimize(myExpr, obj.getName());
			} else {
				myCplex.addMaximize(myExpr, obj.getName());
			}

			for (Iterator it = ints.iterator(); it.hasNext();) {
				IloNumVar var = (IloNumVar) it.next();
				if (var.getName().startsWith("sel_b") || var.getName().startsWith("nw") || var.getName().startsWith("n") || var.getName().startsWith("s") || var.getName().startsWith("sw")
						|| var.getName().startsWith("sel_c")) {
					System.out.println("setting priority for " + var);
					if (var.getName().startsWith("sel_b")) {
						myCplex.setPriority(var, 2);
						// myCplex.setDirection(var, IloCplex.BranchDirection.Up);
					} else {
						myCplex.setPriority(var, 1);
						// myCplex.setDirection(var, IloCplex.BranchDirection.Up);
					}
				}

			}

			return myCplex;
		} catch (IloException e) {
			System.err.println("Concert exception caught: " + e);
		}
		return myCplex;
	}

}
