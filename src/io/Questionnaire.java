package io;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import mip.GraphTools;
import model.MetroEdge;
import model.MetroPath;
import model.MetroVertex;
import model.Point;
import model.TransferGraphEdge;
import app.Dijkstra;
import app.MetroMapper;
import app.Settings;
import app.SvgCanvas;
import edu.uci.ics.jung.graph.Graph;

public class Questionnaire {

	private static String RAW = "raw";
	private static String MODIFIED = "modified";

	private static String SOURCE_BASED = "source based";
	private static String ALL_PAIRS_BASED = "all-pairs based";
	private static String FIXED_SOURCE_BASED = "fixed source based";
	private static String FIXED = "fixed";
	
	public static String generateQuestion(MetroMapper app) throws Exception 
	{
		SvgCanvas canvas = app.getSvgCanvas();
		String rand = Questionnaire.generateString("abcdefghijklmnopqrstuvwxyz0123456789", 8);
		String basepath = Settings.questionnaireBasePath;
		
		// Save map.png		
		BufferedImage img = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = img.createGraphics();
		canvas.printAll(g2d);
		g2d.dispose();
		try {
			File dir = new File(basepath + rand);
			dir.mkdirs();
		    ImageIO.write(img, "png", new File(basepath + rand + "/map.png"));
		} catch (IOException ex) {
		    ex.printStackTrace();
		}
		
		
		// Save info.php
		if(Settings.sourceStation != null && Settings.destinationStation != null) {
			Graph g = app.getGraph();
			
			String map = Settings.inputMap;
			if(Settings.loadedGraphMLFile instanceof File) {
				map = Settings.loadedGraphMLFile.getName();
			}
			
			// Schematization
			String schematization = (Settings.modifiedSchematization ? Questionnaire.MODIFIED : Questionnaire.RAW);
			
			// Visualization
			String visualization = "";
			if(Settings.shortestPathsAlgorithm == Settings.SHORTEST_PATHS_SINGLE_SOURCE) {
				if(Settings.fixedLineThicknesses == true) {
					visualization = Questionnaire.FIXED_SOURCE_BASED; 
				} else {
					visualization = Questionnaire.SOURCE_BASED;
				}
			} else if(Settings.fixedLineThicknesses == true) {
				visualization = Questionnaire.FIXED;
			} else if(Settings.shortestPathsAlgorithm == Settings.SHORTEST_PATHS_ALL_PAIRS) {
				visualization = Questionnaire.ALL_PAIRS_BASED;
			} else {
				throw new Exception("Could not determine visualization type.");
			}
			
			// Arrow hints
			String arrowHints = (Settings.drawArrowHints ? "true" : "false");
			
			// Correct answer
			List shortestPath = Dijkstra.shortestPaths.get(Settings.sourceStation).get(Settings.destinationStation);
			ArrayList<MetroVertex> answerIntersections = new ArrayList<MetroVertex>();
			for(Object o : shortestPath) {
				TransferGraphEdge tge = (TransferGraphEdge) o;
				MetroVertex first = tge.getFirst().getOriginalVertex();
				MetroVertex second = tge.getSecond().getOriginalVertex();
				if(first.degree() > 2 && answerIntersections.contains(first) == false && first.getName().equals(Settings.sourceStation.getName()) == false && first.getName().equals(Settings.destinationStation.getName()) == false) {
					answerIntersections.add(first);
				}
				if(second.degree() > 2 && answerIntersections.contains(second) == false && first.getName().equals(Settings.sourceStation.getName()) == false && first.getName().equals(Settings.destinationStation.getName()) == false) {
					answerIntersections.add(second);
				}
			}
			
			// Output
			String output = "<?php\r\n";
			output += "return array(\r\n";
			output += "\t'map' => '" + map + "',\r\n";
			output += "\t'start' => new Point('start', " + (int)(canvas.getImageX(Settings.sourceStation)) + ", " + (int)(canvas.getImageY(Settings.sourceStation)) + "),\r\n";
			output += "\t'end' => new Point('end', " + (int)(canvas.getImageX(Settings.destinationStation)) + ", " + (int)(canvas.getImageY(Settings.destinationStation)) + "),\r\n";
			output += "\t'intersections' => array(\r\n";
			for (Object vertexObject : g.getVertices()) {
				MetroVertex vertex = (MetroVertex) vertexObject;
				if(vertex.degree() > 2 && vertex.getName().equals(Settings.sourceStation.getName()) == false && vertex.getName().equals(Settings.destinationStation.getName()) == false) {
					output += "\t\tnew Point('" + vertex.getName() + "', " + (int)(canvas.getImageX(vertex)) + ", " + (int)(canvas.getImageY(vertex)) + "),\r\n";
				}
			}
			output += "\t),\r\n";
			output += "\t'answer' => array(\r\n";
			for (MetroVertex vertex : answerIntersections) {
				output += "\t\t'" + vertex.getName() + "',\r\n";
			}
			output += "\t),\r\n";
			output += "\t'schematization' => '" + schematization + "',\r\n";
			output += "\t'visualization' => '" + visualization + "',\r\n";
			output += "\t'arrowhints' => " + arrowHints + ",\r\n";

			// Average edge time
			int timeTotal = 0;
			int timeTotalCount = 0;
			for (Object pathObject : GraphTools.getMetroLines(g)) {
				MetroPath path = (MetroPath) pathObject;
				for (Object edgeObject : path) {
					MetroEdge edge = (MetroEdge) edgeObject;
					timeTotal += edge.getTime(path.getName());
					timeTotalCount++;
				}
			}
			output += "\t'averageEdgeTime' => " + (timeTotal / timeTotalCount) + ",\r\n";
			
			output += ");\r\n";
		
			try {
	            File file = new File(basepath + rand + "/info.php");
	            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	            bw.write(output);
	            bw.close();
	        } catch ( IOException e ) {
	            e.printStackTrace();
	        }
		}
		
		
		return basepath + rand;
	}
	
	public static String generateString(String characters, int length)
	{
		Random rng = new Random();
	    char[] text = new char[length];
	    for (int i = 0; i < length; i++)
	    {
	        text[i] = characters.charAt(rng.nextInt(characters.length()));
	    }
	    return new String(text);
	}
	
}
