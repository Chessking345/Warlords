package com.ebicep.warlords.game;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.game.option.*;
import com.ebicep.warlords.game.state.ClosedState;
import com.ebicep.warlords.game.state.PreLobbyState;
import com.ebicep.warlords.game.state.State;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.util.bukkit.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.ebicep.warlords.menu.generalmenu.WarlordsShopMenu.openTeamMenu;

public enum GameAddon {

    PRIVATE_GAME(
            "Private Game",
            null,
            "Initiates a private game where no other people can join."
    ) {
        @Override
        public void modifyGame(@Nonnull Game game) {
            switch (game.getGameMode()) {
                case CAPTURE_THE_FLAG:
                case INTERCEPTION:
                case TEAM_DEATHMATCH:
                case DEBUG:
                case SIMULATION_TRIAL:
                    game.getOptions().add(new PreGameItemOption(5, new ItemBuilder(Material.NOTE_BLOCK)
                            .name(ChatColor.GREEN + "Team Selector " + ChatColor.GRAY + "(Right-Click)")
                            .lore(ChatColor.YELLOW + "Click to select your team!")
                            .get(), (g, p) -> openTeamMenu(p)));
                    break;
            }
            game.getOptions().add(new AFKDetectionOption());
            game.setMinPlayers(1);
            game.setAcceptsPlayers(false);
        }

        @Override
        public void stateHasChanged(@Nonnull Game game, State oldState, @Nonnull State newState) {
            if (newState instanceof ClosedState) {
                return;
            }
            if (newState instanceof PreLobbyState) {
                PreLobbyState preLobbyState = (PreLobbyState) newState;
                preLobbyState.setMaxTimer(30 * 20);
                preLobbyState.resetTimer();
                game.setAcceptsPlayers(false);
            }
        }
    },
    CUSTOM_GAME(
            "Custom Game",
            null,
            "Makes the game custom, preventing stats from counting."
    ) {
    },
    FREEZE_GAME(
            "Freeze Failsafe",
            null,
            "Pauses the game when a player is missing for longer than 10 seconds. The game will automatically resume when they join back."
    ) {
        @Override
        public void modifyGame(@Nonnull Game game) {
            game.getOptions().add(new GameFreezeWhenOfflineOption());
        }
    },
    IMPOSTER_MODE(
            "Imposter Mode",
            null,
            "The game will assign players to intentionally boycott the game to make their team lose without being caught."
    ) {
        @Override
        public void modifyGame(@Nonnull Game game) {
            game.getOptions().add(new ImposterModeOption());
        }

        @Override
        public boolean canCreateGame(@Nonnull GameManager.GameHolder holder) {
            // At the moment, only 1 game can be an imposter game at the same time
            return !Warlords.getGameManager().getGames().stream().anyMatch(e -> e.getGame() != null && e.getGame().getAddons().contains(this));
        }
    },
    COOLDOWN_MODE(
            "Cooldown Mode",
            null,
            "Reduces energy costs and cooldowns by 75%"
    ) {
        @Override
        public void warlordsEntityCreated(@Nonnull Game game, @Nonnull WarlordsEntity player) {
            player.setEnergyModifier(player.getEnergyModifier() * 0.25);
            player.setCooldownModifier(player.getCooldownModifier() * 0.25);
        }
    },
    TRIPLE_HEALTH(
            "Triple Health",
            null,
            "Triples all players' health."
    ) {
        @Override
        public void warlordsEntityCreated(@Nonnull Game game, @Nonnull WarlordsEntity player) {
            player.setMaxHealth(player.getMaxHealth() * 3);
            player.setHealth(player.getHealth() * 3);
        }
    },
    DISABLE_CRIT(
            "No Critical Hits",
            null,
            "Prevents all players from hitting critical hits."
    ) {
        @Override
        public void warlordsEntityCreated(@Nonnull Game game, @Nonnull WarlordsEntity player) {
            player.setCanCrit(false);
        }
    },
    DOUBLE_TIME(
            "Double Time",
            null,
            "Doubles the max duration of the game."
    ) {
    },
    MEGA_GAME(
            "Mega Game",
            null,
            "Allows any map to hold unlimited players."
    ) {
        @Override
        public int getMaxPlayers(@Nonnull GameMap map, int maxPlayers) {
            return 1000;
        }

    },
    INTERCHANGE_MODE(
            "Interchange Mode",
            null,
            "Players on the same team will swap locations with each other at random intervals."
    ) {
        @Override
        public void modifyGame(@Nonnull Game game) {
            game.getOptions().add(new InterchangeModeOption());
            game.getOptions().add(new GameFreezeWhenOfflineOption());
        }

    },
    TOURNAMENT_MODE(
            "Tournament Mode",
            null,
            "Tournament Mode"
    ),
    PVE(
            "PVE",
            null,
            "Test"
    ) {
        @Override
        public void warlordsEntityCreated(@Nonnull Game game, @Nonnull WarlordsEntity player) {
            player.setInPve(true);
            if (player.getEntity() instanceof Player) {
                game.setPlayerTeam((OfflinePlayer) player, Team.BLUE);
                player.updateArmor();
            }
        }
    }

    ;

    public static final GameAddon[] VALUES = values();
    private final String name;
    @Nullable
    private final String permission;
    private final String description;

    GameAddon(String name, @Nullable String permission, String description) {
        this.name = name;
        this.permission = permission;
        this.description = description;
    }

    public boolean hasPermission(CommandSender sender) {
        return this.permission == null || sender.hasPermission(permission);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void modifyGame(@Nonnull Game game) {
    }

    /**
     * Gets the maximum amount of internalPlayers allowed in a map.Map modifiers
     * such as mega games could override the map provided map maximum.
     *
     * @param map The map to check
     * @param maxPlayers The max internalPlayers from the previous step, or the
     * map maximum internalPlayers if it is the first check
     * @return The maximum amount of internalPlayers supported by the map
     */
    public int getMaxPlayers(@Nonnull GameMap map, int maxPlayers) {
        return maxPlayers;
    }

    @Nullable
    public State stateWillChange(@Nonnull Game game, @Nullable State oldState, @Nonnull State newState) {
        return newState;
    }

    public void stateHasChanged(@Nonnull Game game, @Nullable State oldState, @Nonnull State newState) {
    }

    public void warlordsEntityCreated(@Nonnull Game game, @Nonnull WarlordsEntity player) {
    }

    public boolean canCreateGame(@Nonnull GameManager.GameHolder holder) {
        return true;
    }

}
