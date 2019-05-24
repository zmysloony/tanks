package test1;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;

import java.sql.SQLException;

import java.util.Random;


enum MapState {
	W84PLAYER, LOADEDMAP, ENTRY, CANSHOOT, BULLETFLYING
}

enum Comm {
	sSHAKE, sURTURN, sSHOT, sENDCONNECTION,
	cSHAKE, cURTURN, cSHOT,
	ERROR
}

class CommExt {
	Comm task;
	double x;
	double y;
	int ext;
	boolean fin; // false if not complete
	
	CommExt(Comm a, double b, double c, int d) {
		task = a;
		x = b;
		y = c;
		ext = d;
		fin = true;
	}
}

public class Sand extends Application {
	
	int width;
	int height;
	int splashRadius;
	int maxDmg;
	
	MapState state;
	boolean myTurn;
	boolean stable;
	boolean map[];
	Image mapka;
	ImageView iv;
	Stage mainStage;
	AnimationTimer timer;
	
	Arta p1Arta, p2Arta;
	Arta player, enemy;
	Text playerHP, enemyHP;
	
	Scene logScene, regScene, mmScene, servScene;
	
	int loggedPID;
	CommThread clientThread;
	CommThread serverThread;
	
	public Sand() {
		
		width = 800;
		height = 640;
		splashRadius = 40;
		
		
		maxDmg = 35;
		
		loggedPID = -1; // error state
		
		state = MapState.ENTRY;
		stable = false;
		map = new boolean[height*width];	//automatically fills with zeros
	}
	
	protected void loginScene() {
		if(logScene != null) {
		    mainStage.setScene(logScene);
		    return;
		}
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("loginscreen.fxml"));
			Parent root = loader.load();
			LoginScreenController ctrl = loader.getController();
			ctrl.setParent(this);
			logScene = new Scene(root, width, height);
		} catch (IOException e) {
			e.printStackTrace();
		}
       
        mainStage.setScene(logScene);
        mainStage.show();
	}
	
	protected void registerScene() {
	    if(regScene != null) {
	    	mainStage.setScene(regScene);
	    	return;
	    }
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("registerscreen.fxml"));
			Parent root = loader.load();
			RegisterScreenController ctrl = loader.getController();
			ctrl.setParent(this);
			regScene = new Scene(root, width, height);
		} catch (IOException e) {
			e.printStackTrace();
		}
       
        mainStage.setScene(regScene);
        mainStage.show();
	}
	
	public void mainMenu() {
       if(mmScene != null) {
    	   mainStage.setScene(mmScene);
    	   return;
       }
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("mainmenu.fxml"));
			Parent root = loader.load();
			MainMenuController ctrl = loader.getController();
			ctrl.setParent(this);
			mmScene = new Scene(root, width, height);
		} catch (IOException e) {
			e.printStackTrace();
		}
       
        mainStage.setScene(mmScene);
        mainStage.show();
	}
	

	public void serverSelectionScene() {
	     if(servScene != null) {
	    	 mainStage.setScene(servScene);
	    	 return;
	     }
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("ServerSelection.fxml"));
				Parent root = loader.load();
				ServerSelectionController ctrl = loader.getController();
				ctrl.setParent(this);
				servScene = new Scene(root, width, height);
			} catch (IOException e) {
				e.printStackTrace();
			}
	       
	        mainStage.setScene(servScene);
	        mainStage.show();
	}
	
	
	
	public class CommThread extends Thread {	// server is always player 1
		ServerSocket servSocket;
		Socket clientSocket;
		DataInputStream dataIn;
		DataOutputStream dataOut;
		CommExt current;
		boolean isServer;
		String nameHandle;
		

        boolean quit;
		
		public CommThread(boolean server, int socketNum) throws IOException {
			if(server) servSocket = new ServerSocket(socketNum);
			isServer = server;
			quit = false;
			nameHandle = server ? "(server) " : "(client) ";
			System.out.println(nameHandle + "constructed");
		}
		
		public void sendShot(double x, double y) {
			try {
				dataOut.writeInt(getValue(Comm.sSHOT));
				dataOut.writeDouble(x);
				dataOut.writeDouble(y);
				dataOut.writeInt(player.shotVelocity);
				dataOut.flush();
			} catch (IOException e) {
				System.out.println(nameHandle +"sending ioexception");
			}
		}
		
		public CommExt getSignal() {
			CommExt fullComm = null;
			try {
				Comm commType =  toComm(dataIn.readInt());
				double x = dataIn.readDouble();
				double y = dataIn.readDouble();
				int ext = dataIn.readInt();
				fullComm = new CommExt(commType, x, y, ext);
			} catch (IOException e) {
				System.out.println(nameHandle+ " got ioexception");
			}
			return fullComm;
		}
		
		private void serverSetup() {
			try {
				servSocket.setSoTimeout(2000);
				Socket connSocket = null;
				while(connSocket == null)	// necessary timeout so thread never hangs after closing
					try {
						if(quit == true) 
							return;
						connSocket = servSocket.accept();
					} catch (SocketTimeoutException e) {
						System.out.println(nameHandle + "still waiting for client");
					} catch (SocketException e) {
						System.out.println("No exception, just closing an accepting socket.");
					}
				
	    		System.out.println(nameHandle + "client connected");
	            dataIn = new DataInputStream(connSocket.getInputStream());
	            dataOut = new DataOutputStream(connSocket.getOutputStream());
	            //log4j
	            if (connSocket.isConnected()) {
	            	dataOut.writeInt(getValue(Comm.sSHAKE));
	            	dataOut.writeDouble(0);
	            	dataOut.writeDouble(0);
	            	dataOut.writeInt(1);
	            	dataOut.flush();
	            	// sent handshake
	            	//TODO handshake method
	            	System.out.println(nameHandle + "sent handshake");
	            	Comm fromClient = toComm(dataIn.readInt());
	            	if(fromClient == Comm.cSHAKE);
	            	System.out.println(nameHandle + "we shook");
	            	sleep(5); // test
	            	System.out.println(nameHandle + "sending shot command");
	            	sendShot(15,15);
	            	while(player==null) {}
	            	state = MapState.BULLETFLYING;
	            	player.shootAt(15, 15);
	            	System.out.println(nameHandle + "done test shot");
	            }
	            waiter();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private void clientSetup() {
			try {
				clientSocket = new Socket("localhost", 4443);
				dataIn = new DataInputStream(clientSocket.getInputStream());
                dataOut = new DataOutputStream(clientSocket.getOutputStream());
                System.out.println(nameHandle + "created socket");
                waiter();
			}  catch (Exception e) {
                e.printStackTrace();
            }
		}
		
		private void waiter() throws IOException {
			CommExt signal;
            while(quit == false) {
                signal = getSignal();
                System.out.println(nameHandle + "read " + signal.task);
                // get full extended command
                switch(signal.task) {
                case sSHAKE: // only Comm
                	System.out.println(nameHandle + "sending shake back");
                	dataOut.writeInt(getValue(Comm.cSHAKE));
                	break;
                case sSHOT: // full CommExt
                	System.out.println(nameHandle + "recieved shot command");
                	//sleep(5);
                	while(stable==false || state==MapState.ENTRY) {
                		System.out.println("waiting for sand to drop...");
                	}
                	state = MapState.BULLETFLYING;
                	enemy.shootAt(signal.x, signal.y, signal.ext);
                	
            		System.out.println(nameHandle + "recieved full shot");
                	break;
                default:
                	System.out.println("Erroneous command.");
                	break;
                }
            }
		}
		
		@Override
        public void run() {
			if(isServer) {
				serverSetup();
			} else {
				clientSetup();
			}
        }
		
		

		public void quit() {
			try {
				if(servSocket != null) servSocket.close();
				if(clientSocket != null) clientSocket.close();
			} catch (Exception e) {
				System.out.println("Exception while closing sockets");
			}
        	quit = true;
        }
	}
	
	public boolean isServer(String ip, int port) {
		Socket tester = new Socket();
		try {
			tester.connect(new InetSocketAddress(ip, port), 10);
			tester.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void startServer() {
		try {
    		serverThread = new CommThread(true, 4443);
    		serverThread.start();
    		System.out.println("(main menu) server starting done");
    		setGame(1);
    	} catch (Exception a) {
    		a.printStackTrace();
    	}
	}
	
	public void startClient() {
		try {
    		clientThread = new CommThread(false, 4443);
    		clientThread.start();
    		System.out.println("(main menu) client starting done");
    		setGame(2);
    	} catch (Exception a) {
    		a.printStackTrace();
    	}
	}
	
	public static int getValue(Comm comm) {
		switch(comm) {
		case sSHAKE:
			return 1;
		case sURTURN:
			return 2;
		case sSHOT:
			return 3;
		case sENDCONNECTION:
			return 4;
		case cSHAKE:
			return 5;
		case cURTURN:
			return 6;
		case cSHOT:
			return 7;
		default:
			return -1;
		}
	}
	
	public static Comm toComm(int comm) {
		switch(comm) {
		case 1:
			return Comm.sSHAKE;
		case 2:
			return Comm.sURTURN;
		case 3:
			return Comm.sSHOT;
		case 4:
			return Comm.sENDCONNECTION;
		case 5:
			return Comm.cSHAKE;
		case 6:
			return Comm.cURTURN;
		case 7:
			return Comm.cSHOT;
		default:
			return Comm.ERROR;
		}
	}
	
	
	
	
	@Override
    public void start(Stage stage) throws InterruptedException, ClassNotFoundException, SQLException {
		stage.setTitle("105 leFH18B2");
		//stage.initStyle(StageStyle.UNDECORATED);
        mainStage = stage;
        mainStage.setResizable(false);
        mainStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                System.out.println("Stage is closing");
                
                if(serverThread!=null) serverThread.stop();
                if(clientThread!=null) clientThread.stop();
                if(timer!=null) timer.stop();
                Platform.exit();
                
            }
        });  
        loginScene();
    }
	
	
	protected void setGame(int playerS) throws InterruptedException, ClassNotFoundException, SQLException {
        generateRandom(2);
        p1Arta = new Arta(0, width, height, 150, 500);// TODO randomize position (or static?)
		p2Arta = new Arta(1, width, height, 650, 500);
		if(playerS==1) {
			player = p1Arta;	// side chosen in server/client selection
			enemy = p2Arta;
			myTurn = true;
		} else {
			player = p2Arta;
			enemy = p1Arta;
			myTurn = false;
		}
		
		mapka = getImageFromArray(mapToColor(), width, height);
		iv = new ImageView();
        iv.setImage(mapka);
        
        //HBox hbox = new HBox(iv);
        Button button = new Button("go back to menu");
        
        playerHP = new Text("100");
        enemyHP = new Text("100");
		playerHP.setStyle("-fx-font: 14 \"Courier New\"; -fx-font-weight: bold; -fx-fill: green; -fx-stroke: white; -fx-stroke-width: 1px; -fx-font-size: 16px;");
		enemyHP.setStyle("-fx-font: 14 \"Courier New\"; -fx-font-weight: bold; -fx-fill: green; -fx-stroke: white; -fx-stroke-width: 1px; -fx-font-size: 16px;");
		
        
        HBox topMenuHBox = new HBox(playerHP, button, enemyHP);
        
        VBox hpLabelP1 =  p1Arta.getHpLabel();
        VBox hpLabelP2 =  p2Arta.getHpLabel();
        
       
        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(iv, hpLabelP1, hpLabelP2, p1Arta.getBulletHBox(), p2Arta.getBulletHBox(),topMenuHBox, player.getLSPCircle());
        
       // HBox root = new HBox();
       // root.getChildren().add(stackPane);
        
        Scene scene = new Scene(stackPane, width, height);
        
        mainStage.setScene(scene);
        mainStage.show();
        
        
        // hook up mouse events
        stackPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	switch(state) {
            	case CANSHOOT:
            		if(myTurn==false || !stable) {
            			System.out.println("Not my turn or map not stable!");
            			return;
            		}
            		state = MapState.BULLETFLYING;
                    player.shootAt(event.getX(), event.getY());
                    if(playerS==1) {
                    	serverThread.sendShot(event.getX(), event.getY());
                    	System.out.println("sent shot to client");
                    } else {
                    	clientThread.sendShot(event.getX(), event.getY());
                    	System.out.println("sent shot to server");
                    }
                    break;
            	default:
            		System.out.println("No onMouseClicked action for MapState " + state);
            		break;
            	}
            }
        });
        
        
        
        // hook up keyboard clicks
        scene.setOnKeyPressed(e -> {
    		KeyCode k = e.getCode();
            System.out.println("\n" + k + " key was pressed");
            switch(k) {
        	case W:
        		System.out.println("velocity zero: " + player.higherV());
            	return;
        	case S:
        		System.out.println("velocity zero: " + player.lowerV());
        		return;
            default:
            	System.out.println("No handler for " + k + " key");
            	break;
        	}
        	switch(state) {
        	case CANSHOOT:
            	
            	break;
        	default:
        		System.out.println("No keyboard actions for state " + state);
        		break;
        	}
        	
        });
        
        

        state = MapState.LOADEDMAP;
        
        //drop all the randomized sand
        updateMap updateMapRunnable = new updateMap();
        
        timer = new AnimationTimer() {
        	@Override
        	public void handle(long now) {
        		updateMapRunnable.run();
        	}
        };
        timer.start();
        mainStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                System.out.println("Stage is closing");
                if(serverThread!=null) serverThread.stop();
                if(clientThread!=null) clientThread.stop();
                timer.stop();
                Platform.exit();
                
            }
        });
        
        button.setOnAction((event) -> {
        	try {
        		if(serverThread != null) serverThread.quit();
        		if(clientThread != null) clientThread.quit();
        		mainMenu();
        		state = MapState.ENTRY;
        		stable = false;
        		map = new boolean[height*width];
        		timer.stop();
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        	System.out.println("went back to menu");
        	return;
        });
        //state = MapState.CANSHOOT; // NOT MANUAL
        
    }
	
	
	class updateMap implements Runnable {
		private Thread t;
		@Override
        public void run() {
            if(!stable) {
            	boolean stabletemp = true;
            	for(int i=height*width-1; i>=0; i--) {
            		if(map[i]==true && below(i)==1) {	// drop it down
            			map[i] = false;
            			map[i+width] = true;
            			stabletemp = false;
            		}
            	}
            	
            	//update players positions
            	if(map[p1Arta.position]==true) {
            		p1Arta.raise();
            		stabletemp = false;
            	} else if(below(p1Arta.position)==1) {
            		p1Arta.drop();
            		stabletemp = false;
            	}
            	if(map[p2Arta.position]==true) {
            		p2Arta.raise();
            		stabletemp = false;
            	} else if(below(p2Arta.position)==1) {
            		p2Arta.drop();
            		stabletemp = false;
            	}
            	
            	//draw to image
            	mapka = getImageFromArray(mapToColor(), width, height);
            	iv.setImage(mapka);
            	stable = stabletemp;
            }
            if(state == MapState.ENTRY && stable) {	// end the entry stage (sand falling down)
            	state = MapState.CANSHOOT;
            	System.out.println("State set to " + state);
            }
            if(state == MapState.BULLETFLYING) {
            	Arta who = (myTurn) ? player : enemy;
            	stable = false;
            	//System.out.println("ipdaasdasd");
            	int bpos = who.bullet.tick();

            	if(bpos == -1) {
            		System.out.println("Flew out of bounds.");
            		who.returnBullet();
            		state = MapState.CANSHOOT;
            		stable = true;
            		return;
            	}
            	//System.out.println(bpos);
            	if(bpos>0 && map[bpos]==true) {
            		recieveHit(bpos%width, bpos/width);
            		who.returnBullet();
            		state = MapState.CANSHOOT;
            	}
            }
        }
		
		public void start () {
		     // System.out.println("Starting map thread");
		      if (t == null) {
		         t = new Thread (this, "mapth");
		         t.start ();
		      }
		}
	}
	
	private void recieveHit(int x, int y) {
		// enemy hit
		double enemyMultiplier, playerMultiplier;
		if(myTurn) {
			playerMultiplier = 0.6;
			enemyMultiplier = 1;
		} else {
			playerMultiplier = 1;
			enemyMultiplier = 0.6;
		}
		System.out.println("hit " + x + " " + y);
		int xDist = x-enemy.x();
		int yDist = y-enemy.y();
		int dist  = (int) Math.sqrt(xDist*xDist+yDist*yDist);
		if (dist < splashRadius)
			enemy.removeHP(maxDmg*(int)((splashRadius-dist)*enemyMultiplier)/splashRadius);
		if(enemy.hp>70) {
			enemyHP.setStyle("-fx-font: 14 \"Courier New\"; -fx-font-weight: bold; -fx-fill: green; -fx-stroke: white; -fx-stroke-width: 1px; -fx-font-size: 16px;");
		} else if (enemy.hp>40) {
			enemyHP.setStyle("-fx-font: 14 \"Courier New\"; -fx-font-weight: bold; -fx-fill: orange; -fx-stroke: white; -fx-stroke-width: 1px; -fx-font-size: 16px;");
		} else {
			enemyHP.setStyle("-fx-font: 14 \"Courier New\"; -fx-font-weight: bold; -fx-fill: red; -fx-stroke: white; -fx-stroke-width: 1px; -fx-font-size: 16px;");
		}
		// player hit
		xDist = x-player.x();
		yDist = y-player.y();
		dist  = (int) Math.sqrt(xDist*xDist+yDist*yDist);
		if (dist < splashRadius)
			player.removeHP(maxDmg*(int)((splashRadius-dist)*playerMultiplier)/splashRadius);
		if(player.hp>70) {
        	playerHP.setStyle("-fx-font: 14 \"Courier New\"; -fx-font-weight: bold; -fx-fill: green; -fx-stroke: white; -fx-stroke-width: 1px; -fx-font-size: 16px;");
		} else if (player.hp>40) {
			playerHP.setStyle("-fx-font: 14 \"Courier New\"; -fx-font-weight: bold; -fx-fill: orange; -fx-stroke: white; -fx-stroke-width: 1px; -fx-font-size: 16px;");
		} else {
			playerHP.setStyle("-fx-font: 14 \"Courier New\"; -fx-font-weight: bold; -fx-fill: red; -fx-stroke: white; -fx-stroke-width: 1px; -fx-font-size: 16px;");
		}
		for(int i=-splashRadius; i<=splashRadius; i++) {
			for(int j=-splashRadius; j<=splashRadius; j++) {
				int nx = x+i, ny= y+j;
				if(isLegal(nx, ny) && ((nx-x)*(nx-x)+(ny-y)*(ny-y))<splashRadius*splashRadius) {
					map[xyToPos(nx,ny)] = false;
				}
			}
		}
		stable = false;
		switchTurns();
	}
	
	private boolean isLegal(int x, int y) {
		if(x<0 || x>=width || y<0 || y>=height) return false;
		return true;
	}
	
	private int xyToPos(int x, int y) {
		return y*width+x;
	}
	
	private int below(int pos) {
		if(pos/width==height-1) {	// if its the last line
			return 0;
		}
		if(map[pos+width]==true) {
			return 0;
		}
		return 1;
	}
	
	private void switchTurns() {
		if(myTurn) {
			state = MapState.W84PLAYER;	// enemy turn
			myTurn = false;
		} else {
			state = MapState.CANSHOOT;
			myTurn = true;
		}
	}
	
	public void generateRandom(int type) {
		Random rand = new Random();
		switch(type) {
		case 1:
			for(int i=0; i<height*width; i++) {
				if(rand.nextInt(101)>70)
					map[i] = true;
			}
			break;
		case 2:
			for(double x=0; x<width; x++) {
				for(double y=height/2; y<height; y++) { // real height: height-1-y
					map[xyToPos((int)x,(int)y)] = Math.abs(Math.cos(x*Math.PI*2/width))*height/2>height-y ? true : false;
				}
			}
			break;
		default:
			System.out.println("Incorrect generation type");
			break;
		}
	}
	
	public int[] mapToColor() {
		int[] colorMap = new int[height*width];
		for(int i=0; i<height*width; i++) {
			if(map[i]==true)
				colorMap[i]=0xFFffb366;//992255;
			else
				colorMap[i]=0xFF130323;
		}
		return colorMap;
	}
	
	public static Image getImageFromArray(int[] pixels, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
       // WritableRaster raster = (WritableRaster) image.getData();
        WritableRaster raster = image.getRaster();
        raster.setDataElements(0,0,width,height,pixels);
        return SwingFXUtils.toFXImage(image, null);
    }
	
	public static void main(String[] args) {
		Application.launch(Sand.class, args);
	}
}
