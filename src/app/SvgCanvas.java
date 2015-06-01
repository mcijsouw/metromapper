package app;

import io.CSVCountReader;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JPanel;

import mip.GraphTools;
import model.MetroEdge;
import model.MetroPath;
import model.MetroVertex;
import model.Point;
import model.TransferGraphEdge;
import edu.uci.ics.jung.graph.Graph;

public class SvgCanvas extends JPanel implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

	// 1 pixel accounts for this amount of distance in latitude or longitude
	private int zoomLevel = 5;
	private double[] zoomMultipliers = new double[] { 1, 1.2, 1.4, 1.6, 1.8, 2, 2.2, 2.4, 2.6, 2.8, 3.0, 3.2, 3.4, 3.6, 3.8, 4, 4.5, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 24, 28, 32 };
	private double computedZoomLevel = zoomMultipliers[zoomLevel];
	private double scaleMultiplier = 1;

	private double orgYCenter;
	private double orgXCenter;
	private double orgZoom = 1.0;

	private double yCenter;
	private double xCenter;
	private double zoom;

	private int dragStartX = -1;
	private int dragStartY = -1;
	private int dragOffsetX = 0;
	private int dragOffsetY = 0;
	private int dragOffsetXBeforeDragging = 0;
	private int dragOffsetYBeforeDragging = 0;

	private Cursor openHand;
	private Cursor closedHand;

	private Graph g;
	private CSVCountReader countReader;
	private MetroMapper app;

	private boolean shiftButtonIsDown = false;
	
	private ArrayList<String> inverseSlotsDirtyList = new ArrayList<String>();

	HashMap<MetroEdge, HashMap<Integer, MetroPath>> slotLists;

	public SvgCanvas(MetroMapper app) {
		this.app = app;

		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		openHand = toolkit.createCustomCursor(toolkit.createImage("resources\\img\\cursor-open-hand.png"), new java.awt.Point(8, 6), "openHand");
		closedHand = toolkit.createCustomCursor(toolkit.createImage("resources\\img\\cursor-closed-hand.png"), new java.awt.Point(8, 6), "closedHand");
		this.setCursor(openHand);
		this.countReader = new CSVCountReader();
		
		// Inverse slot sequence for these stations
		this.inverseSlotsDirtyList.add("Central");
		this.inverseSlotsDirtyList.add("Town Hall");
		this.inverseSlotsDirtyList.add("Circular Quay");
		this.inverseSlotsDirtyList.add("d0");
		this.inverseSlotsDirtyList.add("St James");
		this.inverseSlotsDirtyList.add("Museum");
		this.inverseSlotsDirtyList.add("Erskineville");
		
	}

	public void initializeView() {

		if (this.g == null) {
			return;
		}

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

		orgYCenter = orgYRangeMin + (orgYRangeMax - orgYRangeMin) / 2.0;
		orgXCenter = orgXRangeMin + (orgXRangeMax - orgXRangeMin) / 2.0;
		yCenter = orgYCenter;
		xCenter = orgXCenter;

		// System.out.println("Centering view on (" + xCenter + ", " + yCenter + ")");

		scaleMultiplier = (double) 500 / (orgYRangeMax - orgYRangeMin);

		dragStartX = -1;
		dragStartY = -1;
		dragOffsetX = 0;
		dragOffsetY = 0;
		dragOffsetXBeforeDragging = 0;
		dragOffsetYBeforeDragging = 0;
		zoomLevel = 3;
		computedZoomLevel = zoomMultipliers[zoomLevel];

	}
	
	public double getScaleMultiplier() {
		return scaleMultiplier;
	}

	public ViewPosition getViewPositionObject() {
		return new ViewPosition(this.zoomLevel, this.dragOffsetX, this.dragOffsetY);
	}

	public void setViewPosition(ViewPosition vp) {
		this.zoomLevel = vp.zoomLevel;
		this.dragOffsetX = vp.dragOffsetX;
		this.dragOffsetY = vp.dragOffsetY;
	}

	public void addNotify() {
		super.addNotify();
		requestFocus();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		Dimension d = this.getSize();
		int oldZoomLevel = zoomLevel;

		if (e.getPreciseWheelRotation() > 0) {
			zoomLevel = Math.max(0, zoomLevel - 1);
		} else {
			zoomLevel = Math.min(zoomMultipliers.length - 1, zoomLevel + 1);
		}

		double shrinkAmount = zoomMultipliers[zoomLevel] / zoomMultipliers[oldZoomLevel];
		dragOffsetX = (int) ((d.getWidth() / 2 - e.getX()) * (shrinkAmount - 1) + shrinkAmount * dragOffsetX);
		dragOffsetY = (int) ((d.getHeight() / 2 - e.getY()) * (shrinkAmount - 1) + shrinkAmount * dragOffsetY);

		if (zoomLevel != oldZoomLevel) {
			computedZoomLevel = zoomMultipliers[zoomLevel];
			this.repaint();
		}
		e.consume();
	}

	public void renderGraph(Graph myGraph) {
		this.g = myGraph;
		this.countReader.applyToGraph(g);
		initializeView();
		repaint();
	}

	public double getImageX(Point p) {
		return getImageX(p.getX());
	}

	public double getImageX(MetroVertex mv) {
		return getImageX(mv.getX());
	}

	public double getImageX(double x) {
		Dimension d = this.getSize();
		return (d.getWidth() / 2) + (x - xCenter) * scaleMultiplier * zoomMultipliers[zoomLevel] + dragOffsetX;
	}

	public double getImageY(Point p) {
		return getImageY(p.getY());
	}

	public double getImageY(MetroVertex mv) {
		return getImageY(mv.getY());
	}

	public double getImageY(double y) {
		Dimension d = this.getSize();
		return (d.getHeight() / 2) - (y - yCenter) * scaleMultiplier * zoomMultipliers[zoomLevel] + dragOffsetY;
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHints(rh);
		super.paintComponent(g2d);
		if (this.g instanceof Graph) {
			try {
				if (Settings.renderTransferGraph) {
					drawGraphToG2D(g2d, (Graph) this.g.getUserDatum("transferGraph"));
				} else {
					drawGraphToG2D(g2d, this.g);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private double getPassengerCountThickness(MetroEdge edge, MetroPath path) {
		Integer[] count1 = edge.getFirst().getPassengerCount();
		Integer[] count2 = edge.getSecond().getPassengerCount();
		if (count1[Settings.passengerCountTime] != null && count2[Settings.passengerCountTime] != null) {
			int avg = (count1[Settings.passengerCountTime] + count2[Settings.passengerCountTime]) / 2;
			return Math.atan(Math.pow(avg / 300.0, 3)); // passenger count curve
		}
		return 0;
	}

	private double getDijkstraCountThickness(MetroEdge edge, MetroPath path) {
		// @todo remove debug r
		// double r = edge.getDijkstraCount() / 500.0 - 0.5; // dijkstra count curve
		// double r = Math.atan(Math.pow(edge.getDijkstraCount(path.getName()) / 5.0, 2)) * 5; // dijkstra count curve

		double count;
		int diff;
		int c1;
		if (Settings.shortestPathsAlgorithm == Settings.SHORTEST_PATHS_SINGLE_SOURCE) {
			diff = Dijkstra.maxSingleSourceCount - Dijkstra.minSingleSourceCount;
			count = edge.getDijkstraCount(path.getName());
			c1 = 3;
		} else {
			diff = Dijkstra.maxAllPairsCount - Dijkstra.minAllPairsCount;
			count = edge.getAllPairsDijkstraCount(path.getName());
			c1 = 1;
		}

		double baseThickness = 1.0;

		if (Settings.fixedLineThicknesses == true) {
			return 2.5;
		}

		// System.out.println("count: " + count);
		// System.out.println(Math.atan(count / diff));
		double r = baseThickness + Math.atan(Math.pow(count, 2) * c1 / Math.pow(diff, 2)) * 10; // dijkstra count curve

		return r;
	}

	private Point computePoint(MetroPath path, MetroEdge edge, MetroVertex mv, Integer slot, boolean debug) {

		HashMap<Integer, MetroPath> slotlist = getSlotListForEdge(edge);
		double angle;
		if(Settings.perpendicularAngleByPath) {
			angle = mv.getPerpendicularAngleByPath(path);
		} else {
			angle = mv.getPerpendicularAngle();
		}

		if (debug) {
			System.out.println("DEBUG: slotlist for edge " + edge + " and vertex " + mv + ": " + slotlist);
			System.out.println(slotLists);
		}

		double offset = 0.0;
		double totalEdgeOffset = 0.0;
		boolean pathFound = false;
		if (slotlist != null) {
			for (Entry<Integer, MetroPath> entry : slotlist.entrySet()) {
				int s = entry.getKey();
				MetroPath p = entry.getValue();
				double thickness = mv.getCombinedThickness(this, p);

				// System.out.println("thickness of line " + edge + " on vertex " + mv.getName() + " in path " + p + " (slot " + s + "): " + thickness);

				if (pathFound == false) {
					offset += thickness;
				}
				totalEdgeOffset += thickness;

				if (path.equals(p)) {
					pathFound = true;
					// subtract half of its own thickness
					offset -= thickness * 0.5;
				}
			}
			// in the end, subtract half of the total (combined) offset
			offset -= totalEdgeOffset * 0.5;
		}

		int dirtyFixConstant = 1;
		if(this.inverseSlotsDirtyList.contains(mv.getName())) {
			dirtyFixConstant = -1;
		}
		
		double x = mv.getX() + Math.cos(angle) * offset / scaleMultiplier * dirtyFixConstant;
		double y = mv.getY() + Math.sin(angle) * offset / scaleMultiplier * dirtyFixConstant;

		return new Point(x, y);
	}

	private void findFreeSlot(MetroPath path, MetroEdge edge) {

		HashMap<Integer, MetroPath> m = slotLists.get(edge);

		if (m == null) {
			HashMap<Integer, MetroPath> sub = new HashMap<Integer, MetroPath>();
			sub.put(new Integer(1), path);
			slotLists.put(edge, sub);
		} else {
			m.put((m.size() + 1), path);
			slotLists.put(edge, m);
		}
	}

	private Integer getSlot(MetroPath path, MetroEdge edge) {
		HashMap<Integer, MetroPath> m = slotLists.get(edge);
		if (m != null) {
			// Get key by value
			for (Entry<Integer, MetroPath> entry : m.entrySet()) {
				if (entry.getValue().equals(path)) {
					return entry.getKey();
				}
			}
		}
		return -1;
	}

	private HashMap<Integer, MetroPath> getSlotListForEdge(MetroEdge edge) {
		return slotLists.get(edge);
	}

	public double getLineThickness(MetroPath path, MetroEdge edge) {
		return this.getLineThickness(path, edge, false);
	}

	public double getLineThickness(MetroPath path, MetroEdge edge, boolean debug) {
		if (Settings.lineThicknessAlgorithm == Settings.LINE_THICKNESS_PASSENGER_COUNT) {
			return getPassengerCountThickness(edge, path);
		} else if (Settings.lineThicknessAlgorithm == Settings.LINE_THICKNESS_DIJKSTRA_PATH_COUNT) {
			return getDijkstraCountThickness(edge, path);
		}
		return 0;
	}

	private void drawGraphToG2D(Graphics2D g2d, Graph g) {
		
		int timeTotal = 0;
		int timeTotalCount = 0;

		double scale = zoomMultipliers[zoomLevel];
		boolean isTransferGraph = (g.getUserDatum("isTransferGraph") != null && g.getUserDatum("isTransferGraph").equals(true));
		List shortestPath = null;
		if (Settings.sourceStation instanceof MetroVertex && Settings.destinationStation instanceof MetroVertex) {
			if (Dijkstra.shortestPaths.containsKey(Settings.sourceStation)) {
				shortestPath = Dijkstra.shortestPaths.get(Settings.sourceStation).get(Settings.destinationStation);
			}
		}

		// Edges
		Color[] colors = (Color[]) ((Vector) g.getUserDatum("colors")).toArray(new Color[0]);
		slotLists = new HashMap<MetroEdge, HashMap<Integer, MetroPath>>(g.numEdges());

		// Preprocessing step: compute SLOTS first!
		for (Object pathObject : GraphTools.getMetroLines(g)) {
			MetroPath path = (MetroPath) pathObject;
			MetroEdge previousEdge = null;
			for (Object edgeObject : path) {
				MetroEdge edge = (MetroEdge) edgeObject;
				MetroVertex first = edge.getFirst();
				MetroVertex second = edge.getSecond();
				// turn around if necessary
				if (previousEdge != null && second.isIncident(previousEdge)) {
					// System.out.println("turned around");
					MetroVertex secondTemp = second;
					second = first;
					first = secondTemp;
				}

				findFreeSlot(path, edge);

				previousEdge = edge;
			}
		}
		// End preprocessing step

		// System.out.println("preprocessing done");

		for (Object pathObject : GraphTools.getMetroLines(g)) {
			Path2D path2d = null;
			MetroPath path = (MetroPath) pathObject;

			// System.out.println("--- path " + path + " ---");

			MetroEdge previousEdge = null;
			Point previousFrom = null;
			Point previousTo = null;

			// START MULTIEDGE CODE
			for (Object edgeObject : path) {
				path2d = new Path2D.Float();
				MetroEdge edge = (MetroEdge) edgeObject;
				TransferGraphEdge tge = null;
				if (isTransferGraph) {
					tge = (TransferGraphEdge) edge;
				}

				int dijkstraCount = 0;
				if (isTransferGraph) {
					dijkstraCount = tge.getDijkstraCount();
				} else {
					dijkstraCount = (int) edge.getDijkstraCount(path.getName());
				}

				// Check if this edge is contained in the source~destination shortest path
				boolean inShortestPath = false;
				if (this.shiftButtonIsDown) {
					if (shortestPath != null) {
						if (isTransferGraph) {
							if (shortestPath.contains(tge)) {
								Boolean[] lines = (Boolean[]) tge.getUserDatum("lineArray");
								for (int i = 0; i < lines.length; i++) {
									if (lines[i].booleanValue() && path.getName().equals("l" + i)) {
										inShortestPath = true;
										break;
									}
								}
								break;
							}
						} else {
							for (TransferGraphEdge t : edge.getTransferGraphEdges().values()) {
								if (shortestPath.contains(t)) {
									Boolean[] lines = (Boolean[]) t.getUserDatum("lineArray");
									for (int i = 0; i < lines.length; i++) {
										if (lines[i].booleanValue() && path.getName().equals("l" + i)) {
											inShortestPath = true;
											break;
										}
									}
									break;
								}
							}
						}
					}
				}

				// Edge color
				Color c = colors[path.getColor()];
				float red = (float) ((double) c.getRed() / 255.0);
				float green = (float) ((double) c.getGreen() / 255.0);
				float blue = (float) ((double) c.getBlue() / 255.0);
				// float alpha = (float) Math.max(0.2, Math.min((dijkstraCount + 1) * 0.2, 1.0));
				float alpha = 1f;
				if (shiftButtonIsDown == true && inShortestPath == false) {
					alpha = 0.15f;
				}
				g2d.setColor(new Color(red, green, blue, alpha));

				// line thickness
				double thickness = getLineThickness(path, edge);

				// Compute average edge time
				timeTotal += edge.getTime(path.getName());
				timeTotalCount++;
				
				MetroVertex first = edge.getFirst();
				MetroVertex second = edge.getSecond();

				// System.out.println("Dijkstra count " + dijkstraCount + " => opacity " + alpha);

				// turn around if necessary
				if (previousEdge != null && second.isIncident(previousEdge)) {
					// System.out.println("turned around");
					MetroVertex secondTemp = second;
					second = first;
					first = secondTemp;
				}

				
				double pAngleFirst;
				double pAngleSecond;
				if(Settings.perpendicularAngleByPath) {
					pAngleFirst = first.getPerpendicularAngleByPath(path);
					pAngleSecond = second.getPerpendicularAngleByPath(path);
				} else {
					pAngleFirst = first.getPerpendicularAngle();
					pAngleSecond = second.getPerpendicularAngle();
				}
				

				Point p1org = new Point(first.getX(), first.getY());
				Point p2org = new Point(second.getX(), second.getY());

				Integer slot = getSlot(path, edge);

				Point p1 = computePoint(path, edge, first, slot, false);
				Point p2 = computePoint(path, edge, second, slot, false);

				double averageThicknessFirst = first.getCombinedThickness(this, path);
				double averageThicknessSecond = second.getCombinedThickness(this, path);

				// Draw polygon

				// Debug path, draw line from ORG to COMPUTED vertex
				/*
				 * Color tempColor = g2d.getColor(); g2d.setColor(Color.black); g2d.setStroke(new BasicStroke(2.0f)); Path2D debugPath = new Path2D.Double(); debugPath.moveTo(getImageX(p1org.getX()),
				 * getImageY(p1org.getY())); debugPath.lineTo(getImageX(p1.getX()), getImageY(p1.getY())); g2d.draw(debugPath);
				 * 
				 * debugPath = new Path2D.Double(); debugPath.moveTo(getImageX(p2org.getX()), getImageY(p2org.getY())); debugPath.lineTo(getImageX(p2.getX()), getImageY(p2.getY()));
				 * g2d.draw(debugPath);
				 * 
				 * g2d.setColor(tempColor);
				 * 
				 * this.drawTestCircle(g2d, p1); this.drawTestCircle(g2d, p2);
				 */
				// End debug path

				Line2D line1 = new Line2D.Double((int) (getImageX(p1) + (0.5 * averageThicknessFirst * scale * Math.cos(pAngleFirst))),
						(int) (getImageY(p1) + (0.5 * averageThicknessFirst * scale * -Math.sin(pAngleFirst))),
						(int) (getImageX(p2) + (0.5 * averageThicknessSecond * scale * Math.cos(pAngleSecond))),
						(int) (getImageY(p2) + (0.5 * averageThicknessSecond * scale * -Math.sin(pAngleSecond))));

				Line2D line2 = new Line2D.Double((int) (getImageX(p1) - (0.5 * averageThicknessFirst * scale * Math.cos(pAngleFirst))),
						(int) (getImageY(p1) - (0.5 * averageThicknessFirst * scale * -Math.sin(pAngleFirst))),
						(int) (getImageX(p2) - (0.5 * averageThicknessSecond * scale * Math.cos(pAngleSecond))),
						(int) (getImageY(p2) - (0.5 * averageThicknessSecond * scale * -Math.sin(pAngleSecond))));

				Polygon polygon = new Polygon();
				if (line1.intersectsLine(line2)) {
					polygon.addPoint((int) line1.getX1(), (int) line1.getY1()); // 1
					polygon.addPoint((int) line2.getX2(), (int) line2.getY2()); // 3
					polygon.addPoint((int) line1.getX2(), (int) line1.getY2()); // 2
					polygon.addPoint((int) line2.getX1(), (int) line2.getY1()); // 4
				} else {
					polygon.addPoint((int) line1.getX1(), (int) line1.getY1()); // 1
					polygon.addPoint((int) line1.getX2(), (int) line1.getY2()); // 2
					polygon.addPoint((int) line2.getX2(), (int) line2.getY2()); // 3
					polygon.addPoint((int) line2.getX1(), (int) line2.getY1()); // 4
				}

				if (Settings.drawPolygonWireframes == true) {

					g2d.setStroke(new BasicStroke((float) (0.25 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
					g2d.drawPolygon(polygon);

				} else if (dijkstraCount == 0 && Settings.shortestPathsAlgorithm == Settings.SHORTEST_PATHS_SINGLE_SOURCE && Settings.drawArrowHints == false) {

					// Dashed line

					float dash[] = { (float) (thickness * scale * 0.5), (float) (thickness * scale * 1.0) };
					g2d.setStroke(new BasicStroke((float) (thickness * scale), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0.0f));
					path2d.moveTo(getImageX(p1), getImageY(p1));
					path2d.lineTo(getImageX(p2), getImageY(p2));
					g2d.draw(path2d);

				} else if (Settings.fixedLineThicknesses) {

					g2d.setStroke(new BasicStroke((float) (thickness * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					path2d.moveTo(getImageX(p1), getImageY(p1));
					path2d.lineTo(getImageX(p2), getImageY(p2));
					g2d.draw(path2d);

				} else {

					g2d.setStroke(new BasicStroke(0f));
					g2d.fillPolygon(polygon);

				}
				// End of polygon drawing

				if (Settings.drawEdgeLabels) {
					// Put weight in the middle of the edge
					// g2d.setColor(new Color(0, 0, 0, 0.8f));
					g2d.setFont(new Font("Arial", Font.PLAIN, (int) (3 * scale)));
					double offset = 0.2;
					String dijkstraCountString = "";
					String directionString = "dir: ";
					if (isTransferGraph) {
						if (Settings.shortestPathsAlgorithm == Settings.SHORTEST_PATHS_SINGLE_SOURCE) {
							dijkstraCountString += "<" + String.valueOf(tge.getDijkstraCount()) + "," + String.valueOf(tge.getTime()) + ">";
						} else {
							dijkstraCountString += "<" + String.valueOf(tge.getAllPairsDijkstraCount()) + "," + String.valueOf(tge.getTime()) + ">";
						}
						directionString += tge.getOriginalEdge().getArrowDirection();
					} else {
						for (TransferGraphEdge tge2 : edge.getTransferGraphEdges().values()) {
							if (Settings.shortestPathsAlgorithm == Settings.SHORTEST_PATHS_SINGLE_SOURCE) {
								dijkstraCountString += "<" + String.valueOf(tge2.getDijkstraCount()) + "," + String.valueOf(tge2.getTime()) + ">";
							} else {
								dijkstraCountString += "<" + String.valueOf(tge2.getAllPairsDijkstraCount()) + "," + String.valueOf(tge2.getTime()) + ">";
							}
						}
						directionString += edge.getArrowDirection();
					}

					g2d.setColor(new Color(0, 0, 0, 0.5f));
					g2d.drawString(dijkstraCountString, (float) (((getImageX(first) + getImageX(second)) / 2.0) - (offset * scale)),
							(float) (((getImageY(first) + getImageY(second)) / 2.0) + (offset * scale)));
					// g2d.drawString(directionString, (float) (((getImageX(first) + getImageX(second)) / 2.0) - (offset*scale)), (float) (((getImageY(first) + getImageY(second)) / 2.0) +
					// (offset*scale)));
					// double angle = Math.atan2(Math.abs(first.getY() - second.getY()), Math.abs(first.getX() - second.getX()));
					// g2d.drawString(String.valueOf(angle), (float) (((getImageX(first) + getImageX(second)) / 2.0) - (offset*scale)), (float) (((getImageY(first) + getImageY(second)) / 2.0) +
					// (offset*scale)));

				}

				if (Settings.drawArrowHints && Settings.shortestPathsAlgorithm == Settings.SHORTEST_PATHS_SINGLE_SOURCE) {
					MetroEdge orgEdge = edge;
					if (isTransferGraph) {
						orgEdge = tge.getOriginalEdge();
					}

					int dir = orgEdge.getArrowDirection();

					MetroVertex drawFrom = null;
					if (dir < 0) {
						drawFrom = orgEdge.getSecond();
					} else if (dir > 0) {
						drawFrom = orgEdge.getFirst();
					}
					if (drawFrom != null) {

						double angle = Math.atan2(first.getY() - second.getY(), first.getX() - second.getX());
						double edgeLength = scale * scaleMultiplier * -dir * Math.sqrt(Math.pow(first.getY() - second.getY(), 2) + Math.pow(first.getX() - second.getX(), 2));
						double arrowLength = edgeLength * (Settings.arrowSizePercentage / 100.0);

						double arrowTipLength = arrowLength * 0.1;

						double arrowStartX = getImageX(drawFrom) + (edgeLength - arrowLength) * 0.5 * Math.cos(angle);
						double arrowStartY = getImageY(drawFrom) - (edgeLength - arrowLength) * 0.5 * Math.sin(angle);

						double arrowTipX = arrowStartX + arrowLength * Math.cos(angle);
						double arrowTipY = arrowStartY - arrowLength * Math.sin(angle);

						double arrowLeaf1X = arrowStartX + (arrowLength - arrowTipLength) * Math.cos(angle + 0.1);
						double arrowLeaf1Y = arrowStartY - (arrowLength - arrowTipLength) * Math.sin(angle + 0.1);

						double arrowLeaf2X = arrowStartX + (arrowLength - arrowTipLength) * Math.cos(angle - 0.1);
						double arrowLeaf2Y = arrowStartY - (arrowLength - arrowTipLength) * Math.sin(angle - 0.1);

						// Draw white arrow (border)
						g2d.setColor(new Color(0f, 0f, 0f, 1f));
						g2d.setStroke(new BasicStroke((float) ((0.7 + Settings.arrowSizePercentage * 0.005) * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						Path2D arrow = new Path2D.Float();
						arrow.moveTo(arrowStartX, arrowStartY);
						arrow.lineTo(arrowTipX, arrowTipY);
						arrow.lineTo(arrowLeaf1X, arrowLeaf1Y);
						arrow.moveTo(arrowTipX, arrowTipY);
						arrow.lineTo(arrowLeaf2X, arrowLeaf2Y);
						g2d.draw(arrow);

						// Draw black arrow
						g2d.setColor(Color.white);
						g2d.setStroke(new BasicStroke((float) ((0.4 + Settings.arrowSizePercentage * 0.005) * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						arrow = new Path2D.Float();
						arrow.moveTo(arrowStartX, arrowStartY);
						arrow.lineTo(arrowTipX, arrowTipY);
						arrow.lineTo(arrowLeaf1X, arrowLeaf1Y);
						arrow.moveTo(arrowTipX, arrowTipY);
						arrow.lineTo(arrowLeaf2X, arrowLeaf2Y);
						g2d.draw(arrow);
					}
				}

				previousEdge = edge;

			}

		}

		// Vertices
		double transferGraphScaling = 1;
		if (isTransferGraph) {
			transferGraphScaling = 0.5;
			for (Object vertexObject : g.getVertices()) {
				MetroVertex vertex = (MetroVertex) vertexObject;
				MetroVertex org = vertex.getOriginalVertex();
				if (org != null) {
					org.setOriginalVertexIsDrawn(false);
				}
			}
		}

		for (Object vertexObject : g.getVertices()) {

			MetroVertex vertex = (MetroVertex) vertexObject;

			double x = 0;
			double y = 0;

			if (!(vertex.isDummy() || vertex.isLabelVertex() || vertex.isJoint())) {

				if (isTransferGraph) {
					// Draw arching circle
					MetroVertex org = vertex.getOriginalVertex();
					if (org != null && org.getOriginalVertexIsDrawn() == false) {
						double stationRadius = 3.0 * scale;
						if (org.getTransferVertexMap().size() == 1) {
							stationRadius = 1.0 * scale;
						}
						x = getImageX(org) - stationRadius;
						y = getImageY(org) - stationRadius;
						Ellipse2D ellipse = new Ellipse2D.Float();
						ellipse.setFrame(x, y, stationRadius * 2, stationRadius * 2);
						g2d.setColor(new Color(0f, 0f, 0f, 0.2f));
						g2d.setStroke(new BasicStroke(0));
						g2d.fill(ellipse);
						g2d.draw(ellipse);
						org.setOriginalVertexIsDrawn(true);
					}
				}

				Ellipse2D ellipse;
				double stationRadius = 0;

				// DEBUG! REMOVE
				if (true) {
					if (!isTransferGraph && (vertex.degree() > 2 || vertex.isIntersection())) {
						// Draw circle
						stationRadius = 5 * scale * transferGraphScaling;
						x = getImageX(vertex) - stationRadius;
						y = getImageY(vertex) - stationRadius;
						ellipse = new Ellipse2D.Float();
						ellipse.setFrame(x, y, stationRadius * 2, stationRadius * 2);
						g2d.setColor(Color.white);
						g2d.fill(ellipse);
						g2d.setColor(Color.black);
						g2d.setStroke(new BasicStroke((float) (1.0 * scale * transferGraphScaling)));
						g2d.draw(ellipse);

					} else {
						// Draw line tick
						double angle = vertex.getPerpendicularAngle();
						double thickness = vertex.getCombinedPathThickness(this, this.getGraph());
						
						double width = (1.0 * scale * transferGraphScaling);
						double c = 0.33;
						if (!isTransferGraph && vertex.degree() == 1) {
							width = width * 2;
							c = 1.0;
						}
						
						double x1 = vertex.getX() + Math.cos(angle) * c * thickness / scaleMultiplier;
						double y1 = vertex.getY() + Math.sin(angle) * c * thickness / scaleMultiplier;
						double x2 = vertex.getX() - Math.cos(angle) * c * thickness / scaleMultiplier;
						double y2 = vertex.getY() - Math.sin(angle) * c * thickness / scaleMultiplier;
						Path2D tick = new Path2D.Double();
						x = getImageX(vertex);
						y = getImageY(vertex);
						tick.moveTo(getImageX(x1), getImageY(y1));
						tick.lineTo(getImageX(x2), getImageY(y2));
						g2d.setColor(Color.black);
						
						g2d.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						g2d.draw(tick);

					}

					double bigStationRadius = scale * 15;
					if (Settings.isGeneratingQuestion == false && vertex.equals(Settings.sourceStation)) {
						ellipse = new Ellipse2D.Float();
						ellipse.setFrame(x + stationRadius - bigStationRadius, y + stationRadius - bigStationRadius, bigStationRadius * 2, bigStationRadius * 2);
						g2d.setColor(new Color(1f, 0f, 0f, 0.2f));
						g2d.setStroke(new BasicStroke(0));
						g2d.fill(ellipse);
						g2d.draw(ellipse);
					}

					if (Settings.isGeneratingQuestion == false && vertex.equals(Settings.destinationStation)) {
						ellipse = new Ellipse2D.Float();
						ellipse.setFrame(x + stationRadius - bigStationRadius, y + stationRadius - bigStationRadius, bigStationRadius * 2, bigStationRadius * 2);
						g2d.setColor(new Color(0f, 1f, 0f, 0.2f));
						g2d.setStroke(new BasicStroke(0));
						g2d.fill(ellipse);
						g2d.draw(ellipse);
					}
				}

				// Draw vertex label
				if (Settings.drawVertexLabels) {
					g2d.setFont(new Font("Arial", Font.PLAIN, (int) (3 * scale)));
					g2d.setColor(new Color(0, 0, 0, 0.5f));
					g2d.drawString(vertex.getName(), (float) (getImageX(vertex) + (1.5 * scale)), (float) (getImageY(vertex) + (0.7 * scale)));
				}

				// System.out.println("Drew vertex on position (" + x + ", " + y + ")");

				// Name label
				// g2d.setColor(new Color(0, 0, 0, 0.5f));
				// g2d.setFont(new Font("Arial", Font.PLAIN, (int) (10.0 * scale)));
				// g2d.drawString(vertex.getName() + " (" + vertex.degree() + ")", (float) (x + 5*scale), (float) (y + 3*scale));

			}

		}
		//System.out.println("Average edge time: " + (timeTotal / timeTotalCount) + "s");
		// System.out.println("--- drawing done ---");
	}

	private void drawTestCircle(Graphics2D g2d, Point vertex) {

		double scale = zoomMultipliers[zoomLevel];
		double stationRadius = 0.5 * scale;
		double x = getImageX(vertex) - stationRadius;
		double y = getImageY(vertex) - stationRadius;
		Ellipse2D ellipse = new Ellipse2D.Float();
		ellipse.setFrame(x, y, stationRadius * 2, stationRadius * 2);
		Color tempColor = g2d.getColor();
		g2d.setColor(new Color(1f, 0f, 0f, 0.5f));
		g2d.setStroke(new BasicStroke(0));
		g2d.fill(ellipse);
		g2d.draw(ellipse);
		g2d.setColor(tempColor);
	}

	private long lastDragRepaint = 0;
	private int dragRepaintThrottle = 100; // minimal time between two repaints while dragging
	private int mouseButtonDown = -1;

	private static final int LEFT_BUTTON = MouseEvent.BUTTON1;
	private static final int RIGHT_BUTTON = MouseEvent.BUTTON3;

	@Override
	public void mouseDragged(MouseEvent e) {
		if (mouseButtonDown == SvgCanvas.LEFT_BUTTON) {
			dragOffsetX = dragOffsetXBeforeDragging + (e.getX() - dragStartX);
			dragOffsetY = dragOffsetYBeforeDragging + (e.getY() - dragStartY);
			long t = System.currentTimeMillis();
			if (t > lastDragRepaint + dragRepaintThrottle) {
				lastDragRepaint = t;
				this.repaint();
			}
		} else if (mouseButtonDown == SvgCanvas.RIGHT_BUTTON) {
			long t = System.currentTimeMillis();
			if (t > lastDragRepaint + dragRepaintThrottle) {
				this.selectSourceStationClosestTo(e.getX(), e.getY());
				lastDragRepaint = t;
				this.repaint();
			}
		}
	}

	private void handleShiftDownComputation(int x, int y) {
		MetroVertex dvOld = null;
		if (Settings.destinationStation instanceof MetroVertex) {
			dvOld = Settings.destinationStation;
		}
		long t = System.currentTimeMillis();
		if (t > lastDragRepaint + dragRepaintThrottle) {
			this.selectDestinationStationClosestTo(x, y);
			if (Settings.destinationStation instanceof MetroVertex && Settings.destinationStation.equals(dvOld) == false) {
				lastDragRepaint = t;
				this.repaint();
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// if(this.shiftButtonIsDown) {
		// this.handleShiftDownComputation(e.getX(), e.getY());
		// }
	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {
		this.addNotify();
		mouseButtonDown = e.getButton();
		switch (e.getButton()) {
			case SvgCanvas.LEFT_BUTTON:
				dragStartX = e.getX();
				dragStartY = e.getY();
				dragOffsetXBeforeDragging = dragOffsetX;
				dragOffsetYBeforeDragging = dragOffsetY;
				this.setCursor(closedHand);
				break;
			case SvgCanvas.RIGHT_BUTTON:
				if (this.shiftButtonIsDown) {
					this.handleShiftDownComputation(e.getX(), e.getY());
				} else {
					this.selectSourceStationClosestTo(e.getX(), e.getY());
				}
				this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
				break;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mouseButtonDown = -1;
		switch (e.getButton()) {
			case SvgCanvas.LEFT_BUTTON:
				dragStartX = -1;
				dragStartY = -1;
				this.setCursor(openHand);
				this.repaint();
				break;
			case SvgCanvas.RIGHT_BUTTON:
				this.setCursor(openHand);
				this.repaint();
				break;
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			shiftButtonIsDown = true;
			this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			java.awt.Point p = this.getMousePosition();
			if (p instanceof java.awt.Point) {
				this.handleShiftDownComputation(p.x, p.y);
				this.repaint();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			shiftButtonIsDown = false;
			this.setCursor(openHand);
			this.repaint();
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	public Graph getGraph() {
		return this.g;
	}

	private void selectSourceStationClosestTo(int cursorX, int cursorY) {

		double smallestDistance = Double.MAX_VALUE;
		MetroVertex closestVertex = null;

		for (Object vertexObject : this.g.getVertices()) {
			MetroVertex vertex = (MetroVertex) vertexObject;
			double distance = Math.sqrt(Math.pow(getImageX(vertex) - cursorX, 2) + Math.pow(getImageY(vertex) - cursorY, 2));
			if (distance < smallestDistance) {
				closestVertex = vertex;
				smallestDistance = distance;
			}
		}

		if (closestVertex != null) {
			this.app.getGUIBuilder().stationComboBox.setSelectedItem(closestVertex);
		}

	}

	private void selectDestinationStationClosestTo(int cursorX, int cursorY) {

		double smallestDistance = Double.MAX_VALUE;
		MetroVertex closestVertex = null;

		for (Object vertexObject : this.g.getVertices()) {
			MetroVertex vertex = (MetroVertex) vertexObject;
			double distance = Math.sqrt(Math.pow(getImageX(vertex) - cursorX, 2) + Math.pow(getImageY(vertex) - cursorY, 2));
			if (distance < smallestDistance) {
				closestVertex = vertex;
				smallestDistance = distance;
			}
		}

		if (closestVertex != null) {
			Settings.destinationStation = closestVertex;
		}

	}

}