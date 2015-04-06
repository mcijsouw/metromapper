package model;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;

public class TransferGraphEdge extends MetroEdge {

	private int time = 0;
	private int dijkstraCount = 0;
	private int allPairsDijkstraCount = 0;
	private MetroEdge originalEdge;

	public TransferGraphEdge(Vertex arg0, Vertex arg1) {
		super(arg0, arg1);
		multiplicity = 1;
	}

	public String toString() {
		return "tge" + this.id + "[" + this.getFirst().getName() + "-" + this.getSecond().getName() + "]";
	}
	
	public int getId() {
		return this.id;
	}
	
	public int getTime() {
		return this.time;
	}
	
	public void setTime(int w) {
		this.time = w;
	}

	public MetroVertex getFirst() {
		return (MetroVertex) this.getEndpoints().getFirst();
	}

	public MetroVertex getSecond() {
		return (MetroVertex) this.getEndpoints().getSecond();
	}

	public int getDijkstraCount() {
		return this.dijkstraCount;
	}
	
	public void setDijkstraCount(int count) {
		this.dijkstraCount = count;
	}

	public void increaseDijkstraCount() {
		//System.out.println("Increasing dijkstra count on " + this);
		this.dijkstraCount++;
	}

	public void setOriginalEdge(MetroEdge edge) {
		this.originalEdge = edge;
	}
	
	public MetroEdge getOriginalEdge() {
		return this.originalEdge;
	}

	public int getAllPairsDijkstraCount() {
		return this.allPairsDijkstraCount;
	}
	
	public void setAllPairsDijkstraCount(int count) {
		this.allPairsDijkstraCount = count;
	}

	public void increaseAllPairsDijkstraCount() {
		//System.out.println("Increasing all-pairs dijkstra count on " + this);
		this.allPairsDijkstraCount++;
	}
}
