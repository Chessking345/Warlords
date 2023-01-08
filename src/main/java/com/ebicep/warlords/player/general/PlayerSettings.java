package com.ebicep.warlords.player.general;

import com.ebicep.warlords.database.DatabaseManager;
import com.ebicep.warlords.game.Team;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.ebicep.warlords.player.general.ArmorManager.Helmets.*;
import static com.ebicep.warlords.player.general.Weapons.FELFLAME_BLADE;

public class PlayerSettings {

    public static final HashMap<UUID, PlayerSettings> PLAYER_SETTINGS = new HashMap<>();

    private final UUID uuid;
    private final HashMap<Specializations, SkillBoosts> classesSkillBoosts = new HashMap<>() {{
        for (Specializations value : Specializations.VALUES) {
            put(value, value.skillBoosts.get(0));
        }
    }};
    private final HashMap<Specializations, Weapons> weaponSkins = new HashMap<>() {{
        for (Specializations value : Specializations.VALUES) {
            put(value, FELFLAME_BLADE);
        }
    }};
    private Specializations selectedSpec = Specializations.PYROMANCER;
    private Settings.HotkeyMode hotkeyMode = Settings.HotkeyMode.NEW_MODE;
    private Settings.ParticleQuality particleQuality = Settings.ParticleQuality.HIGH;
    private Settings.FlagMessageMode flagMessageMode = Settings.FlagMessageMode.ABSOLUTE;
    /**
     * Preferred team in the upcoming warlords game
     */
    private transient Team wantedTeam = null;

    private ArmorManager.Helmets mageHelmet = SIMPLE_MAGE_HELMET;
    private ArmorManager.Helmets warriorHelmet = SIMPLE_WARRIOR_HELMET;
    private ArmorManager.Helmets paladinHelmet = SIMPLE_PALADIN_HELMET;
    private ArmorManager.Helmets shamanHelmet = SIMPLE_SHAMAN_HELMET;
    private ArmorManager.Helmets rogueHelmet = SIMPLE_ROGUE_HELMET;
    private ArmorManager.ArmorSets mageArmor = ArmorManager.ArmorSets.SIMPLE_CHESTPLATE;
    private ArmorManager.ArmorSets warriorArmor = ArmorManager.ArmorSets.SIMPLE_CHESTPLATE;
    private ArmorManager.ArmorSets paladinArmor = ArmorManager.ArmorSets.SIMPLE_CHESTPLATE;
    private ArmorManager.ArmorSets shamanArmor = ArmorManager.ArmorSets.SIMPLE_CHESTPLATE;
    private ArmorManager.ArmorSets rogueArmor = ArmorManager.ArmorSets.SIMPLE_CHESTPLATE;

    private Settings.ChatSettings.ChatDamage chatDamageMode = Settings.ChatSettings.ChatDamage.ALL;
    private Settings.ChatSettings.ChatHealing chatHealingMode = Settings.ChatSettings.ChatHealing.ALL;
    private Settings.ChatSettings.ChatEnergy chatEnergyMode = Settings.ChatSettings.ChatEnergy.ALL;

    public PlayerSettings(UUID uuid) {
        this.uuid = uuid;
    }

    @Nonnull
    public static PlayerSettings getPlayerSettings(@Nonnull Player player) {
        return getPlayerSettings(player.getUniqueId());
    }

    @Nonnull
    public static PlayerSettings getPlayerSettings(@Nonnull UUID uuid) {
        return PLAYER_SETTINGS.computeIfAbsent(uuid, (k) -> new PlayerSettings(uuid));
    }

    public SkillBoosts getSkillBoostForClass() {
        return classesSkillBoosts.get(selectedSpec);
    }

    public HashMap<Specializations, SkillBoosts> getClassesSkillBoosts() {
        return classesSkillBoosts;
    }

    public void setSkillBoostForSelectedSpec(SkillBoosts classesSkillBoost) {
        if (classesSkillBoost != null) {
            classesSkillBoosts.put(selectedSpec, classesSkillBoost);
        }
    }

    public void setSpecsSkillBoosts(HashMap<Specializations, SkillBoosts> classesSkillBoosts) {
        if (classesSkillBoosts != null) {
            classesSkillBoosts.values().removeAll(Collections.singleton(null));
            this.classesSkillBoosts.putAll(classesSkillBoosts);
        }
    }

    @Nullable
    public Team getWantedTeam() {
        if (wantedTeam == null) {
            Team newTeam = Math.random() <= .5 ? Team.BLUE : Team.RED;
            setWantedTeam(newTeam);
            return newTeam;
        }
        return wantedTeam;
    }

    public void setWantedTeam(@Nullable Team wantedTeam) {
        this.wantedTeam = wantedTeam;
    }

    public Weapons getWeaponSkinForSelectedSpec() {
        return this.getWeaponSkins().getOrDefault(this.getSelectedSpec(), FELFLAME_BLADE);
    }

    public HashMap<Specializations, Weapons> getWeaponSkins() {
        return weaponSkins;
    }

    @Nonnull
    public Specializations getSelectedSpec() {
        if (selectedSpec == null) {
            System.out.println("ERROR: SELECTED SPEC IS NULL");
            return Specializations.PYROMANCER;
        }
        return selectedSpec;
    }

    public void setSelectedSpec(Specializations selectedSpec) {
        if (selectedSpec != null) {
            this.selectedSpec = selectedSpec;
        }
    }

    public void setWeaponSkins(HashMap<Specializations, Weapons> weaponSkins) {
        if (weaponSkins != null) {
            weaponSkins.values().removeAll(Collections.singleton(null));
            this.weaponSkins.putAll(weaponSkins);
        }
    }

    public Settings.ParticleQuality getParticleQuality() {
        return particleQuality;
    }

    public void setParticleQuality(Settings.ParticleQuality particleQuality) {
        this.particleQuality = particleQuality;
    }

    public Settings.HotkeyMode getHotkeyMode() {
        return hotkeyMode;
    }

    public void setHotkeyMode(Settings.HotkeyMode hotkeyMode) {
        this.hotkeyMode = hotkeyMode;
    }

    public Settings.FlagMessageMode getFlagMessageMode() {
        return flagMessageMode;
    }

    public void setFlagMessageMode(Settings.FlagMessageMode flagMessageMode) {
        this.flagMessageMode = flagMessageMode;
    }

    public ArmorManager.Helmets getHelmet(Specializations spec) {
        int index = spec.ordinal() / 3;
        return getHelmets().get(index);
    }

    public List<ArmorManager.Helmets> getHelmets() {
        List<ArmorManager.Helmets> armorSets = new ArrayList<>();
        armorSets.add(mageHelmet);
        armorSets.add(warriorHelmet);
        armorSets.add(paladinHelmet);
        armorSets.add(shamanHelmet);
        armorSets.add(rogueHelmet);
        return armorSets;
    }

    public ArmorManager.Helmets getHelmet(Classes classes) {
        return getHelmets().get(classes.ordinal());
    }

    public ArmorManager.ArmorSets getArmorSet(Specializations spec) {
        int index = spec.ordinal() / 3;
        return getArmorSets().get(index);
    }

    public List<ArmorManager.ArmorSets> getArmorSets() {
        List<ArmorManager.ArmorSets> armorSets = new ArrayList<>();
        armorSets.add(mageArmor);
        armorSets.add(warriorArmor);
        armorSets.add(paladinArmor);
        armorSets.add(shamanArmor);
        armorSets.add(rogueArmor);
        return armorSets;
    }

    public ArmorManager.ArmorSets getArmorSet(Classes classes) {
        return getArmorSets().get(classes.ordinal());
    }

    public void setHelmet(Classes classes, ArmorManager.Helmets helmet) {
        switch (classes) {
            case MAGE:
                this.mageHelmet = helmet;
                break;
            case WARRIOR:
                this.warriorHelmet = helmet;
                break;
            case PALADIN:
                this.paladinHelmet = helmet;
                break;
            case SHAMAN:
                this.shamanHelmet = helmet;
                break;
            case ROGUE:
                this.rogueHelmet = helmet;
                break;
        }
        DatabaseManager.updatePlayer(uuid, databasePlayer -> databasePlayer.getClass(classes).setHelmet(helmet));
    }

    public void setArmor(Classes classes, ArmorManager.ArmorSets armor) {
        switch (classes) {
            case MAGE:
                this.mageArmor = armor;
                break;
            case WARRIOR:
                this.warriorArmor = armor;
                break;
            case PALADIN:
                this.paladinArmor = armor;
                break;
            case SHAMAN:
                this.shamanArmor = armor;
                break;
            case ROGUE:
                this.rogueArmor = armor;
                break;
        }
        DatabaseManager.updatePlayer(uuid, databasePlayer -> databasePlayer.getClass(classes).setArmor(armor));
    }

    public Settings.ChatSettings.ChatDamage getChatDamageMode() {
        return chatDamageMode;
    }

    public void setChatDamageMode(Settings.ChatSettings.ChatDamage chatDamageMode) {
        this.chatDamageMode = chatDamageMode;
    }

    public Settings.ChatSettings.ChatHealing getChatHealingMode() {
        return chatHealingMode;
    }

    public void setChatHealingMode(Settings.ChatSettings.ChatHealing chatHealingMode) {
        this.chatHealingMode = chatHealingMode;
    }

    public Settings.ChatSettings.ChatEnergy getChatEnergyMode() {
        return chatEnergyMode;
    }

    public void setChatEnergyMode(Settings.ChatSettings.ChatEnergy chatEnergyMode) {
        this.chatEnergyMode = chatEnergyMode;
    }
}
