package mip.callback;

import ilog.concert.IloException;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex.LazyConstraintCallback;

import java.util.Iterator;
import java.util.Set;

import mip.MetroCplex;

public class AddCutCallback extends LazyConstraintCallback {

	MetroCplex cplex;

	public AddCutCallback(MetroCplex myCplex) {
		cplex = myCplex;
	}

	public void main() throws IloException {
		if (cplex.isAddCut()) {
			Set newCuts = cplex.getNewCuts();
			// IloRange[] cuts = (IloRange[]) newCuts.toArray(new IloRange[0]);
			System.out.println("will add " + newCuts.size() + " cuts at this point");
			cplex.increaseCutCounter(newCuts.size());

			try {
				int i = 0;
				for (Iterator iter = newCuts.iterator(); iter.hasNext(); i++) {
					IloRange cut = (IloRange) iter.next();
					// System.out.println( i + "th time in loop");

					add(cut);
				}

				/*
				 * for (int i = 0; i < cuts.length; i++) { System.out.println( i + "th time in loop"); if (cuts[i] != null) add(cuts[i]); }
				 */
			} catch (Exception e) {
				System.out.println("Caught exception " + e);
				e.printStackTrace();
			}
			// cplex.setNewCuts(null);
			newCuts.clear();
			cplex.setAddCut(false);
			// abort();
		}
	}

}
