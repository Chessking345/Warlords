package com.ebicep.warlords.game.option;

import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.game.Team;
import com.ebicep.warlords.game.option.marker.TeamMarker;
import com.ebicep.warlords.game.state.EndState;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.util.bukkit.PacketUtils;
import com.ebicep.warlords.util.warlords.GameRunnable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class InterchangeModeOption implements Option {

    public final int MAX_SWAP_TIME = 80;
    public final int MIN_SWAP_TIME = 50;

    private int secondsUntilNextSwap = 0;

    @Override
    public void register(@Nonnull Game game) {

    }

    @Override
    public void start(@Nonnull Game game) {
        generateNextSwapTime();

        new GameRunnable(game) {
            int secondsPast = 0;

            @Override
            public void run() {
                if (game.getState() instanceof EndState) {
                    return;
                }
                if (secondsPast >= secondsUntilNextSwap) {
                    swap(game);
                    generateNextSwapTime();
                    secondsPast = 0;
                }
                secondsPast++;
            }

        }.runTaskTimer(GameRunnable.SECOND, GameRunnable.SECOND);
    }

    private void generateNextSwapTime() {
        this.secondsUntilNextSwap = new Random().nextInt(MAX_SWAP_TIME - MIN_SWAP_TIME) + MIN_SWAP_TIME;
        System.out.println("Swapping in " + secondsUntilNextSwap + " seconds");
    }

    private void swap(Game game) {
        TeamMarker.getTeams(game).forEach(team -> swapTeamMembers(game, team));
        for (Option option : game.getOptions()) {
            if (option instanceof FlagSpawnPointOption) {
                ((FlagSpawnPointOption) option).getRenderer().render();
            }
        }
    }

    //the player BEFORE becomes the player AFTER
    //the last player BECOMES the first player
    private void swapTeamMembers(Game game, Team team) {
        List<WarlordsEntity> teamPlayers = game.warlordsEntities()
                .filter(warlordsPlayer -> warlordsPlayer.getTeam() == team)
                .collect(Collectors.toList());
        if (teamPlayers.size() <= 1) {
            return;
        }

        //Storing all player information as swapping jumbles it up
        HashMap<UUID, Location> playerLocations = new HashMap<>();
        HashMap<UUID, Boolean> playerOnHorse = new HashMap<>();
        for (WarlordsEntity teamPlayer : teamPlayers) {
            UUID uuid = teamPlayer.getUuid();
            playerLocations.put(uuid, teamPlayer.getLocation());
            playerOnHorse.put(uuid, teamPlayer.getEntity().getVehicle() != null);
        }

        //take beginning player to swap with end
        WarlordsEntity secondPlayer = teamPlayers.get(0);
        String secondPlayerName = secondPlayer.getName();
        UUID secondPlayerUuid = secondPlayer.getUuid();
        LivingEntity secondPlayerEntity = secondPlayer.getEntity();

        for (int i = 0; i < teamPlayers.size() - 1; i++) {
            transferPlayerStats(teamPlayers.get(i), teamPlayers.get(i + 1),
                    playerLocations,
                    playerOnHorse
            );
        }

        //give last player first players old stats
        WarlordsEntity firstPlayer = teamPlayers.get(teamPlayers.size() - 1);
        System.out.println("LAST SWAP - " + firstPlayer.getName() + " <<< " + secondPlayerName);

        UUID firstPlayerUuid = firstPlayer.getUuid();
        firstPlayer.setName(secondPlayerName);
        firstPlayer.setUuid(secondPlayerUuid);
        secondPlayerEntity.teleport(playerLocations.get(firstPlayerUuid));
        firstPlayer.setEntity(secondPlayerEntity);
        if (playerOnHorse.get(firstPlayerUuid)) {
            firstPlayer.getHorse().spawn();
        }
        if (firstPlayer.getEntity() instanceof Player) {
            PacketUtils.sendTitle((Player) firstPlayer.getEntity(),
                    ChatColor.YELLOW + "Swapped to",
                    ChatColor.GREEN.toString() + ChatColor.MAGIC + "00" + ChatColor.GREEN + " " + firstPlayer.getSpecClass().name + "! " + ChatColor.MAGIC + "00",
                    10, 40, 10
            );
        }

        firstPlayer.updateEntity();
        Warlords.getPlayers().put(secondPlayerUuid, firstPlayer);


//        Warlords.newChain()
//                .delay(100, TimeUnit.MILLISECONDS)
//                .sync(() -> {
//                    ArmorManager.resetArmor(firstPlayer.getUuid(), firstPlayer.getEntity(), firstPlayer.getSpecClass(), firstPlayer.getTeam());
//                }).execute();
    }

    //firstplayer gets the stats of the second
    private void transferPlayerStats(
            WarlordsEntity firstPlayer, WarlordsEntity secondPlayer,
            HashMap<UUID, Location> playerLocations,
            HashMap<UUID, Boolean> playerOnHorse
    ) {
        System.out.println("SWAP - " + firstPlayer.getName() + " <<< " + secondPlayer.getName());

        UUID firstPlayerUuid = firstPlayer.getUuid();
        firstPlayer.setName(secondPlayer.getName());
        firstPlayer.setUuid(secondPlayer.getUuid());
        secondPlayer.teleport(playerLocations.get(firstPlayerUuid));
        firstPlayer.setEntity(secondPlayer.getEntity());
        if (playerOnHorse.get(firstPlayerUuid)) {
            firstPlayer.getHorse().spawn();
        }
        if (firstPlayer.getEntity() instanceof Player) {
            PacketUtils.sendTitle((Player) firstPlayer.getEntity(),
                    ChatColor.YELLOW + "Swapped to",
                    ChatColor.GREEN.toString() + ChatColor.MAGIC + "00" + ChatColor.GREEN + " " + firstPlayer.getSpecClass().name + "! " + ChatColor.MAGIC + "00",
                    10, 40, 10
            );
        }
        firstPlayer.updateEntity();
        Warlords.getPlayers().put(secondPlayer.getUuid(), firstPlayer);
    }

}
