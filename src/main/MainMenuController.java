package main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

public class MainMenuController {
	Sand parent;
	DbHandler dbHandler;
	
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
    
    protected void setParent(Sand prt, DbHandler dbh) {
    	parent = prt;
    	dbHandler = dbh;
    }
    
    @FXML
    private Pane youWonPane;
    
    @FXML
    private Text youWhat;

    @FXML
    private Text playerWr;

    @FXML
    private Text enemyWr;

    @FXML
    void hideYouWon(ActionEvent event) {
    	youWonPane.setVisible(false);
    }
    
    @FXML
    void initialize() {
    	youWonPane.setVisible(false);
    }
    
    void showYouWon() {
    	youWonPane.setVisible(true);
    }
    
    void enemyQuit(int player, int enemy) {
    	setWinrates(player, enemy);
    	youWhat.setText("Enemy quit :/");
    	youWonPane.setVisible(true);
    }
    
    void setWinrates(int player, int enemy) {
    	playerWr.setText((double)dbHandler.getWinrate(player) + "%");
    	enemyWr.setText((double)dbHandler.getWinrate(enemy) + "%");
    }
    
    void loserScreen(int player, int enemy) {
    	setWinrates(player, enemy);
    	youWhat.setText("You lost :(");
    	youWonPane.setVisible(true);
    }
    
    void winnerScreen(int player, int enemy) {
    	setWinrates(player, enemy);
    	youWhat.setText("You won!");
    	youWonPane.setVisible(true);
    }
    
    @FXML
    void quitClicked(ActionEvent event) {
    	parent.close();
    }
}