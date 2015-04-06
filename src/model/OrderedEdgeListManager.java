package model;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.event.GraphEvent;
import edu.uci.ics.jung.graph.event.GraphEventListener;

public class OrderedEdgeListManager implements GraphEventListener {

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uci.ics.jung.graph.event.GraphEventListener#vertexAdded(edu.uci.ics.jung.graph.event.GraphEvent)
	 */
	public void vertexAdded(GraphEvent arg0) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uci.ics.jung.graph.event.GraphEventListener#vertexRemoved(edu.uci.ics.jung.graph.event.GraphEvent)
	 */
	public void vertexRemoved(GraphEvent arg0) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uci.ics.jung.graph.event.GraphEventListener#edgeAdded(edu.uci.ics.jung.graph.event.GraphEvent)
	 */
	public void edgeAdded(GraphEvent arg0) {

		Graph g = (Graph) arg0.getGraph();
		MetroEdge e = (MetroEdge) arg0.getGraphElement();
		// if (!e.isLabelConnectDummy()) {//label edges do not play a role here
		MetroVertex first = (MetroVertex) e.getEndpoints().getFirst();
		MetroVertex second = (MetroVertex) e.getEndpoints().getSecond();
		first.addEdgeToList(e);
		second.addEdgeToList(e);
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uci.ics.jung.graph.event.GraphEventListener#edgeRemoved(edu.uci.ics.jung.graph.event.GraphEvent)
	 */
	public void edgeRemoved(GraphEvent arg0) {

		Graph g = (Graph) arg0.getGraph();
		MetroEdge e = (MetroEdge) arg0.getGraphElement();
		// if (!e.isLabelConnectDummy()) {//label edges do not play a role here
		MetroVertex first = (MetroVertex) e.getEndpoints().getFirst();
		MetroVertex second = (MetroVertex) e.getEndpoints().getSecond();
		first.removeEdgeFromList(e);
		second.removeEdgeFromList(e);
		// }
	}

}
