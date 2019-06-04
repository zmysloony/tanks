package test1;

import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class Arta {
	int owner;
	boolean ready;
	int mapWidth;
	int mapHeight;
	
	int shotVelocity;
	int position;
	int hp;
	Bullet bullet;
	Text hpLabel;
	Rectangle artyShape;
	Circle lastShotPos;
	
	Arta(int o, int w, int h, int x, int y) {
		position = y*w+x;
		mapHeight = h;
		mapWidth = w;
		owner = o;
		ready = (owner==1) ? false : true;
		hp = 10;
		shotVelocity = 10;
		bullet = new Bullet(mapWidth, mapHeight);
		
		//TODO rotating gun - rectangle?
		
		artyShape = new Rectangle(15,7);
		if(owner==0)
			artyShape.setFill(Color.BLUE);
		else
			artyShape.setFill(Color.RED);
		
		lastShotPos = new Circle(3);
		lastShotPos.setFill(Color.RED);
		lastShotPos.setVisible(false);
		
		DropShadow ds = new DropShadow();
		ds.setColor(Color.WHITE);
		hpLabel = new Text();
		hpLabel.setEffect(ds);
		updateArtaPos();
		removeHP(0);
	}
	
	protected void removeHP(int dmg) {
		hp -= dmg;
		if(hp>70) {
			hpLabel.setStyle("-fx-font: 12 \"Courier New\"; -fx-font-weight: bold; -fx-fill: green; -fx-stroke: black; -fx-stroke-width: 1px; -fx-font-size: 16px;");
		} else if (hp>40) {
			hpLabel.setStyle("-fx-font: 12 \"Courier New\"; -fx-font-weight: bold; -fx-fill: orange; -fx-stroke: black; -fx-stroke-width: 1px; -fx-font-size: 16px;");
		} else {
			hpLabel.setStyle("-fx-font: 12 \"Courier New\"; -fx-font-weight: bold; -fx-fill: red; -fx-stroke: black; -fx-stroke-width: 1px; -fx-font-size: 16px;");
		}
	}
	
	private void updateArtaPos() {
		artyShape.setTranslateX(position%mapWidth);
		artyShape.setTranslateY(position/mapWidth);
		hpLabel.setText(hp + "/100");
		hpLabel.setTranslateY(position/mapWidth-25);
		hpLabel.setTranslateX(position%mapWidth-30);

		bullet.move(position);
		bullet.precisePosReset();
		removeHP(0);
		//System.out.println("dropped " + owner);
	}
	
	protected void shootAt(double x, double y) {	// mouse x and mouse y when clicked
		//double speed = 20;
		bullet.launch(x-position%mapWidth,y-position/mapWidth, shotVelocity);
		
		lastShotPos.setTranslateX(-mapWidth/2 + x);
		lastShotPos.setTranslateY(-mapHeight/2 + y);
		lastShotPos.setVisible(true);
	}
	
	protected void shootAt(double x, double y, int newVel) {	// mouse x and mouse y when clicked
		//double speed = 20;
		shotVelocity = newVel;
		bullet.launch(x-position%mapWidth,y-position/mapWidth, shotVelocity);
		
		lastShotPos.setTranslateX(-mapWidth/2 + x);
		lastShotPos.setTranslateY(-mapHeight/2 + y);
		lastShotPos.setVisible(true);
	}
	
	protected int lowerV() {
		if(shotVelocity>1) shotVelocity--;
		return shotVelocity;
	}
	
	protected int higherV() {
		if(shotVelocity<20) shotVelocity++;
		return shotVelocity;
	}
	
	protected void returnBullet() {
		bullet.move(position);
		bullet.precisePosReset();
	}
	
	protected void raise() {
		hp--;
		position -= mapWidth;
		updateArtaPos();
	}
	
	protected void drop() {
		position += mapWidth;
		updateArtaPos();
	}
	
	protected int getColor() {
		if(owner==0) {
			return 0xFF4286f4;
		}
		return 0xFFf90404;
	}
	
	protected int x() {
		return position%mapWidth;
	}

	protected int y() {
		return position/mapWidth;
	}
	
	protected VBox getHpLabel() {
		VBox hpLabelbx = new VBox(artyShape, hpLabel);
		return hpLabelbx;
	}
	
	protected HBox getBulletHBox() {
		return bullet.getHBox();
	}

	protected Circle getLSPCircle() {
		return lastShotPos;
	}
	
}
