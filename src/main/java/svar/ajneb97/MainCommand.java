package svar.ajneb97;




import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import svar.ajneb97.managers.MessagesManager;
import svar.ajneb97.model.VariableResult;
import svar.ajneb97.model.structure.Variable;

import java.util.ArrayList;
import java.util.List;


public class MainCommand implements CommandExecutor, TabCompleter {

	private ServerVariables plugin;
	public MainCommand(ServerVariables plugin){
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!sender.hasPermission("servervariables.admin")){
			return false;
		}

		FileConfiguration config = plugin.getConfig();
		MessagesManager msgManager = plugin.getMessagesManager();
		if(args.length >= 1){
			if(args[0].equalsIgnoreCase("set")){
				set(sender,args,config,msgManager);
			}else if(args[0].equalsIgnoreCase("reload")){
				reload(sender,args,config,msgManager);
			}else if(args[0].equalsIgnoreCase("get")){
				get(sender,args,config,msgManager);
			}else if(args[0].equalsIgnoreCase("add")){
				add(sender,args,config,msgManager);
			}else if(args[0].equalsIgnoreCase("take")){
				take(sender,args,config,msgManager);
			}else if(args[0].equalsIgnoreCase("reduce")){
				reduce(sender,args,config,msgManager);
			}else if(args[0].equalsIgnoreCase("reset")){
				reset(sender,args,config,msgManager);
			}else{
				help(sender,args,config,msgManager);
			}
		}else{
			help(sender,args,config,msgManager);
		}

		return true;

	}

	public void help(CommandSender sender, String[] args, FileConfiguration config, MessagesManager msgManager){
		sender.sendMessage(MessagesManager.getColoredMessage("&7[ [ &8[&服务器变量&8] &7] ]"));
		sender.sendMessage(MessagesManager.getColoredMessage(" "));
		sender.sendMessage(MessagesManager.getColoredMessage("&6/svar help &8显示此消息。"));
		sender.sendMessage(MessagesManager.getColoredMessage("&6/svar set <变量名> <值> (可选)<玩家名> (可选)silent:true &8设置变量的值。"));
		sender.sendMessage(MessagesManager.getColoredMessage("&6/svar get <变量名> (可选)<玩家名> (可选)silent:true &8获取变量的值。"));
		sender.sendMessage(MessagesManager.getColoredMessage("&6/svar add <变量名> <值> (可选)<玩家名> (可选)silent:true &8给变量添加一个值（整数或双精度浮点数）。"));
		sender.sendMessage(MessagesManager.getColoredMessage("&6/svar reduce <变量名> <值> (可选)<玩家名> (可选)silent:true &8减少变量的值（整数或双精度浮点数）。"));
		sender.sendMessage(MessagesManager.getColoredMessage("&6/svar reset <变量名> <值> (可选)<玩家名> (可选)silent:true &8重置变量的值。"));
		sender.sendMessage(MessagesManager.getColoredMessage("&6/svar reload &8重新加载配置。"));
		sender.sendMessage(MessagesManager.getColoredMessage(" "));
		sender.sendMessage(MessagesManager.getColoredMessage("&7[ [ &8[&服务器变量&8] &7] ]"));
	}


	public void set(CommandSender sender, String[] args, FileConfiguration config, MessagesManager msgManager){
		//servervariables set <variable> <value> (Set global variable)
		//servervariables set <variable> <value> <player> (Set player variable)
		if(args.length <= 2){
			msgManager.sendMessage(sender,config.getString("messages.commandSetError"),true);
			return;
		}

		String variableName = args[1];
		String newValue = args[2];
		String playerName = null;

		VariableResult result = null;
		if(args.length >= 4 && !args[3].equals("silent:true")){
			playerName = args[3];
			result = plugin.getPlayerVariablesManager().setVariable(playerName,variableName,newValue);
		}else{
			result = plugin.getServerVariablesManager().setVariable(variableName,newValue);
		}

		boolean silent = args[args.length-1].equals("silent:true");

		sendMessageSet(sender,result,msgManager,config,variableName,playerName,silent);
	}

	public void get(CommandSender sender, String[] args, FileConfiguration config, MessagesManager msgManager) {
		//servervariables get <variable> (Get global variable)
		//servervariables get <variable> <player> (Get player variable)
		if (args.length <= 1) {
			msgManager.sendMessage(sender, config.getString("messages.commandGetError"), true);
			return;
		}

		String variableName = args[1];
		String playerName = null;
		VariableResult result = null;

		if(args.length >= 3){
			playerName = args[2];
			result = plugin.getPlayerVariablesManager().getVariableValue(playerName,variableName, false);
		}else{
			result = plugin.getServerVariablesManager().getVariableValue(variableName,false);
		}

		if(result.isError()){
			msgManager.sendMessage(sender,result.getErrorMessage(),true);
		}else{
			if(playerName != null){
				msgManager.sendMessage(sender,config.getString("messages.commandGetCorrectPlayer").replace("%variable%",variableName)
						.replace("%value%",result.getResultValue()).replace("%player%",playerName),true);
			}else{
				msgManager.sendMessage(sender,config.getString("messages.commandGetCorrect").replace("%variable%",variableName)
						.replace("%value%",result.getResultValue()),true);
			}
		}
	}
	public void take(CommandSender sender, String[] args, FileConfiguration config, MessagesManager msgManager){
		// servervariables take <variable> <value> (Subtract value from server variable if INTEGER or DOUBLE)
		// servervariables take <variable> <value> <player> (Subtract value from player variable if INTEGER or DOUBLE)
		if(args.length <= 2){
			msgManager.sendMessage(sender,config.getString("messages.commandAddError").replace("add","take"), true);
			return;
		}

		String variableName = args[1];
		String valueStr = args[2];
		String playerName = null;
		VariableResult currentVarResult = null;

		if(args.length >= 4 && !args[3].equals("silent:true")){
			playerName = args[3];
			currentVarResult = plugin.getPlayerVariablesManager().getVariableValue(playerName, variableName, false);
		}else{
			currentVarResult = plugin.getServerVariablesManager().getVariableValue(variableName, false);
		}

		if(currentVarResult.isError()){
			msgManager.sendMessage(sender, currentVarResult.getErrorMessage(), true);
			return;
		}

		int currentValue = Integer.parseInt(currentVarResult.getResultValue());
		int valueToSubtract = Integer.parseInt(valueStr);
		int newValue = Math.max(0, currentValue - valueToSubtract);
		// 如果newValue与currentValue相同，无需更新
		if (newValue == currentValue) {
			// 可以选择在这里发送消息给 sender，告知他们变量值没有改变
			return;
		}
		// 进行变量更新
		VariableResult result;
		if(playerName != null){
			playerName = args[3];
			result = plugin.getPlayerVariablesManager().setVariable(playerName,variableName, String.valueOf(newValue));
		}else{
			result = plugin.getServerVariablesManager().setVariable(variableName, String.valueOf(newValue));
		}

		boolean silent = args[args.length - 1].equals("silent:true");
		sendMessageSet(sender, result, msgManager, config, variableName, playerName, silent);
	}


	public void add(CommandSender sender, String[] args, FileConfiguration config, MessagesManager msgManager){
		//servervariables add <variable> <value> (Add value to server variable if INTEGER or DOUBLE)
		//servervariables add <variable> <value> <player> (Add value to player variable if INTEGER or DOUBLE)
		if(args.length <= 2){
			msgManager.sendMessage(sender,config.getString("messages.commandAddError"),true);
			return;
		}

		String variableName = args[1];
		String value = args[2];
		String playerName = null;

		VariableResult result = null;
		if(args.length >= 4 && !args[3].equals("silent:true")){
			playerName = args[3];
			result = plugin.getPlayerVariablesManager().modifyVariable(playerName,variableName,value,true);
		}else{
			result = plugin.getServerVariablesManager().modifyVariable(variableName,value,true);
		}

		boolean silent = args[args.length-1].equals("silent:true");

		sendMessageSet(sender,result,msgManager,config,variableName,playerName,silent);
	}

	public void reduce(CommandSender sender, String[] args, FileConfiguration config, MessagesManager msgManager){
		//servervariables reduce <variable> <value> (Reduce value of server variable if INTEGER or DOUBLE)
		//servervariables reduce <variable> <value> <player> (Reduce value of player variable if INTEGER or DOUBLE)
		if(args.length <= 2){
			msgManager.sendMessage(sender,config.getString("messages.commandReduceError"),true);
			return;
		}

		String variableName = args[1];
		String value = args[2];
		String playerName = null;

		VariableResult result = null;
		if(args.length >= 4 && !args[3].equals("silent:true")){
			playerName = args[3];
			result = plugin.getPlayerVariablesManager().modifyVariable(playerName,variableName,value,false);
		}else{
			result = plugin.getServerVariablesManager().modifyVariable(variableName,value,false);
		}

		boolean silent = args[args.length-1].equals("silent:true");

		sendMessageSet(sender,result,msgManager,config,variableName,playerName,silent);
	}

	public void reset(CommandSender sender, String[] args, FileConfiguration config, MessagesManager msgManager){
		//servervariables reset <variable> (Resets a global variable to the default value)
		//servervariables reset <variable> <player> (Resets a player variable to the default value)
		if(args.length <= 1){
			msgManager.sendMessage(sender,config.getString("messages.commandResetError"),true);
			return;
		}

		String variableName = args[1];
		String playerName = null;

		VariableResult result = null;
		if(args.length >= 3 && !args[2].equals("silent:true")){
			playerName = args[2];
			result = plugin.getPlayerVariablesManager().resetVariable(playerName,variableName);
		}else{
			result = plugin.getServerVariablesManager().resetVariable(variableName);
		}

		boolean silent = args[args.length-1].equals("silent:true");

		if(result.isError()){
			msgManager.sendMessage(sender,result.getErrorMessage(),true);
		}else{
			if(silent){
				return;
			}
			if(playerName != null){
				msgManager.sendMessage(sender,config.getString("messages.commandResetCorrectPlayer").replace("%variable%",variableName)
						.replace("%player%",playerName),true);
			}else{
				msgManager.sendMessage(sender,config.getString("messages.commandResetCorrect").replace("%variable%",variableName),true);
			}
		}
	}

	private void sendMessageSet(CommandSender sender,VariableResult result,MessagesManager msgManager,FileConfiguration config,
							   String variableName,String playerName,boolean silent){
		if(result.isError()){
			msgManager.sendMessage(sender,result.getErrorMessage(),true);
		}else{
			if(silent){
				return;
			}
			if(playerName != null){
				msgManager.sendMessage(sender,config.getString("messages.commandSetCorrectPlayer").replace("%variable%",variableName)
						.replace("%value%",result.getResultValue()).replace("%player%",playerName),true);
			}else{
				msgManager.sendMessage(sender,config.getString("messages.commandSetCorrect").replace("%variable%",variableName)
						.replace("%value%",result.getResultValue()),true);
			}
		}
	}

	public void reload(CommandSender sender, String[] args, FileConfiguration config, MessagesManager msgManager){
		// /servervariables reload
		plugin.getConfigsManager().reloadConfigs();
		msgManager.sendMessage(sender,config.getString("messages.pluginReloaded"),true);
	}


	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if(!sender.hasPermission("servervariables.admin")){
			return null;
		}

		if(args.length == 1){
			// 显示所有命令
			List<String> completions = new ArrayList<String>();
			List<String> commands = new ArrayList<String>();
			commands.add("reload"); commands.add("set"); commands.add("get"); commands.add("add"); commands.add("take");
			commands.add("reduce"); commands.add("reset"); commands.add("help");
			for(String c : commands) {
				if(args[0].isEmpty() || c.startsWith(args[0].toLowerCase())) {
					completions.add(c);
				}
			}
			return completions;
		} else {
			List<String> completions = new ArrayList<String>();
			ArrayList<Variable> variables = plugin.getVariablesManager().getVariables();
			if((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("get") || args[0].equalsIgnoreCase("add")
					|| args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("reduce") || args[0].equalsIgnoreCase("reset"))
					&& args.length == 2) {
				// 对于 set, get, add, take, reduce, reset 命令的变量名补全
				String argVariable = args[1];
				for(Variable variable : variables) {
					if(argVariable.isEmpty() || variable.getName().toLowerCase().startsWith(argVariable.toLowerCase())) {
						completions.add(variable.getName());
					}
				}
				return completions;
			} else if(args[0].equalsIgnoreCase("set") && args.length == 3){
				// set 命令的值补全
				Variable variable = plugin.getVariablesManager().getVariable(args[1]);
				String argVariable = args[2];

				if(variable != null){
					List<String> possibleRealValues = variable.getPossibleRealValues();
					for(String possibleValue : possibleRealValues){
						if(argVariable.isEmpty() || possibleValue.toLowerCase().startsWith(argVariable.toLowerCase())) {
							completions.add(possibleValue);
						}
					}
				}
				completions.add("<value>");
				return completions;
			} else if((args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("reduce") || args[0].equalsIgnoreCase("take")) && args.length == 3){
				// add, reduce, take 命令的值补全
				completions.add("<value>");
				return completions;
			}
		}
		return null;
	}

}
