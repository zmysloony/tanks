package main;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Sand extends Application {
	Logger log;			// main logger
	int width; 			// window width (static)
	int height; 		// window height (static)
	int splashRadius;	// explosion radius of a bullet
	int maxDmg;			// maxiumum damage a bullet can do to an artillery station
	
	MapState state; // current game state
	boolean myTurn; // true if player can shoot, false otherwise
	boolean stable; // if false, map updater thread will keep refreshing the map
	boolean map[];	// pixelmap representing game terrain
	int mapSeed;	// TODO seed-based map generation
	Image mapka;	// image created from pixelmap map[]
	ImageView iv;	// contains Image mapka 
	Stage mainStage;// main JavaFX stage, where all scenes are switched
	AnimationTimer timer; // thread that runs the map updater
	
	Text turnText;			// updateable text in the top game menu
	Arta p1Arta, p2Arta;	// two artilleries...
	Arta player, enemy;		// that are assigned to these two
	
	Scene logScene, regScene, mmScene, servScene;	// program scenes
	
	int loggedPID, enemyPID, currentGID;	// self-explanatory (PID - PlayerID, GID - GameID);
	CommThread clientThread;	// communication threads
	CommThread serverThread;	//
	DbHandler dbHandler;		// database connection and query handler object
	
	MainMenuController mmCtrl;			// these two are needed to display text alerts (server not found etc)
	ServerSelectionController ssCtrl;	//
	
	enum MapState {	// represents states that a running game can be in
		W84PLAYER, LOADEDMAP, ENTRY, CANSHOOT, BULLETFLYING
	}
	
	enum Comm {	// used in sending signals between client and server
		SHOT, ENDCONNECTION, ERROR;

		protected static int getValue(Comm comm) {
			switch(comm) {
			case SHOT:
				return 1;
			case ENDCONNECTION:
				return 2;
			default:
				return -1;
			}
		}
		
		protected static Comm toComm(int comm) {
			switch(comm) {
			case 1:
				return Comm.SHOT;
			case 2:
				return Comm.ENDCONNECTION;
			default:
				return Comm.ERROR;
			}
		}
	}
	
	class CommExt {	// full signal command
		Comm task;
		double x;
		double y;
		int ext;
		
		CommExt(Comm a, double b, double c, int d) {
			task = a;
			x = b;
			y = c;
			ext = d;
		}
	}
	
	public Sand() {	// initializes the game with default values
		log = LogManager.getRootLogger();
		width = 800;
		height = 640;
		splashRadius = 40;
		maxDmg = 35;
		
		loggedPID = -1; // error state
		currentGID = -1; // error state
		enemyPID = -1; // error state
		
		state = MapState.ENTRY;
		stable = false;
		map = new boolean[height*width];	//automatically fills with zeros
		mapSeed = 137;		// TODO generating map with seeds
		dbHandler = new DbHandler();
	}
	
	// four methods that generate or switch (if already generated) to a scene
	// FXML based scene creation
	protected void loginScene() {
		if(logScene != null) {
		    mainStage.setScene(logScene);
		    return;
		}
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("loginscreen.fxml"));
			Parent root = loader.load();
			LoginScreenController ctrl = loader.getController();
			ctrl.setParent(this, dbHandler);
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
			ctrl.setParent(this, dbHandler);
			regScene = new Scene(root, width, height);
		} catch (IOException e) {
			e.printStackTrace();
		}
       
        mainStage.setScene(regScene);
        mainStage.show();
	}
	
	protected void mainMenu() {
       if(mmScene != null) {
    	   mainStage.setScene(mmScene);
    	   return;
       }
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("mainmenu.fxml"));
			Parent root = loader.load();
			mmCtrl = loader.getController();
			mmCtrl.setParent(this, dbHandler);
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
	    	 ssCtrl.refreshList(null);
	    	 return;
	     }
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("ServerSelection.fxml"));
				Parent root = loader.load();
				ssCtrl = loader.getController();
				ssCtrl.setParent(this, dbHandler);
				ssCtrl.refreshList(null);
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
		boolean isServer;
        boolean quit;
		
        // creates a server thread at a selected socket
		public CommThread(int socketNum) {
			
			isServer = true;
			quit = false;
			// TODO working in port range (fix slow scanning)
			try {
				servSocket = new ServerSocket(socketNum);
			} catch (IOException ignored) {
				log.error("socket 44020 taken, cannot start server");
				quit = true;
			}
			
			this.setName("server thread");
			log.info("server created successfully");
		}
		
		// checks if thread is configured properly and set to run (not to shutdown)
		public boolean correct() {
			return quit ? false : true;
		}
		
		// constructor that tries to connect to a server
		public CommThread(String ip, int port) {	
			int attempt = 1;
			while(clientSocket == null && attempt < 4) { // performs 3 attemps at communicating with socket
				try {
					clientSocket = new Socket(ip, port);
					dataIn = new DataInputStream(clientSocket.getInputStream());
	                dataOut = new DataOutputStream(clientSocket.getOutputStream());
	                log.info("connected succesfully to " + ip + ":" + port);
	                dataOut.writeBoolean(true);
	                dataOut.writeInt(loggedPID);
	                log.info("sent my PID (" + loggedPID + ") to server");
	                quit = false;
	    			this.setName("client thread");
	    			log.info("constructed");
				} catch (IOException e) {
					log.error("couldn't connect to server (maybe outdated server list), attempt "+attempt+"/3");
					attempt++;
					quit = true;
				}
			}
		}
		
		
		// reads a full CommExt signal and returns it as CommExt
		protected CommExt getSignal() {
			CommExt fullComm = null;
			try {
				Comm commType = Comm.toComm(dataIn.readInt());
				double x = dataIn.readDouble();
				double y = dataIn.readDouble();
				int ext = dataIn.readInt();
				fullComm = new CommExt(commType, x, y, ext);
			} catch (IOException e) {
				// log.error("recieved ioexception");
			}
			return fullComm;
		}
		
		// sends a full CommExt without actually creating an object
		private void sendCommExt(int comm, double x, double y, int ext) {
			try {
				dataOut.writeInt(comm);
				dataOut.writeDouble(x);
				dataOut.writeDouble(y);
				dataOut.writeInt(ext);
				dataOut.flush();
			} catch (IOException e) {
				log.error("ioexception while sending (correct after finishing a game)");
			}
		}
		
		// sends a singular shot signal
		public void sendShot(double x, double y) {
			sendCommExt(Comm.getValue(Comm.SHOT), x, y, player.shotVelocity);
		}
		
		// sends an end-connection request 
		public void requestConnectedQuit() {
			if(dataOut==null) return;
			sendCommExt(Comm.getValue(Comm.ENDCONNECTION), 0, 0, 0);
		}
		
		
		// looping while waiting for a client connection; responds to server check requests with PID
		private void serverCheckWaiter() {
			try {
				servSocket.setSoTimeout(2000);
				Socket connSocket = null;
				while(connSocket == null)	// necessary timeout so thread never hangs after closing
					try {
						if(quit == true) // allows exiting the loop when requested to close server
							return;
						connSocket = servSocket.accept();
						log.info("probed by " + connSocket.getInetAddress() + ":" + connSocket.getPort());
			            dataIn = new DataInputStream(connSocket.getInputStream());
			            dataOut = new DataOutputStream(connSocket.getOutputStream());
			            if(dataIn.readBoolean()==false) { 	// recieving false means just a server check (not joining the game)
			            	dataOut.writeInt(loggedPID);
			            	dataIn = null;
			            	dataOut = null;
			            	connSocket = null;
			            } else {	// actual player connected
			            	enemyPID = dataIn.readInt();
				    		dbHandler.updateSecondPlayer(enemyPID, currentGID);
				    		log.info("user connected (PID=" + enemyPID + ")");
			            	state = MapState.CANSHOOT;
			            }
					} catch (SocketTimeoutException e) {
						log.info("still waiting for client");
						connSocket = null;
					} catch (SocketException e) {
						log.info("closing server socket");
					}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		// starts the server thread after initial construction, sends GameID to database
		private void serverSetup() {
			try {
				// TODO database server listing and making a real server

				dbHandler.deleteAllGamesWithPid(loggedPID);
				dbHandler.addNewGame(loggedPID);
				servSocket.setSoTimeout(2000);
				// get new GID
				currentGID = dbHandler.getGidByPid(loggedPID); // new GID loaded
				log.info("new GID: " + currentGID);
	            serverCheckWaiter(); // waits until client connects, while responding to other connections
	            updateTurnText("Your turn", Color.WHITE);
	            waiter();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		// starts the client thread after initial construction
		private void clientSetup() {
			try {
                waiter();
			}  catch (Exception e) {
                e.printStackTrace();
            }
		}
		
		
		// handles in-game communication after connection server with client
		private void waiter() {		
			CommExt signal;
            while(quit == false) {
                signal = getSignal();
                if(signal==null) {
                	continue;
                }
                log.info("recieved " + signal.task);
                // get full extended command
                switch(signal.task) {
                case SHOT:
                	while(stable==false || state==MapState.ENTRY) {	// has to wait until sand drops (when game start
                		log.info("waiting for sand to drop");		// or when player recieves shot command while sand
                	}												// is still falling
                	shoot(enemy, signal.x, signal.y, signal.ext);
                	break;
                case ENDCONNECTION:
                	log.info("enemy quit the game, result not saved");
                	Platform.runLater(
                			() -> {
                            	mmCtrl.enemyQuit(loggedPID, enemyPID);
                				mainMenu();
                			}
                	);
                	quit(false);
                	
                	break;
                default:
                	log.error("recieved an erroneous command");
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
		
		
		// shuts down commThreads and requests the opponent (if connected) to do the same
		public void quit(boolean sentToEnemy) {
			try {
				if(currentGID!=-1) { // has to delete game if started
					dbHandler.deleteAllGamesWithPid(loggedPID);
				}
				
				if(sentToEnemy) requestConnectedQuit();
				if(servSocket != null) servSocket.close();
				if(clientSocket != null) clientSocket.close();
				currentGID = -1;
				enemyPID = -1;
			} catch (IOException e) {
				log.error("exception while closing socket");
				e.printStackTrace();
			}
        	quit = true;
        }
	}
	
	// cuts last 3 digits of an IPv4 address 
	private String cutSubnet(String fullIp) {
		String resultIp = fullIp;
		for(int i=resultIp.length()-1; i>=5; i--) {
			if(resultIp.charAt(i)=='.') {
				resultIp = resultIp.substring(0,i+1);
				break;
			}
		}
		return resultIp;
	}
	
	// gets incomplete local area network address (ex. 192.168.1.)
	public String getLanIp() {
		return cutSubnet(getLocalIp());
	}
	
	// gets computer's local IP address
	static public String getLocalIp() {
		try(final DatagramSocket socket = new DatagramSocket()){
			  socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			  return socket.getLocalAddress().getHostAddress();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "error getting local IP address";
	}
	
	// check if there's a game server on given ip + port
	// returns -1 if not, opponents PID otherwise
	public int isServer(String ip, int port) {	
		Socket tester = new Socket();
		int maybePID;
		try {
			tester.connect(new InetSocketAddress(ip, port), 10);
			DataInputStream dataIn = new DataInputStream(tester.getInputStream());
	        DataOutputStream dataOut = new DataOutputStream(tester.getOutputStream());
	        dataOut.writeBoolean(false);
	        tester.setSoTimeout(500);
	        maybePID = dataIn.readInt();
	        if(maybePID == -1) {	
	        	tester.close();
	        	return -1;
	        }
			tester.close();
		} catch (IOException e) {
			return -1;
		}
		return maybePID;
	}
	
	// creates a commThread as a server, initiates waiting for a client
	public void startServer() {	
		try {
    		serverThread = new CommThread(44020);
    		if(serverThread.correct()) {
    			log.info("Server started on " + getLocalIp() + ":44020");
    			serverThread.start();
        		setGame(1);
        		updateTurnText("Waiting for an opponent...", Color.WHITE);
    		} else {
    			log.error("couldn't create server correctly");
    		}
    	} catch (Exception a) {
    		a.printStackTrace();
    	}
	}
	
	
	// creates new commThread as a client, initiates the game after successful connection
	public void startClient(String ip, int port, int newPid) {	
		try {
    		clientThread = new CommThread(ip, port);
    		if(clientThread.correct()) {
    			log.info("client connected correctly");
        		setGame(2);
        		updateTurnText("Opponent's turn", Color.WHITE);
        		clientThread.start();
    		} else {
    			log.error("Couldn't establish connection to server (maybe refresh server list before connecting?)");
    			ssCtrl.refreshList(null);
    			ssCtrl.displayPopup("Couldn't connect to the server. Your server list was probably outdated.");
    		}
    		enemyPID = newPid;
    	} catch (Exception a) {
    		a.printStackTrace();
    	}
	}
	
	
	// sets title and window options; starts with a login scene
	@Override
    public void start(Stage stage) {	
		stage.setTitle("105 leFH18B2");
		//stage.initStyle(StageStyle.UNDECORATED);
        mainStage = stage;
        mainStage.setResizable(false);
        mainStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                log.info("game is closing");
                
                dbHandler.deleteAllGamesWithGid(currentGID);
                
                if(serverThread!=null) {
                	serverThread.quit(false);
                }
                if(clientThread!=null) {
                	clientThread.quit(false);
                }
                if(timer!=null) timer.stop();
                log.info("shutdown complete");
                Platform.exit();
            }
        });  
        loginScene();
    }
	
	
	// closes the application (used only by QUIT button in main menu)
	public void close() {	
		mainStage.getOnCloseRequest()
	    .handle(
	        new WindowEvent(
	            mainStage,
	            WindowEvent.WINDOW_CLOSE_REQUEST
	        )
	    );
	}
	
	
	// sets up the game interface after connecting two players
	private void setGame(int playerS)  {
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
		
		mapka = getImageFromArray(mapToColor());
		iv = new ImageView();
        iv.setImage(mapka);
        
        // top menu
        turnText = new Text("you shouldn't be seeing this");
        turnText.setFont(Font.font ("Verdana", 20));
        turnText.setTextAlignment(TextAlignment.CENTER);
        turnText.setWrappingWidth(600);
        Button button = new Button("Quit to menu");
      
        HBox topMenuHBox = new HBox(button, turnText);
        topMenuHBox.setPrefWidth(width);
        
        StackPane stackPane = new StackPane();
        
        //gathers all javafx objects in a stack pane
        stackPane.getChildren().addAll(iv, p1Arta.getBulletHBox(), p2Arta.getBulletHBox(), p1Arta.getHpLabel(), p2Arta.getHpLabel(), topMenuHBox, player.getLSPCircle());
        
        Scene scene = new Scene(stackPane, width, height);
        mainStage.setScene(scene);
        mainStage.show();
        
        stackPane.setOnMouseClicked(new EventHandler<MouseEvent>() { // hook up a mouse event for shooting
            @Override
            public void handle(MouseEvent event) {
            	switch(state) {
            	case CANSHOOT:
            		if(myTurn==false || !stable) {
            			return;
            		}
            		shoot(player, event.getX(), event.getY());
                    if(playerS==1) {
                    	serverThread.sendShot(event.getX(), event.getY());
                    	log.info("sent shot to client");
                    } else {
                    	clientThread.sendShot(event.getX(), event.getY());
                    	log.info("sent shot to server");
                    }
                    break;
            	default:
            		log.debug("No onMouseClicked action for MapState " + state);
            		break;
            	}
            }
        });
        
        scene.setOnKeyPressed(e -> {  // hook up keyboard clicks
    		KeyCode k = e.getCode();
            switch(k) {
        	case W:
        		log.info("velocity zero: " + player.higherV());
            	return;
        	case S:
        		log.info("velocity zero: " + player.lowerV());
        		return;
            default:
            	break;
        	}
        });
        
        state = MapState.LOADEDMAP;
        
        updateMap updateMapRunnable = new updateMap(); // refreshes all the randomized sand
        
        timer = new AnimationTimer() {
        	@Override
        	public void handle(long now) {
        		updateMapRunnable.run();
        	}
        };
        timer.start();
        
        button.setOnAction((event) -> {	// responsible for quitting to menu
        	try {
        		if(serverThread != null) serverThread.quit(true);
        		if(clientThread != null) clientThread.quit(true);
        		mainMenu();
        		state = MapState.ENTRY;
        		stable = false;
        		map = new boolean[height*width];
        		timer.stop();
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        	log.info("player quit the game to menu");
        	return;
        });
    }
	
	
	// usable when getting power from external source (enemy's power UPDATE is NOT received)
	private void shoot(Arta who, double x, double y, int power) {	
		state = MapState.BULLETFLYING;
    	who.shootAt(x, y, power);
	}
	
	
	// shoots with default power
	private void shoot(Arta who, double x, double y) {	
		state = MapState.BULLETFLYING;
		who.shootAt(x,y);
	}
	
	
	// refreshes the map, player's and bullet positions, detects bullets collision with terrain
	private class updateMap implements Runnable {	
		@Override
        public void run() {
            if(!stable) {	// only works when stable is set to false
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
            	mapka = getImageFromArray(mapToColor());
            	iv.setImage(mapka);
            	stable = stabletemp;
            }
            
            if(state == MapState.ENTRY && stable) {	// end the entry stage (sand falling down)
            	state = MapState.CANSHOOT;
            	log.info("state set to " + state);
            }
            
            if(state == MapState.BULLETFLYING) {	// update bullet's position
            	Arta who = (myTurn) ? player : enemy;
            	stable = false;
            	int bpos = who.bullet.tick();

            	if(bpos == -1) {
            		log.info("bullet flew out of bounds");
            		switchTurns();
            		who.returnBullet();
            		state = MapState.CANSHOOT;
            		stable = true;
            		return;
            	}
            	
            	if(bpos>0 && map[bpos]==true) {
            		recieveHit(bpos%width, bpos/width);
            		who.returnBullet();
            		state = MapState.CANSHOOT;
            	}
            }
        }
		
	}
	
	
	// updates text at the top
	private void updateTurnText(String text, Color color) {
		turnText.setFill(color);
		turnText.setText(text);
	}
	
	
	// handles terrain destruction and player damage after bullet landing at [x,y]
	public void recieveHit(int x, int y) {
		double enemyMultiplier, playerMultiplier;
		if(myTurn) {	// player takes only 60% self-inflicted damage
			playerMultiplier = 0.6;
			enemyMultiplier = 1;
		} else {
			playerMultiplier = 1;
			enemyMultiplier = 0.6;
		}
		
		log.info("hit " + x + " " + y);
		
		// enemy hit
		int xDist = x-enemy.x();
		int yDist = y-enemy.y();
		int dist  = (int) Math.sqrt(xDist*xDist+yDist*yDist);
		if (dist < splashRadius)
			enemy.removeHP(maxDmg*(int)((splashRadius-dist)*enemyMultiplier)/splashRadius);
		
		// player hit
		xDist = x-player.x();
		yDist = y-player.y();
		dist  = (int) Math.sqrt(xDist*xDist+yDist*yDist);
		if (dist < splashRadius)
			player.removeHP(maxDmg*(int)((splashRadius-dist)*playerMultiplier)/splashRadius);
		
		// remove sand that was destroyed
		for(int i=-splashRadius; i<=splashRadius; i++) {
			for(int j=-splashRadius; j<=splashRadius; j++) {
				int nx = x+i, ny= y+j;
				if(ny<height-20 && isLegal(nx, ny) && ((nx-x)*(nx-x)+(ny-y)*(ny-y))<splashRadius*splashRadius) {
					map[xyToPos(nx,ny)] = false;
				}
			}
		}
		
		checkWinners();
		stable = false;	// to initiate sand falling (updateMap will start working)
		switchTurns();
	}
	
	
	// checks if anyone has won
	private void checkWinners() {
		if(player.hp<=0) {
			if(serverThread!=null) {
				dbHandler.updateGameWon(currentGID, loggedPID, enemyPID, mapSeed, 1);
				mmCtrl.loserScreen(loggedPID, enemyPID);
				serverThread.quit(false);
			}
			if(clientThread!=null) {
				mmCtrl.loserScreen(loggedPID, enemyPID);
				clientThread.quit(false);
			}
			
			
			mainMenu();			// TODO make a reset method
    		state = MapState.ENTRY;
    		stable = false;
    		map = new boolean[height*width];
    		timer.stop();
		}
		
		if(enemy.hp<=0) {
			if(serverThread!=null) {
				dbHandler.updateGameWon(currentGID, loggedPID, enemyPID, mapSeed, 0);
				mmCtrl.winnerScreen(loggedPID, enemyPID);
				serverThread.quit(false);
			}
			if(clientThread!=null) {
				mmCtrl.winnerScreen(loggedPID, enemyPID);
				clientThread.quit(false);
			}
			
			
			mainMenu();
    		state = MapState.ENTRY;
    		stable = false;
    		map = new boolean[height*width];
    		timer.stop();
		}
	}
	
	// checks if [x,y] position is not out of bounds
	private boolean isLegal(int x, int y) {
		if(x<0 || x>=width || y<0 || y>=height) return false;
		return true;
	}
	
	// converts (x,y) position to a singular pixel number
	private int xyToPos(int x, int y) {
		return y*width+x;
	}
	
	// returns 0 if position below a terrain-pixel is taken by another terrain-pixel, 1 otherwise
	private int below(int pos) {
		if(pos/width==height-1) {	// if its the last line
			return 0;
		}
		if(map[pos+width]==true) {
			return 0;
		}
		return 1;
	}
	
	// switches turns, so player can shoot or to block his shooting attempts
	private void switchTurns() {
		if(myTurn) {
			state = MapState.W84PLAYER;	// enemy turn
			myTurn = false;
			updateTurnText("Opponent's turn", Color.WHITE);
		} else {
			state = MapState.CANSHOOT;
			myTurn = true;
			updateTurnText("Your turn", Color.WHITE);
		}
	}
	
	// generates "random" (not yet) terrain, needs to be seed-based
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
				for(double y=height/2; y<height-20; y++) { // real height: height-1-y
					map[xyToPos((int)x,(int)y)] = Math.abs(Math.cos(x*Math.PI*2/width))*height/2>height-y ? true : false;
				}
				for(int y=height-20; y<height; y++) {
					map[xyToPos((int)x,(int)y)] = true;
				}
			}
			break;
		default:
			System.out.println("Incorrect generation type");
			break;
		}
	}
	
	// converts a bool[] terrain map into an int[] pixel map, basically colorizes
	public int[] mapToColor() {
		int[] colorMap = new int[height*width];
		for(int i=height-20; i<height*width; i++) {
			colorMap[i]=0xFFd6d6d6;
		}
		for(int i=0; i<(height-20)*width; i++) {
			if(map[i]==true)
				colorMap[i]=0xFFffb366;//992255;
			else
				colorMap[i]=0xFF130323;
		}
		return colorMap;
	}
	
	// converts a pixel map (int[]) into  an image
	private Image getImageFromArray(int[] pixels) {
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
