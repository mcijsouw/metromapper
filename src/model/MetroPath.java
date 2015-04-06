package model;

public class MetroPath extends java.util.LinkedList {

	private static final long serialVersionUID = 7925456499149027807L;
	int color = -1;
	String name = "";
	
	public MetroPath() {
		super();
	}
	
	public String toString() {
		return this.name;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
}
