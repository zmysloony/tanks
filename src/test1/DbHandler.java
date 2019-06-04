package test1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbHandler {
	Connection connect;
	
	DbHandler() {	// sets up initial database connection
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			connect = DriverManager.getConnection("jdbc:mysql://remotemysql.com:3306/44MpGSOxW4?" + "useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=CET&user=44MpGSOxW4&password=vCu05UiHkx");
			System.out.println("DB connection established.");
		} catch (Exception e) {
			System.out.println("Error while making initial DB connection.");
		}
	}
	
	public void deleteAllGamesWithPid(int pid) {
		String select = "DELETE FROM games WHERE p1_id = ? OR p2_id = ?";
		try {
			PreparedStatement preparedStatement = connect.prepareStatement(select);
			preparedStatement.setInt(1, pid);
			preparedStatement.setInt(2, pid);
			preparedStatement.executeUpdate();	// added game record to DB
		}  catch (SQLException e) {
			System.out.println("Problem when deleting games with PID " + pid + ".");
		}
	}
	
	public void deleteAllGamesWithGid(int gid) {
		 String deleteActiveGame = "DELETE FROM games WHERE gid = " + gid;
         try {
         	Statement statement = connect.createStatement();
         	statement.executeUpdate(deleteActiveGame);
         } catch (SQLException e) {
         	System.out.println("Error when deleting from games by GID!");
         }
	}
	
	public void addNewGame(int p1_id) {
		
		String select = "INSERT INTO games VALUES (default, ?, ?, 3, 'placeholder', 1, 'OPEN', default)";
		try {
			PreparedStatement preparedStatement = connect.prepareStatement(select);
			preparedStatement.setInt(1, p1_id);
			preparedStatement.setInt(2, p1_id);
			preparedStatement.executeUpdate();	// added game record to DB
		} catch (SQLException e) {
			System.out.println("DB error while creating a new game.");
		}
	}
	
	public int getGidByPid(int p1_id) { // only works for player1 id
		String select = "SELECT gid FROM games WHERE p1_id=?";
		try {
			PreparedStatement preparedStatement = connect.prepareStatement(select);
			preparedStatement.setInt(1, p1_id);
			ResultSet rs = preparedStatement.executeQuery();
			rs.next();
			return rs.getInt(1);
		} catch (SQLException e) {
			System.out.println("Error getting GID by logged PID.");
		}
		return -1;
	}
	
	public void updateSecondPlayer(int enemyPID, int gid) {
		String select = "UPDATE games SET p2_id = ?, state = 'IN_PROGRESS' WHERE gid = ?";
		try {
			PreparedStatement preparedStatement = connect.prepareStatement(select);
			preparedStatement.setInt(1, enemyPID);
			preparedStatement.setInt(2, gid);
			preparedStatement.executeUpdate();	// added game record to DB
		} catch (SQLException e) {
			System.out.println("Couldn't update second players' PID in DB.");
		}
	}
	
	public void updateGameWon(int gid, int p1_id, int p2_id, int mapSeed, int winner) {
		String select = "INSERT INTO games_history VALUES (?, ?, ?, ?, ?)";
		try {
			PreparedStatement preparedStatement = connect.prepareStatement(select);
			preparedStatement.setInt(1, gid);
			preparedStatement.setInt(2, p1_id);
			preparedStatement.setInt(3, p2_id);
			preparedStatement.setInt(4, mapSeed);
			preparedStatement.setInt(5, (winner==0)? p1_id : p2_id);
			preparedStatement.executeUpdate();	
		} catch (SQLException e) {
			System.out.println("Error inserting a finished game into database.");
		}
	}
	
	public double getWinrate(int pid) {
		String playerGames = "SELECT ROUND(100*COUNT(*)/(SELECT COUNT(*) FROM games_history WHERE p1_id = p.winner OR p2_id = p.winner),2)"
							+ " FROM games_history p WHERE p.winner = " + pid;
		try {
			Statement statement = connect.createStatement();
			ResultSet rs = statement.executeQuery(playerGames);
			rs.next();
			return rs.getDouble(1);
		} catch (SQLException e) {
			System.out.println("Error getting player's " + pid + " winrate.");
		}
		return -1;
	}
}
