package mip;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Aborter;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;
import io.GraphMLReader;
import io.GraphMLWriter;

import java.awt.geom.Path2D;
import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.SwingWorker;

import mip.callback.AddCutCallback;
import mip.callback.PlanarityBranchCallback;
import mip.callback.PlanarityCallback;
import mip.callback.SolutionInfoCallback;
import mip.ilp.Constraint;
import mip.ilp.CplexAdapter;
import mip.ilp.ILP;
import mip.ilp.ILP.UnknownVariableException;
import model.MetroEdge;
import model.MetroPath;
import model.MetroVertex;
import model.TransferGraph;
import model.TransferGraphEdge;
import app.Dijkstra;
import app.MetroMapper;
import app.Settings;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.utils.UserData;

public class Solver extends SwingWorker<Void, Void> {

	public static final int COMPLETE = 0;
	public static final int NONE = 1;
	public static final int FACES = 2;
	public static final int PENDANTEDGES = 3;

	public static final int BEND_INTERSECTION = 3; // penalty factor for bends in intersections
	static boolean lazy = true;
	static boolean ext_penalty = true;

	MetroMapper app;
	JButton solveBtn;

	private MetroCplex myCplex;

	private Aborter aborter;

	public Solver(MetroMapper app, JButton solveBtn) {
		this.app = app;
		this.solveBtn = solveBtn;
	}

	public void done() {
		this.abortCplex();
		this.solveBtn.setText("Solve!");
		this.solveBtn.setEnabled(true);
	}

	public void abortCplex() {
		if (this.myCplex instanceof IloCplex && this.aborter instanceof IloCplex.Aborter) {
			this.aborter.abort();
		}
	}

	@Override
	protected Void doInBackground() throws Exception {

		GraphMLReader reader = new GraphMLReader();
		Graph g = null;
		Graph labelG = null;

		int tilim = 36000;

		String out = "solver/" + Settings.inputMap;
		String infile = "resources/graphml/" + Settings.inputMap;
		String outfile;
		MetroCplex myCplex;
		ILP myILP;

		try {
			// BufferedWriter out = new BufferedWriter(new FileWriter(new File("output.lp")));
			GraphMLWriter myWriter = new GraphMLWriter();
			g = reader.loadGraph(infile);
			try {
				GraphTools.addLabelLengths(g);
			} catch (IOException e) {
				System.out.println("Couldn't find label lengths");
			}
			GraphTools.setIntersectionStatus(g);
			GraphTools.setTerminusStatus(g);
			GraphTools.insertDummyVertices(g);
			GraphTools.computePendantEdges(g);

			int oldVertices = g.numVertices();
			int oldEdges = g.numEdges();
			switch (Settings.degreeTwoVerticesContractionMode) {
				case Settings.CONTRACT_ALL:
					GraphTools.contractDegTwoVertices(g);
					GraphTools.computePendantEdges(g);
					GraphTools.insertJoints(g);
					GraphTools.insertBends(g); // experimental!!
					Set removeEdges = GraphTools.addConvexHullEdges(g);
					System.out.println("added " + removeEdges.size() + " dummy edges");
					GraphTools.computeFaces(g);
					g.removeEdges(removeEdges);
					// labelG = GraphTools.createLabelGraph(g, true);// true/false determines external labels
					// GraphTools.addSectorInfo(labelG);
					break;
				case Settings.CONTRACT_NON_AJACENT_TO_INTERSECTION:
					GraphTools.contractMostDegTwoVertices(g, false);
					GraphTools.insertBends(g); // experimental!!
					GraphTools.computeFaces(g);
					break;
				case Settings.CONTRACT_NON_AJACENT_TO_INTERSECTION_OR_PENDANT_EDGE:
					GraphTools.contractMostDegTwoVertices(g, true);
					GraphTools.insertBends(g); // experimental!!
					GraphTools.computeFaces(g);
					break;
				case Settings.CONTRACT_NONE:
					GraphTools.insertBends(g); // experimental!!
					GraphTools.computeFaces(g);
					break;
			}

			try {
				this.app.getGUIBuilder().saveCurrentSourceStationSelection();
				labelG = GraphTools.createLabelGraph(g, true);// true/false determines external labels
				GraphTools.addSectorInfo(labelG);
				Graph tg = TransferGraph.convert(labelG);
				labelG.addUserDatum("transferGraph", tg, UserData.SHARED);
				Dijkstra.computeShortestPaths(labelG);
				this.app.getGUIBuilder().loadCurrentSourceStationSelection();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

			System.out.println("reduced vertices/edges from " + oldVertices + "/" + oldEdges + " to " + g.numVertices() + "/" + g.numEdges());
			System.out.println("mprime is " + GraphTools.getMPrime(g));

			if (Settings.convexHullDummyEdgesInFaceComputation) {
				Set removeEdges = GraphTools.addConvexHullEdges(g);
				System.out.println("added " + removeEdges.size() + " dummy edges");
				GraphTools.clearFaces(g);
				GraphTools.computeFaces(g);
				g.removeEdges(removeEdges);
			}

			GraphTools.computePendantEdges(g);

			// Planarity choice mapping
			int planarityChoice = Solver.NONE;
			if (Settings.planarityMode == Settings.PLANARITY_ALL_EDGE_PAIRS) {
				planarityChoice = Solver.COMPLETE;
			} else if (Settings.planarityMode == Settings.PLANARITY_NONE) {
				planarityChoice = Solver.NONE;
			} else if (Settings.planarityMode == Settings.PLANARITY_FACE_HEURISTIC) {
				planarityChoice = Solver.FACES;
			} else if (Settings.planarityMode == Settings.PLANARITY_PENDANT_EDGES_ONLY) {
				planarityChoice = Solver.PENDANTEDGES;
			}

			String lpFile = "solver/" + Settings.inputMap + ".lp";
			myILP = this.generateLabelledMetroILP(labelG, lpFile, planarityChoice, Settings.totalEdgeLengthImportance, Settings.geographicDeviationImportance, Settings.bendsOnLineImportance,
					Settings.minimumEdgeLength);
			// myILP.writeLP("debug.lp", (String) g.getUserDatum("sourceFile"));
			this.runCplex(labelG, myILP, true, true, 'y', out, tilim);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public void runCplex(Graph g, ILP myILP, boolean isLabeled, boolean interactive, char callback, String file, int tilim) throws IloException, IOException {

		boolean checkPlanar = (Settings.planarityMode != Settings.PLANARITY_NONE);

		this.myCplex = CplexAdapter.convertToCplex(myILP, g);

		GraphMLWriter myWriter = new GraphMLWriter();
		// if (checkPlanar) {//dynamic search in cplex only possible without callbacks!
		myCplex.setParam(IntParam.MIPEmphasis, 4);
		myCplex.setParam(IntParam.Probe, 3);
		myCplex.setParam(IntParam.Cliques, 2);
		myCplex.setParam(IntParam.Covers, 2);
		myCplex.setParam(IntParam.DisjCuts, 2);
		myCplex.setParam(IntParam.FlowCovers, 2);
		myCplex.setParam(IntParam.FlowPaths, 2);
		myCplex.setParam(IntParam.FracCuts, 2);
		myCplex.setParam(IntParam.GUBCovers, 2);
		myCplex.setParam(IntParam.ImplBd, 2);
		myCplex.setParam(IntParam.MIRCuts, 2);
		myCplex.setParam(IntParam.FracCand, 10000);
		myCplex.setParam(IntParam.FracPass, 10);
		// myCplex.setParam(IntParam.VarSel, 3);

		this.aborter = new IloCplex.Aborter();
		myCplex.use(this.aborter);

		if (checkPlanar) {
			myCplex.use(new AddCutCallback(myCplex));
			myCplex.use(new PlanarityBranchCallback(myCplex));
			// myCplex.use(new TuneBranchCallback());
			myCplex.use(new PlanarityCallback(g, myCplex, myWriter, file, isLabeled, !checkPlanar));
		} else {
			myCplex.use(new SolutionInfoCallback(g, myCplex, myWriter, file, isLabeled, this.app));
		}
		// }

		int loop = 0;
		double accumulatedTime = 0;
		long start = System.currentTimeMillis();
		myCplex.setStart(start);
		// myCplex.solve(new PlanarityGoal(g, myCplex, myWriter, file));

		myCplex.setParam(DoubleParam.TiLim, tilim);
		// myCplex.setParam(DoubleParam.TuningTiLim, 100);
		// myCplex.tuneParam();
		myCplex.setParam(DoubleParam.PolishTime, 6000);

		if (myCplex.solve()) {
			long end = System.currentTimeMillis();
			accumulatedTime += (end - start) / 1000.;

			GraphTools.changeCoordinates(g, myCplex);
			System.out.println("successfully changed the coordinates");
			g.setUserDatum("edgeLength", Double.valueOf(myCplex.getLengthCosts()), UserData.SHARED);
			g.setUserDatum("sectorDeviation", Double.valueOf(myCplex.getDeviateCosts()), UserData.SHARED);
			g.setUserDatum("bendCost", Double.valueOf(myCplex.getBendCosts()), UserData.SHARED);
			g.setUserDatum("objective", Double.valueOf((Math.round(10000 * myCplex.getObjValue()) / 10000.0)), UserData.SHARED);
			g.setUserDatum("gap", Double.valueOf(1 - ((Math.round(10000 * myCplex.getBestObjValue()) / 10000.0) / (Math.round(10000 * myCplex.getObjValue()) / 10000.0))), UserData.SHARED);
			g.setUserDatum("elapsedTime", Double.valueOf(accumulatedTime), UserData.SHARED);
			g.setUserDatum("numConstraints", Integer.valueOf(myCplex.getNrows()), UserData.SHARED);
			g.setUserDatum("numVars", Integer.valueOf(myCplex.getNcols()), UserData.SHARED);
			g.setUserDatum("numBinVars", Integer.valueOf(myCplex.getNbinVars()), UserData.SHARED);
			g.setUserDatum("numIntVars", Integer.valueOf(myCplex.getNintVars()), UserData.SHARED);

			String outFile = file + "-final";

			// myWriter.writeGraphML(g, file + ((loop<10)?"0":"") + loop + ".graphml");
			myWriter.writeGraphML(g, outFile + ".graphml", true);
			System.out.println("Wrote solution to file " + outFile + ".graphml");

			// Load graph
			this.app.initializeMap(outFile);

			System.out.println("This solution has total length " + myCplex.getLengthCosts());
			System.out.println("This solution has bend cost " + myCplex.getBendCosts());
			System.out.println("This solution has sector deviation cost " + myCplex.getDeviateCosts());
			System.out.println("The value of the objective function is " + (Math.round(10000 * myCplex.getObjValue()) / 10000.0));
		} else {
			System.out.println("NO SOLUTION EXISTS!");
		}
	}

	private ILP generateLabelledMetroILP(Graph g, String outFile, int planarity, int lengthFac, int deviationFac, int bendFac, int minLength) throws IOException, UnknownVariableException {

		int orgMinLength = minLength;
		
		Set paths = GraphTools.getMetroLines(g);
		
		System.out.println("generating labelled ILP");

		// need the StringLabeller associated with this graph
		StringLabeller vertexIDs = StringLabeller.getLabeller(g);
		ILP myILP = new ILP();
		final int MPOS = ((Integer) g.getUserDatum("origNumVertices")).intValue() + 1;
		final int MNEG = -MPOS;
		final int M7 = 7;
		final int M8 = 8;
		final int M9 = 9;
		final int M12 = 12;
		final int M3 = 3;
		final double LABELPREFFAC = 0.01; // weight for labelPref; should be small enough in order to not influence the layout
		boolean labelPref = true; // prefer labels to east or to northeast
		int areaUpperBound = 5 * ((Integer) g.getUserDatum("origNumVertices")).intValue();

		/*
		 * generate constraints part 1 - consistent coordinates, i.e. forall v \in V: x(v) + y(v) = z1(v) and x(v) - y(v) = z2(v)
		 * 
		 * in total 2n constraints (plus additional 2n needed for the objective function)
		 */
		Iterator it = g.getVertices().iterator();
		while (it.hasNext()) {
			MetroVertex v = (MetroVertex) it.next();
			String vertexID = vertexIDs.getLabel(v);
			// add first constraint
			Constraint con1 = myILP.createConstraint();
			myILP.addConstraintSummand(con1, 1, "x(" + vertexID + ")");
			myILP.addConstraintSummand(con1, 1, "y(" + vertexID + ")");
			myILP.addConstraintSummand(con1, -1, "z1(" + vertexID + ")");
			myILP.setConstraintEqual(con1);
			myILP.setConstraintRhs(con1, 0);
			// add second constraint
			Constraint con2 = myILP.createConstraint();
			myILP.addConstraintSummand(con2, 1, "x(" + vertexID + ")");
			myILP.addConstraintSummand(con2, -1, "y(" + vertexID + ")");
			myILP.addConstraintSummand(con2, -1, "z2(" + vertexID + ")");
			myILP.setConstraintEqual(con2);
			myILP.setConstraintRhs(con2, 0);
			// set variable properties
			// set 0 < x < +inf, x integer
			myILP.setVariableLower("x(" + vertexID + ")", 0);
			myILP.setVariableLower("y(" + vertexID + ")", 0);
			myILP.setVariableUpper("x(" + vertexID + ")", areaUpperBound);
			myILP.setVariableUpper("y(" + vertexID + ")", areaUpperBound);
			myILP.setVariableReal("x(" + vertexID + ")");
			myILP.setVariableReal("y(" + vertexID + ")");
			myILP.setVariableReal("z1(" + vertexID + ")");
			myILP.setVariableReal("z2(" + vertexID + ")");

		} // end while

		/*
		 * generate constraints part 3 - 4gonality and relative positions for each {u,v} \in E compute sector of u in which v lies edge {u,v} can be drawn in three different directions according to
		 * the sector calculated above and its two neighbouring sectors therefore we have the following constraint to pick one of three sectors: sel_b1(u,v) + sel_b2(u,v) + sel_b3(u,v) = 1, binary
		 * variables sel_bi
		 * 
		 * each case implies 7 constraints as follows: dir(u,v) + sel_b1(u,v) M <= M + sector1 - dir(u,v) + sel_b1(u,v) M <= M - sector1//i.e. dir(u,v) == sector dir(v,u) + sel_b1(u,v) M <= M +
		 * (sector1+4)%8 - dir(v,u) + sel_b1(u,v) M <= M - (sector1/+4)%8/i.e. dir(v,u) == opposite sector and e.g. (depends on the sector) y(u) - y(v) + sel_b1(u,v) M <= M - y(u) + y(v) + sel_b1(u,v)
		 * M <= M //i.e. y(u) == y(v) x(u) - x(v) + sel_b1(u,v) M <= M - 1 //i.e. x(u) < x(v)
		 * 
		 * in total 22m constraints
		 */
		it = g.getEdges().iterator();
		while (it.hasNext()) {
			MetroEdge e = (MetroEdge) it.next();
			if (e.getUserDatum("length") != null) {
				int len = ((Integer) e.getUserDatum("length")).intValue();
				if (len - 1 > e.getLength())
					e.setLength(len);
			}


			MetroVertex first = (MetroVertex) e.getEndpoints().getFirst();
			MetroVertex second = (MetroVertex) e.getEndpoints().getSecond();
			String id_first = vertexIDs.getLabel(first);
			String id_second = vertexIDs.getLabel(second);
			
			// @Michel
			if(Settings.modifiedSchematization == true && Settings.increaseSpaceAroundIntersections == true) {
				minLength = orgMinLength;
				if(first.degree() > 2) {
					minLength += (int) (first.getCombinedPathThickness(this.app.getSvgCanvas(), g) * 0.1);
				}
				if(second.degree() > 2) {
					minLength += (int) (second.getCombinedPathThickness(this.app.getSvgCanvas(), g) * 0.1);
				}
				if(first.degree() > 2 && second.degree() > 2) {
					minLength = (int) (minLength * 2);
				}
				minLength = (int) (minLength + minLength * e.getMultiplicity() * 0.2);
				System.out.println("first comb path thickness: " + first.getCombinedPathThickness(this.app.getSvgCanvas(), g));
				System.out.println("scaleMultiplier: " + this.app.getSvgCanvas().getScaleMultiplier());
				System.out.println("minLength: " + minLength);
				if(true) {
					//throw new UnknownVariableException("die..");
				}
			}
			
			/* regular and main edges */
			if (e.isRegular() || e.isLabelMainDummy()) {
				int sector = ((Integer) e.getUserDatum("sector1to2")).intValue();
				int previousSector = (sector + 7) % 8; // coz (sector-1) could be negative causing trouble
				int nextSector = (sector + 1) % 8;

				// constraint to select one of three possible directions
				Constraint con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintEqual(con);
				myILP.setConstraintRhs(con, 1);
				myILP.setVariableBinary("sel_b1(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b2(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b3(" + id_first + "," + id_second + ")");

				// constraints that set the direction equal to the selected sector
				// this is more efficient than what we had before: 1 constraint instead of 6 and no M
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, previousSector, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, sector, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, nextSector, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintEqual(con);
				myILP.setConstraintRhs(con, 0);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_second + "," + id_first + ")");
				myILP.addConstraintSummand(con, ((previousSector + 4) % 8), "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, ((sector + 4) % 8), "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, ((nextSector + 4) % 8), "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintEqual(con);
				myILP.setConstraintRhs(con, 0);
				myILP.setVariableReal("dir(" + id_first + "," + id_second + ")");
				myILP.setVariableReal("dir(" + id_second + "," + id_first + ")");
				myILP.setVariableLower("dir(" + id_first + "," + id_second + ")", 0);
				myILP.setVariableUpper("dir(" + id_first + "," + id_second + ")", 7);
				myILP.setVariableLower("dir(" + id_second + "," + id_first + ")", 0);
				myILP.setVariableUpper("dir(" + id_second + "," + id_first + ")", 7);

				// //constraints that set the direction equal to the selected sector
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b1(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 + previousSector);
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b1(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 - previousSector);
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b2(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 + sector);
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b2(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 - sector);
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b3(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 + nextSector);
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b3(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 - nextSector);
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, 1, "dir(" + id_second + "," + id_first + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b1(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 + ((previousSector+4)%8));
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, -1, "dir(" + id_second + "," + id_first + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b1(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 - ((previousSector+4)%8));
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, 1, "dir(" + id_second + "," + id_first + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b2(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 + ((sector+4)%8));
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, -1, "dir(" + id_second + "," + id_first + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b2(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 - ((sector+4)%8));
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, 1, "dir(" + id_second + "," + id_first + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b3(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 + ((nextSector+4)%8));
				// con = myILP.createConstraint();
				// myILP.addConstraintSummand(con, -1, "dir(" + id_second + "," + id_first + ")");
				// myILP.addConstraintSummand(con, M7, "sel_b3(" + id_first + "," + id_second + ")");
				// myILP.setConstraintLess(con);
				// myILP.setConstraintRhs(con, M7 - ((nextSector+4)%8));
				// //dir(u,v) is integer valued between 0 and 7
				// myILP.setVariableGeneral("dir(" + id_first + "," + id_second + ")");
				// myILP.setVariableLower("dir(" + id_first + "," + id_second + ")", 0);
				// myILP.setVariableUpper("dir(" + id_first + "," + id_second + ")", 7);
				// myILP.setVariableGeneral("dir(" + id_second + "," + id_first + ")");
				// myILP.setVariableLower("dir(" + id_second + "," + id_first + ")", 0);
				// myILP.setVariableUpper("dir(" + id_second + "," + id_first + ")", 7);

				// constraints to ensure 4gonality in each of the three possible cases
				// sel_b1 selects previousSector, sel_b2 sector and sel_b3 nextSector
				for (int i = 1; i <= 3; i++) {
					int switchMe = -1;
					switch (i) {
						case 1:
							switchMe = previousSector;
							break;
						case 2:
							switchMe = sector;
							break;
						case 3:
							switchMe = nextSector;
							break;
					}
					// each of the eight directions gives rise to a different set of constraints
					// basic principle: one coordinate value must be equal, the orthogonal one strictly
					// smaller|greater, e.g. x(u)=x(v), y(u)<y(v)
					switch (switchMe) {
						case 0:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							// myILP.setConstraintRhs(con, MPOS-Math.max(e.getLength(), minLength));
							myILP.setConstraintRhs(con, MPOS - (e.getLength() * minLength));
							break;
						case 1:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							// myILP.setConstraintRhs(con, MPOS-2*Math.max(e.getLength(), minLength));
							myILP.setConstraintRhs(con, MPOS - 2 * (e.getLength() * minLength));
							break;
						case 2:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							// myILP.setConstraintRhs(con, MPOS-Math.max(e.getLength(), minLength));
							myILP.setConstraintRhs(con, MPOS - (e.getLength() * minLength));
							break;
						case 3:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							// myILP.setConstraintRhs(con, MPOS-2*Math.max(e.getLength(), minLength));
							myILP.setConstraintRhs(con, MPOS - 2 * (e.getLength() * minLength));
							break;
						case 4:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							// myILP.setConstraintRhs(con, MPOS-Math.max(e.getLength(), minLength));
							myILP.setConstraintRhs(con, MPOS - (e.getLength() * minLength));
							break;
						case 5:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							// myILP.setConstraintRhs(con, MPOS-2*Math.max(e.getLength(), minLength));
							myILP.setConstraintRhs(con, MPOS - 2 * (e.getLength() * minLength));
							break;
						case 6:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							// myILP.setConstraintRhs(con, MPOS-Math.max(e.getLength(), minLength));
							myILP.setConstraintRhs(con, MPOS - (e.getLength() * minLength));
							break;
						case 7:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_first + "," + id_second + ")");
							myILP.setConstraintLess(con);
							// myILP.setConstraintRhs(con, MPOS-2*Math.max(e.getLength(), minLength));
							myILP.setConstraintRhs(con, MPOS - 2 * (e.getLength() * minLength));
							break;
					} // end switch
				} // end for
			} else if (e.isLabelConnectDummy() || e.isLabelParallelDummy()) {
				/* parallel edges to the main edge */
				MetroEdge mainEdge = e.getMainEdge();
				MetroVertex mainFirst = (MetroVertex) mainEdge.getEndpoints().getFirst();
				MetroVertex mainSecond = (MetroVertex) mainEdge.getEndpoints().getSecond();
				MetroVertex jointVertex = first.isIncident(mainEdge) ? first : second;
				String id_mainFirst = vertexIDs.getLabel(mainFirst);
				String id_mainSecond = vertexIDs.getLabel(mainSecond);
				int sector = ((Integer) mainEdge.getUserDatum("sector1to2")).intValue();
				int previousSector = (sector + 7) % 8; // coz (sector-1) could be negative causing trouble
				int nextSector = (sector + 1) % 8;
				Constraint con;

				if (e.isLabelConnectDummy() || e.isLabelParallelDummy()) {
					// if (e.isLabelParallelDummy() || (!jointVertex.isJoint() && e.isLabelConnectDummy())) {
					/* set dir(e) equal to dir(mainEdge) */
					con = myILP.createConstraint();
					myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
					myILP.addConstraintSummand(con, -1, "dir(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintEqual(con);
					myILP.setConstraintRhs(con, 0);
					con = myILP.createConstraint();
					myILP.addConstraintSummand(con, 1, "dir(" + id_second + "," + id_first + ")");
					myILP.addConstraintSummand(con, -1, "dir(" + id_mainSecond + "," + id_mainFirst + ")");
					myILP.setConstraintEqual(con);
					myILP.setConstraintRhs(con, 0);
					myILP.setVariableGeneral("dir(" + id_first + "," + id_second + ")");
					myILP.setVariableLower("dir(" + id_first + "," + id_second + ")", 0);
					myILP.setVariableUpper("dir(" + id_first + "," + id_second + ")", 7);
					myILP.setVariableGeneral("dir(" + id_second + "," + id_first + ")");
					myILP.setVariableLower("dir(" + id_second + "," + id_first + ")", 0);
					myILP.setVariableUpper("dir(" + id_second + "," + id_first + ")", 7);
				}
				// constraints to ensure 4gonality in each of the three possible cases
				// sel_b1 selects previousSector, sel_b2 sector and sel_b3 nextSector
				for (int i = 1; i <= 3; i++) {
					int switchMe = -1;
					switch (i) {
						case 1:
							switchMe = previousSector;
							break;
						case 2:
							switchMe = sector;
							break;
						case 3:
							switchMe = nextSector;
							break;
					}
					// each of the eight directions gives rise to a different set of constraints
					// basic principle: one coordinate value must be equal, the orthogonal one strictly
					// smaller|greater, e.g. x(u)=x(v), y(u)<y(v)
					switch (switchMe) {
						case 0:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS - e.getLength());
							break;
						case 1:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS - 2 * e.getLength());
							break;
						case 2:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS - e.getLength());
							break;
						case 3:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS - 2 * e.getLength());
							break;
						case 4:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS - e.getLength());
							break;
						case 5:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS - 2 * e.getLength());
							break;
						case 6:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "y(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "y(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS - e.getLength());
							break;
						case 7:
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
							myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "z2(" + id_first + ")");
							myILP.addConstraintSummand(con, -1, "z2(" + id_second + ")");
							myILP.addConstraintSummand(con, MPOS, "sel_b" + i + "(" + id_mainFirst + "," + id_mainSecond + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, MPOS - 2 * e.getLength());
							break;
					} // end switch
				} // end for

			} else if (e.isLabelDirectionDummy()) {
				/* label direction edges */
				MetroEdge mainEdge = e.getMainEdge();
				MetroVertex mainFirst = (MetroVertex) mainEdge.getEndpoints().getFirst();
				MetroVertex mainSecond = (MetroVertex) mainEdge.getEndpoints().getSecond();
				String id_mainFirst = vertexIDs.getLabel(mainFirst);
				String id_mainSecond = vertexIDs.getLabel(mainSecond);
				int sector = ((Integer) mainEdge.getUserDatum("sector1to2")).intValue();
				int previousSector = (sector + 7) % 8; // coz (sector-1) could be negative causing trouble
				int nextSector = (sector + 1) % 8;
				Constraint con;

				/* is it possible that mainEdge is horizontal? */
				int hor = -1;
				if (previousSector == 0 || previousSector == 4) {
					hor = 1;
				} else if (sector == 0 || sector == 4) {
					hor = 2;
				} else if (nextSector == 0 || nextSector == 4) {
					hor = 3;
				}

				if (hor == -1) {// label is horizontal in all cases
					con = myILP.createConstraint();
					myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
					myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
					myILP.setConstraintEqual(con);
					myILP.setConstraintRhs(con, 0);

					con = myILP.createConstraint();// to the right <=> sel_ll=1
					myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
					myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - e.getLength());
					con = myILP.createConstraint();// to the right <=> sel_ll=1
					myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
					myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintGreater(con);
					myILP.setConstraintRhs(con, -MPOS - e.getLength());

					con = myILP.createConstraint();// to the left
					myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
					myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, -e.getLength());
					con = myILP.createConstraint();// to the left
					myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
					myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintGreater(con);
					myILP.setConstraintRhs(con, -e.getLength());

					myILP.setVariableBinary("sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					if (labelPref)
						myILP.addObjectiveFunctionSummand(-1 * LABELPREFFAC, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
				} else {
					/* horizontal or diagonal label */
					con = myILP.createConstraint();
					myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
					myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, 0);
					con = myILP.createConstraint();
					myILP.addConstraintSummand(con, -1, "y(" + id_first + ")");
					myILP.addConstraintSummand(con, 1, "y(" + id_second + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, 0);
					con = myILP.createConstraint();
					myILP.addConstraintSummand(con, 1, "z2(" + id_first + ")");
					myILP.addConstraintSummand(con, -1, "z2(" + id_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS);
					con = myILP.createConstraint();
					myILP.addConstraintSummand(con, -1, "z2(" + id_first + ")");
					myILP.addConstraintSummand(con, 1, "z2(" + id_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS);

					con = myILP.createConstraint();// to the right <=> sel_ll=1
					myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
					myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - e.getLength());
					con = myILP.createConstraint();// to the right <=> sel_ll=1
					myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
					myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintGreater(con);
					myILP.setConstraintRhs(con, -MPOS - e.getLength());

					con = myILP.createConstraint();// to the left
					myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
					myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, -e.getLength());
					con = myILP.createConstraint();// to the left
					myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
					myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintGreater(con);
					myILP.setConstraintRhs(con, -e.getLength());

					con = myILP.createConstraint();// to the upper right
					myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
					myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, 2 * MPOS - 2 * e.getLength() / Math.sqrt(2));
					con = myILP.createConstraint();
					myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
					myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintGreater(con);
					myILP.setConstraintRhs(con, -2 * MPOS - 2 * e.getLength() / Math.sqrt(2));

					con = myILP.createConstraint();// to the lower left
					myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
					myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 2 * e.getLength() / Math.sqrt(2));
					con = myILP.createConstraint();
					myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
					myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.addConstraintSummand(con, -MPOS, "sel_b" + hor + "(" + id_mainFirst + "," + id_mainSecond + ")");
					myILP.setConstraintGreater(con);
					myILP.setConstraintRhs(con, -MPOS - 2 * e.getLength() / Math.sqrt(2));

					myILP.setVariableBinary("sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
					if (labelPref)
						myILP.addObjectiveFunctionSummand(-1 * LABELPREFFAC, "sel_ll" + "(" + id_mainFirst + "," + id_mainSecond + ")");
				}

			} else if (e.isLabelIntersectionMainDummy() && (!((MetroVertex) e.getEndpoints().getFirst()).isLabelVertex() || !((MetroVertex) e.getEndpoints().getSecond()).isLabelVertex())) {
				// String id_mainFirst = ((MetroVertex)e.getMainEdge().getEndpoints().getFirst()).getName();
				// String id_mainSecond = ((MetroVertex)e.getMainEdge().getEndpoints().getSecond()).getName();
				Constraint con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b4(" + id_first + "," + id_second + ")");
				// myILP.addConstraintSummand(con, 1, "sel_b5(" + id_mainFirst + "," + id_mainSecond + ")");
				// myILP.addConstraintSummand(con, 1, "sel_b6(" + id_mainFirst + "," + id_mainSecond + ")");
				// myILP.addConstraintSummand(con, 1, "sel_b7(" + id_mainFirst + "," + id_mainSecond + ")");
				// myILP.addConstraintSummand(con, 1, "sel_b8(" + id_mainFirst + "," + id_mainSecond + ")");
				myILP.setConstraintEqual(con);
				myILP.setConstraintRhs(con, 1);
				myILP.setVariableBinary("sel_b1(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b2(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b3(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b4(" + id_first + "," + id_second + ")");
				// myILP.setVariableBinary("sel_b5(" + id_mainFirst + "," + id_mainSecond + ")");
				// myILP.setVariableBinary("sel_b6(" + id_mainFirst + "," + id_mainSecond + ")");
				// myILP.setVariableBinary("sel_b7(" + id_mainFirst + "," + id_mainSecond + ")");
				// myILP.setVariableBinary("sel_b8(" + id_mainFirst + "," + id_mainSecond + ")");

				// constraints that set the direction equal to the selected sector
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 0);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 0);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 1);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 1);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 4);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 4);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 5);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 5);
				// dir(u,v) is integer valued between 0 and 7
				myILP.setVariableGeneral("dir(" + id_first + "," + id_second + ")");
				myILP.setVariableLower("dir(" + id_first + "," + id_second + ")", 0);
				myILP.setVariableUpper("dir(" + id_first + "," + id_second + ")", 7);

				// bonus for labels of dir =0 or =1 // =0 or =4
				if (labelPref)
					myILP.addObjectiveFunctionSummand(-1 * LABELPREFFAC, "sel_b1" + "(" + id_first + "," + id_second + ")");
				// if (labelPref) myILP.addObjectiveFunctionSummand(-1*LABELPREFFAC, "sel_b2" + "(" + id_first + "," + id_second + ")");
				if (labelPref)
					myILP.addObjectiveFunctionSummand(-1 * LABELPREFFAC, "sel_b3" + "(" + id_first + "," + id_second + ")");

				// constraints to ensure 4gonality in each of the three possible cases
				// each of the eight directions gives rise to a different set of constraints
				// basic principle: one coordinate value must be equal, the orthogonal one strictly
				// smaller|greater, e.g. x(u)=x(v), y(u)<y(v)
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "y(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "y(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - e.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - e.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z2(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "z2(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z2(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "z2(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - 2 * e.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - 2 * e.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - e.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - e.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - 2 * e.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - 2 * e.getLength() / Math.sqrt(2));

			} else if (e.isLabelIntersectionConnectDummy()) {
				// these edges exist only if we have two-edge labels at intersections
				// thus we deal with both edges at this point

				// get the main edge
				String id_mainFirst = vertexIDs.getLabel((MetroVertex) e.getMainEdge().getEndpoints().getFirst());
				String id_mainSecond = vertexIDs.getLabel((MetroVertex) e.getMainEdge().getEndpoints().getSecond());
				MetroEdge mainEdge = e.getMainEdge();

				Constraint con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, 1, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintEqual(con);
				myILP.setConstraintRhs(con, 1);
				myILP.setVariableBinary("sel_b1(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b2(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b3(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b4(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b5(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b6(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b7(" + id_first + "," + id_second + ")");
				myILP.setVariableBinary("sel_b8(" + id_first + "," + id_second + ")");

				// constraints that set the direction of both edges
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 1);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 1);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 2);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 2);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 3);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 3);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 4);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 4);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 5);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 5);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 6);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 6);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 7);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 7);

				// now direction for the main edge
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_mainFirst + "," + id_mainSecond + ")");
				myILP.addConstraintSummand(con, M7, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_mainFirst + "," + id_mainSecond + ")");
				myILP.addConstraintSummand(con, M7, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7);

				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_mainFirst + "," + id_mainSecond + ")");
				myILP.addConstraintSummand(con, M7, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 4);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_mainFirst + "," + id_mainSecond + ")");
				myILP.addConstraintSummand(con, M7, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, M7, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 4);

				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_mainFirst + "," + id_mainSecond + ")");
				myILP.addConstraintSummand(con, M7, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 1);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_mainFirst + "," + id_mainSecond + ")");
				myILP.addConstraintSummand(con, M7, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 1);

				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + id_mainFirst + "," + id_mainSecond + ")");
				myILP.addConstraintSummand(con, M7, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 + 5);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "dir(" + id_mainFirst + "," + id_mainSecond + ")");
				myILP.addConstraintSummand(con, M7, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, M7 - 5);

				// dir(u,v) is integer valued between 0 and 7
				myILP.setVariableGeneral("dir(" + id_first + "," + id_second + ")");
				myILP.setVariableLower("dir(" + id_first + "," + id_second + ")", 0);
				myILP.setVariableUpper("dir(" + id_first + "," + id_second + ")", 7);

				myILP.setVariableGeneral("dir(" + id_mainFirst + "," + id_mainSecond + ")");
				myILP.setVariableLower("dir(" + id_mainFirst + "," + id_mainSecond + ")", 0);
				myILP.setVariableUpper("dir(" + id_mainFirst + "," + id_mainSecond + ")", 7);

				// set coordinate constraints for connect-edge
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "y(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "y(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - e.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - e.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - e.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - e.getLength());

				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "x(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "x(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "x(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "x(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - e.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "y(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "y(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - e.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "y(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "y(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - e.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "y(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "y(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - e.getLength());

				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z2(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "z2(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - 2 * e.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z2(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "z2(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - 2 * e.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z2(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "z2(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - 2 * e.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z2(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "z2(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - 2 * e.getLength() / Math.sqrt(2));

				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z2(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "z2(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z2(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "z2(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - 2 * e.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z1(" + id_first + ")");
				myILP.addConstraintSummand(con, -1, "z1(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - 2 * e.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - 2 * e.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z1(" + id_first + ")");
				myILP.addConstraintSummand(con, 1, "z1(" + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - 2 * e.getLength() / Math.sqrt(2));

				// bonus for labels of dir =0 or =4 (main edge)
				if (labelPref) {
					myILP.addObjectiveFunctionSummand(-2 * LABELPREFFAC, "sel_b1" + "(" + id_first + "," + id_second + ")");
					myILP.addObjectiveFunctionSummand(-1 * LABELPREFFAC, "sel_b2" + "(" + id_first + "," + id_second + ")");
					myILP.addObjectiveFunctionSummand(-1 * LABELPREFFAC, "sel_b4" + "(" + id_first + "," + id_second + ")");
					myILP.addObjectiveFunctionSummand(-2 * LABELPREFFAC, "sel_b5" + "(" + id_first + "," + id_second + ")");
					myILP.addObjectiveFunctionSummand(-1 * LABELPREFFAC, "sel_b6" + "(" + id_first + "," + id_second + ")");
					myILP.addObjectiveFunctionSummand(-1 * LABELPREFFAC, "sel_b8" + "(" + id_first + "," + id_second + ")");
				}

				// constraints to ensure 4gonality in each of the three possible cases
				// each of the eight directions gives rise to a different set of constraints
				// basic principle: one coordinate value must be equal, the orthogonal one strictly
				// smaller|greater, e.g. x(u)=x(v), y(u)<y(v)
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "y(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, -1, "y(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "y(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, 1, "y(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "x(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, -1, "x(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - mainEdge.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "x(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, -1, "x(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b1(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b2(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b8(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - mainEdge.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "x(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, 1, "x(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - mainEdge.getLength());
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "x(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, 1, "x(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b4(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b5(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b6(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - mainEdge.getLength());

				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z2(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, -1, "z2(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z2(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, 1, "z2(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS);
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z1(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, -1, "z1(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - 2 * mainEdge.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "z1(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, -1, "z1(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b3(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - 2 * mainEdge.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z1(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, 1, "z1(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, MPOS, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, MPOS - 2 * mainEdge.getLength() / Math.sqrt(2));
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, -1, "z1(" + id_mainFirst + ")");
				myILP.addConstraintSummand(con, 1, "z1(" + id_mainSecond + ")");
				myILP.addConstraintSummand(con, -MPOS, "sel_b7(" + id_first + "," + id_second + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, -MPOS - 2 * mainEdge.getLength() / Math.sqrt(2));

			}
		}// end while

		/*
		 * generate constraints part 4 - ensure planarity for each pair of non-incident edges make sure that edge1 is either w, nw, n, ne, e, se, s or sw of edge2 this is guaranteed by enforcing that
		 * e.g. in case w both endpoints of e1 are to the left of both endpoints of e2
		 * 
		 * in total 33 ((m choose 2) - #incident edge pairs) constraints
		 */
		// trying to skip this part....
		if (planarity != NONE) {
			int countCons = 0;
			Edge[] edges = (Edge[]) g.getEdges().toArray(new Edge[0]);
			for (int i = 0; i < edges.length; i++) {
				for (int j = i + 1; j < edges.length; j++) {
					// edge pair must not be incident
					MetroEdge edge_i = (MetroEdge) edges[i];
					MetroEdge edge_j = (MetroEdge) edges[j];
					if (edge_i.isIncident((Vertex) edge_j.getEndpoints().getFirst()) || edge_i.isIncident((Vertex) edge_j.getEndpoints().getSecond()))
						continue;
					// ok, edges are not incident

					// if using face heuristic check whether e_i and e_j are not incident to a common face
					// if that is the case then continue with the next pair as the check is not necessary
					if (planarity == FACES || planarity == PENDANTEDGES) {
						if (edge_i.getLeftFace() == edge_j.getLeftFace() || edge_i.getLeftFace() == edge_j.getRightFace() || edge_i.getRightFace() == edge_j.getLeftFace()
								|| edge_i.getRightFace() == edge_j.getRightFace()
						// || (edge_i.isLabelIntersectionMainDummy() && edge_i.isFaceCandidate(edge_j.getLeftFace()))
						// || (edge_i.isLabelIntersectionMainDummy() && edge_i.isFaceCandidate(edge_j.getRightFace()))
						// || (edge_j.isLabelIntersectionMainDummy() && edge_j.isFaceCandidate(edge_i.getLeftFace()))
						// || (edge_j.isLabelIntersectionMainDummy() && edge_j.isFaceCandidate(edge_i.getRightFace()))
						) {// have a face in common

							if (edge_i.getMainEdge() == edge_j.getMainEdge())
								continue; // both edges belong to same label box
							if (!(edge_i.isCriticalLabelEdge() || edge_j.isCriticalLabelEdge())) {
								// continue; //both edges uncritical
							} /*
							 * else if ((edge_i.isPendant() && edge_i.isRegular()) || (edge_j.isPendant() && edge_j.isRegular())) { continue; //overlap with these edges is ok in the srn example... }
							 */

						} else if ((planarity == FACES) && !edge_i.isLabelIntersectionDummy() && !edge_j.isLabelIntersectionDummy()) {
							continue;
						} else {

							if (!edge_i.isLabelIntersectionDummy() && !edge_j.isLabelIntersectionDummy())
								continue; // both edges are not LabelIntersection so we skip this pair

							if (!(GraphTools.canShareFace(edge_i, edge_j) || edge_i.isFaceCandidate(edge_j.getLeftFace()) || edge_i.isFaceCandidate(edge_j.getRightFace())
									|| edge_j.isFaceCandidate(edge_i.getLeftFace()) || edge_j.isFaceCandidate(edge_i.getRightFace())))
								continue; //

						}
					}

					countCons++;
					String edge_i_id = "{" + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getSecond()) + "}";
					String edge_j_id = "{" + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getFirst()) + "," + vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getSecond()) + "}";
					// constraint to pick (at least) one of eight possibilities for the relative position of the edegs
					Constraint con = myILP.createConstraint();
					myILP.addConstraintSummand(con, 1, "w(" + edge_i_id + "," + edge_j_id + ")");
					myILP.addConstraintSummand(con, 1, "w(" + edge_j_id + "," + edge_i_id + ")");
					myILP.addConstraintSummand(con, 1, "s(" + edge_i_id + "," + edge_j_id + ")");
					myILP.addConstraintSummand(con, 1, "s(" + edge_j_id + "," + edge_i_id + ")");
					myILP.addConstraintSummand(con, 1, "sw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.addConstraintSummand(con, 1, "sw(" + edge_j_id + "," + edge_i_id + ")");
					myILP.addConstraintSummand(con, 1, "nw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.addConstraintSummand(con, 1, "nw(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintGreater(con);
					myILP.setConstraintRhs(con, 1);
					myILP.setVariableBinary("w(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setVariableBinary("w(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setVariableBinary("s(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setVariableBinary("s(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setVariableBinary("sw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setVariableBinary("sw(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setVariableBinary("nw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setVariableBinary("nw(" + edge_j_id + "," + edge_i_id + ")");

					String edge_i_first = vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getFirst());
					String edge_i_second = vertexIDs.getLabel((Vertex) edge_i.getEndpoints().getSecond());
					String edge_j_first = vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getFirst());
					String edge_j_second = vertexIDs.getLabel((Vertex) edge_j.getEndpoints().getSecond());
					// case w(edge_i, edge_j) == 1
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "x(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, -1, "x(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "w(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "x(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, -1, "x(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "w(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "x(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, -1, "x(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "w(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "x(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, -1, "x(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "w(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					// case w(edge_j, edge_i) == 1
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "x(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, 1, "x(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "w(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "x(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, 1, "x(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "w(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "x(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, 1, "x(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "w(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "x(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, 1, "x(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "w(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					// case s(edge_i, edge_j) == 1
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "y(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, -1, "y(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "s(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "y(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, -1, "y(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "s(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "y(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, -1, "y(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "s(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "y(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, -1, "y(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "s(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					// case s(edge_j, edge_i) == 1
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "y(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, 1, "y(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "s(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "y(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, 1, "y(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "s(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "y(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, 1, "y(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "s(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "y(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, 1, "y(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "s(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					// case sw(edge_i, edge_j) == 1
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "z1(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, -1, "z1(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "sw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "z1(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, -1, "z1(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "z1(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, -1, "z1(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "sw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "z1(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, -1, "z1(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					// case sw(edge_j, edge_i) == 1
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "z1(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, 1, "z1(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "sw(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "z1(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, 1, "z1(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sw(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "z1(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, 1, "z1(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "sw(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "z1(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, 1, "z1(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "sw(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					// case nw(edge_i, edge_j) == 1
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "z2(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, -1, "z2(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "nw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "z2(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, -1, "z2(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "nw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "z2(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, -1, "z2(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "nw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, 1, "z2(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, -1, "z2(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "nw(" + edge_i_id + "," + edge_j_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					// case nw(edge_j, edge_i) == 1
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "z2(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, 1, "z2(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "nw(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "z2(" + edge_i_first + ")");
					myILP.addConstraintSummand(con, 1, "z2(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "nw(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "z2(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, 1, "z2(" + edge_j_first + ")");
					myILP.addConstraintSummand(con, MPOS, "nw(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);
					con = myILP.createConstraint();
					con.setLazy(lazy);
					con.setName(edge_i_id + "," + edge_j_id);
					myILP.addConstraintSummand(con, -1, "z2(" + edge_i_second + ")");
					myILP.addConstraintSummand(con, 1, "z2(" + edge_j_second + ")");
					myILP.addConstraintSummand(con, MPOS, "nw(" + edge_j_id + "," + edge_i_id + ")");
					myILP.setConstraintLess(con);
					myILP.setConstraintRhs(con, MPOS - 1);

				}// end inner for loop
			}// end outer for loop
			System.out.println(countCons + " edge pairs forced to be nonintersecting");
		} else {
			// do nothing
		}

		/*
		 * generate constraints part 5 - keep original combinatorial embedding
		 */
		it = g.getVertices().iterator();
		while (it.hasNext()) {
			MetroVertex v = (MetroVertex) it.next();
			if (!v.isLabelVertex()) {// at label vertices there is no need to preserve the embedding
				if (v.isDummy() || v.getLabelLength() <= 0) {
					if (v.degree() <= 1)
						continue;
					if (v.degree() == 2) {
						Vertex[] neighbors = (Vertex[]) v.getNeighbors().toArray(new Vertex[0]);
						String v_id = vertexIDs.getLabel(v);
						String n0_id = vertexIDs.getLabel(neighbors[0]);
						String n1_id = vertexIDs.getLabel(neighbors[1]);
						Constraint con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + n0_id + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + n1_id + ")");
						myILP.addConstraintSummand(con, -M8, "sel_c1(" + v_id + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, -1);
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + n1_id + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + n0_id + ")");
						myILP.addConstraintSummand(con, M8, "sel_c1(" + v_id + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, M8 - 1);
						myILP.setVariableBinary("sel_c1(" + v_id + ")");
					} else if (v.degree() > 2) { // need to compute original edge order
						// MetroVertex[] neighbors = (MetroVertex[]) v.getNeighbors().toArray(new MetroVertex[0]);
						// Arrays.sort(neighbors, new CircularOrderComparator((MetroVertex)v));
						MetroVertex[] neighbors = (MetroVertex[]) v.getOrderedNeighbors().toArray(new MetroVertex[0]);
						String[] neighborIDs = new String[neighbors.length];
						for (int i = 0; i < neighbors.length; i++) {
							neighborIDs[i] = vertexIDs.getLabel(neighbors[i]);
						}
						int degree = v.degree();
						String v_id = vertexIDs.getLabel(v);
						Constraint con = myILP.createConstraint();
						for (int i = 1; i <= degree; i++) {
							myILP.addConstraintSummand(con, 1, "sel_c" + i + "(" + v_id + ")");
							myILP.setVariableBinary("sel_c" + i + "(" + v_id + ")");
						}
						myILP.setConstraintEqual(con);
						myILP.setConstraintRhs(con, 1);

						for (int i = 0; i < degree - 1; i++) {
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + neighborIDs[i] + ")");
							myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + neighborIDs[i + 1] + ")");
							myILP.addConstraintSummand(con, -M8, "sel_c" + (i + 1) + "(" + v_id + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, -1);
						}
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + neighborIDs[degree - 1] + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + neighborIDs[0] + ")");
						myILP.addConstraintSummand(con, -M8, "sel_c" + degree + "(" + v_id + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, -1);

						/*
						 * System.out.println("main vertex: " + vertexIDs.getLabel(v) + ((MetroVertex)v).getX() + " : " + ((MetroVertex)v).getY()); for (int i = 0; i < neighbors.length; i++) {
						 * System.out.println(neighbors[i].getX() + " : " + neighbors[i].getY()); } System.out.println("***");
						 * 
						 * for (int i = 0; i < neighbors.length; i++) { System.out.println(neighbors[i].getX() + " : " + neighbors[i].getY()); } System.out.println("###");
						 */

					} // end else if
				} else {
					if (v.degree() <= 2)
						continue;
					if (v.degree() == 3) {
						MetroVertex[] neighbors = (MetroVertex[]) v.getNeighbors().toArray(new MetroVertex[0]);
						String v_id = vertexIDs.getLabel(v);
						String n0_id = neighbors[0].isLabelVertex() ? vertexIDs.getLabel(neighbors[1]) : vertexIDs.getLabel(neighbors[0]);
						String n1_id = (neighbors[0].isLabelVertex() || neighbors[1].isLabelVertex()) ? vertexIDs.getLabel(neighbors[2]) : vertexIDs.getLabel(neighbors[1]);
						String labelVertexID;
						if (neighbors[0].isLabelVertex()) {
							labelVertexID = vertexIDs.getLabel(neighbors[0]);
						} else if (neighbors[1].isLabelVertex()) {
							labelVertexID = vertexIDs.getLabel(neighbors[1]);
						} else {
							labelVertexID = vertexIDs.getLabel(neighbors[2]);
						}

						Constraint con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + n0_id + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + n1_id + ")");
						myILP.addConstraintSummand(con, -M8, "sel_c1(" + v_id + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, -1);
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + n1_id + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + n0_id + ")");
						myILP.addConstraintSummand(con, M8, "sel_c1(" + v_id + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, M8 - 1);
						myILP.setVariableBinary("sel_c1(" + v_id + ")");

						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + labelVertexID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + n0_id + ")");
						myILP.addConstraintSummand(con, M8, "sel_f1(" + labelVertexID + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, M8 - 1);
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + labelVertexID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + n0_id + ")");
						myILP.addConstraintSummand(con, M8, "sel_f1(" + labelVertexID + ")");
						myILP.setConstraintGreater(con);
						myILP.setConstraintRhs(con, 1);
						myILP.setVariableBinary("sel_f1(" + labelVertexID + ")");
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + labelVertexID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + n1_id + ")");
						myILP.addConstraintSummand(con, M8, "sel_f2(" + labelVertexID + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, M8 - 1);
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + labelVertexID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + n1_id + ")");
						myILP.addConstraintSummand(con, M8, "sel_f2(" + labelVertexID + ")");
						myILP.setConstraintGreater(con);
						myILP.setConstraintRhs(con, 1);
						myILP.setVariableBinary("sel_f2(" + labelVertexID + ")");
					} else if (v.degree() > 3) { // need to compute original edge order
						// MetroVertex[] neighbors = (MetroVertex[]) v.getNeighbors().toArray(new MetroVertex[0]);
						// Arrays.sort(neighbors, new CircularOrderComparator((MetroVertex)v));
						MetroVertex[] neighbors_withlabel = (MetroVertex[]) v.getOrderedNeighbors().toArray(new MetroVertex[0]);
						MetroVertex[] neighbors;
						/* we know that one of the neighbors is a label vertex which has to be excluded here... */
						Vector myVec = new Vector(v.degree() - 1);
						MetroVertex labelVertex;
						String labelVertexID = null;
						for (int i = 0; i < neighbors_withlabel.length; i++) {
							MetroVertex mv = neighbors_withlabel[i];
							if (!mv.isLabelIntersectionVertex()) {
								myVec.add(mv);
							} else {
								labelVertex = mv;
								labelVertexID = vertexIDs.getLabel(labelVertex);
								// System.out.println("removed vertex " + vertexIDs.getLabel(neighbors_withlabel[i]) + " from " + v.getName());
							}
						}
						neighbors = (MetroVertex[]) myVec.toArray(new MetroVertex[0]);
						String[] neighborIDs = new String[neighbors.length];
						for (int i = 0; i < neighbors.length; i++) {
							neighborIDs[i] = vertexIDs.getLabel(neighbors[i]);
						}
						int degree = v.degree() - 1;
						String v_id = vertexIDs.getLabel(v);
						Constraint con = myILP.createConstraint();
						for (int i = 1; i <= degree; i++) {
							myILP.addConstraintSummand(con, 1, "sel_c" + i + "(" + v_id + ")");
							myILP.setVariableBinary("sel_c" + i + "(" + v_id + ")");
						}
						myILP.setConstraintEqual(con);
						myILP.setConstraintRhs(con, 1);

						for (int i = 0; i < degree - 1; i++) {
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + neighborIDs[i] + ")");
							myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + neighborIDs[i + 1] + ")");
							myILP.addConstraintSummand(con, -M8, "sel_c" + (i + 1) + "(" + v_id + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, -1);
						}
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + neighborIDs[degree - 1] + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + neighborIDs[0] + ")");
						myILP.addConstraintSummand(con, -M8, "sel_c" + degree + "(" + v_id + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, -1);

						/* make sure that the label edge is not directed like one of the regular edges */
						for (int i = 0; i < neighborIDs.length; i++) {
							String neighborID = neighborIDs[i];
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + labelVertexID + ")");
							myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + neighborID + ")");
							myILP.addConstraintSummand(con, M8, "sel_f" + i + "(" + labelVertexID + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, M8 - 1);
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "dir(" + v_id + "," + labelVertexID + ")");
							myILP.addConstraintSummand(con, -1, "dir(" + v_id + "," + neighborID + ")");
							myILP.addConstraintSummand(con, M8, "sel_f" + i + "(" + labelVertexID + ")");
							myILP.setConstraintGreater(con);
							myILP.setConstraintRhs(con, 1);
							myILP.setVariableBinary("sel_f" + i + "(" + labelVertexID + ")");
						}

						/*
						 * System.out.println("main vertex: " + vertexIDs.getLabel(v) + ((MetroVertex)v).getX() + " : " + ((MetroVertex)v).getY()); for (int i = 0; i < neighbors.length; i++) {
						 * System.out.println(neighbors[i].getX() + " : " + neighbors[i].getY()); } System.out.println("***");
						 * 
						 * for (int i = 0; i < neighbors.length; i++) { System.out.println(neighbors[i].getX() + " : " + neighbors[i].getY()); } System.out.println("###");
						 */
					}
				}
			}
		} // end while

		/*
		 * create the objective function
		 * 
		 * D(n1, n2) gives an upper bound on the length of this edge the sum over all D is then minimized
		 * 
		 * additionally sum(|dir() - sec()|) is minimized in order to favor the original direction
		 * 
		 * further bends along a line are penalized
		 */

		it = g.getEdges().iterator();
		while (it.hasNext()) {
			MetroEdge e = (MetroEdge) it.next();
			if (e.isLabelParallelDummy() || e.isLabelDirectionDummy() || e.isLabelIntersectionConnectDummy() || e.isLabelIntersectionMainDummy())
				continue; // length of label edges does not count
			Vertex first = (Vertex) e.getEndpoints().getFirst();
			String firstID = vertexIDs.getLabel(first);
			Vertex second = (Vertex) e.getEndpoints().getSecond();
			String secondID = vertexIDs.getLabel(second);
			int sector = e.isRegular() ? ((Integer) e.getUserDatum("sector1to2")).intValue() : ((Integer) e.getMainEdge().getUserDatum("sector1to2")).intValue();
			int previousSector = (sector + 7) % 8; // i.e. sector - 1 (mod 8)
			int nextSector = (sector + 1) % 8;
			String mainFirstID = e.isRegular() ? firstID : vertexIDs.getLabel((MetroVertex) e.getMainEdge().getEndpoints().getFirst());
			String mainSecondID = e.isRegular() ? secondID : vertexIDs.getLabel((MetroVertex) e.getMainEdge().getEndpoints().getSecond());

			// 08-06-20: no need to use sel_bi variables since D(uv) is an upper bound on max {|x(u)-x(v)|, |y(u)-y(v)|}
			// TODO: in some cases constraints are repeated in the three switch statements; doesn't bother cplex though.
			Constraint con = myILP.createConstraint();
			myILP.addConstraintSummand(con, 1, "D(" + firstID + "," + secondID + ")");
			myILP.setConstraintRhs(con, 0);
			switch (previousSector) {
				case 0:
				case 1:
					myILP.addConstraintSummand(con, 1, "x(" + firstID + ")");
					myILP.addConstraintSummand(con, -1, "x(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b1(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);
					break;
				case 2:
				case 3:
					myILP.addConstraintSummand(con, 1, "y(" + firstID + ")");
					myILP.addConstraintSummand(con, -1, "y(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b1(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);
					break;
				case 4:
				case 5:
					myILP.addConstraintSummand(con, -1, "x(" + firstID + ")");
					myILP.addConstraintSummand(con, 1, "x(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b1(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);
					break;
				case 6:
				case 7:
					myILP.addConstraintSummand(con, -1, "y(" + firstID + ")");
					myILP.addConstraintSummand(con, 1, "y(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b1(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);

			}
			con = myILP.createConstraint();
			myILP.addConstraintSummand(con, 1, "D(" + firstID + "," + secondID + ")");
			myILP.setConstraintRhs(con, 0);
			switch (sector) {
				case 0:
				case 1:
					myILP.addConstraintSummand(con, 1, "x(" + firstID + ")");
					myILP.addConstraintSummand(con, -1, "x(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b2(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);
					break;
				case 2:
				case 3:
					myILP.addConstraintSummand(con, 1, "y(" + firstID + ")");
					myILP.addConstraintSummand(con, -1, "y(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b2(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);
					break;
				case 4:
				case 5:
					myILP.addConstraintSummand(con, -1, "x(" + firstID + ")");
					myILP.addConstraintSummand(con, 1, "x(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b2(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);
					break;
				case 6:
				case 7:
					myILP.addConstraintSummand(con, -1, "y(" + firstID + ")");
					myILP.addConstraintSummand(con, 1, "y(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b2(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);

			}
			con = myILP.createConstraint();
			myILP.addConstraintSummand(con, 1, "D(" + firstID + "," + secondID + ")");
			myILP.setConstraintRhs(con, 0);
			switch (nextSector) {
				case 0:
				case 1:
					myILP.addConstraintSummand(con, 1, "x(" + firstID + ")");
					myILP.addConstraintSummand(con, -1, "x(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b3(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);
					break;
				case 2:
				case 3:
					myILP.addConstraintSummand(con, 1, "y(" + firstID + ")");
					myILP.addConstraintSummand(con, -1, "y(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b3(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);
					break;
				case 4:
				case 5:
					myILP.addConstraintSummand(con, -1, "x(" + firstID + ")");
					myILP.addConstraintSummand(con, 1, "x(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b3(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);
					break;
				case 6:
				case 7:
					myILP.addConstraintSummand(con, -1, "y(" + firstID + ")");
					myILP.addConstraintSummand(con, 1, "y(" + secondID + ")");
					// myILP.addConstraintSummand(con, -MPOS, "sel_b3(" + mainFirstID + "," + mainSecondID + ")");
					myILP.setConstraintGreater(con);
					// myILP.setConstraintRhs(con, -MPOS);

			}

			if ((e.isLabelConnectDummy() && e.getMainEdge().getNumVertices() >= 2) || e.isLabelJointEdge()) {
				// charge more for those edges of the triple that do not contain the tickmarks in order to keep them as short as possible
				myILP.addObjectiveFunctionSummand(2 * lengthFac, "D(" + firstID + "," + secondID + ")");
			} else {
				myILP.addObjectiveFunctionSummand(lengthFac, "D(" + firstID + "," + secondID + ")");
			}
			myILP.setVariableLower("D(" + firstID + "," + secondID + ")", 0);
			// not necessarily integer valued...
			myILP.setVariableReal("D(" + firstID + "," + secondID + ")");

			// constraints for the deviation from the original sector
			// 08-06-20: previously had two binary variables (sel_d0 and sel_d1), one in each constraint; unified them.
			if (e.isLabelMainDummy() || e.isRegular()) {
				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
				myILP.addConstraintSummand(con, -7, "sel_d0(" + firstID + "," + secondID + ")");
				myILP.setConstraintLess(con);
				myILP.setConstraintRhs(con, ((Integer) e.getUserDatum("sector1to2")).intValue());

				con = myILP.createConstraint();
				myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
				myILP.addConstraintSummand(con, 7, "sel_d0(" + firstID + "," + secondID + ")");
				myILP.setConstraintGreater(con);
				myILP.setConstraintRhs(con, ((Integer) e.getUserDatum("sector1to2")).intValue());

				myILP.setVariableBinary("sel_d0(" + firstID + "," + secondID + ")");
				// myILP.setVariableBinary("sel_d1(" + firstID + "," + secondID + ")");

				myILP.addObjectiveFunctionSummand(deviationFac, "sel_d0(" + firstID + "," + secondID + ")");
				// myILP.addObjectiveFunctionSummand(deviationFac, "sel_d1(" + firstID + "," + secondID + ")");
			}
		}
		
		// determine min, max, and average all-pairs-count
		int allPairsDijkstraMinCount = Integer.MAX_VALUE;
		int allPairsDijkstraMaxCount = 0;
		int allPairsDijkstraAvgCount = 0;
		int allPairsDijkstraAvgCountTotal = 0;
		int allPairsDijkstraAvgCountAmount = 0;
		
		Set lineSet = GraphTools.getMetroLines(g);
		it = lineSet.iterator();
		while (it.hasNext()) {
			MetroPath currentLine = (MetroPath) it.next();
			MetroEdge currentEdge = (MetroEdge) currentLine.getFirst();
			MetroEdge nextEdge = null;
			ListIterator lit = currentLine.listIterator(1);
			while (lit.hasNext()) {
				nextEdge = (MetroEdge) lit.next();
				
				int count = (int) currentEdge.getAllPairsDijkstraCount(currentLine.getName());
				allPairsDijkstraMinCount = Math.min(allPairsDijkstraMinCount, count);
				allPairsDijkstraMaxCount = Math.max(allPairsDijkstraMaxCount, count);
				allPairsDijkstraAvgCountTotal += count;
				allPairsDijkstraAvgCountAmount++;
				
				currentEdge = nextEdge;
			}
		}
		allPairsDijkstraAvgCount = allPairsDijkstraAvgCountTotal / allPairsDijkstraAvgCountAmount;
		System.out.println("allPairsDijkstraAvgCount " + allPairsDijkstraAvgCount);

		// bend costs
		// 08-06-23: rephrased the constraints with only two binary variables and less constraints
		//Set lineSet = GraphTools.getMetroLines(g);
		it = lineSet.iterator();
		while (it.hasNext()) {
			MetroPath currentLine = (MetroPath) it.next();
			if (currentLine.size() > 1) {// otherwise nothing to do
				MetroEdge currentEdge = (MetroEdge) currentLine.getFirst();
				MetroVertex first = (MetroVertex) currentEdge.getEndpoints().getFirst();
				MetroVertex second = (MetroVertex) currentEdge.getEndpoints().getSecond();
				MetroVertex currentVertex;
				int currentBendFac = bendFac;
				MetroEdge nextEdge = null;
				if (((Edge) currentLine.get(1)).isIncident(first)) { // get the connecting vertex
					currentVertex = (MetroVertex) first;
				} else {
					currentVertex = (MetroVertex) second;
				}
				ListIterator lit = currentLine.listIterator(1);
				while (lit.hasNext()) {
					nextEdge = (MetroEdge) lit.next();
					String firstID = vertexIDs.getLabel(currentEdge.getOpposite(currentVertex));
					String secondID = vertexIDs.getLabel(currentVertex);
					String thirdID = vertexIDs.getLabel(nextEdge.getOpposite(currentVertex));
					if (!(currentVertex.isDummy() || currentVertex.isLabelVertex())) {
						if (currentVertex.degree() > 2) { // we have an intersection vertex
							currentBendFac = bendFac * BEND_INTERSECTION; // bend penalty x 3 on intersections
						} else {
							currentBendFac = bendFac;
						}

						// Modification by Michel
						// if(line is important) {
						// currentBendFac *= thickness;
						// }

						// Find out real first and real second, so that we can do a lookup in the actual graph
						// Only useful when insertBends is used
						/*
						 * MetroVertex realFirst = null; MetroVertex realSecond = null; if(false == ((MetroVertex)currentEdge.getOpposite(currentVertex)).isJoint()) { realFirst = (MetroVertex)
						 * currentEdge.getOpposite(currentVertex); } if(false == currentVertex.isJoint()) { if(realFirst == null) { realFirst = currentVertex; } else { realSecond = currentVertex; } }
						 * if(false == ((MetroVertex)nextEdge.getOpposite(currentVertex)).isJoint()) { if(realFirst == null) { realFirst = (MetroVertex) nextEdge.getOpposite(currentVertex); } else {
						 * realSecond = (MetroVertex) nextEdge.getOpposite(currentVertex); } }
						 */

						// Find shared incident edge
						// System.out.println("find shared edge between " + realFirst + " and " + realSecond);
						if(Settings.modifiedSchematization == true) {
							int count = (int) currentEdge.getAllPairsDijkstraCount(currentLine.getName());
							int multiplier = (int) (((double) (count - allPairsDijkstraAvgCount)) / ((double) (allPairsDijkstraMaxCount - allPairsDijkstraAvgCount)) * 10.0);
							currentBendFac = Math.max(0, multiplier + 5); // varies from 1 - (10 + 5)
							System.out.println("currentBendFac dev(" + firstID + "," + secondID + "," + thirdID + "): " + currentBendFac);
						}
						
						Constraint con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, 1, "sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						// myILP.addConstraintSummand(con, 1, "sel_e2(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, 1);
						myILP.setVariableBinary("sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setVariableBinary("sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						// myILP.setVariableBinary("sel_e2(" + firstID + "," + secondID + "," + thirdID + ")");
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, M3, "sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, -M9, "sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setConstraintGreater(con);
						myILP.setConstraintRhs(con, -4);
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, -M3, "sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, M9, "sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, 4);
						// con = myILP.createConstraint();
						// myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						// myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						// myILP.addConstraintSummand(con, M3, "sel_e2(" + firstID + "," + secondID + "," + thirdID + ")");
						// myILP.setConstraintGreater(con);
						// myILP.setConstraintRhs(con, -4);
						// con = myILP.createConstraint();
						// myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						// myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						// myILP.addConstraintSummand(con, -M3, "sel_e2(" + firstID + "," + secondID + "," + thirdID + ")");
						// myILP.setConstraintLess(con);
						// myILP.setConstraintRhs(con, 4);
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, 8, "sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, -8, "sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, -1, "dev(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, 0);
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, 8, "sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, -8, "sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, 1, "dev(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setConstraintGreater(con);
						myILP.setConstraintRhs(con, 0);
						myILP.setVariableReal("dev(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addObjectiveFunctionSummand(currentBendFac, "dev(" + firstID + "," + secondID + "," + thirdID + ")");
						// Testing a modified penalty scheme: bends larger than 90 degrees cost overproportionally more than 0 or 45
						if (ext_penalty) {
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "dev(" + firstID + "," + secondID + "," + thirdID + ")");
							myILP.addConstraintSummand(con, -1, "dev2(" + firstID + "," + secondID + "," + thirdID + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, 1);
							myILP.setVariableReal("dev2(" + firstID + "," + secondID + "," + thirdID + ")");
							myILP.setVariableLower("dev2(" + firstID + "," + secondID + "," + thirdID + ")", 0);
							myILP.setVariableUpper("dev2(" + firstID + "," + secondID + "," + thirdID + ")", 3);
							myILP.addObjectiveFunctionSummand(currentBendFac, "dev2(" + firstID + "," + secondID + "," + thirdID + ")");
						}

					} else if (currentVertex.isDummy()) { // at a dummy vertex both directions have to be equal
						Constraint con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						myILP.setConstraintEqual(con);
						myILP.setConstraintRhs(con, 0);
					}
					currentEdge = nextEdge;
					currentVertex = (MetroVertex) currentEdge.getOpposite(currentVertex);
				}
				if (currentVertex.isIncident((Edge) currentLine.getFirst())) { // is it a cycle? then close the loop...
					nextEdge = (MetroEdge) currentLine.getFirst();
					String firstID = vertexIDs.getLabel(currentEdge.getOpposite(currentVertex));
					String secondID = vertexIDs.getLabel(currentVertex);
					String thirdID = vertexIDs.getLabel(nextEdge.getOpposite(currentVertex));
					if (!(currentVertex.isDummy() || currentVertex.isLabelVertex())) {
						if (currentVertex.degree() > 2) { // we have an intersection vertex
							currentBendFac = bendFac * BEND_INTERSECTION;
						} else {
							currentBendFac = bendFac;
						}
						Constraint con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, 1, "sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						// myILP.addConstraintSummand(con, 1, "sel_e2(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, 1);
						myILP.setVariableBinary("sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setVariableBinary("sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						// myILP.setVariableBinary("sel_e2(" + firstID + "," + secondID + "," + thirdID + ")");
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, M3, "sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, -M9, "sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setConstraintGreater(con);
						myILP.setConstraintRhs(con, -4);
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, -M3, "sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, M9, "sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, 4);
						// con = myILP.createConstraint();
						// myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						// myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						// myILP.addConstraintSummand(con, M3, "sel_e2(" + firstID + "," + secondID + "," + thirdID + ")");
						// myILP.setConstraintGreater(con);
						// myILP.setConstraintRhs(con, -4);
						// con = myILP.createConstraint();
						// myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						// myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						// myILP.addConstraintSummand(con, -M3, "sel_e2(" + firstID + "," + secondID + "," + thirdID + ")");
						// myILP.setConstraintLess(con);
						// myILP.setConstraintRhs(con, 4);
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, 8, "sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, -8, "sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, -1, "dev(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setConstraintLess(con);
						myILP.setConstraintRhs(con, 0);
						con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, 8, "sel_e0(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, -8, "sel_e1(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addConstraintSummand(con, 1, "dev(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.setConstraintGreater(con);
						myILP.setConstraintRhs(con, 0);
						myILP.setVariableReal("dev(" + firstID + "," + secondID + "," + thirdID + ")");
						myILP.addObjectiveFunctionSummand(currentBendFac, "dev(" + firstID + "," + secondID + "," + thirdID + ")");
						// Testing a modified penalty scheme: bends larger than 90 degrees cost overproportionally more than 0 or 45
						if (ext_penalty) {
							con = myILP.createConstraint();
							myILP.addConstraintSummand(con, 1, "dev(" + firstID + "," + secondID + "," + thirdID + ")");
							myILP.addConstraintSummand(con, -1, "dev2(" + firstID + "," + secondID + "," + thirdID + ")");
							myILP.setConstraintLess(con);
							myILP.setConstraintRhs(con, 1);
							myILP.setVariableReal("dev2(" + firstID + "," + secondID + "," + thirdID + ")");
							myILP.setVariableLower("dev2(" + firstID + "," + secondID + "," + thirdID + ")", 0);
							myILP.setVariableUpper("dev2(" + firstID + "," + secondID + "," + thirdID + ")", 3);
							myILP.addObjectiveFunctionSummand(currentBendFac, "dev2(" + firstID + "," + secondID + "," + thirdID + ")");
						}
					} else if (currentVertex.isDummy()) { // at a dummy vertex both directions have to be equal
						Constraint con = myILP.createConstraint();
						myILP.addConstraintSummand(con, 1, "dir(" + firstID + "," + secondID + ")");
						myILP.addConstraintSummand(con, -1, "dir(" + secondID + "," + thirdID + ")");
						myILP.setConstraintEqual(con);
						myILP.setConstraintRhs(con, 0);
					}
				}

			}
		}

		/* print some stats about the ilp */
		System.out.println("the mip has " + myILP.numVariables() + " variables");
		System.out.println("the mip has " + myILP.numConstraints() + " constraints");
		System.out.println("the mip has " + myILP.numLazyConstraints() + " lazy constraints");

		myILP.writeLP(outFile, (String) g.getUserDatum("sourceFile"));
		return myILP;
	}

}
