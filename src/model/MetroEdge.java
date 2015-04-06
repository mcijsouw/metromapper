package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.utils.Pair;

public class MetroEdge extends UndirectedSparseEdge {

	static public final int REGULAR = 0;
	static public final int CONVEX_HULL_DUMMY = 1;
	static public final int LABEL_DIRECTION_DUMMY = 2;
	static public final int LABEL_PARALLEL_DUMMY = 3;
	static public final int LABEL_MAIN_DUMMY = 4;
	static public final int LABEL_CONNECT_DUMMY = 5;
	static public final int LABEL_INTERSECTION_CONNECT = 6;
	static public final int LABEL_INTERSECTION_MAIN = 7;
	// static public final int SKIPPED_LABEL = 8;

	private Face leftFace, rightFace;
	private boolean pendant;
	private boolean labelJointEdge;
	private boolean bendEdge;
	// private boolean dummy; //marks edges that are temporarilly introduced in the convex hull heuristic
	private List myVertexNames;
	private List myVertexLengths;
	private double currentLabelWidth;
	private int numVertices;
	private MetroEdge mainEdge;
	private int dummyType;
	private double length;
	private boolean skipped;
	protected int multiplicity;
	private int direction;
	private Set myFaceCandidates;
	private HashMap<String, Integer> time;
	private HashMap<String, TransferGraphEdge> transferGraphEdges;
	private int dijkstraCount = 0;
	private int arrowDirection;

	/**
	 * @param arg0
	 * @param arg1
	 */
	public MetroEdge(Vertex arg0, Vertex arg1) {
		super(arg0, arg1);
		leftFace = null;
		rightFace = null;
		pendant = false;
		labelJointEdge = false;
		bendEdge = false;
		dummyType = REGULAR;
		mainEdge = null;
		myVertexNames = null;
		myVertexLengths = null;
		currentLabelWidth = 0;
		numVertices = 0;
		multiplicity = 0;
		direction = -1;
		dummyType = 0;
		length = 1.0;
		skipped = false;
		myFaceCandidates = new HashSet();
		time = new HashMap<String, Integer>();
		transferGraphEdges = new HashMap<String, TransferGraphEdge>();
	}

	public String toString() {
		return "e" + this.id + "[" + this.getFirst().getName() + "-" + this.getSecond().getName() + "]";
	}
	
	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}
	
	public void setArrowDirection(int direction) {
		this.arrowDirection = direction;
	}
	
	public int getArrowDirection() {
		return this.arrowDirection;
	}
	
	public int getTime(String line) {
		if(this.time.get(line) != null) {
			return this.time.get(line);
		}
		return 1;
		//return Integer.MAX_VALUE; // @todo
	}
	
	public void setTime(String line, int time) {
		
		this.time.put(line, time);
	}

	public boolean findFaceCandidates() {
		if (dummyType == LABEL_INTERSECTION_CONNECT || dummyType == LABEL_INTERSECTION_MAIN) {
			MetroVertex intersection = this.getFirst();
			for (Iterator it = intersection.getIncidentEdges().iterator(); it.hasNext();) {
				MetroEdge myEdge = (MetroEdge) it.next();
				myFaceCandidates.add(myEdge.getLeftFace());
				myFaceCandidates.add(myEdge.getRightFace());
			}
			return true;
		} else {
			return false;
		}
	}

	public Set getFaceCandidates() {
		return myFaceCandidates;
	}

	public void setFaceCandidates(Set mySet) {
		this.myFaceCandidates = mySet;
	}

	public boolean isFaceCandidate(Face face) {
		return myFaceCandidates.contains(face);
	}

	public void printFaceCandidates() {
		for (Iterator it = myFaceCandidates.iterator(); it.hasNext();) {
			Face face = (Face) it.next();
			System.out.println(face.toString());
		}
	}

	/**
	 * @return Returns the dummy.
	 */
	public boolean isDummy() {
		return (dummyType == CONVEX_HULL_DUMMY);
	}

	/**
	 * @param dummy
	 *            The dummy to set.
	 */
	public void setDummyType(int dummy) {
		this.dummyType = dummy;
	}

	public boolean isRegular() {
		return (dummyType == REGULAR);
	}

	public boolean isCriticalLabelEdge() {
		return ((dummyType == LABEL_DIRECTION_DUMMY) || (dummyType == LABEL_PARALLEL_DUMMY) || (dummyType == LABEL_INTERSECTION_CONNECT) || (dummyType == LABEL_INTERSECTION_MAIN));
	}

	public boolean isLabelParallelDummy() {
		return (dummyType == LABEL_PARALLEL_DUMMY);
	}

	public boolean isLabelDirectionDummy() {
		return (dummyType == LABEL_DIRECTION_DUMMY);
	}

	public boolean isLabelMainDummy() {
		return (dummyType == LABEL_MAIN_DUMMY);
	}

	public boolean isLabelConnectDummy() {
		return (dummyType == LABEL_CONNECT_DUMMY);
	}

	public boolean isLabelIntersectionConnectDummy() {
		return (dummyType == LABEL_INTERSECTION_CONNECT);
	}

	public boolean isLabelIntersectionMainDummy() {
		return (dummyType == LABEL_INTERSECTION_MAIN);
	}

	public boolean isLabelIntersectionDummy() {
		return (dummyType == LABEL_INTERSECTION_MAIN || dummyType == LABEL_INTERSECTION_CONNECT);
	}

	public boolean isSkippedLabelEdge() {
		return skipped;
	}

	public void setSkippedLabelEdge(boolean skipped) {
		this.skipped = skipped;
	}

	public boolean isLabelJointEdge() {
		return labelJointEdge;
	}

	public void setLabelJointEdge(boolean status) {
		labelJointEdge = status;
	}

	/**
	 * @return Returns the leftFace.
	 */
	public Face getLeftFace() {
		return leftFace;
	}

	/**
	 * @param leftFace
	 *            The leftFace to set.
	 */
	public void setLeftFace(Face leftFace) {
		this.leftFace = leftFace;
	}

	/**
	 * @return Returns the rightFace.
	 */
	public Face getRightFace() {
		return rightFace;
	}

	/**
	 * @param rightFace
	 *            The rightFace to set.
	 */
	public void setRightFace(Face rightFace) {
		this.rightFace = rightFace;
	}

	public Pair getFaces() {
		return new Pair(leftFace, rightFace);
	}

	/**
	 * @return Returns the pendant.
	 */
	public boolean isPendant() {
		return pendant;
	}

	/**
	 * @param pendant
	 *            The pendant to set.
	 */
	public void setPendant(boolean pendant) {
		this.pendant = pendant;
	}

	/**
	 * @return Returns the currentLabelWidth.
	 */
	public double getCurrentLabelWidth() {
		return currentLabelWidth;
	}

	/**
	 * @param currentLabelWidth
	 *            The currentLabelWidth to set.
	 */
	public void setCurrentLabelWidth(double currentLabelWidth) {
		this.currentLabelWidth = currentLabelWidth;
	}

	/**
	 * @return Returns the myVertices.
	 */
	public List getMyVertexNames() {
		return myVertexNames;
	}

	public List getInvertedNames() {
		List theList = new LinkedList();
		ListIterator iter = myVertexNames.listIterator(myVertexNames.size());
		while (iter.hasPrevious()) {
			Object name = iter.previous();
			theList.add(name);
		}
		return theList;
	}

	/**
	 * @param myVertices
	 *            The myVertices to set.
	 */
	public void setMyVertexNames(List myVertexNames) {
		this.myVertexNames = myVertexNames;
	}

	/**
	 * @return Returns the myVertexLengths.
	 */
	public List getMyVertexLengths() {
		return myVertexLengths;
	}

	public List getInvertedLengths() {
		List theList = new LinkedList();
		ListIterator iter = myVertexLengths.listIterator(myVertexLengths.size());
		while (iter.hasPrevious()) {
			Object name = iter.previous();
			theList.add(name);
		}
		return theList;
	}

	/**
	 * @param myVertexLengths
	 *            The myVertexLengths to set.
	 */
	public void setMyVertexLengths(List myVertexLengths) {
		this.myVertexLengths = myVertexLengths;
	}

	/**
	 * @return Returns the numVertices.
	 */
	public int getNumVertices() {
		return numVertices;
	}

	/**
	 * @param numVertices
	 *            The numVertices to set.
	 */
	public void setNumVertices(int numVertices) {
		this.numVertices = numVertices;
	}

	/**
	 * @return Returns the multiplicity.
	 */
	public int getMultiplicity() {
		return multiplicity;
	}

	/**
	 * @param multiplicity
	 *            The edge multiplicity to set.
	 */
	public void setMultiplicity(int multiplicity) {
		this.multiplicity = multiplicity;
	}

	/**
	 * increases the edge multiplicity.
	 */
	public void increaseMultiplicity() {
		multiplicity++;
	}

	/**
	 * @return Returns the length.
	 */
	public double getLength() {
		return length;
	}

	/**
	 * @param length
	 *            The length to set.
	 */
	public void setLength(double length) {
		this.length = length;
	}

	/**
	 * @return Returns the parentEdge.
	 */
	public MetroEdge getMainEdge() {
		return mainEdge;
	}

	/**
	 * @param parentEdge
	 *            The parentEdge to set.
	 */
	public void setMainEdge(MetroEdge mainEdge) {
		this.mainEdge = mainEdge;
	}

	public MetroVertex getFirst() {
		return (MetroVertex) this.getEndpoints().getFirst();
	}

	public MetroVertex getSecond() {
		return (MetroVertex) this.getEndpoints().getSecond();
	}

	public boolean isBendEdge() {
		return bendEdge;
	}

	public void setBendEdge(boolean bendEdge) {
		this.bendEdge = bendEdge;
	}
	
	public HashMap<String, TransferGraphEdge> getTransferGraphEdges() {
		return this.transferGraphEdges;
	}

	public void addTransferGraphEdge(String name, TransferGraphEdge tge) {
		this.transferGraphEdges.put(name, tge);
	}

	public double getDijkstraCount(String pathName) {
		TransferGraphEdge edge = this.transferGraphEdges.get(pathName);
		//System.out.println(edge + " with count " + edge.getDijkstraCount());
		if(edge != null) {
			return edge.getDijkstraCount();
		}
		return 0;
	}

	public double getAllPairsDijkstraCount(String pathName) {
		TransferGraphEdge edge = this.transferGraphEdges.get(pathName);
		//System.out.println(edge + " with count " + edge.getDijkstraCount());
		if(edge != null) {
			return edge.getAllPairsDijkstraCount();
		}
		return 0;
	}
}
