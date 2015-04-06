package mip.callback;

import ilog.concert.IloException;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex.LazyConstraintCallback;
import mip.MetroCplex;

public class Cut extends LazyConstraintCallback {

	MetroCplex cpx;
	IloRange cut;

	Cut(MetroCplex cpx, IloRange cut) {
		this.cpx = cpx;
		this.cut = cut;
	}

	protected void main() throws IloException {

		if (cpx.isAddCut()) {
			System.out.println("trying to add cut");
			add(cut);
			System.out.println("added test cut");
			cpx.setAddCut(false);
		}
	}

}
