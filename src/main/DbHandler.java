package main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DbHandler {
	Connection connect;
	Logger log;
	
	
	// sets up initial database connection
	DbHandler() {	
		log = LogManager.getRootLogger();
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			connect = DriverManager.getConnection("jdbc:mysql://remotemysql.com:3306/44MpGSOxW4?" + "useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=CET&user=44MpGSOxW4&password=vCu05UiHkx");
			System.out.println("DB connection established.");
		} catch (Exception e) {
			log.fatal("Error while making initial DB connection.");
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
			log.error("DB error: deleteAllGamesWithPid (code " + e.getErrorCode() + ")");
		}
	}
	
	public void deleteAllGamesWithGid(int gid) {
		 String deleteActiveGame = "DELETE FROM games WHERE gid = " + gid;
         try {
         	Statement statement = connect.createStatement();
         	statement.executeUpdate(deleteActiveGame);
         } catch (SQLException e) {
        	log.error("DB error: deleteAllGamesWithGid (code " + e.getErrorCode() + ")");
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
			log.error("DB error: addNewGame (code " + e.getErrorCode() + ")");
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
			log.error("DB error: getGidByPid (code " + e.getErrorCode() + ")");
		}
		return -1;
	}
	
	public void updateSecondPlayer(int enemyPID, int gid) {
		String select = "UPDATE games SET p2_id = ?, state = 'IN_PROGRESS' WHERE gid = ?";
		try {
			PreparedStatement preparedStatement = connect.prepareStatement(select);
			preparedStatement.setInt(1, enemyPID);
			preparedStatement.setInt(2, gid);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			log.error("DB error: updateSecondPlayer (code " + e.getErrorCode() + ")");
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
			log.error("DB error: updateGameWon (code " + e.getErrorCode() + ")");
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
			log.error("DB error: getWinrate (code " + e.getErrorCode() + ")");
		}
		return -1;
	}
	
	public int login(String user, String password) {
		try {
			String select = "SELECT COUNT(*), pid FROM players WHERE name = ? AND passwd = ? GROUP BY pid";
			PreparedStatement preparedStatement = connect.prepareStatement(select);
			preparedStatement.setString(1, user);
			preparedStatement.setString(2, password);
			ResultSet resultSet = preparedStatement.executeQuery();
			if(resultSet.next()==true && resultSet.getInt(1)==1) {
				return resultSet.getInt(2);
			}
			return -1;
		} catch(SQLException e) {
			log.error("DB error: login (code " + e.getErrorCode() + ")");
			return -2;
		}
	}
	
	public int register(String user, String password) {
		try {
    		String select = "SELECT COUNT(*), pid FROM players WHERE name = ? GROUP BY pid";
    		PreparedStatement preparedStatement = connect.prepareStatement(select);
    		preparedStatement.setString(1, user);
    		ResultSet resultSet = preparedStatement.executeQuery();
    		
    		if(resultSet.next()==true && resultSet.getInt(1)==1) {
    			return -1; // same user name exists
    		} else {	// we can create a new user record in DB
    			select = "INSERT INTO players VALUES (default, ?, ?)";
    			preparedStatement = connect.prepareStatement(select);
    			preparedStatement.setString(1, user);
    			preparedStatement.setString(2, password);
    			preparedStatement.executeUpdate();
    			// gets new user's PID to return
    			select = "SELECT COUNT(*), pid FROM players WHERE name = ? GROUP BY pid";
        		preparedStatement = connect.prepareStatement(select);
        		preparedStatement.setString(1, user);
        		resultSet = preparedStatement.executeQuery();
        		if(resultSet.next()==true && resultSet.getInt(1)==1) {
        			log.info("added user " + resultSet.getInt(2) + "/" + user + "/" + password);
        			return resultSet.getInt(2);
        		}
    		}
    	} catch(SQLException e) {
    		log.error("DB error: register (code " + e.getErrorCode() + ")");
    	}
		
		return -2;
	}
	
	public boolean loginExists(String user) {
		try {
    		String select = "SELECT COUNT(*), pid FROM players WHERE name = ? GROUP BY pid";
    		PreparedStatement preparedStatement = connect.prepareStatement(select);
    		preparedStatement.setString(1, user);
    		ResultSet resultSet = preparedStatement.executeQuery();
    		
    		if(resultSet.next()==true && resultSet.getInt(1)==1) {
    			return true;
    		}
		} catch (SQLException e) {
			log.error("DB error: loginExists (code " + e.getErrorCode() + ")");
		}
		return false;
	}
	
	public String pidToName(int pid) {
		try {
			String select = "SELECT name FROM players WHERE pid = ?";
			PreparedStatement preparedStatement = connect.prepareStatement(select);
			preparedStatement.setInt(1, pid);
			ResultSet rs = preparedStatement.executeQuery();
			rs.next();
			String name = rs.getString(1);
			return name;
		} catch (SQLException e) {
			log.error("DB error: pidToName (code " + e.getErrorCode() + ")");
		}
		return "DATABASE CONNECTION ERROR";
	}
}
