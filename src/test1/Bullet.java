package test1;

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
		ellipse = new Ellipse(3,3);
		ellipse.setFill(Color.LIGHTGRAY);
	}
	
	protected boolean isFlying() {
		return (velX==0 && velY==0);
	}
	
	protected void precisePosReset() {
		pX = position%mapWidth;
		pY = position/mapWidth;
	}
	
	protected void move(int pos) {
		position =  pos;
		ellipse.setTranslateX(position%mapWidth);
		ellipse.setTranslateY(position/mapWidth);
	}
	
	protected void launch(double x, double y, int vel) {
		System.out.println("Shot at [" + x + ", " + y + "]");
		double xyDist = Math.sqrt(x*x+y*y);
		velX = vel*x/xyDist;
		velY = -vel*y/xyDist;
	}
	
	protected int tick() {	// returns bullet position
		velY -= 0.3;
		pY -= velY;
		pX += velX; 
		position = (int) (Math.floor(pX) + mapWidth*Math.floor(pY));
		move(position);
		if(position>=arrayBounds || pX<0 || pX>mapWidth-1) return -1;// -1 error state for flying too far right/left/down
		if(pY<0) return -2; // -2 for flying above stage
		return position;
	}
	
	protected HBox getHBox() {
		HBox hbox = new HBox(ellipse);
		return hbox;
	}
	protected Ellipse getEllipse() {
		return ellipse;
	}
}
