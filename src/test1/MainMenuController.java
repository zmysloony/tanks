package test1;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class MainMenuController {
	Sand parent;
	
	
    @FXML
    private Button newGame;

    @FXML
    private Button connect;

    @FXML
    void newGameClicked(ActionEvent event) {
    	parent.startServer();
    }

    @FXML
    void connectClicked(ActionEvent event) {
    	parent.serverSelectionScene();
    }
    
    protected void setParent(Sand prt) {
    	parent = prt;
    }

}