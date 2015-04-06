package model;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import mip.comparator.CircularOrderComparator;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;

public class MetroVertex extends UndirectedSparseVertex {

	private double x;
	private double y;
	private boolean dummy = false;
	private boolean labelVertex = false;
	private boolean labelIntersectionVertex = false;
	private boolean intersection = false;
	private boolean terminus = false;
	private boolean joint = false;
	private String name = "";
	private LinkedList cyclicEdgeList;
	private double labelLength = 0;
	private Color color = Color.BLACK;
	private Integer[] passengerCount = new Integer[96];
	
	private MetroVertex originalVertex = null; // used in transfer graph
	private boolean originalVertexIsDrawn = false;
	private HashMap<MetroPath, MetroVertex> transferVertexMap = new HashMap<MetroPath, MetroVertex>();
	
	/**
	 * 
	 */
	public MetroVertex() {
		super();
		cyclicEdgeList = new LinkedList();
		x = 0;
		y = 0;
	}

	/**
	 * 
	 */
	public MetroVertex(double x, double y) {
		super();
		cyclicEdgeList = new LinkedList();
		this.x = x;
		this.y = y;
	}
	
	public String toString() {
		String v = super.toString();
		return v + "[" + this.getName() + "]";
	}
	
	public boolean getOriginalVertexIsDrawn()
	{
		return this.originalVertexIsDrawn;
	}
	
	public void setOriginalVertexIsDrawn(boolean b)
	{
		this.originalVertexIsDrawn = b;
	}
	
	public MetroVertex getOriginalVertex()
	{
		return this.originalVertex;
	}
	
	public void setOriginalVertex(MetroVertex v)
	{
		this.originalVertex = v;
	}
	
	public void setTransferVertexMap(HashMap<MetroPath, MetroVertex> map) 
	{
		this.transferVertexMap = map;
	}
	
	public HashMap<MetroPath, MetroVertex> getTransferVertexMap() 
	{
		return this.transferVertexMap;
	}
	
	public void setPassengerCount(Integer[] passengerCount) {
		this.passengerCount = passengerCount;
	}
	
	public Integer[] getPassengerCount() {
		return this.passengerCount;
	}
	
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the labelLength.
	 */
	public double getLabelLength() {
		return labelLength;
	}

	/**
	 * @param labelLength
	 *            The labelLength to set.
	 */
	public void setLabelLength(double labelLength) {
		this.labelLength = labelLength;
	}

	/**
	 * @return Returns the intersection.
	 */
	public boolean isIntersection() {
		return intersection;
	}

	/**
	 * @param intersection
	 *            The intersection to set.
	 */
	public void setIntersection(boolean intersection) {
		this.intersection = intersection;
	}

	/**
	 * @return Returns the labelVertex.
	 */
	public boolean isLabelVertex() {
		return labelVertex;
	}

	/**
	 * @param labelVertex
	 *            The labelVertex to set.
	 */
	public void setLabelVertex(boolean labelVertex) {
		this.labelVertex = labelVertex;
	}

	/**
	 * @return Returns the labelIntersectionVertex.
	 */
	public boolean isLabelIntersectionVertex() {
		return labelIntersectionVertex;
	}

	/**
	 * @param labelIntersectionVertex
	 *            The labelIntersectionVertex to set.
	 */
	public void setLabelIntersectionVertex(boolean labelIntersectionVertex) {
		this.labelIntersectionVertex = labelIntersectionVertex;
	}

	/**
	 * @return Returns the x.
	 */
	public double getX() {
		return x;
	}

	/**
	 * @param x
	 *            The x to set.
	 */
	public void setX(double x) {
		this.x = x;
	}

	/**
	 * @return Returns the y.
	 */
	public double getY() {
		return y;
	}

	/**
	 * @param y
	 *            The y to set.
	 */
	public void setY(double y) {
		this.y = y;
	}

	/**
	 * @return Returns the dummy.
	 */
	public boolean isDummy() {
		return dummy;
	}

	/**
	 * @param dummy
	 *            The dummy to set.
	 */
	public void setDummy(boolean dummy) {
		this.dummy = dummy;
	}

	/*
	 * computes the distance to vertex v in the maximum norm
	 */
	public double distance(MetroVertex other) {
		return Math.max(Math.abs(this.x - other.getX()), Math.abs(this.y - other.getY()));
	}

	/*
	 * called whenever a new edge involving this vertex is added adds the new edge in the right order to the list of incident edges
	 */
	public boolean addEdgeToList(Edge e) {
		if (e.isIncident(this)) {
			/* debug */
			if (name.equalsIgnoreCase("woodford"))
				System.out.println("trying to insert: " + this.name + "<>" + ((MetroVertex) e.getOpposite(this)).getName());
			MetroVertex opposite = (MetroVertex) e.getOpposite(this);
			CircularOrderComparator myCOC = new CircularOrderComparator(this);
			ListIterator it = cyclicEdgeList.listIterator();
			if (it.hasNext()) { // list not empty
				while (it.hasNext()) {
					MetroVertex mv = (MetroVertex) ((Edge) it.next()).getOpposite(this);
					// is the next element larger than e? then insert e before it.
					if ((myCOC.compare(opposite, mv) < 0)) {
						it.previous();// need to go one step backward
						it.add(e);
						break; // can abort the while loop
					}
				}
				if (!it.hasNext()) { // e must be the last edge in the list
					it.add(e);
				}
			} else { // list is empty
				cyclicEdgeList.addFirst(e);
			}
			return true;
		} else {
			return false;
		}
	}

	/*
	 * needed when an edge incident to this vertex is replaced by a new edge due to contraction it guarantees that the circular order remains as originally
	 */
	public boolean replaceEdgeInList(Edge oldEdge, Edge newEdge) {
		if (cyclicEdgeList.contains(oldEdge) && cyclicEdgeList.contains(newEdge)) {
			int oldIndex = cyclicEdgeList.indexOf(oldEdge);
			int newIndex = cyclicEdgeList.indexOf(newEdge);
			cyclicEdgeList.remove(oldEdge);
			cyclicEdgeList.add(oldIndex, newEdge); // immediately fill the slot of the old edge
			cyclicEdgeList.remove(newIndex); // thereafter remove the other occurance of the new edge
			return true;
		} else {
			return false;
		}
	}

	public Edge getNextEdgeFromList(Edge e) {
		if (cyclicEdgeList.contains(e)) {
			int listSize = cyclicEdgeList.size();
			int index = cyclicEdgeList.indexOf(e);
			if (index + 1 < listSize) {
				return (Edge) cyclicEdgeList.get(index + 1);
			} else {
				return (Edge) cyclicEdgeList.getFirst();
			}
		} else {
			return null;
		}
	}

	public Edge getPreviousEdgeFromList(Edge e) {
		if (cyclicEdgeList.contains(e)) {
			int listSize = cyclicEdgeList.size();
			int index = cyclicEdgeList.indexOf(e);
			if (index - 1 >= 0) {
				return (Edge) cyclicEdgeList.get(index - 1);
			} else {
				return (Edge) cyclicEdgeList.getLast();
			}
		} else {
			return null;
		}
	}

	public List getOrderedIncidentEdges() {
		return Collections.unmodifiableList(cyclicEdgeList);
	}

	public List getOrderedNeighbors() {
		List myList = new LinkedList();
		ListIterator it = cyclicEdgeList.listIterator();
		while (it.hasNext()) {
			Edge e = (Edge) it.next();
			try {
				myList.add((MetroVertex) e.getOpposite(this));
			} catch (IllegalArgumentException iae) {
				iae.printStackTrace();
				System.out.println("edge is " + ((MetroVertex) e.getEndpoints().getFirst()).getName() + "<>" + ((MetroVertex) e.getEndpoints().getSecond()).getName());
				System.out.println("active vertex is " + name);
				System.out.println("grpah of edge is " + e.getGraph());
				System.out.println("grpah of vertex is " + this.getGraph());
			}
		}
		return Collections.unmodifiableList(myList);
	}

	public int getIndexOfEdge(Edge e) {
		return cyclicEdgeList.indexOf(e);
	}

	/*
	 * the edge will be placed in the list in front of the element currently at list[index]
	 */
	public boolean setEdgeIndex(Edge e, int index) {
		if (cyclicEdgeList.contains(e)) {
			if (name.equalsIgnoreCase("woodford"))
				System.out.println("trying to change index: " + this.name + "<>" + ((MetroVertex) e.getOpposite(this)).getName());
			int oldIndex = cyclicEdgeList.indexOf(e);
			Object myDummyObj = new Object();
			cyclicEdgeList.add(index, myDummyObj);
			cyclicEdgeList.remove(e);
			int newPos = cyclicEdgeList.indexOf(myDummyObj);
			cyclicEdgeList.set(newPos, e);
			// if (name.equalsIgnoreCase("woodford")) System.out.println("changing index removed: " + this.name + "<>" + ((MetroVertex)f.getOpposite(this)).getName());

			return true;
		} else {
			return false;
		}
	}

	public boolean removeEdgeFromList(Edge e) {
		if (cyclicEdgeList.contains(e)) {
			if (name.equalsIgnoreCase("woodford"))
				System.out.println("trying to remove: " + this.name + "<>" + ((MetroVertex) e.getOpposite(this)).getName());
			while (cyclicEdgeList.contains(e)) {// just in case we have multiple occurances
				cyclicEdgeList.remove(e);
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean isTerminus() {
		return terminus;
	}

	public void setTerminus(boolean terminus) {
		this.terminus = terminus;
	}

	public boolean isJoint() {
		return joint;
	}

	public void setJoint(boolean joint) {
		this.joint = joint;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}
	
	public double getPerpendicularAngle(MetroPath path) {
		double angleSum = 0;
		int angleSumAmount = 0;
		for(Object o : this.getIncidentEdges()) {
			MetroEdge e = (MetroEdge) o;
			
			int i = 0;
			for(Boolean b : (Boolean[]) e.getUserDatum("lineArray")) {
				if(b == true && path.getName().equals("l" + i)) {
					MetroVertex first = e.getFirst();
					MetroVertex second = e.getSecond();
					
					if(second.equals(this)) {
						// turn around
						MetroVertex tempFirst = first;
						first = second;
						second = tempFirst;
					}
					double angle = Math.atan2(first.getY() - second.getY(), first.getX() - second.getX());
					angleSum += angle;
					angleSumAmount++;
				}
				i++;
			}
			
		}
		double degreeOneAdjustment = 0.0;
		if(this.degree() == 1) {
			degreeOneAdjustment = Math.PI * 0.5;
		}
		return (angleSum / (double)angleSumAmount) + degreeOneAdjustment;
	}

}
