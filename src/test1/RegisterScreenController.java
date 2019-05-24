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

public class RegisterScreenController {
	Sand parent;
	
	@FXML
	private TextField login;
	
    @FXML
    private TextField password;

    @FXML
    private Text passNoMatch;

    @FXML
    private Button submit;

    @FXML
    private TextField passwordCheck;

    @FXML
    private Button backToLogin;

    @FXML
    private Text loginTaken;

    @FXML
    void submitRegister(ActionEvent event) {
    	//TODO read all 3 fields to static variables, so u cant change them during algorithm
    	Connection connect;
    	if(password.getText().length()<3 || passwordCheck.getText().length()<3) {
    		passNoMatch.setText("Passwords need to be 3 characters minimum");
    		passNoMatch.setVisible(true);
    		return;
    	}
    	
    	if(!password.getText().equals(passwordCheck.getText())) {
    		passNoMatch.setText("Passwords do not match!");
    		passNoMatch.setVisible(true);
    		return;
    	}
    	
    	try {	// try connecting to DB
    		Class.forName("com.mysql.cj.jdbc.Driver");
    		// Setup the connection with the DB
    		connect = DriverManager.getConnection("jdbc:mysql://remotemysql.com:3306/"
    				+ "?" + "useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=CET&user=44MpGSOxW4&password=vCu05UiHkx");
    	
    	     //connected to DB
    		String select = "SELECT COUNT(*), pid FROM players WHERE name = ? GROUP BY pid";
    		PreparedStatement preparedStatement = connect.prepareStatement(select);
    		preparedStatement.setString(1, login.getText());
    		ResultSet resultSet = preparedStatement.executeQuery();
    		
    		if(resultSet.next()==true && resultSet.getInt(1)==1) {
    			loginTaken.setVisible(true);
    			System.out.println("Same name at PID " + resultSet.getInt(2));
    		} else {
    			select = "INSERT INTO players VALUES (default, ?, ?)";
    			preparedStatement = connect.prepareStatement(select);
    			preparedStatement.setString(1, login.getText());
    			preparedStatement.setString(2, password.getText());
    			preparedStatement.executeUpdate();
    			// get fresh PID
    			select = "SELECT COUNT(*), pid FROM players WHERE name = ? AND passwd = ? GROUP BY pid";
        		preparedStatement = connect.prepareStatement(select);
        		preparedStatement.setString(1, login.getText());
        		preparedStatement.setString(2, password.getText());
        		resultSet = preparedStatement.executeQuery();
        		if(resultSet.next()==true && resultSet.getInt(1)==1) {
        			System.out.println("Added user " + resultSet.getInt(2) + "/\"" + login + "\"" + password);
        			parent.loggedPID = resultSet.getInt(2);
        			parent.mainMenu();
        		}
    		}
    		connect.close();
    	} catch(SQLException a) {
    		System.out.println("Error connecting to database or executing task: " + a.getErrorCode());
    	
    	} catch(Exception a) {
    		System.out.println("MySQL connector class missing.");
    	}

    	
    }

    @FXML
    void gotoLogin(ActionEvent event) {
    	parent.loginScene();
    }
    
    public void initialize() {
    	passNoMatch.setVisible(false);
    	loginTaken.setVisible(false);
    	login.focusedProperty().addListener((ov, oldV, newV) -> {
    		if (!newV) { // focus lost
    			try {	// try connecting to DB
    	    		Class.forName("com.mysql.cj.jdbc.Driver");
    	    		// Setup the connection with the DB
    	    		Connection connect = DriverManager.getConnection("jdbc:mysql://remotemysql.com:3306/44MpGSOxW4?" + "useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=CET&user=44MpGSOxW4&password=vCu05UiHkx");
    	    	
    	    	     //connected to DB
    	    		String select = "SELECT COUNT(*), pid FROM players WHERE name = ? GROUP BY pid";
    	    		PreparedStatement preparedStatement = connect.prepareStatement(select);
    	    		preparedStatement.setString(1, login.getText());
    	    		ResultSet resultSet = preparedStatement.executeQuery();
    	    		
    	    		if(resultSet.next()==true && resultSet.getInt(1)==1) {
    	    			loginTaken.setVisible(true);
    	    			System.out.println("Same name at PID " + resultSet.getInt(2));
    	    		} else {
    	    			loginTaken.setVisible(false);
    	    		}
    	    		connect.close();
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    	});
    	
    	passwordCheck.focusedProperty().addListener((ov, oldV, newV) -> {
    		if (!newV) { // focus lost

    	    	passNoMatch.setVisible(false);
    			if(password.getText().length()<3 || passwordCheck.getText().length()<3) {
    	    		passNoMatch.setText("Passwords need to be 3 characters minimum");
    	    		passNoMatch.setVisible(true);
    	    		return;
    	    	}
    	    	
    	    	if(!password.getText().equals(passwordCheck.getText())) {
    	    		passNoMatch.setText("Passwords do not match!");
    	    		passNoMatch.setVisible(true);
    	    		return;
    	    	}
    	    }
    	});
    }
    
    protected void setParent(Sand prt) {
    	parent = prt;
    }
}
