package com.ebicep.warlords.database.repositories.player.pojos.pve;

import com.ebicep.warlords.database.repositories.games.pojos.pve.DatabaseGamePlayerPvEBase;
import com.ebicep.warlords.database.repositories.games.pojos.pve.DatabaseGamePvEBase;
import com.ebicep.warlords.database.repositories.player.pojos.StatsWarlordsSpecs;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public interface PvEStatsWarlordsSpecs<
        DatabaseGameT extends DatabaseGamePvEBase<DatabaseGamePlayerT>,
        DatabaseGamePlayerT extends DatabaseGamePlayerPvEBase,
        T extends PvEStats<DatabaseGameT, DatabaseGamePlayerT>>
        extends PvEStats<DatabaseGameT, DatabaseGamePlayerT>, StatsWarlordsSpecs<DatabaseGameT, DatabaseGamePlayerT, T> {

    @Nonnull
    private HashMap<String, Long> getStat(Function<PvEStats<DatabaseGameT, DatabaseGamePlayerT>, Map<String, Long>> statFunction) {
        return Arrays.stream(getSpecs())
                     .map(statFunction)
                     .collect(
                             HashMap::new,
                             (m, v) -> v.forEach((k, w) -> m.merge(k, w, Long::sum)),
                             (map1, map2) -> map2.forEach((k, v) -> map1.merge(k, v, Long::sum))
                     );
    }

    @Override
    default long getTotalTimePlayed() {
        return Arrays.stream(getSpecs())
                     .map(PvEStats::getTotalTimePlayed)
                     .reduce(Long::sum)
                     .orElse(0L);
    }

    @Override
    default Map<String, Long> getMobKills() {
        return getStat(PvEStats::getMobKills);
    }

    @Override
    default Map<String, Long> getMobAssists() {
        return getStat(PvEStats::getMobAssists);
    }

    @Override
    default Map<String, Long> getMobDeaths() {
        return getStat(PvEStats::getMobDeaths);
    }

//    @Override
//    default long getMostDamageInRound() {
//        return Arrays.stream(getSpecs())
//                     .map(PvEStats::getMostDamageInRound)
//                     .max(Long::compareTo)
//                     .orElse(0L);
//    }
//
//    @Override
//    default void setMostDamageInRound(long mostDamageInRound) {
//        // only set for specs
//    }

}
