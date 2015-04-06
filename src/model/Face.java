package model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.uci.ics.jung.graph.Edge;

public class Face {

	private LinkedList edgeList;

	/**
	 * 
	 */
	public Face() {
		edgeList = new LinkedList();
	}

	public void appendEdge(Edge e) {
		edgeList.add(e);
	}

	public boolean contains(Edge e) {
		return edgeList.contains(e);
	}

	public Edge getNext(Edge e) {
		if (edgeList.contains(e)) {
			int listSize = edgeList.size();
			int index = edgeList.indexOf(e);
			if (index + 1 < listSize) {
				return (Edge) edgeList.get(index + 1);
			} else {
				return (Edge) edgeList.getFirst();
			}
		} else {
			return null;
		}
	}

	public Edge getPrevious(Edge e) {
		if (edgeList.contains(e)) {
			int listSize = edgeList.size();
			int index = edgeList.indexOf(e);
			if (index - 1 >= 0) {
				return (Edge) edgeList.get(index - 1);
			} else {
				return (Edge) edgeList.getLast();
			}
		} else {
			return null;
		}
	}

	public List getEdgeList() {
		return Collections.unmodifiableList(edgeList);
	}

	public List getEdgeListFrom(Edge e) {
		if (edgeList.contains(e)) {
			int index = edgeList.indexOf(e);
			return Collections.unmodifiableList(edgeList.subList(index, edgeList.size()));
		} else {
			return null;
		}
	}

}
