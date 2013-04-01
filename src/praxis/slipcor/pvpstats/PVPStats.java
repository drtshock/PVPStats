package praxis.slipcor.pvpstats;

import java.sql.SQLException;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * main class
 * 
 * @author slipcor
 * 
 * @version: v0.1.2
 * 
 */

public class PVPStats extends JavaPlugin {
	Plugin paHandler = null;
	//mySQL access
	protected lib.JesiKat.SQL.MySQLConnection sqlHandler; // MySQL handler

	// Settings Variables
	Boolean MySQL = false;
	String dbHost = null;
	String dbUser = null;
	String dbPass = null;
	String dbDatabase = null;
	int dbPort = 3306;

	private final PSListener entityListener = new PSListener(this);
	final PSPAListener paListener = new PSPAListener(this);

	public void onEnable() {
		PluginDescriptionFile pdfFile = getDescription();

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(entityListener, this);

		load_config();
		load_hooks();

		if (paHandler != null) {
			getLogger().info("registering PVP Arena events");
			pm.registerEvents(paListener, this);
		}

		if(getConfig().getBoolean("check-updates"))
			UpdateManager.updateCheck(this);

		getLogger().info("enabled. (version " + pdfFile.getVersion() + ")");
	}

	private void load_hooks() {
		Plugin pa = getServer().getPluginManager().getPlugin("pvparena");
		if (pa != null && pa.isEnabled()) {
			getLogger().info("<3 PVP Arena");
			this.paHandler = pa;
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!commandLabel.equalsIgnoreCase("pvpstats") || !commandLabel.equalsIgnoreCase("stats"))
			return true;

		if (args == null || args.length < 1 || !args[0].equalsIgnoreCase("reload")) {
			return parsecommand(sender, args);
		}

		try {
			Player p = (Player) sender;
			if (!p.isOp() && !p.hasPermission("pvpstats.reload")) {
				p.sendMessage("[PVP Stats] No permission to reload!");
				return true;
			}
		} catch (Exception e) {
			// nothing
		}

		if (args[0].equalsIgnoreCase("reload")) {
			load_config();
		} else {
			return false; // show command
		}

		sender.sendMessage("[PVP Stats] config reloaded!");

		return true;
	}

	private boolean parsecommand(CommandSender sender, String[] args) {
		if (args == null || args.length < 1) {
			String[] info = PSMySQL.info(sender.getName());
			int i = 1;
			for (String stat : info) {
				sender.sendMessage(String.valueOf(i++) + ": "+stat);
			}
			return true;
		}
		try {
			int count = Integer.parseInt(args[0]);
			if (count > 20) {
				count = 20;
			}
			String[] top = PSMySQL.top(count);
			sender.sendMessage("---------------");
			sender.sendMessage("PVP Stats Top "+args[0]);
			sender.sendMessage("---------------");
			int i = 1;
			for (String stat : top) {
				sender.sendMessage(String.valueOf(i++) + ": "+stat);
			}
			return true;
		} catch (Exception e) {
			String[] info = PSMySQL.info(args[0]);
			int i = 1;
			for (String stat : info) {
				sender.sendMessage(String.valueOf(i++) + ": "+stat);
			}
			return true;
		}
	}

	private void load_config() {

		getConfig().options().copyDefaults(true);
		saveConfig();

		// get variables from settings handler
		if (getConfig().getBoolean("MySQL", false)) {
			this.MySQL = getConfig().getBoolean("MySQL", false);
			this.dbHost = getConfig().getString("MySQLhost", "");
			this.dbUser = getConfig().getString("MySQLuser", "");
			this.dbPass = getConfig().getString("MySQLpass", "");
			this.dbDatabase = getConfig().getString("MySQLdb", "");
			this.dbPort = getConfig().getInt("MySQLport", 3306);
		}

		// Check Settings
		if (this.MySQL) {
			if (this.dbHost.equals("")) { this.MySQL = false;  }
			else if (this.dbUser.equals("")) { this.MySQL = false; }
			else if (this.dbPass.equals("")) { this.MySQL = false; }
			else if (this.dbDatabase.equals("")) { this.MySQL = false; }
		}

		// Enabled SQL/MySQL
		if (this.MySQL) {
			// Declare MySQL Handler
			try {
				sqlHandler = new lib.JesiKat.SQL.MySQLConnection(dbHost, dbPort, dbDatabase, dbUser,
						dbPass);
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}

			getLogger().info("MySQL Initializing");
			// Initialize MySQL Handler

			if (sqlHandler.connect(true)) {
				getLogger().info("MySQL connection successful");
				// Check if the tables exist, if not, create them
				if (!sqlHandler.tableExists(dbDatabase,"pvpstats")) {
					getLogger().info("Creating table pvpstats");
					String query = "CREATE TABLE `pvpstats` ( `id` int(5) NOT NULL AUTO_INCREMENT, `name` varchar(42) NOT NULL, `kills` int(8), `deaths` int(8), PRIMARY KEY (`id`) ) AUTO_INCREMENT=1 ;";
					try {
						sqlHandler.executeQuery(query, true);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			} else {
				getLogger().severe("MySQL connection failed");
				this.MySQL = false;
			}
			PSMySQL.plugin = this;
		} else {
			Bukkit.getServer().getPluginManager().disablePlugin(this);
		}
	}

}