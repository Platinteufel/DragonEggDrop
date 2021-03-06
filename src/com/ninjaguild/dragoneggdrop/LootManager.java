/*
    DragonEggDrop
    Copyright (C) 2016  NinjaStix
    ninjastix84@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.ninjaguild.dragoneggdrop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_10_R1.block.CraftChest;
import org.bukkit.inventory.ItemStack;

public class LootManager {

	private final DragonEggDrop plugin;
	
	private final Random rand;
	private RandomCollection<ItemStack> loot = null;
	
	private File lootConfigFile = null;
	private FileConfiguration lootConfig = null;
	
	public LootManager(final DragonEggDrop plugin) {
		this.plugin = plugin;
		rand = new Random();
		loadLootItems();
	}
	
	private void loadLootItems() {
		loot = new RandomCollection<>();
	    
		plugin.saveResource("loot.yml", false);
		lootConfigFile = new File(plugin.getDataFolder() + "/loot.yml");
		lootConfig = YamlConfiguration.loadConfiguration(lootConfigFile);

		@SuppressWarnings("unchecked")
		Set<LootEntry> lootEntries = (Set<LootEntry>)lootConfig.get("loot-items");
		if (lootEntries != null && !lootEntries.isEmpty()) {
			for (LootEntry entry : lootEntries) {
				double weight = entry.getWeight();
				ItemStack item = entry.getItem();

				loot.add(weight, item);
			}
		}
	}
	
	protected void placeChest(Location loc) {
		if (loot.values().isEmpty()) {
			plugin.getLogger().log(Level.SEVERE, ChatColor.RED + "No Loot Items defined! Aborting Chest Placement.");
			return;
		}
		
		int minLoot = plugin.getConfig().getInt("min-loot", 2);
		minLoot = Math.max(minLoot, 1);
		int maxLoot = plugin.getConfig().getInt("max-loot", 6);
		maxLoot = Math.max(maxLoot, 1);
		int numItems = Math.max(rand.nextInt(maxLoot), minLoot);
		
		Block chestBlock = loc.getWorld().getBlockAt(loc);
		chestBlock.setType(Material.CHEST);
		Chest chest = (Chest)chestBlock.getState();
		//set custom title
		String chestTitle = ChatColor.translateAlternateColorCodes('&',
				plugin.getConfig().getString("loot-chest-title", "Chest"));
		((CraftChest)chest).getTileEntity().a(chestTitle);

		for (int i = 0; i < numItems; i++) {
			int slot = rand.nextInt(chest.getBlockInventory().getSize());
			ItemStack slotItem = chest.getBlockInventory().getItem(slot);
			if (slotItem != null && slotItem.getType() != Material.AIR) {
				i--;
				continue;
			}
			chest.getBlockInventory().setItem(slot, loot.next());
		}
	}
	
	protected void placeChestAll(Location loc) {
		if (loot.values().isEmpty()) {
			plugin.getLogger().log(Level.SEVERE, ChatColor.RED + "No Loot Items defined! Aborting Chest Placement.");
			return;
		}
		
		Block chestBlock = loc.getWorld().getBlockAt(loc);
		chestBlock.setType(Material.CHEST);
		Chest chest = (Chest)chestBlock.getState();
		//set custom title
		String chestTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("loot-chest-title", "Chest"));
		((CraftChest)chest).getTileEntity().a(chestTitle);
		
		List<ItemStack> lootItems = new ArrayList<>(loot.values());
		for (int i = 0; i < lootItems.size(); i++) {
			int slot = rand.nextInt(chest.getBlockInventory().getSize());
			ItemStack slotItem = chest.getBlockInventory().getItem(slot);
			if (slotItem != null && slotItem.getType() != Material.AIR) {
				i--;
				continue;
			}
			chest.getBlockInventory().setItem(slot, lootItems.get(i));
		}
	}

	protected boolean addItem(final double weight, final ItemStack item) {
		LootEntry le = new LootEntry(weight, item);
		@SuppressWarnings("unchecked")
		Set<LootEntry> lootEntries = (Set<LootEntry>)lootConfig.get("loot-items");
		if (lootEntries == null) {
			lootEntries = new HashSet<>();
		}
		boolean result = lootEntries.add(le);
		if (result) {
			result = loot.add(weight, item);
			lootConfig.set("loot-items", lootEntries);
			try {
				lootConfig.save(lootConfigFile);
			} catch (IOException e) {
				e.printStackTrace();
				result = false;
			}
		}

		return result;
	}

}
