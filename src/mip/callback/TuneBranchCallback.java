package mip.callback;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.BranchCallback;
import ilog.cplex.IloCplex.BranchDirection;
import ilog.cplex.IloCplex.BranchType;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TuneBranchCallback extends BranchCallback {

	/*
	 * (non-Javadoc)
	 * 
	 * @see ilog.cplex.IloCplex.Callback#main()
	 */
	@Override
	protected void main() throws IloException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		IloNumVar[][] vars = new IloNumVar[getNbranches()][];
		double[][] bounds = new double[getNbranches()][];
		BranchDirection[][] dirs = new BranchDirection[getNbranches()][];
		double[] estimates = getBranches(vars, bounds, dirs);

		if (getBranchType().equals(BranchType.BranchOnVariable) && vars[0][0].getName().startsWith("sel_b") && getValue(vars[0][0]) >= 0.25) {
			makeBranch(vars[0][0], 1.0, BranchDirection.Up, estimates[0]);
			makeBranch(vars[0][0], 0.0, BranchDirection.Down, estimates[0]);
		} else if (getBranchType().equals(BranchType.BranchOnVariable)
				&& (vars[0][0].getName().startsWith("w(") || vars[0][0].getName().startsWith("s(") || vars[0][0].getName().startsWith("nw(") || vars[0][0].getName().startsWith("sw("))
				&& getValue(vars[0][0]) >= 0.25) {
			makeBranch(vars[0][0], 1.0, BranchDirection.Up, estimates[0]);
			makeBranch(vars[0][0], 0.0, BranchDirection.Down, estimates[0]);
		}
		System.out.println(vars[0][0].getName() + " " + getValue(vars[0][0]) + " " + getDirection(vars[0][0]));
		/*
		 * System.out.println("Values of current branch node"); for (int i = 0; i < getNbranches(); i++) { for (int j = 0; j < vars[i].length; j++) { System.out.println(vars[i][j].getName());
		 * System.out.println(getValue(vars[i][j])); System.out.println(bounds[i][j]); System.out.println(dirs[i][j]); } System.out.println(estimates[i]);
		 * 
		 * }
		 */
		/*
		 * try { in.readLine(); } catch (java.io.IOException e) { System.out.println(e); }
		 */
	}

}
