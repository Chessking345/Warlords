package com.ebicep.warlords.maps.state;

import com.ebicep.jda.BotManager;
import com.ebicep.warlords.Warlords;
import com.ebicep.warlords.commands.debugcommands.RecordGamesCommand;
import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.database.FieldUpdateOperators;
import com.ebicep.warlords.database.LeaderboardRanking;
import com.ebicep.warlords.events.WarlordsPointsChangedEvent;
import com.ebicep.warlords.maps.Game;
import com.ebicep.warlords.maps.Gates;
import com.ebicep.warlords.maps.Team;
import com.ebicep.warlords.maps.flags.FlagManager;
import com.ebicep.warlords.maps.flags.GroundFlagLocation;
import com.ebicep.warlords.maps.flags.PlayerFlagLocation;
import com.ebicep.warlords.maps.flags.SpawnFlagLocation;
import com.ebicep.warlords.player.*;
import com.ebicep.warlords.powerups.PowerupManager;
import com.ebicep.warlords.util.PacketUtils;
import com.ebicep.warlords.util.PlayerFilter;
import com.ebicep.warlords.util.RemoveEntities;
import com.ebicep.warlords.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.ebicep.warlords.util.Utils.sendMessage;

public class PlayingState implements State, TimerDebugAble {
    private static final int GATE_TIMER = 10 * 20;
    private static final int POWERUP_TIMER = 60 * 20;
    private static final int OVERTIME_TIME = 60 * 20;

    private static final int SCORE_KILL_POINTS = 5;
    private static final int SCORE_CAPTURE_POINTS = 250;

    private static final int ENDING_SCORE_LIMIT = 1000;
    private static final int MERCY_LIMIT = 550;

    private int timer = 0;
    private int gateTimer = 0;
    private int powerupTimer = 0;
    private boolean overTimeActive = false;
    private int pointLimit = 0;
    private final Game game;
    private boolean forceEnd;

    private final EnumMap<Team, Stats> stats = new EnumMap(Team.class);

    {
        resetStats();
    }

    @Nullable
    private FlagManager flags = null;
    @Nullable
    private BukkitTask powerUps = null;

    public PlayingState(@Nonnull Game game) {
        this.game = game;
    }

    public void addKill(@Nonnull Team victim, boolean isSuicide) {
        Stats myStats = getStats(victim);
        myStats.deaths++;
        Stats enemyStats = getStats(victim.enemy());
        enemyStats.kills++;
        addPoints(victim.enemy(), SCORE_KILL_POINTS);
    }

    @Nonnull
    public Stats getStats(@Nonnull Team team) {
        return stats.get(team);
    }

    public void addPoints(@Nonnull Team team, int i) {
        getStats(team).addPoints(i);
    }

    @Deprecated
    public int getBluePoints() {
        return stats.get(Team.BLUE).points();
    }

    @Deprecated
    public void addBluePoints(int i) {
        this.addPoints(Team.BLUE, i);
    }

    @Deprecated
    public int getRedPoints() {
        return stats.get(Team.RED).points();
    }

    @Deprecated
    public void addRedPoints(int i) {
        this.addPoints(Team.RED, i);
    }

    public int getTimer() {
        return timer;
    }

    public int getTimerInSeconds() {
        return getTimer() / 20;
    }

    public boolean isOvertime() {
        return this.overTimeActive;
    }

    @Nonnull
    @SuppressWarnings("null")
    public FlagManager flags() {
        if (this.flags == null) {
            throw new IllegalStateException("Cannot access flag sub component, state not enabled");
        }
        return this.flags;
    }

    @Nonnull
    public Game getGame() {
        return game;
    }

    @Override
    @SuppressWarnings("null")
    public void begin() {
        this.resetTimer();
        this.forceEnd = false;
        this.gateTimer = GATE_TIMER;
        this.powerupTimer = POWERUP_TIMER;
        RemoveEntities.doRemove(this.game.getMap());
        this.flags = new FlagManager(this, game.getMap().getRedFlag(), game.getMap().getBlueFlag());

        this.game.forEachOfflinePlayer((player, team) -> {
            PlayerSettings playerSettings = Warlords.getPlayerSettings(player.getUniqueId());
            Warlords.addPlayer(new WarlordsPlayer(
                    player,
                    this,
                    team,
                    playerSettings
            ));
        });
        this.game.forEachOfflineWarlordsPlayer(wp -> {
            CustomScoreboard customScoreboard = Warlords.playerScoreboards.get(wp.getUuid());
            updateBasedOnGameState(true, customScoreboard, wp);
            if (wp.getEntity() instanceof Player) {
                wp.applySkillBoost((Player) wp.getEntity());
                PacketUtils.sendTitle((Player) wp.getEntity(), ChatColor.GREEN + "GO!", ChatColor.YELLOW + "Steal and capture the enemy flag!", 0, 40, 20);
            }
        });

        Warlords.newChain()
                .async(() -> {
                    game.forEachOfflinePlayer((player, team) -> {
                        HashMap<String, Object> newInfo = new HashMap<>();
                        newInfo.put("last_spec", Classes.getSelected(player).name);
                        Warlords.getPlayerSettings(player.getUniqueId()).getWeaponSkins().forEach((classes, weapons) -> {
                            newInfo.put(
                                    Classes.getClassesGroup(classes).name.toLowerCase() + "." + classes.name.toLowerCase() + ".weapon",
                                    weapons.name);
                        });
                        newInfo.put("mage.helm", ArmorManager.Helmets.getSelected(player.getPlayer()).get(0).name);
                        newInfo.put("mage.armor", ArmorManager.ArmorSets.getSelected(player.getPlayer()).get(0).name);
                        newInfo.put("warrior.helm", ArmorManager.Helmets.getSelected(player.getPlayer()).get(1).name);
                        newInfo.put("warrior.armor", ArmorManager.ArmorSets.getSelected(player.getPlayer()).get(1).name);
                        newInfo.put("paladin.helm", ArmorManager.Helmets.getSelected(player.getPlayer()).get(2).name);
                        newInfo.put("paladin.armor", ArmorManager.ArmorSets.getSelected(player.getPlayer()).get(2).name);
                        newInfo.put("shaman.helm", ArmorManager.Helmets.getSelected(player.getPlayer()).get(3).name);
                        newInfo.put("shaman.armor", ArmorManager.ArmorSets.getSelected(player.getPlayer()).get(3).name);
                        newInfo.put("hotkeymode", Settings.HotkeyMode.getSelected(player.getPlayer()).name());
                        DatabaseManager.updatePlayerInformation(player, newInfo, FieldUpdateOperators.SET, false);
                    });
                }).execute();
    }

    @Override
    public State run() {
        this.timer--;
        if (forceEnd) {
            return getEndState(null);
        }
        if (this.timer <= 0) {
            if (this.overTimeActive) {
                return getEndState(null);
            } else {
                State next = nextStateByPoints();
                if (next == null) {
                    this.timer = OVERTIME_TIME;
                    this.overTimeActive = true;
                    assert getStats(Team.BLUE).points == getStats(Team.RED).points;
                    this.pointLimit = getStats(Team.BLUE).points + 20;
                    this.game.forEachOnlinePlayer((player, team) -> {
                        PacketUtils.sendTitle(player, ChatColor.LIGHT_PURPLE + "OVERTIME!", ChatColor.YELLOW + "First team to reach 20 points wins!", 0, 60, 0);
                        player.sendMessage("§dOvertime is now active!");
                        player.playSound(player.getLocation(), Sound.PORTAL_TRAVEL, 500, 1);
                    });
                } else {
                    return next;
                }
            }
        }
        if(timer % 10 == 0) {
            for (WarlordsPlayer value : Warlords.getPlayers().values()) {
                updateBasedOnGameState(false, Warlords.playerScoreboards.get(value.getUuid()), value);
            }
            for (UUID spectator : game.getSpectators()) {
                updateBasedOnGameState(false, Warlords.playerScoreboards.get(spectator), null);
            }
        }

        int redPoints = getStats(Team.RED).points;
        int bluePoints = getStats(Team.BLUE).points;
        if (redPoints >= this.pointLimit || bluePoints >= this.pointLimit || (Math.abs(redPoints - bluePoints) >= MERCY_LIMIT && this.timer < game.getMap().getGameTimerInTicks() - 20 * 60 * 5)) {
            return nextStateByPoints();
        }
        if (gateTimer >= 0) {
            gateTimer--;
            if (gateTimer % 20 == 0) {
                int remaining = gateTimer / 20;
                game.forEachOnlinePlayer((player, team) -> {
                    player.playSound(player.getLocation(), remaining == 0 ? Sound.WITHER_SPAWN : Sound.NOTE_STICKS, 1, 1);
                    String number;
                    if (remaining >= 8) {
                        number = ChatColor.GREEN.toString();
                    } else if (remaining >= 4) {
                        number = ChatColor.YELLOW.toString();
                    } else {
                        number = ChatColor.RED.toString();
                    }
                    number += remaining;
                    PacketUtils.sendTitle(player, number, "", 0, 40, 0);
                });
                switch (remaining) {
                    case 0:
                        Gates.changeGates(game.getMap(), true);
                        game.forEachOnlinePlayer((player, team) -> {
                            sendMessage(player, false, ChatColor.YELLOW + "Gates opened! " + ChatColor.RED + "FIGHT!");
                            PacketUtils.sendTitle(player, ChatColor.GREEN + "GO!", ChatColor.YELLOW + "Steal and capture the enemy flag!", 0, 40, 20);

                            Utils.resetPlayerMovementStatistics(player);
                        });
                        break;
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 10:
                        game.forEachOnlinePlayer((player, team) -> {
                            String s = remaining == 1 ? "" : "s";
                            sendMessage(player, false, ChatColor.YELLOW + "The gates will fall in " + ChatColor.RED + remaining + ChatColor.YELLOW + " second" + s + "!");
                        });
                        break;
                }
            }
        }
        if (powerupTimer >= 0) {
            powerupTimer--;
            if (powerupTimer == 0) {
                if (this.powerUps != null) {
                    this.powerUps.cancel();
                }
                this.powerUps = new PowerupManager(game).runTaskTimer(Warlords.getInstance(), 0, 0);
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("null")
    public void end() {
        if (this.flags != null) {
            this.flags.stop();
        }
        if (this.powerUps != null) {
            this.powerUps.cancel();
            this.powerUps = null;
        }
        Warlords.getPlayers().forEach(((uuid, warlordsPlayer) -> warlordsPlayer.removeGrave()));
        if (RecordGamesCommand.recordGames && !forceEnd && game.playersCount() >= 16 && timer <= 12000) {
            if (getBluePoints() > getRedPoints()) {
                BotManager.sendMessageToNotificationChannel("[GAME] A game ended with **BLUE** winning " + getBluePoints() + " to " + getRedPoints());
            } else if (getBluePoints() < getRedPoints()) {
                BotManager.sendMessageToNotificationChannel("[GAME] A game ended with **RED** winning " + getRedPoints() + " to " + getBluePoints());
            } else {
                BotManager.sendMessageToNotificationChannel("[GAME] A game ended with a **DRAW**");
            }
            List<WarlordsPlayer> players = new ArrayList<>(Warlords.getPlayers().values());
            float highestDamage = players.stream().sorted(Comparator.comparing(WarlordsPlayer::getTotalDamage).reversed()).collect(Collectors.toList()).get(0).getTotalDamage();
            float highestHealing = players.stream().sorted(Comparator.comparing(WarlordsPlayer::getTotalHealing).reversed()).collect(Collectors.toList()).get(0).getTotalHealing();
            if (highestDamage <= 500000 && highestHealing <= 500000) {
                DatabaseManager.addGame(PlayingState.this, true);
            } else {
                DatabaseManager.addGame(PlayingState.this, false);
                game.forEachOnlinePlayer(((player, team) -> {
                    if (player.isOp()) {
                        player.sendMessage(ChatColor.RED + "This game was not added to the database");
                    }
                }));
                System.out.println(ChatColor.GREEN + "[Warlords] This game was not added to the database (INVALID DAMAGE/HEALING)");
            }
        } else {
            DatabaseManager.addGame(PlayingState.this, false);
            game.forEachOnlinePlayer(((player, team) -> {
                if (player.isOp()) {
                    player.sendMessage(ChatColor.RED + "This game was not added to the database");
                }
            }));
            System.out.println(ChatColor.GREEN + "[Warlords] This game was not added to the database");
        }
    }

    @Override
    public void skipTimer() {
        if (this.gateTimer > 0) {
            this.timer -= this.gateTimer - 1;
            this.gateTimer = 1;
        } else {
            this.timer = 0;
        }
    }

    @Override
    public void resetTimer() throws IllegalStateException {
        this.timer = game.getMap().getGameTimerInTicks();
        this.pointLimit = ENDING_SCORE_LIMIT;
        this.overTimeActive = false;
    }

    private EndState getEndState(@Nullable Team winner) {
        return new EndState(this.game, winner, this.getStats(Team.RED), this.getStats(Team.BLUE));
    }

    @Nullable
    public Team calculateWinnerByPoints() {
        int redPoints = getStats(Team.RED).points();
        int bluePoints = getStats(Team.BLUE).points();
        if (redPoints > bluePoints) {
            return Team.RED;
        }
        if (bluePoints > redPoints) {
            return Team.BLUE;
        }
        return null;
    }

    @Nullable
    private State nextStateByPoints() {
        Team winner = calculateWinnerByPoints();
        if (winner != null) {
            return getEndState(winner);
        }
        return null;
    }

    public void resetStats() {
        for (Team team : Team.values()) {
            stats.put(team, new Stats(team));
        }
    }

    public void addCapture(WarlordsPlayer capper) {
        getStats(capper.getTeam()).captures++;
        addPoints(capper.getTeam(), SCORE_CAPTURE_POINTS);
    }

    public void endGame() {
        this.forceEnd = true;
    }

    public boolean isForceEnd() {
        return forceEnd;
    }

    public int getPointLimit() {
        return pointLimit;
    }

    //scoreboard to this game
    public void giveScoreboard(UUID uuid) {
        CustomScoreboard customScoreboard = Warlords.playerScoreboards.get(uuid);
        Scoreboard scoreboard = customScoreboard.getScoreboard();
        Objective health = customScoreboard.getHealth();

    }

    public void updateHealth(CustomScoreboard customScoreboard) {
        Scoreboard scoreboard = customScoreboard.getScoreboard();
        Objective health = customScoreboard.getHealth();
        if (health == null || scoreboard.getObjective("health") == null) {
            health = scoreboard.registerNewObjective("health", "dummy");
            health.setDisplaySlot(DisplaySlot.BELOW_NAME);
            health.setDisplayName(ChatColor.RED + "❤");
            customScoreboard.setHealth(health);
        }
        Objective finalHealth = health;
        this.getGame().forEachOfflinePlayer((player, team) -> {
            WarlordsPlayer warlordsPlayer = Warlords.getPlayer(player);
            if (warlordsPlayer != null) {
                finalHealth.getScore(warlordsPlayer.getName()).setScore(warlordsPlayer.getHealth());
            }
        });
    }

    public void updateNames(CustomScoreboard customScoreboard) {
        Scoreboard scoreboard = customScoreboard.getScoreboard();
        this.getGame().forEachOfflinePlayer((player, team) -> {
            WarlordsPlayer warlordsPlayer = Warlords.getPlayer(player);
            if (warlordsPlayer != null) {
                if (scoreboard.getTeam(warlordsPlayer.getName()) == null) {
                    org.bukkit.scoreboard.Team temp = scoreboard.registerNewTeam(warlordsPlayer.getName());
                    temp.setPrefix(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + warlordsPlayer.getSpec().getClassNameShort() + ChatColor.DARK_GRAY + "] " + team.teamColor());
                    temp.addEntry(warlordsPlayer.getName());
                    temp.setSuffix(ChatColor.DARK_GRAY + " [" + ChatColor.GOLD + "Lv90" + ChatColor.DARK_GRAY + "]");
                } else {
                    scoreboard.getTeam(warlordsPlayer.getName()).setPrefix(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + warlordsPlayer.getSpec().getClassNameShort() + ChatColor.DARK_GRAY + "] " + team.teamColor());
                    if (warlordsPlayer.getGameState().flags().hasFlag(warlordsPlayer)) {
                        scoreboard.getTeam(warlordsPlayer.getName()).setSuffix(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Lv90" + ChatColor.DARK_GRAY + "]" + ChatColor.WHITE + "⚑");
                    } else {
                        scoreboard.getTeam(warlordsPlayer.getName()).setSuffix(ChatColor.DARK_GRAY + " [" + ChatColor.GOLD + "Lv90" + ChatColor.DARK_GRAY + "]");
                    }
                }
            }
        });
    }

    public void updatePlayerName(CustomScoreboard customScoreboard, WarlordsPlayer warlordsPlayer) {
        Scoreboard scoreboard = customScoreboard.getScoreboard();

        this.getGame().forEachOfflinePlayer((player, team) -> {
            WarlordsPlayer wp = Warlords.getPlayer(player);
            if (wp != null) {
                scoreboard.getTeam(warlordsPlayer.getName()).setPrefix(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + warlordsPlayer.getSpec().getClassNameShort() + ChatColor.DARK_GRAY + "] " + warlordsPlayer.getTeam().teamColor());
                scoreboard.getTeam(warlordsPlayer.getName()).setSuffix(ChatColor.DARK_GRAY + " [" + ChatColor.GOLD + "Lv90" + ChatColor.DARK_GRAY + "]");
            }
        });
    }

    public void updateBasedOnGameState(boolean init, CustomScoreboard customScoreboard, WarlordsPlayer warlordsPlayer) {
        this.updateHealth(customScoreboard);
        this.updateNames(customScoreboard);

        CustomScoreboardPair[] pairs = new CustomScoreboardPair[15];
        for (int i = 0; i < pairs.length; i++) {
            pairs[i] = new CustomScoreboardPair();
        }

        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat format2 = new SimpleDateFormat("kk:mm");
        format2.setTimeZone(TimeZone.getTimeZone("EST"));

        //date
        pairs[14].setPrefixAndSuffix(ChatColor.GRAY + format.format(new Date()) + " - ", format2.format(new Date()));

        // Points
        pairs[12].setPrefixAndSuffix(ChatColor.BLUE + "BLU: ", ChatColor.AQUA.toString() + this.getBluePoints() + ChatColor.GOLD + "/" + this.getPointLimit());
        pairs[11].setPrefixAndSuffix(ChatColor.RED + "RED: ", ChatColor.AQUA.toString() + this.getRedPoints() + ChatColor.GOLD + "/" + this.getPointLimit());

        // Timer
        int secondsRemaining = this.getTimer() / 20;
        int minute = secondsRemaining / 60;
        int second = secondsRemaining % 60;
        String timeLeft = "";
        if (minute < 10) {
            timeLeft += "0";
        }
        timeLeft += minute + ":";
        if (second < 10) {
            timeLeft += "0";
        }
        timeLeft += second;

        com.ebicep.warlords.maps.Team team = this.calculateWinnerByPoints();
        if (team != null) {
            pairs[9].setPrefix(team.coloredPrefix() + ChatColor.GOLD + " Wins in:");
        } else {
            pairs[9].setPrefix(ChatColor.WHITE + "Time Left:");
        }
        pairs[9].setSuffix(" " + ChatColor.GREEN + timeLeft);


        // Flags
        if (this.flags().getRed().getFlag() instanceof SpawnFlagLocation) {
            pairs[7].setPrefixAndSuffix(ChatColor.RED + "RED Flag: ", ChatColor.GREEN + "Safe");
        } else if (this.flags().getRed().getFlag() instanceof PlayerFlagLocation) {
            PlayerFlagLocation flag = (PlayerFlagLocation) this.flags().getRed().getFlag();
            if (flag.getPickUpTicks() == 0) {
                pairs[7].setPrefixAndSuffix(ChatColor.RED + "RED Flag: ", ChatColor.RED + "Stolen!");
            } else {
                pairs[7].setPrefixAndSuffix(ChatColor.RED + "RED Flag: " + ChatColor.RED + "St", "olen!" + ChatColor.YELLOW + " +" + flag.getComputedHumanMultiplier() + "§e%");
            }
        } else if (this.flags().getRed().getFlag() instanceof GroundFlagLocation) {
            GroundFlagLocation flag = (GroundFlagLocation) this.flags().getRed().getFlag();
            pairs[7].setPrefixAndSuffix(ChatColor.RED + "RED Flag: ", ChatColor.YELLOW + "Dropped! " + ChatColor.GRAY + flag.getDespawnTimerSeconds());
        } else {
            pairs[7].setPrefixAndSuffix(ChatColor.RED + "RED Flag: ", ChatColor.GRAY + "Respawning...");
        }

        if (this.flags().getBlue().getFlag() instanceof SpawnFlagLocation) {
            pairs[6].setPrefixAndSuffix(ChatColor.BLUE + "BLU Flag: ", ChatColor.GREEN + "Safe");
        } else if (this.flags().getBlue().getFlag() instanceof PlayerFlagLocation) {
            PlayerFlagLocation flag = (PlayerFlagLocation) this.flags().getBlue().getFlag();
            if (flag.getPickUpTicks() == 0) {
                pairs[6].setPrefixAndSuffix(ChatColor.BLUE + "BLU Flag: ", ChatColor.RED + "Stolen!");
            } else {
                pairs[6].setPrefixAndSuffix(ChatColor.BLUE + "BLU Flag: " + ChatColor.RED + "St", "olen!" + ChatColor.YELLOW + " +" + flag.getComputedHumanMultiplier() + "§e%");
            }
        } else if (this.flags().getBlue().getFlag() instanceof GroundFlagLocation) {
            GroundFlagLocation flag = (GroundFlagLocation) this.flags().getBlue().getFlag();
            pairs[6].setPrefixAndSuffix(ChatColor.BLUE + "BLU Flag: ", ChatColor.YELLOW + "Dropped! " + ChatColor.GRAY + flag.getDespawnTimerSeconds());
        } else {
            pairs[6].setPrefixAndSuffix(ChatColor.BLUE + "BLU Flag: ", ChatColor.GRAY + "Respawning...");
        }

        if (warlordsPlayer != null) {
            pairs[4].setPrefixAndSuffix(ChatColor.WHITE + "Spec: ", ChatColor.GREEN + warlordsPlayer.getSpec().getClass().getSimpleName());

            pairs[2].setPrefixAndSuffix(
                    ChatColor.GREEN.toString() + warlordsPlayer.getTotalKills() + ChatColor.RESET + " Kills ",
                    ChatColor.GREEN.toString() + warlordsPlayer.getTotalAssists() + ChatColor.RESET + " Assists"
            );
        }

        pairs[0].setPrefixAndSuffix(ChatColor.YELLOW + Warlords.VERSION, "");

        Collections.reverse(Arrays.asList(pairs));

        customScoreboard.giveNewSideBar(init, pairs);
    }

    private static <K, V, M extends Map<K, V>> BinaryOperator<M> mapMerger(BinaryOperator<V> mergeFunction) {
        return (m1, m2) -> {
            for (Map.Entry<K, V> e : m2.entrySet())
                m1.merge(e.getKey(), e.getValue(), mergeFunction);
            return m1;
        };
    }

    // We have to copy this to allow null keys
    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(
            Function<? super T, ? extends K> classifier,
            Supplier<M> mapFactory,
            Collector<? super T, A, D> downstream
    ) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BiConsumer<Map<K, A>, T> accumulator = (m, t) -> {
            K key = classifier.apply(t);
            A container = m.computeIfAbsent(key, k -> downstreamSupplier.get());
            downstreamAccumulator.accept(container, t);
        };
        BinaryOperator<Map<K, A>> merger = PlayingState.<K, A, Map<K, A>>mapMerger(downstream.combiner());
        @SuppressWarnings("unchecked")
        Supplier<Map<K, A>> mangledFactory = (Supplier<Map<K, A>>) mapFactory;
        @SuppressWarnings("unchecked")
        Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();
        Function<Map<K, A>, M> finisher = intermediate -> {
            intermediate.replaceAll((k, v) -> downstreamFinisher.apply(v));
            @SuppressWarnings("unchecked")
            M castResult = (M) intermediate;
            return castResult;
        };
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return Collector.of(mangledFactory, accumulator, merger, finisher, Collector.Characteristics.IDENTITY_FINISH);
        } else {
            return Collector.of(mangledFactory, accumulator, merger, finisher);
        }
    }

    public class Stats {
        private final Team team;
        int points;
        int kills;
        int captures;
        int deaths;

        public Stats(Team team) {
            this.team = team;
        }

        public int points() {
            return points;
        }

        public void setPoints(int points) {
            int oldPoints = this.points;
            this.points = points;
            Bukkit.getPluginManager().callEvent(new WarlordsPointsChangedEvent(game, team, oldPoints, this.points));
        }

        private void addPoints(int i) {
            setPoints(points() + i);
        }

        public int kills() {
            return kills;
        }

        public void setKills(int kills) {
            this.kills = kills;
        }

        public int captures() {
            return captures;
        }

        public void setCaptures(int captures) {
            this.captures = captures;
        }

        public int deaths() {
            return deaths;
        }

        public void setDeaths(int deaths) {
            this.deaths = deaths;
        }

        @Override
        public String toString() {
            return "Stats{" + "points=" + points + ", kills=" + kills + ", captures=" + captures + ", deaths=" + deaths + '}';
        }

    }
}
