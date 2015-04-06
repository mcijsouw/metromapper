package model;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import mip.GraphTools;
import app.Settings;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.UserData;

public class TransferGraph {
	
	public static Graph convert(Graph g) 
	{
		Graph tg = new UndirectedSparseGraph();
		tg.addUserDatum("isTransferGraph", true, UserData.SHARED);
		Set metroLines = GraphTools.getMetroLines(g);
		
		
		// Line colors
		tg.addUserDatum("colors", g.getUserDatum("colors"), UserData.SHARED);
		Vector lineNames = (Vector) g.getUserDatum("lines");
		tg.addUserDatum("lines", lineNames, UserData.SHARED);
		
		//      original-vtx      corresp.path   transfer-vertex
		HashMap<MetroVertex, HashMap<MetroPath, MetroVertex>> transferVertices = new HashMap<MetroVertex, HashMap<MetroPath, MetroVertex>>();

		

		double orgYRangeMin = Double.POSITIVE_INFINITY;
		double orgYRangeMax = Double.NEGATIVE_INFINITY;
		double orgXRangeMin = Double.POSITIVE_INFINITY;
		double orgXRangeMax = Double.NEGATIVE_INFINITY;

		for (Object v : g.getVertices()) {
			MetroVertex mv = (MetroVertex) v;
			orgYRangeMin = Math.min(orgYRangeMin, mv.getY());
			orgYRangeMax = Math.max(orgYRangeMax, mv.getY());
			orgXRangeMin = Math.min(orgXRangeMin, mv.getX());
			orgXRangeMax = Math.max(orgXRangeMax, mv.getX());
		}

		double scaleMultiplier = (double) (orgYRangeMax - orgYRangeMin) / 300;
		
		
		
		// Put all vertices in the HashMap transferVertices
		for (Object pathObject : metroLines) {
			MetroPath path = (MetroPath) pathObject;
			
			for(Object edgeObject : path) {
				MetroEdge edge = (MetroEdge) edgeObject;
				HashMap<MetroPath, MetroVertex> pathVertexPair;
				MetroVertex tvFirst;
				MetroVertex tvSecond;
				
				// First
				MetroVertex first = edge.getFirst();
				pathVertexPair = transferVertices.get(first);
				if(pathVertexPair == null) {
					pathVertexPair = new HashMap<MetroPath,MetroVertex>();
					transferVertices.put(first, pathVertexPair);
				}
				tvFirst = pathVertexPair.get(path);
				if(tvFirst == null) {
					tvFirst = new MetroVertex(first.getX(), first.getY());
					tvFirst.setName(first.getName() + " (" + (pathVertexPair.size() + 1) + ")");
					tvFirst = (MetroVertex) tg.addVertex(tvFirst);
					tvFirst.setOriginalVertex(first);
					pathVertexPair.put(path, tvFirst);
				}
				//System.out.println("added vertex " + tvFirst + " (" + tvFirst.getName() + ")");
				
				// Second
				MetroVertex second = edge.getSecond();
				pathVertexPair = transferVertices.get(second);
				if(pathVertexPair == null) {
					pathVertexPair = new HashMap<MetroPath,MetroVertex>();
					transferVertices.put(second, pathVertexPair);
				}
				tvSecond = pathVertexPair.get(path);
				if(tvSecond == null) {
					tvSecond = new MetroVertex(second.getX(), second.getY());
					tvSecond.setName(second.getName() + " (" + (pathVertexPair.size() + 1) + ")");
					tvSecond = (MetroVertex) tg.addVertex(tvSecond);
					tvSecond.setOriginalVertex(second);
					pathVertexPair.put(path, tvSecond);
				}
				//System.out.println("added vertex " + tvSecond + " (" + tvSecond.getName() + ")");
				
				// Edge
				TransferGraphEdge tge = new TransferGraphEdge(tvFirst, tvSecond);
				tge.setTime(edge.getTime(path.getName()));
				tge.setOriginalEdge(edge);
				tge = (TransferGraphEdge) tg.addEdge(tge);
				edge.addTransferGraphEdge(path.getName(), tge);
				//System.out.println("added edge " + tge);
				
				// Line array
				tge.addUserDatum("lineArray", new Boolean[lineNames.size()], UserData.SHARED);
				Boolean[] lines = (Boolean[]) tge.getUserDatum("lineArray");
				for (int i = 0; i < lines.length; i++) {
					lines[i] = Boolean.FALSE;
				}
				lines[lineNames.indexOf(path.getName())] = Boolean.TRUE;
				
			}
		}
		
		// Spread out vertices that lie on top of each other in a circle
		for(Entry<MetroVertex, HashMap<MetroPath, MetroVertex>> orgVertexEntry : transferVertices.entrySet()) {
			HashMap<MetroPath, MetroVertex> map = orgVertexEntry.getValue();
			int i = 0;
			orgVertexEntry.getKey().setTransferVertexMap(map);
			for(Entry<MetroPath, MetroVertex> tv : map.entrySet()) {
				MetroVertex v = tv.getValue();
				double angle = Math.PI * 2.0 / (double)map.size() * i;
				double radius = 1.0 * scaleMultiplier;
				if(map.size() == 1) {
					radius = 0;
				}
				v.setX(v.getX() + radius * Math.cos(angle));
				v.setY(v.getY() + radius * Math.sin(angle));
				i++;
			}
		}
		
		// Make a complete graph of every group of vertices that belong to one station
		//        original-vtx      corresp.path   transfer-vertex
		//HashMap<MetroVertex, HashMap<MetroPath, MetroVertex>> transferVertices
		
		for(Entry<MetroVertex, HashMap<MetroPath, MetroVertex>> orgVertexEntry : transferVertices.entrySet()) {
			
			HashMap<MetroPath, MetroVertex> map = orgVertexEntry.getValue();
			
			for(Entry<MetroPath,MetroVertex> tv1 : map.entrySet()) {
				MetroVertex v1 = tv1.getValue();
				boolean equalFound = false;
				for(Entry<MetroPath,MetroVertex> tv2 : map.entrySet()) {
					MetroVertex v2 = tv2.getValue();
					
					// Break on equal pair to ensure that only pairs (v1,v2) are made for which v1 < v2  
					if(v1.equals(v2)) { 
						break;
					}

					TransferGraphEdge tge = new TransferGraphEdge(v1, v2);
					tge.setDijkstraCount(0);
					tge.setTime(Settings.defaultTransferTimeInSeconds);
					tge = (TransferGraphEdge) tg.addEdge(tge);
					
					
					// Add to line 0
					tge.addUserDatum("lineArray", new Boolean[lineNames.size()], UserData.SHARED);
					Boolean[] lines = (Boolean[]) tge.getUserDatum("lineArray");
					for (int i = 0; i < lines.length; i++) {
						lines[i] = Boolean.FALSE;
					}
					lines[0] = Boolean.TRUE;
						
				}
			}
		}
		
		return tg;
	}
	
	public void computeDijkstraCounts()
	{
		
	}
	
}
