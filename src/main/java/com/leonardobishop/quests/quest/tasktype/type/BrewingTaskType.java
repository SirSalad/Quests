package com.leonardobishop.quests.quest.tasktype.type;

import com.leonardobishop.quests.util.QuestsConfigLoader;
import com.leonardobishop.quests.api.QuestsAPI;
import com.leonardobishop.quests.player.QPlayer;
import com.leonardobishop.quests.player.questprogressfile.QuestProgress;
import com.leonardobishop.quests.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.quest.Quest;
import com.leonardobishop.quests.quest.Task;
import com.leonardobishop.quests.quest.tasktype.ConfigValue;
import com.leonardobishop.quests.quest.tasktype.TaskType;
import com.leonardobishop.quests.quest.tasktype.TaskUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class BrewingTaskType extends TaskType {

    private List<ConfigValue> creatorConfigValues = new ArrayList<>();
    private HashMap<Location, UUID> brewingStands = new HashMap<>();

    public BrewingTaskType() {
        super("brewing", TaskUtils.TASK_ATTRIBUTION_STRING, "Brew a potion.");
        this.creatorConfigValues.add(new ConfigValue("amount", true, "Amount of potions to be brewed."));
        this.creatorConfigValues.add(new ConfigValue("worlds", false, "Permitted worlds the player must be in."));
    }

    @Override
    public List<QuestsConfigLoader.ConfigProblem> detectProblemsInConfig(String root, HashMap<String, Object> config) {
        ArrayList<QuestsConfigLoader.ConfigProblem> problems = new ArrayList<>();
        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");
        return problems;
    }

    @Override
    public List<ConfigValue> getCreatorConfigValues() {
        return creatorConfigValues;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock().getType() == Material.BREWING_STAND) {
                brewingStands.put(event.getClickedBlock().getLocation(), event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BrewEvent event) {
        UUID uuid;
        if ((uuid = brewingStands.get(event.getBlock().getLocation())) != null) {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || player.hasMetadata("NPC")) {
                return;
            }

            QPlayer qPlayer = QuestsAPI.getPlayerManager().getPlayer(player.getUniqueId());
            if (qPlayer == null) {
                return;
            }

            for (Quest quest : super.getRegisteredQuests()) {
                if (qPlayer.hasStartedQuest(quest)) {
                    QuestProgress questProgress = qPlayer.getQuestProgressFile().getQuestProgress(quest);

                    for (Task task : quest.getTasksOfType(super.getType())) {
                        if (!TaskUtils.validateWorld(player, task)) continue;

                        TaskProgress taskProgress = questProgress.getTaskProgress(task.getId());

                        if (taskProgress.isCompleted()) {
                            continue;
                        }

                        int potionsNeeded = (int) task.getConfigValue("amount");

                        int progress;
                        if (taskProgress.getProgress() == null) {
                            progress = 0;
                        } else {
                            progress = (int) taskProgress.getProgress();
                        }

                        ItemStack potion1 = event.getContents().getItem(0);
                        ItemStack potion2 = event.getContents().getItem(1);
                        ItemStack potion3 = event.getContents().getItem(2);

                        taskProgress.setProgress(progress + (potion1 == null ? 0 : 1) + (potion2 == null ? 0 : 1) + (potion3 == null ? 0 : 1));

                        if (((int) taskProgress.getProgress()) >= potionsNeeded) {
                            taskProgress.setCompleted(true);
                        }
                    }
                }
            }
        }
    }

}
