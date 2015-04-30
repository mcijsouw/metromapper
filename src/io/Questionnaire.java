package io;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import model.MetroVertex;
import app.MetroMapper;
import app.Settings;
import app.SvgCanvas;
import edu.uci.ics.jung.graph.Graph;

public class Questionnaire {

	public static String generateQuestion(MetroMapper app) 
	{
		SvgCanvas canvas = app.getSvgCanvas();
		String rand = Questionnaire.generateString("abcdefghijklmnopqrstuvwxyz0123456789", 8);
		String basepath = "D:/Xampp/htdocs/metromapper.dev/public_html/maps/";
		
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
			String output = "<?php\r\n";
			output += "return array(\r\n";
			output += "\t'start' => new Point('start', " + (int)(canvas.getImageX(Settings.sourceStation)) + ", " + (int)(canvas.getImageY(Settings.sourceStation)) + "),\r\n";
			output += "\t'end' => new Point('end', " + (int)(canvas.getImageX(Settings.destinationStation)) + ", " + (int)(canvas.getImageY(Settings.destinationStation)) + "),\r\n";
			output += "\t'intersections' => array(\r\n";
			int i = 0;
			for (Object vertexObject : g.getVertices()) {
				MetroVertex vertex = (MetroVertex) vertexObject;
				if(vertex.degree() > 2 && vertex.equals(Settings.sourceStation) == false && vertex.equals(Settings.destinationStation) == false) {
					output += "\t\tnew Point(" + i + ", " + (int)(canvas.getImageX(vertex)) + ", " + (int)(canvas.getImageY(vertex)) + "),\r\n";
					i++;
				}
			}
			output += "\t),\r\n";
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
