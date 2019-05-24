package test1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class LoginScreenController {
	private Sand parent;
	
	@FXML
	private Text comms;
	
	@FXML
	private TextField password;

	@FXML
	private Button submit;

	@FXML
	private TextField login;

	@FXML
	private Button register;

	@FXML
	void submitLogin(ActionEvent event) {
		Connection connect;
    	try {	// try connecting to DB
    		Class.forName("com.mysql.cj.jdbc.Driver");
    		// Setup the connection with the DB
    		connect = DriverManager.getConnection("jdbc:mysql://remotemysql.com:3306/44MpGSOxW4?" + "useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=CET&user=44MpGSOxW4&password=vCu05UiHkx");
    	
    	     //connected to DB
    		String select = "SELECT COUNT(*), pid FROM players WHERE name = ? AND passwd = ? GROUP BY pid";
    		PreparedStatement preparedStatement = connect.prepareStatement(select);
    		preparedStatement.setString(1, login.getText());
    		preparedStatement.setString(2, password.getText());
    		ResultSet resultSet = preparedStatement.executeQuery();
    		
    		if(resultSet.next()==true && resultSet.getInt(1)==1) {
    			parent.loggedPID = resultSet.getInt(2);
    			
    			parent.mainMenu();
    			System.out.println("(login screen) exit");
    		} else {
    			comms.setText("Wrong password or user!");
    		}
    		connect.close();
    	} catch(SQLException a) {
    		System.out.println("Error connecting to database or executing task: " + a.getErrorCode());
    	
    	} catch(Exception a) {
    		System.out.println("MySQL connector class missing.");
    	}

	}

	@FXML
	void gotoRegister(ActionEvent event) {
		parent.registerScene();
	}


    protected void setParent(Sand prt) {
    	parent = prt;
    	comms.setText("");
    }
}