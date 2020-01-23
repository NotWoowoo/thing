package interpreter;

import java.awt.Color;
import java.util.ArrayList;

import lawt.engine.Entity;
import lawt.engine.Window;
import lawt.graphics.geometry.Circle;

public class Environment {
	
	private ArrayList<Window> windows = new ArrayList<Window>();
	public void createWindow(int w, int h, String title) {
		windows.add(new Window(w,h,title));
	}
	
	private ArrayList<Entity> entities = new ArrayList<Entity>();
	public void createCircle(double x, double y, double r) {
		entities.add(new Circle(x, y, r, Color.WHITE));
	}
	
}
