package test1;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Arc;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class ServerSelectionController {
	Sand parent;
	Connection connect;
	
	@FXML
	public void initialize() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			// Setup the connection with the DB
			connect = DriverManager.getConnection("jdbc:mysql://remotemysql.com:3306/44MpGSOxW4?" + "useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=CET&user=44MpGSOxW4&password=vCu05UiHkx");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    @FXML
    private AnchorPane anchorPane;

    @FXML
    private Arc rotor;
    
    @FXML
    void goBack(ActionEvent event) {
    	parent.mainMenu();
    }
    
    private void rotateStart() {
    	

        rotor.setVisible(true);
    	RotateTransition rt = new RotateTransition(Duration.millis(5000), rotor);
        rt.setByAngle(800);
        rt.setAutoReverse(true);
     
        rt.play();
    }
    
    public void rotateStop() {
    	rotor.setVisible(false);
    }
    
    @FXML
    void refreshList(ActionEvent event) {
    	rotateStart();
    	anchorPane.getChildren().clear();
    	GridPane gridPane = new GridPane();
    	gridPane.setHgap(10);
    	gridPane.setVgap(3);
    	Text playerText = new Text("Player name:");
    	playerText.setStyle("-fx-font-weight: bold");
    	gridPane.add(playerText, 0, 0);
    	Text serverText = new Text("Server address:");
    	serverText.setStyle("-fx-font-weight: bold");
    	gridPane.add(serverText, 1, 0);
    	anchorPane.getChildren().add(gridPane);
    	
    	ListUpdater listUpdater = new ListUpdater(gridPane);
    	listUpdater.start();
    	//Platform.runLater(listUpdater);
    		
    }
    
   
    class ListUpdater extends Thread {
		GridPane gp;
		
		ListUpdater(GridPane grp) {
			gp = grp;
		}
		
		String pidToName(int pid) {
			try {
				String select = "SELECT name FROM players WHERE pid = ?";
				PreparedStatement preparedStatement = connect.prepareStatement(select);
				preparedStatement.setInt(1, pid);
				ResultSet rs = preparedStatement.executeQuery();
				rs.next();
				String name = rs.getString(1);
				return name;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "DATABASE CONNECTION ERROR";
		}
		
		@Override
		public void run() {
			
			String lanIp = parent.getLanIp();
			int howMany = 0;
			for (int ip=1; ip<255; ip++) {
				//for(int port=44020; port<=44020; port++) {	// slow searching, so only 1 port available
				int port = 44020; // TODO make if flexible
				int maybePID = parent.isServer(lanIp+ip, port);
				if(maybePID!=-1) {
					int current = ++howMany;
					System.out.println("Found LAN server: " + lanIp + ip);
					Button connButton = new Button(lanIp+ip);
					Text playerText = new Text(pidToName(maybePID));
					String total = lanIp + ip;
					connButton.setOnAction((event) -> {
				       	System.out.println("trying to connect to " + total);
				       	parent.startClient(total, port, maybePID);
				       	System.out.println("exited server selection screen");
				        return;
				    });
					// have to run later from application thread to update GUI
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							gp.add(playerText, 0, current);
							gp.add(connButton, 1, current);
						}
					});
				}
			}
			System.out.println("LAN search finished");
			rotateStop();
    	// TODO real server
    	/*
    	ArrayList<Integer> toDelete = new ArrayList<Integer>();
    	do {
    		if(parent.isServer(rs.getString(4), rs.getInt(5))==false) {	// check if servers are open
    			toDelete.add(rs.getInt(1));
    			continue;
    		}
    		Button connectButton = new Button("Connect");
    		Text ip = new Text(rs.getString(4) + ":" + rs.getInt(5));
    		Text serverUser = new Text(rs.getString(2));
    		HBox line = new HBox(serverUser, ip, connectButton);
        	vb.getChildren().add(line);
    	} while (rs.next());
    	if(toDelete.size()>0) {
    		String deleteQuery = "DELETE FROM games WHERE (UNIX_TIMESTAMP(CURRENT_TIMESTAMP)-UNIX_TIMESTAMP(created)>300) AND gid IN (";
    		for(int i=0; i<toDelete.size(); i++) {
    			if(i!=0) {
    				deleteQuery += ",";
    			}
    			deleteQuery += toDelete.get(i);
    		}
    		deleteQuery += ")";
    		System.out.println(deleteQuery);
        	Statement statement = connect.createStatement();
        	statement.executeUpdate(deleteQuery);
    	}
    	*/
		}
    }
    
    protected void setParent(Sand prt) {
    	parent = prt;
    }
}
