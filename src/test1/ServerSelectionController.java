package test1;

import java.sql.Connection;
import java.util.ArrayList;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class ServerSelectionController {
	Sand parent;
	
    @FXML
    private AnchorPane anchorPane;

    @FXML
    void goBack(ActionEvent event) {
    	parent.mainMenu();
    }

    @FXML
    void refreshList(ActionEvent event) {
    	anchorPane.getChildren().clear();
    	
    	try {	// try connecting to DB
    		Class.forName("com.mysql.cj.jdbc.Driver");
    		// Setup the connection with the DB
    		Connection connect = DriverManager.getConnection("jdbc:mysql://remotemysql.com:3306/44MpGSOxW4?" + "useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=CET&user=44MpGSOxW4&password=vCu05UiHkx");
    	
    	     //connected to DB
    		String select = "SELECT gid, players.name, seed, server_ip, server_port, created FROM games INNER JOIN players ON games.p1_id=players.pid WHERE state = 'OPEN'";
    		Statement statement = connect.createStatement();
    		ResultSet resultSet = statement.executeQuery(select);
    		
    		if(resultSet.next()==false) {
    			Text text = new Text("No servers available at the time!");
    			anchorPane.getChildren().add(text);
    		} else {
    			
    			anchorPane.getChildren().add(makeList(connect, resultSet));
    		}
    		
    		
    		connect.close();
		} catch (SQLException e) {
			System.out.println(e.getErrorCode());
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    protected VBox makeList(Connection connect, ResultSet rs) throws SQLException {
    	VBox vb = new VBox();
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
    	return vb;
    }
    
    protected void setParent(Sand prt) {
    	parent = prt;
    }
}
