package main;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class RegisterScreenController {
	Sand parent;
	DbHandler dbHandler;
	Logger log;
	
	@FXML
	private TextField login;
	
    @FXML
    private PasswordField password;

    @FXML
    private Text passNoMatch;

    @FXML
    private Button submit;

    @FXML
    private PasswordField passwordCheck;

    @FXML
    private Button backToLogin;

    @FXML
    private Text loginTaken;

    @FXML
    void submitRegister(ActionEvent event) {
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
    	
    	int maybePID = dbHandler.register(login.getText(), password.getText());
    	switch(maybePID) {
    	case -1:	// username exists
    		loginTaken.setVisible(true);
    		break;
    	case -2:
    		log.error("unkown registering error");
    		break;
    	default:	// it means we got a correct pid after registering
    		parent.loggedPID = maybePID;
			parent.mainMenu();
    		break;
    	}
    }

    @FXML
    void gotoLogin(ActionEvent event) {
    	parent.loginScene();
    }
    
    public void initialize() {
    	log = LogManager.getRootLogger();
    	passNoMatch.setVisible(false);
    	loginTaken.setVisible(false);
    	login.focusedProperty().addListener((ov, oldV, newV) -> {
    		if (!newV) { // focus lost
    			if(dbHandler.loginExists(login.getText())) {
    				loginTaken.setVisible(true);
    			} else {
    				loginTaken.setVisible(false);
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
    
    protected void setParent(Sand prt, DbHandler dbh) {
    	parent = prt;
    	dbHandler = dbh;
    }
}
