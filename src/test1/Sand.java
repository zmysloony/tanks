package test1;

//import org.apache.log4j

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
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;





public class Sand extends Application {
	Logger log;
	int width;
	int height;
	int splashRadius;
	int maxDmg;
	
	MapState state;
	boolean myTurn;
	boolean stable;
	boolean map[];
	int mapSeed;
	Image mapka;
	ImageView iv;
	Stage mainStage;
	AnimationTimer timer;
	
	Arta p1Arta, p2Arta;
	Arta player, enemy;
	Text playerHP, enemyHP;
	
	Scene logScene, regScene, mmScene, servScene;
	
	int loggedPID, enemyPID, currentGID;
	CommThread clientThread;
	CommThread serverThread;
	DbHandler dbHandler;
	
	MainMenuController mmCtrl;			// these two are needed to display text alerts (server not found etc)
	ServerSelectionController ssCtrl;
	
	enum MapState {
		W84PLAYER, LOADEDMAP, ENTRY, CANSHOOT, BULLETFLYING
	}
	
	enum Comm {
		SHAKE, URTURN, SHOT, ENDCONNECTION, // TODO remove shake, remove Urturn
		ERROR
	}
	
	 class CommExt {
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
	
	public Sand() {
		log = LogManager.getRootLogger();
		width = 800;
		height = 640;
		splashRadius = 40;
		
		
		maxDmg = 35;
		
		loggedPID = -1; // error state
		currentGID = -1; // error state
		enemyPID = -1;
		
		state = MapState.ENTRY;
		stable = false;
		map = new boolean[height*width];	//automatically fills with zeros
		mapSeed = 137;		// TODO generating map with seeds
		
		dbHandler = new DbHandler();
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
				ssCtrl.setParent(this);
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
		
		public CommThread(int socketNum) throws IOException {
			
			isServer = true;
			quit = false;
			//while(servSocket == null) {
				try {
					//if(socketNum>=44020) throw new IOException();
					servSocket = new ServerSocket(socketNum);
				} catch (IOException ignored) {
					log.error("socket 44020 taken, cannot start server");
					//socketNum++;
					quit = true;
				}
			//}
			
			this.setName("server thread");
			log.info("server created successfully");
		}
		
		public boolean correct() {
			return quit ? false : true;
		}
		
		public CommThread(String ip, int port) throws IOException {
			int attempt = 1;
			while(clientSocket == null && attempt < 4) {
				try {
					clientSocket = new Socket(ip, port);
					dataIn = new DataInputStream(clientSocket.getInputStream());
	                dataOut = new DataOutputStream(clientSocket.getOutputStream());
	                log.info("connected succesfully to " + ip + ":" + port);
	                dataOut.writeBoolean(true);
	                dataOut.writeInt(loggedPID);
	                log.info("sent my PID (" + loggedPID + ")");
	                quit = false;
				} catch (IOException e) {
					log.error("couldn't connect to server (maybe outdated server list), attempt "+attempt+"/3");
					attempt++;
					quit = true;
				}
			}
			log.info("constructed");
		}
		
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
		
		public void sendShot(double x, double y) {
			sendCommExt(getValue(Comm.SHOT), x, y, player.shotVelocity);
		}
		
		public void requestConnectedQuit() {
			if(dataOut==null) return;
			
			sendCommExt(getValue(Comm.ENDCONNECTION), 0, 0, 0);
		}
		
		protected CommExt getSignal() {
			CommExt fullComm = null;
			try {
				Comm commType =  toComm(dataIn.readInt());
				double x = dataIn.readDouble();
				double y = dataIn.readDouble();
				int ext = dataIn.readInt();
				fullComm = new CommExt(commType, x, y, ext);
			} catch (IOException e) {
				log.error("recieved ioexception");
			}
			return fullComm;
		}
		
		
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
	            waiter();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
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
			            	System.out.println("lol");
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
		
		private void clientSetup() {
			try {
                waiter();
			}  catch (Exception e) {
                e.printStackTrace();
            }
		}
		
		private void waiter() {		// handles in-game communication
			CommExt signal;
            while(quit == false) {
                signal = getSignal();
                log.info("recieved " + signal.task);
                // get full extended command
                switch(signal.task) {
                case SHOT:
                	while(stable==false || state==MapState.ENTRY) {	// has to wait until sand drops (when game start
                		log.info("waiting for sand to drop");		// or when player recieves shot command while sand
                	}												// is still falling
                	state = MapState.BULLETFLYING;
                	enemy.shootAt(signal.x, signal.y, signal.ext);
                	break;
                case ENDCONNECTION:
                	System.out.println("Enemy quit the game.");
                	Platform.runLater(
                			() -> {
                            	mmCtrl.enemyQuit(loggedPID, enemyPID);
                				mainMenu();
                			}
                	);
                	quit(false);
                	
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
			} catch (Exception e) {
				System.out.println("Exception while closing sockets");
				e.printStackTrace();
			}
        	quit = true;
        }
	}
	
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
	
	public String getLanIp() {
		return cutSubnet(getLocalIp());
	}
	
	public String getLocalIp() {
		/*try {
			
			InetAddress localHost = InetAddress.getLocalHost();
			//NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
			return localHost.getHostAddress();
			//System.out.println(networkInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength()); // gets subnet
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		try(final DatagramSocket socket = new DatagramSocket()){
			  socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			  return socket.getLocalAddress().getHostAddress();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Error getting local IP address.";
	}
	
	public int isServer(String ip, int port) {
		Socket tester = new Socket();
		int maybePID;
		try {
			//System.out.println(ip + " " + port);
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
			//e.printStackTrace();
			return -1;
		}
		
		return maybePID;
	}
	
	public void startServer() {
		System.out.println("System IP Address : " + getLocalIp());
	
	        
		try {
    		serverThread = new CommThread(44020);
    		System.out.println("(main menu) server starting done");
    		if(serverThread.correct()) {
    			serverThread.start();
        		setGame(1);
    		} else {
    			System.out.println("Couldn't create server correctly.");
    		}
    	} catch (Exception a) {
    		a.printStackTrace();
    	}
	}
	
	public void startClient(String ip, int port, int newPid) {
		try {
    		clientThread = new CommThread(ip, port);
    		System.out.println("(main menu) client starting done");
    		if(clientThread.correct()) {
        		setGame(2);
        		clientThread.start();
    		} else {
    			System.out.println("Couldn't establish connection to server (maybe update server list before connecting?)");
    			ssCtrl.refreshList(null);
    		}
    		enemyPID = newPid;
    	} catch (Exception a) {
    		a.printStackTrace();
    	}
	}
	
	protected static int getValue(Comm comm) {
		switch(comm) {
		case SHAKE:
			return 1;
		case URTURN:
			return 2;
		case SHOT:
			return 3;
		case ENDCONNECTION:
			return 4;
		default:
			return -1;
		}
	}
	
	protected static Comm toComm(int comm) {
		switch(comm) {
		case 1:
			return Comm.SHAKE;
		case 2:
			return Comm.URTURN;
		case 3:
			return Comm.SHOT;
		case 4:
			return Comm.ENDCONNECTION;
		default:
			return Comm.ERROR;
		}
	}
	
	
	
	
	@Override
    public void start(Stage stage) throws InterruptedException {
		stage.setTitle("105 leFH18B2");
		//stage.initStyle(StageStyle.UNDECORATED);
        mainStage = stage;
        mainStage.setResizable(false);
        mainStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                System.out.println("Stage is closing");
                
                dbHandler.deleteAllGamesWithGid(currentGID);
                
                if(serverThread!=null) {
                	serverThread.quit(false);
                }
                if(clientThread!=null) {
                	clientThread.quit(false);
                }
                if(timer!=null) timer.stop();
                Platform.exit();
                
            }
        });  
        loginScene();
    }
	
	public void close() {
		mainStage.getOnCloseRequest()
	    .handle(
	        new WindowEvent(
	            mainStage,
	            WindowEvent.WINDOW_CLOSE_REQUEST
	        )
	    );
	}
	
	
	protected void setGame(int playerS) throws InterruptedException {
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
        
        button.setOnAction((event) -> {
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
        	System.out.println("went back to menu");
        	return;
        });
    }
	
	
	class updateMap implements Runnable {
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
            		switchTurns();
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
		
		
		checkWinners();
		stable = false;
		switchTurns();
	}
	
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
