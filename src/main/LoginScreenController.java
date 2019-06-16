package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class LoginScreenController {
	private Sand parent;
	private DbHandler dbHandler;
	private Logger log;
	
	@FXML
	private Text comms;
	
	@FXML
	private PasswordField password;

	@FXML
	private Button submit;

	@FXML
	private TextField login;

	@FXML
	private Button register;

	@FXML
	void initialize() {
		log = LogManager.getRootLogger();
    	comms.setText("");
	}
	
	@FXML
	void submitLogin(ActionEvent event) {
    	int maybePID = dbHandler.login(login.getText(), password.getText());
    	if(maybePID == -1) {
    		comms.setText("Wrong password or user!");
    	} else if (maybePID == -2) {
    		comms.setText("Error connecting to database.");
    	} else {
    		parent.loggedPID = maybePID;
    		parent.mainMenu();
    		log.info("logged in user " + maybePID);
    	}
	}

	@FXML
	void gotoRegister(ActionEvent event) {
		parent.registerScene();
	}


    protected void setParent(Sand prt, DbHandler dbh) {
    	parent = prt;
    	dbHandler = dbh;
    }
}