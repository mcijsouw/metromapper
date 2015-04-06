package mip.callback;

import ilog.concert.IloException;
import ilog.cplex.IloCplex.IncumbentCallback;
import mip.MetroCplex;

public class Incumbent extends IncumbentCallback {

	MetroCplex cpx;
	int ctr;

	Incumbent(MetroCplex cpx) {
		this.cpx = cpx;
		ctr = 0;
	}

	protected void main() throws IloException {

		if (ctr % 2 == 1) {
			cpx.setAddCut(true);

			System.out.println("set flag to true and rejected");
			// reject();
		}
		ctr++;
	}

}
