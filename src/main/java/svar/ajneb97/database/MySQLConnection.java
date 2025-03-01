package svar.ajneb97.database;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import svar.ajneb97.ServerVariables;
import svar.ajneb97.managers.MessagesManager;
import svar.ajneb97.model.ServerVariablesPlayer;
import svar.ajneb97.model.ServerVariablesVariable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MySQLConnection {

    private ServerVariables plugin;
    private HikariConnection connection;
    private String host;
    private String database;
    private String username;
    private String password;
    private int port;

    public MySQLConnection(ServerVariables plugin){
        this.plugin = plugin;
    }

    public void setupMySql(){
        FileConfiguration config = plugin.getConfigsManager().getMainConfigManager().getConfigFile().getConfig();
        try {
            host = config.getString("config.mysql_database.host");
            port = Integer.valueOf(config.getString("config.mysql_database.port"));
            database = config.getString("config.mysql_database.database");
            username = config.getString("config.mysql_database.username");
            password = config.getString("config.mysql_database.password");
            connection = new HikariConnection(host,port,database,username,password);
            connection.getHikari().getConnection();
            createTables();
            loadData();
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(plugin.prefix+" &aSuccessfully connected to the Database."));
        }catch(Exception e) {
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(plugin.prefix+" &cError while connecting to the Database."));
        }
    }


    public String getDatabase() {
        return this.database;
    }

    public Connection getConnection() {
        try {
            return connection.getHikari().getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void loadData(){
        ArrayList<ServerVariablesPlayer> players = new ArrayList<>();
        try(Connection connection = getConnection()){
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT servervariables_players.UUID, servervariables_players.PLAYER_NAME, " +
                            "servervariables_players_variables.NAME, " +
                            "servervariables_players_variables.VALUE " +
                            "FROM servervariables_players LEFT JOIN servervariables_players_variables " +
                            "ON servervariables_players.UUID = servervariables_players_variables.UUID");

            ResultSet result = statement.executeQuery();

            Set<String> existingUUIDs = new HashSet<>();
            while(result.next()){
                String uuid = result.getString("UUID");
                String playerName = result.getString("PLAYER_NAME");
                String variableName = result.getString("NAME");
                String variableValue = result.getString("VALUE");

                ServerVariablesPlayer currentPlayer;
                if(!existingUUIDs.contains(uuid)){
                    //Create and add it
                    currentPlayer = new ServerVariablesPlayer(uuid,playerName,new ArrayList<>());
                    players.add(currentPlayer);
                    existingUUIDs.add(uuid);
                }else{
                    currentPlayer = players.stream()
                            .filter(p -> p.getUuid().equals(uuid))
                            .findFirst()
                            .orElse(null);
                }

                if(variableName != null && variableValue != null){
                    currentPlayer.addVariable(new ServerVariablesVariable(variableName,variableValue));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        plugin.getPlayerVariablesManager().setPlayerVariables(players);
    }

    public void createTables() {
        try(Connection connection = getConnection()){
            PreparedStatement statement1 = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS servervariables_players" +
                    " (UUID varchar(200) NOT NULL, " +
                    " PLAYER_NAME varchar(50), " +
                    " PRIMARY KEY ( UUID ))"
            );
            statement1.executeUpdate();
            PreparedStatement statement2 = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS servervariables_players_variables" +
                    " (ID int NOT NULL AUTO_INCREMENT, " +
                    " UUID varchar(200) NOT NULL, " +
                    " NAME varchar(100), " +
                    " VALUE text, " +
                    " PRIMARY KEY ( ID ), " +
                    " FOREIGN KEY (UUID) REFERENCES servervariables_players(UUID))");
            statement2.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getPlayer(String uuid,PlayerCallback callback){
        new BukkitRunnable(){
            @Override
            public void run() {
                ServerVariablesPlayer player = null;
                try(Connection connection = getConnection()){
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT servervariables_players.UUID, servervariables_players.PLAYER_NAME, " +
                                    "servervariables_players_variables.NAME, " +
                                    "servervariables_players_variables.VALUE " +
                                    "FROM servervariables_players LEFT JOIN servervariables_players_variables " +
                                    "ON servervariables_players.UUID = servervariables_players_variables.UUID " +
                                    "WHERE servervariables_players.UUID = ?");

                    statement.setString(1, uuid);
                    ResultSet result = statement.executeQuery();

                    boolean firstFind = true;
                    while(result.next()){
                        String playerName = result.getString("PLAYER_NAME");
                        String variableName = result.getString("NAME");
                        String variableValue = result.getString("VALUE");
                        if(firstFind){
                            firstFind = false;
                            player = new ServerVariablesPlayer(uuid,playerName,new ArrayList<>());
                        }
                        if(variableName != null && variableValue != null){
                            player.addVariable(new ServerVariablesVariable(variableName,variableValue));
                        }
                    }

                    ServerVariablesPlayer finalPlayer = player;
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            callback.onDone(finalPlayer);
                        }
                    }.runTask(plugin);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void createPlayer(ServerVariablesPlayer player){
        new BukkitRunnable(){
            @Override
            public void run() {
                try(Connection connection = getConnection()){
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO servervariables_players " +
                                    "(UUID, PLAYER_NAME) VALUE (?,?)");

                    statement.setString(1, player.getUuid());
                    statement.setString(2, player.getName());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void updatePlayerName(ServerVariablesPlayer player){
        new BukkitRunnable(){
            @Override
            public void run() {
                try(Connection connection = getConnection()){
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE servervariables_players SET " +
                                    "PLAYER_NAME=? WHERE UUID=?");

                    statement.setString(1, player.getName());
                    statement.setString(2, player.getUuid());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void updateVariable(ServerVariablesPlayer player,String variable,String value){
        ServerVariablesVariable v = player.getVariable(variable);
        new BukkitRunnable(){
            @Override
            public void run() {
                try(Connection connection = getConnection()){
                    PreparedStatement statement = null;
                    if(v == null){
                        // Insert
                        statement = connection.prepareStatement(
                                "INSERT INTO servervariables_players_variables " +
                                        "(UUID, NAME, VALUE) VALUE (?,?,?)");

                        statement.setString(1, player.getUuid());
                        statement.setString(2, variable);
                        statement.setString(3, value);
                    }else{
                        // Update
                        statement = connection.prepareStatement(
                                "UPDATE servervariables_players_variables SET " +
                                        "VALUE=? WHERE UUID=? AND NAME=?");

                        statement.setString(1, value);
                        statement.setString(2, player.getUuid());
                        statement.setString(3, variable);
                    }
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void resetVariable(ServerVariablesPlayer player,String variable){
        new BukkitRunnable(){
            @Override
            public void run() {
                try(Connection connection = getConnection()){
                    PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM servervariables_players_variables " +
                                    "WHERE UUID=? AND NAME=?");

                    statement.setString(1, player.getUuid());
                    statement.setString(2, variable);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}
