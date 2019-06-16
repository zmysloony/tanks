package main;

import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

public class Arta {
	int owner;
	int mapWidth;
	int mapHeight;
	int offset;
	
	int shotVelocity;
	int position;
	int hp;
	Bullet bullet;
	Text hpLabel;
	ImageView artyShape;
	Circle lastShotPos;
	VBox container;
	
	Arta(int o, int w, int h, int x, int y) {
		position = y*w+x;
		mapHeight = h;
		mapWidth = w;
		owner = o;
		offset = (owner==1) ? 0 : -95;
		hp = 10;
		shotVelocity = 20;
		bullet = new Bullet(mapWidth, mapHeight);
		
		//TODO rotating gun - rectangle?
		
		/*artyShape = new Rectangle(15,7);
		if(owner==0)
			artyShape.setFill(Color.BLUE);
		else
			artyShape.setFill(Color.RED);*/
		
		if(owner==0) {
			Image img= new Image("file:res/duckf.png");
			artyShape = new ImageView(img);
			artyShape.setFitHeight(25);
			artyShape.setFitWidth(25);
		} else {
			Image img = new Image("file:res/duckredf.png");
			artyShape = new ImageView(img);
			artyShape.setFitHeight(25);
			artyShape.setFitWidth(25);
		}
		
		lastShotPos = new Circle(3);
		lastShotPos.setFill(Color.RED);
		lastShotPos.setVisible(false);
		
		DropShadow ds = new DropShadow();
		ds.setColor(Color.WHITE);
		hpLabel = new Text();
		hpLabel.setEffect(ds);
		
		HBox artyShapeContainer = new HBox(artyShape);
		artyShapeContainer.setPrefWidth(100);
		artyShapeContainer.setMaxWidth(100);
		
		if(owner==0) {
			artyShapeContainer.setAlignment(Pos.TOP_RIGHT);
		}
		
		container = new VBox(artyShapeContainer, hpLabel);
		container.setFillWidth(true);
		container.setSpacing(4);

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
		container.setTranslateX(position%mapWidth + offset);
		container.setTranslateY(position/mapWidth);
		hpLabel.setText(hp + "/100");
		bullet.move(position);
		bullet.precisePosReset();
		removeHP(0);
	}
	
	protected void shootAt(double x, double y) {
		bullet.launch(x-position%mapWidth,y-position/mapWidth, shotVelocity);
		
		lastShotPos.setTranslateX(-mapWidth/2 + x);
		lastShotPos.setTranslateY(-mapHeight/2 + y);
		lastShotPos.setVisible(true);
	}
	
	protected void shootAt(double x, double y, int newVel) {
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
		if(shotVelocity<30) shotVelocity++;
		return shotVelocity;
	}
	
	protected void resetV() {
		shotVelocity = 20;
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
		
		return container;
	}
	
	protected HBox getBulletHBox() {
		return bullet.getHBox();
	}

	protected Circle getLSPCircle() {
		return lastShotPos;
	}
	
}
