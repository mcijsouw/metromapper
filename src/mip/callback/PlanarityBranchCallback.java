package mip.callback;

import ilog.concert.IloException;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex.BranchCallback;
import mip.MetroCplex;

public class PlanarityBranchCallback extends BranchCallback {

	MetroCplex cplex;

	public PlanarityBranchCallback(MetroCplex myCplex) {
		cplex = myCplex;
	}

	protected void main() throws IloException {

		if (cplex.isAddCut()) {
			System.out.println("performing branch cut...");
			makeBranch((IloRange[]) cplex.getNewCuts().toArray(new IloRange[0]), getObjValue());
		}
	}

}
