package app;

public class ViewPosition {

	public int dragOffsetX;
	public int dragOffsetY;
	public int zoomLevel;
	
	public ViewPosition(int zoomLevel, int dragOffsetX, int dragOffsetY)
	{
		this.zoomLevel = zoomLevel;
		this.dragOffsetX = dragOffsetX;
		this.dragOffsetY = dragOffsetY;
	}
	
}