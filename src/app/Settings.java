package app;

import model.MetroVertex;

public class Settings {

	public static final int CONTRACT_NONE = 0;
	public static final int CONTRACT_ALL = 1;
	public static final int CONTRACT_NON_AJACENT_TO_INTERSECTION = 2;
	public static final int CONTRACT_NON_AJACENT_TO_INTERSECTION_OR_PENDANT_EDGE = 3;

	public static final int PLANARITY_NONE = 0;
	public static final int PLANARITY_ALL_EDGE_PAIRS = 1;
	public static final int PLANARITY_FACE_HEURISTIC = 2;
	public static final int PLANARITY_PENDANT_EDGES_ONLY = 3;

	public static final int LINE_THICKNESS_PASSENGER_COUNT = 0;
	public static final int LINE_THICKNESS_DIJKSTRA_PATH_COUNT = 1;
	
	public static final int SHORTEST_PATHS_SINGLE_SOURCE = 0;
	public static final int SHORTEST_PATHS_ALL_PAIRS = 1;

	/* Default settings */
	public static final String[] inputMaps = new String[] { "Karlsruhe", "London", "Montreal", "Sydney", "Vienna", "Washington" };
	public static final String defaultInputMap = "Vienna";
	public static String inputMap = defaultInputMap;
	public static int degreeTwoVerticesContractionMode = CONTRACT_NONE;
	public static int planarityMode = PLANARITY_NONE;
	public static boolean convexHullDummyEdgesInFaceComputation = false;
	public static int totalEdgeLengthImportance = 5;
	public static int geographicDeviationImportance = 5;
	public static int bendsOnLineImportance = 5;
	public static boolean modifiedSchematization = true;
	public static int minimumEdgeLength = 3;
	public static int cplexTimeLimit = 10;
	public static boolean drawOriginalVertices = true;
	public static String saveSVGDefaultFilePath = "D:/Eclipse Workspace/MetroMapper";
	public static String loadGraphMLDefaultFilePath = "D:/Eclipse Workspace/MetroMapper/solver";
	public static String loadedGraphMLFile = "";
	public static int passengerCountTime = 30; // 0..95 (24 * 4 time slots)
	public static int lineThicknessAlgorithm = LINE_THICKNESS_DIJKSTRA_PATH_COUNT;
	public static int shortestPathsAlgorithm = SHORTEST_PATHS_ALL_PAIRS;
	public static MetroVertex sourceStation = null;
	public static MetroVertex destinationStation = null;
	public static boolean renderTransferGraph = false;
	public static int defaultTransferTimeInSeconds = 5 * 60;
	public static boolean drawVertexLabels = false;
	public static boolean drawEdgeLabels = false;
	public static boolean drawPolygonWireframes = false;
	public static boolean drawArrowHints = false;
	public static int arrowSizePercentage = 40;
	public static boolean isGeneratingQuestion = false;
	public static boolean fixedLineThicknesses = true;
	public static boolean flipMap = false;
	public static String questionnaireBasePath = "D:/Xampp/htdocs/metromapper.dev/public_html/maps/";
	public static boolean perpendicularAngleByPath = true;
	public static boolean increaseSpaceAroundIntersections = true;

}