package com.ebicep.warlords.game.option.pve.wavedefense.events.modes;

import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.repositories.events.pojos.DatabaseGameEvent;
import com.ebicep.warlords.database.repositories.events.pojos.GameEvents;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.EventMode;
import com.ebicep.warlords.database.repositories.player.pojos.pve.events.modes.libraryarchives.DatabasePlayerPvEEventLibraryArchivesDifficultyStats;
import com.ebicep.warlords.events.game.WarlordsGameTriggerWinEvent;
import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.game.Team;
import com.ebicep.warlords.game.option.Option;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.player.ingame.WarlordsPlayer;
import com.ebicep.warlords.pve.gameevents.libraryarchives.PlayerCodex;
import com.ebicep.warlords.pve.upgrades.AbilityTree;
import com.ebicep.warlords.util.chat.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;
import java.util.*;

public class GrimoiresGraveyardOption implements Option {

    private final Map<UUID, PlayerCodex> codexRewards = new HashMap<>();

    @Override
    public void register(@Nonnull Game game) {
        if (!DatabaseGameEvent.eventIsActive()) {
            return;
        }
        game.registerEvents(new Listener() {
            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            public void onWin(WarlordsGameTriggerWinEvent event) {
                if (event.getDeclaredWinner() != Team.BLUE) {
                    return;
                }
                DatabaseGameEvent currentGameEvent = DatabaseGameEvent.currentGameEvent;
                GameEvents gameEvent = currentGameEvent.getEvent();
                game.warlordsPlayers().forEach(warlordsPlayer -> {
                    DatabaseManager.getPlayer(warlordsPlayer.getUuid(), databasePlayer -> {
                        EventMode eventMode = gameEvent.eventsStatsFunction.apply(databasePlayer.getPveStats().getEventStats()).get(currentGameEvent.getStartDateSecond());
                        if (eventMode != null && !(eventMode instanceof DatabasePlayerPvEEventLibraryArchivesDifficultyStats)) {
                            ChatUtils.MessageType.GAME_EVENTS.sendErrorMessage("Error: stats is not a DatabasePlayerPvEEventLibraryArchivesDifficultyStats - " + eventMode.getClass()
                                                                                                                                                                          .getSimpleName());
                            return;
                        }
                        Set<PlayerCodex> toExclude = eventMode == null ? Collections.emptySet() : ((DatabasePlayerPvEEventLibraryArchivesDifficultyStats) eventMode).getCodexesEarned()
                                                                                                                                                                    .keySet();
                        codexRewards.put(warlordsPlayer.getUuid(), PlayerCodex.getRandomCodex(toExclude));
                    });
                });
            }
        });
    }

    @Override
    public void onWarlordsEntityCreated(@Nonnull WarlordsEntity player) {
        if (player instanceof WarlordsPlayer) {
            AbilityTree abilityTree = ((WarlordsPlayer) player).getAbilityTree();
            abilityTree.setMaxMasterUpgrades(4);
        }
    }

    @Override
    public void sendEventStatsMessage(@Nonnull Game game, @Nonnull Player player) {
        PlayerCodex playerCodex = codexRewards.get(player.getUniqueId());
        if (playerCodex == null) {
            return;
        }
        ChatUtils.sendMessage(player,
                true,
                Component.text("Codex Acquired: ", NamedTextColor.WHITE)
                         .append(Component.text(playerCodex.name, NamedTextColor.GOLD))
        );
    }

    public Map<UUID, PlayerCodex> getCodexRewards() {
        return codexRewards;
    }
}
