package tests;

import org.junit.Before;
import org.junit.Test;

public class MainTester {
	private  main.Sand sand;
	@Before
	public  void setUp() {	// generates a basic map
		sand = new main.Sand();
		sand.generateRandom(2);
	}
	
	@Test(expected = NullPointerException.class) // check if recieving hit out of map bounds executes (it shouldn't)
	public void checkMapDestructionOutOfBounds() {
		sand.recieveHit(800,640);
	}
	
	@Test(expected = Test.None.class)
	public void checkMapDestructionOnBounds() {
		for(int x=0; x<222; x++)
			for(int y=0; y<640; y++)
				sand.recieveHit(x, y);
	}
}
