package app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import model.MetroEdge;
import model.MetroPath;
import model.MetroVertex;
import model.TransferGraphEdge;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;

public class Dijkstra {

	//                    source,              destination, edge-list
	public static HashMap<MetroVertex, HashMap<MetroVertex, List>> shortestPaths; 
	
	/**
	 * Compute paths of underlying transfer graph, and increase the Dijkstra counts in the normal graph.
	 */
	public static void computeShortestPaths(Graph g) {
		
		Graph tg = (Graph) g.getUserDatum("transferGraph");
		
		shortestPaths = new HashMap<MetroVertex, HashMap<MetroVertex, List>>();
		
		HashMap<TransferGraphEdge, Integer> directions = new HashMap<TransferGraphEdge, Integer>();
		ArrayList<TransferGraphEdge> oneWayLines = new ArrayList<TransferGraphEdge>();
		ArrayList<TransferGraphEdge> twoWayLines = new ArrayList<TransferGraphEdge>();
				
		final NumberEdgeValue nev = new NumberEdgeValue() {
	
				@Override
				public Number getNumber(ArchetypeEdge e) {
					TransferGraphEdge edge = (TransferGraphEdge) e;
					return edge.getTime();
				}
	
				@Override
				public void setNumber(ArchetypeEdge e, Number n) {
					// Unused
					//TransferGraphEdge edge = (TransferGraphEdge) e;
					//edge.setTime((Integer) n);
				}
			};
		
		boolean sourceOccurs = false;
		if(Settings.sourceStation instanceof MetroVertex) {
			
			// Occur check
			for(Object oc : g.getVertices()) {
				MetroVertex voc = (MetroVertex) oc;
				if(voc.equals(Settings.sourceStation)) {
					sourceOccurs = true;
					break;
				}
			}
			if(!sourceOccurs) {
				System.err.println("Vertex " + Settings.sourceStation + " is not part of graph " + g);
			}
			
		}
		
		// reset all dijkstra counts
		for(Object e : tg.getEdges()) {
			TransferGraphEdge edge = (TransferGraphEdge) e;
			edge.setDijkstraCount(0);
			edge.setAllPairsDijkstraCount(0);
			if(edge.getOriginalEdge() != null) {
				edge.getOriginalEdge().setArrowDirection(0);
			}
		}
		
		for(Object o1 : g.getVertices()) {
			MetroVertex v1 = (MetroVertex) o1;
			
			shortestPaths.put(v1, new HashMap<MetroVertex, List>());
			
			if(Settings.renderTransferGraph) {
				v1 = v1.getOriginalVertex();
			}
			
			boolean v1isSourceStation = (sourceOccurs && v1.equals(Settings.sourceStation));
			
			DijkstraShortestPath dsp = new DijkstraShortestPath(tg, nev, false);
			for(Object o2 : g.getVertices()) {
				MetroVertex v2 = (MetroVertex) o2;
				if(v1.equals(v2)) {
					continue;
				}
				//System.out.println(v1.getName() + " ... " + v2.getName());
				
				MetroVertex shortestPathFirstVertex = null;
				
				// Compute best path between two "normal" vertices, consider all transfer-vertices and keep the shortest route
				List shortestPath = null; 
				int shortestPathTime = Integer.MAX_VALUE;
				for(Entry<MetroPath, MetroVertex> entry1 : v1.getTransferVertexMap().entrySet()) {
					MetroVertex tv1 = entry1.getValue();
					for(Entry<MetroPath, MetroVertex> entry2 : v2.getTransferVertexMap().entrySet()) {
						MetroVertex tv2 = entry2.getValue();
						int totalTime = 0;
						List currentPath = dsp.getPath(tv1, tv2);
						for(Object intermediate : currentPath) {
							TransferGraphEdge intermediateEdge = (TransferGraphEdge) intermediate;
							totalTime += intermediateEdge.getTime();
						}
						if(totalTime < shortestPathTime) {
							// This route was faster than all previous routes, store it
							shortestPathTime = totalTime;
							shortestPath = currentPath;
							shortestPathFirstVertex = tv1;
							//System.out.println("fastest route updated: " + totalTime + "s");
						}
					}					
				}
				
				if(shortestPath != null && shortestPathFirstVertex != null) {
					
					// Store shortest path to destination v2 in hashmap
					shortestPaths.get(v1).put(v2, shortestPath);
					
					// Holds the last vertex seen in the path
					MetroVertex prevSecond = shortestPathFirstVertex; 

					for(Object intermediate : shortestPath) {
						TransferGraphEdge intermediateEdge = (TransferGraphEdge) intermediate;

						// Increase counts
						intermediateEdge.increaseAllPairsDijkstraCount();
						if(v1isSourceStation) {
							intermediateEdge.increaseDijkstraCount();
							
							int dir;
							if(intermediateEdge.getFirst().equals(prevSecond)) {
								prevSecond = intermediateEdge.getSecond();
								dir = 1; // normal edge direction... first -> second
							} else {
								prevSecond = intermediateEdge.getFirst();
								dir = -1; // reversed edge direction... second -> first
							}
							
							// Check if direction corresponds with all previously seen directions
							Integer entry = directions.get(intermediateEdge);
							if(entry == null) {
								directions.put(intermediateEdge, dir);
								entry = dir;
							}
							
							if(entry != dir) {
								// Found entry in "directions" is different from the direction we just determined
								twoWayLines.add(intermediateEdge);
							} else {
								oneWayLines.add(intermediateEdge);
							}
						}
					}		
				}
			}
		}
		
		// Mark one-way-lines with an arrow
		for(TransferGraphEdge e : oneWayLines) {
			if(e.getOriginalEdge() != null) {
				e.getOriginalEdge().setArrowDirection(directions.get(e));
			}
		}
		
	}
	
}
