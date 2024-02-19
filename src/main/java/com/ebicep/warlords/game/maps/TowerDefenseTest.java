package com.ebicep.warlords.game.maps;

import com.ebicep.warlords.game.GameAddon;
import com.ebicep.warlords.game.GameMap;
import com.ebicep.warlords.game.GameMode;
import com.ebicep.warlords.game.Team;
import com.ebicep.warlords.game.option.BasicScoreboardOption;
import com.ebicep.warlords.game.option.Option;
import com.ebicep.warlords.game.option.SpawnpointOption;
import com.ebicep.warlords.game.option.marker.LobbyLocationMarker;
import com.ebicep.warlords.game.option.marker.TeamMarker;
import com.ebicep.warlords.game.option.pve.CurrencyOnEventOption;
import com.ebicep.warlords.game.option.towerdefense.TowerBuildOption;
import com.ebicep.warlords.game.option.towerdefense.TowerDefenseOption;
import com.ebicep.warlords.game.option.towerdefense.TowerDefenseSpawner;
import com.ebicep.warlords.util.bukkit.LocationBuilder;
import com.ebicep.warlords.util.bukkit.LocationFactory;
import com.ebicep.warlords.util.java.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;

import java.util.EnumSet;
import java.util.List;

import static com.ebicep.warlords.util.warlords.GameRunnable.SECOND;

public class TowerDefenseTest extends GameMap {

    public TowerDefenseTest() {
        super(
                "Tower Defense Test",
                32,
                12,
                60 * SECOND,
                "TD",
                3,
                GameMode.TOWER_DEFENSE
        );
    }

    @Override
    public List<Option> initMap(GameMode category, LocationFactory loc, EnumSet<GameAddon> addons) {
        List<Option> options = category.initMap(this, loc, addons);
        options.add(TeamMarker.create(Team.GAME, Team.BLUE, Team.RED).asOption());
        options.add(LobbyLocationMarker.create(loc.addXYZ(0.5, 65, 0.5, 90, 0), Team.BLUE).asOption());
        options.add(LobbyLocationMarker.create(loc.addXYZ(0.5, 65, 0.5, 90, 0), Team.RED).asOption());


        LocationBuilder blueSpawn = loc.addXYZ(2.5, 65, 0.5, 90, 0);
        LocationBuilder redSpawn = loc.addXYZ(-52.5, 65, 8.5, -90, 0);
        options.add(SpawnpointOption.forTeam(blueSpawn, Team.BLUE));
        options.add(SpawnpointOption.forTeam(redSpawn, Team.RED));

        options.add(new TowerDefenseOption()
                .addCastle(Team.BLUE, loc.addXYZ(-39.5, 70, -9.5), 100_000)
                .addCastle(Team.RED, loc.addXYZ(-10.5, 70, 18.5), 100_000)
        );

        List<Location> bluePath1 = List.of(
                loc.addXYZ(-12.5, 65, 0.5),
                loc.addXYZ(-12.5, 65, -16.5),
                loc.addXYZ(-22.5, 65, -16.5),
                loc.addXYZ(-22.5, 65, -9.5),
                loc.addXYZ(-39.5, 65, -9.5)
        );
        List<Location> bluePath2 = List.of(
                loc.addXYZ(-12.5, 65, 0.5),
                loc.addXYZ(-26.5, 65, 0.5),
                loc.addXYZ(-26.5, 65, -9.5),
                loc.addXYZ(-39.5, 65, -9.5)
        );
        List<Location> redPath1 = List.of(
                loc.addXYZ(-37.5, 65, 8.5),
                loc.addXYZ(-37.5, 65, 25.5),
                loc.addXYZ(-27.5, 65, 25.5),
                loc.addXYZ(-27.5, 65, 18.5),
                loc.addXYZ(-10.5, 65, 18.5)
        );
        List<Location> redPath2 = List.of(
                loc.addXYZ(-37.5, 65, 8.5),
                loc.addXYZ(-23.5, 65, 8.5),
                loc.addXYZ(-23.5, 65, 18.5),
                loc.addXYZ(-10.5, 65, 18.5)
        );
        options.add(new TowerDefenseSpawner()
                        .addPath(blueSpawn, bluePath1)
                        .addPath(blueSpawn, bluePath2)
                        .addPath(redSpawn, redPath1)
                        .addPath(redSpawn, redPath2)
//                .add(new FixedWave()
//                        .add(Mob.ZOMBIE_I, 1)
//                        .delay(5 * SECOND)
//                        .add(Mob.ZOMBIE_I, 5)
//                        .delay(10 * SECOND)
//                        .add(Mob.ZOMBIE_I, 5)
//                )
//                .add(new FixedWave()
//                        .add(Mob.ZOMBIE_I, 5)
//                )
        );
        options.add(new TowerBuildOption()
                .addBuildableArea(Team.BLUE, loc.addXYZ(-0.5, 60, 3.5), loc.addXYZ(-49.5, 80, -23.5))
                .addBuildableArea(Team.RED, loc.addXYZ(-0.5, 60, 5.5), loc.addXYZ(-49.5, 80, 32.5))
        );

        for (Option option : options) {
            if (option instanceof TowerDefenseOption towerDefenseOption) {
                options.add(new CurrencyOnEventOption()
                        .setCurrentCurrencyDisplay(warlordsEntity -> Component.text("Insignia: ", NamedTextColor.YELLOW)
                                                                              .append(Component.text("❂ " + NumberFormat.formatOptionalHundredths(warlordsEntity.getCurrency()),
                                                                                      NamedTextColor.GOLD
                                                                              )))
                        .setCurrencyRate(new CurrencyOnEventOption.CurrencyRate(
                                20 * 5,
                                warlordsEntity -> towerDefenseOption.getPlayerInfo(warlordsEntity).getIncomeRate(),
                                warlordsEntity -> Component.text("Income: ", NamedTextColor.DARK_AQUA)
                                                           .append(Component.text("❂ " + NumberFormat.formatOptionalHundredths(towerDefenseOption.getPlayerInfo(warlordsEntity)
                                                                                                                                                 .getIncomeRate()),
                                                                   NamedTextColor.GOLD
                                                           )),
                                nextCurrency -> Component.text("Next Income: ", NamedTextColor.GRAY).append(Component.text(nextCurrency, NamedTextColor.WHITE))
                        ))
                );
                break;
            }
        }

        options.add(new BasicScoreboardOption());

        return options;
    }
}
