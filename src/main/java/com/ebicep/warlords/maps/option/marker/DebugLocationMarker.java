package com.ebicep.warlords.maps.option.marker;

import com.ebicep.warlords.util.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Marks a location for the debug screen/command
 */
public interface DebugLocationMarker extends LocationMarker {

    /**
     * Gets the name of the marker
     *
     * @return the name
     */
    @Nonnull
    String getName();

    /**
     * Get the extra debug information for this marker. Different calls to this
     * may return a list of a different length.
     *
     * @return The list of extra debug info
     */
    @Nonnull
    List<String> getExtra();

    /**
     * Gets the creator of this debug marker.
     *
     * @return the creator
     */
    @Nonnull
    Class<?> getCreator();

    /**
     * Gets the current location of the marker. Returned locations objects may
     * or may not change on their own, and modification of these objects results
     * in undefined behavior, which includes the actual game object moving
     *
     * @return The current location of this marker
     */
    @Nonnull
    @Override
    Location getLocation();

    /**
     * Gets the material shown in the debug screen
     *
     * @return
     */
    @Nonnull
    Material getMaterial();

    /**
     * Gets the data value belonging to the
     *
     * @return
     */
    short getMaterialData();

    /**
     * Converts this debug marker into an item for the debug screen
     *
     * @return
     */
    @Nonnull
    default ItemStack getAsItem() {
        ItemBuilder item = new ItemBuilder(getMaterial(), getMaterialData());
        item.name(getName());
        Location loc = getLocation();
        List<String> lore = new ArrayList<>();
        lore.add("XYZ: " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + " Yaw/pitch: " + loc.getYaw() + "/" + loc.getPitch());
        lore.add("Source: " + getCreator().getName());
        lore.addAll(getExtra());
        item.lore(lore);
        return item.get();
    }

    static DebugLocationMarker create(@Nullable Material material, int data, Class<?> creator, String name, Location location) {
        return create(material, data, () -> creator, () -> name, () -> location, Collections::emptyList);
    }

    static DebugLocationMarker create(@Nullable Material material, int data, Class<?> creator, String name, Location location, Supplier<List<String>> extra) {
        return create(material, data, () -> creator, () -> name, () -> location, extra);
    }

    static DebugLocationMarker create(@Nullable Material material, int data, Class<?> creator, String name, Supplier<Location> location, Supplier<List<String>> extra) {
        return create(material, data, () -> creator, () -> name, location, extra);
    }

    static DebugLocationMarker create(@Nullable Material material, int data, Class<?> creator, Supplier<String> name, Supplier<Location> location) {
        return create(material, data, () -> creator, name, location, Collections::emptyList);
    }

    static DebugLocationMarker create(@Nullable Material material, int data, Class<?> creator, Supplier<String> name, Supplier<Location> location, Supplier<List<String>> extra) {
        return create(material, data, () -> creator, name, location, extra);
    }

    static DebugLocationMarker create(@Nullable Material material, int data, Supplier<Class<?>> creator, Supplier<String> name, Supplier<Location> location) {
        return create(material, data, creator, name, location, Collections::emptyList);
    }

    static DebugLocationMarker create(@Nullable Material material, int data, Supplier<Class<?>> creator, Supplier<String> name, Supplier<Location> location, Supplier<List<String>> extra) {
        Material newMaterial = material == null ? Material.BARRIER : material;
        return create(() -> newMaterial, () -> data, creator, name, location, Collections::emptyList);
    }
    static DebugLocationMarker create(Supplier<Material> material, IntSupplier data, Supplier<Class<?>> creator, Supplier<String> name, Supplier<Location> location, Supplier<List<String>> extra) {
        return new DebugLocationMarker() {
            @Override
            public String getName() {
                return name.get();
            }

            @Override
            public Class<?> getCreator() {
                return creator.get();
            }

            @Override
            public Location getLocation() {
                return location.get();
            }

            @Override
            public List<String> getExtra() {
                return extra.get();
            }

            @Override
            public Material getMaterial() {
                return material.get();
            }

            @Override
            public short getMaterialData() {
                return (short) data.getAsInt();
            }
            
            @Override
            public String toString() {
                return getCreator().getName() + ": " + getName() + ": " + getLocation() + " - " + getExtra();
            }
        };
    }
}
