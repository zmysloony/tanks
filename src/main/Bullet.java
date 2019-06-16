package main;

import javafx.scene.layout.HBox;
import javafx.scene.shape.Ellipse;
import javafx.scene.paint.Color;

public class Bullet {
	int position;
	double velX, velY;
	double pX, pY;
	int mapWidth;
	int mapHeight;
	int arrayBounds;
	Ellipse ellipse;
	
	Bullet(int w, int h) {
		velY = 0;
		velX = 0;
		mapWidth = w;
		mapHeight = h;
		arrayBounds = w*h;
		ellipse = new Ellipse(2,2);
		ellipse.setFill(Color.LIGHTGRAY);
	}
	
	protected boolean isFlying() {
		return (velX==0 && velY==0);
	}
	
	//resets position to integer values
	protected void precisePosReset() {
		pX = position%mapWidth;
		pY = position/mapWidth;
	}
	
	// graphically moves the bullet to a new position
	protected void move(int pos) {
		position =  pos;
		ellipse.setTranslateX(position%mapWidth);
		ellipse.setTranslateY(position/mapWidth);
	}
	
	// launches a bullet by calculating launch angle and starting x and y velocities
	protected void launch(double x, double y, int vel) {
		double xyDist = Math.sqrt(x*x+y*y);
		velX = vel*x/xyDist;
		velY = -vel*y/xyDist;
	}
	
	// returns bullet position after moving it by one tick
	protected int tick() {	
		velY -= 0.7;
		pY -= velY;
		pX += velX; 
		position = (int) (Math.floor(pX) + mapWidth*Math.floor(pY));
		move(position);
		if(position>=arrayBounds || pX<0 || pX>mapWidth-1) return -1;// -1 error state for flying too far right/left/down
		if(pY<0) return -2; // -2 for flying above stage
		return position;
	}
	
	// returns HBox that represents a bullet
	protected HBox getHBox() {
		HBox hbox = new HBox(ellipse);
		return hbox;
	}
	
	// returns Ellipse that represents a bullet
	protected Ellipse getEllipse() {
		return ellipse;
	}
}
