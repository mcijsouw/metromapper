package app;

import io.GraphMLReader;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import mip.Solver;
import model.TransferGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.utils.UserData;

public class MetroMapper {

	private JFrame frame;
	private GUIBuilder guiBuilder;
	private SvgCanvas svgCanvas;
	private Solver solver;
	private Graph g;

	public MetroMapper() {
		this.frame = new JFrame("MetroMapper");
		this.guiBuilder = new GUIBuilder(this, this.frame);
		this.guiBuilder.build();
		this.svgCanvas = this.guiBuilder.getSvgCanvas();
		this.initializeMap();
	}

	public void resetMap() {

	}
	
	public void initializeMap() {
		if(Settings.loadedGraphMLFile instanceof File) {
			this.initializeMap(Settings.loadedGraphMLFile);
		} else {
			this.initializeMap("resources/graphml/" + Settings.inputMap);
		}
	}
	
	public void initializeMap(File file) {
		try {
			GraphMLReader reader = new GraphMLReader();
			this.g = reader.loadGraphBySelectedFile(file);
			this._initializeMapPostProcess();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void initializeMap(String resource) {
		try {
			GraphMLReader reader = new GraphMLReader();
			this.g = reader.loadGraph(resource);
			this._initializeMapPostProcess();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void _initializeMapPostProcess() {
		try {
			Graph tg = TransferGraph.convert(this.g);
			this.g.addUserDatum("transferGraph", tg, UserData.SHARED);
			if(Settings.renderTransferGraph) {
				this.guiBuilder.setGraph(tg);
			} else {
				this.guiBuilder.setGraph(this.g);
			}
			svgCanvas.renderGraph(this.g);
			this.computeDijkstraWeights();
		} catch(Exception e) {
			e.printStackTrace();
		}

		this.initializeView();
		this.repaintMap();
	}

	public void repaintMap() {
		svgCanvas.repaint();
	}

	public void initializeView() {
		svgCanvas.initializeView();
	}

	public Solver getSolver() {
		return this.solver;
	}

	public void setSolver(Solver s) {
		this.solver = s;
	}
	
	public SvgCanvas getSvgCanvas() {
		return this.guiBuilder.getSvgCanvas();
	}
	
	public GUIBuilder getGUIBuilder() {
		return this.guiBuilder;
	}

	public void cancelSolver() {
		if (this.solver != null && !this.solver.isDone()) {
			this.solver.cancel(true);
			this.solver = null;
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				MetroMapper app = new MetroMapper();
			}
		});
	}

	/** @todo move method */
	public void computeDijkstraWeights() {
		if(this.g instanceof Graph) {
			Dijkstra.computeShortestPaths(this.g);
			this.repaintMap();
		}
	}

	public Graph getGraph() {
		return this.g;
	}

}