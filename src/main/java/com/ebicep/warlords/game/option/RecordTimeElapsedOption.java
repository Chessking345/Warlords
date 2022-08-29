package com.ebicep.warlords.game.option;

import com.ebicep.warlords.game.Game;
import com.ebicep.warlords.game.option.marker.scoreboard.ScoreboardHandler;
import com.ebicep.warlords.game.option.marker.scoreboard.SimpleScoreboardHandler;
import com.ebicep.warlords.game.state.EndState;
import com.ebicep.warlords.player.ingame.WarlordsEntity;
import com.ebicep.warlords.util.warlords.GameRunnable;
import com.ebicep.warlords.util.warlords.Utils;
import org.bukkit.ChatColor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class RecordTimeElapsedOption implements Option {

    private int ticksElapsed;
    private static final int SCOREBOARD_PRIORITY = 50;

    @Override
    public void start(@Nonnull Game game) {
        new GameRunnable(game) {

            @Override
            public void run() {
                if (game.getState(EndState.class).isPresent()) {
                    this.cancel();
                }
                ticksElapsed++;
            }
        }.runTaskTimer(0, 0);
    }

    @Override
    public void register(@Nonnull Game game) {
        game.registerGameMarker(ScoreboardHandler.class, new SimpleScoreboardHandler(SCOREBOARD_PRIORITY, "spec") {
            @Override
            public List<String> computeLines(@Nullable WarlordsEntity player) {
                return Collections.singletonList("Time: " + ChatColor.GREEN + Utils.formatTimeLeft(ticksElapsed / 20));
            }
        });
    }

    public int getTicksElapsed() {
        return ticksElapsed;
    }

    public static int getTicksElapsed(@Nonnull Game game) {
        for (Option option : game.getOptions()) {
            if (option instanceof RecordTimeElapsedOption) {
                RecordTimeElapsedOption recordTimeElapsedOption = (RecordTimeElapsedOption) option;
                return recordTimeElapsedOption.getTicksElapsed();
            }
        }
        return 0;
    }
}