package com.ebicep.warlords.util.bukkit;

import com.ebicep.warlords.util.chat.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TeleportUtils {
    private static Set<Object> teleportFlags;

    private static Constructor packetConstructor;
    private static Constructor vec3D;

    private static Method position;
    private static Method closeInventory;
    private static Method sendMethod;

    private static Field connectionField;
    private static Field justTeleportedField;
    private static Field teleportPosField;
    private static Field lastPosXField;
    private static Field lastPosYField;
    private static Field lastPosZField;
    private static Field teleportAwaitField;
    private static Field AField;
    private static Field eField;
    private static Field yaw;
    private static Field pitch;
    private static Field activeContainer;
    private static Field defaultContainer;

    static {
        Class<?> packet = getNmsClass("Packet");
        Class<?> entity = getNmsClass("Entity");
        Class<?> entityPlayer = getNmsClass("EntityPlayer");
        Class<?> entityHuman = getNmsClass("EntityHuman");
        Class<?> connectionClass = getNmsClass("PlayerConnection");
        Class<?> packetClass = getNmsClass("PacketPlayOutPosition");
        Class<?> vecClass = getNmsClass("Vec3D");
        try {
            sendMethod = connectionClass.getMethod("sendPacket", packet);

            position = entity.getDeclaredMethod("setLocation", Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE);
            closeInventory = entityPlayer.getDeclaredMethod("closeInventory");

            yaw = getField(entity, "yaw");
            pitch = getField(entity, "pitch");
            connectionField = getField(entityPlayer, "playerConnection");
            activeContainer = getField(entityHuman, "activeContainer");
            defaultContainer = getField(entityHuman, "defaultContainer");

            packetConstructor = packetClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Set.class);
            vec3D = vecClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE);

            Object[] enumObjects = getNmsClass("PacketPlayOutPosition$EnumPlayerTeleportFlags").getEnumConstants();
            teleportFlags = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(enumObjects[4], enumObjects[3])));

            justTeleportedField = getField(connectionClass, "justTeleported");
            //teleportPosField = getField(connectionClass, "teleportPos");
            lastPosXField = getField(connectionClass, "lastPosX");
            lastPosYField = getField(connectionClass, "lastPosY");
            lastPosZField = getField(connectionClass, "lastPosZ");
            //teleportAwaitField = getField(connectionClass, "teleportAwait");
            //AField = getField(connectionClass, "A");
            //eField = getField(connectionClass, "e");
        } catch (Exception e) {
            ChatUtils.MessageType.WARLORDS.sendErrorMessage(e.getMessage());
        }
    }

    private static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static void teleport(Player player, Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        Object handle = getHandle(player);
        try {
            if(activeContainer.get(handle) != defaultContainer.get(handle)) closeInventory.invoke(handle);
            position.invoke(handle, x,y,z, yaw.get(handle), pitch.get(handle));
            Object connection = connectionField.get(handle);
            justTeleportedField.set(connection, true);
            //teleportPosField.set(connection, vec3D.newInstance(x, y, z));
            lastPosXField.set(connection, x);
            lastPosYField.set(connection, y);
            lastPosZField.set(connection, z);
            //int teleportAwait = teleportAwaitField.getInt(connection) + 1;
            //if(teleportAwait == 2147483647) teleportAwait = 0;
            //teleportAwaitField.set(connection, teleportAwait);
            //AField.set(connection, eField.get(connection));

            Object packet = packetConstructor.newInstance(x, y, z, 0, 0, teleportFlags);
            sendPacket(packet, player);
        } catch (Exception e) {
            ChatUtils.MessageType.WARLORDS.sendErrorMessage(e.getMessage());
        }
    }

    private static void sendPacket(Object packet, Player p) {
        try {
            Object handle = getHandle(p);
            Object pConnection = connectionField.get(handle);
            sendMethod.invoke(pConnection, packet);
        } catch (Exception var9) {
            var9.printStackTrace();
        }
    }

    private static Object getHandle(Entity entity) {
        try {
            Method entity_getHandle = entity.getClass().getMethod("getHandle");
            return entity_getHandle.invoke(entity);
        } catch (Exception var2) {
            var2.printStackTrace();
            return null;
        }
    }
    private static Class<?> getNmsClass(String name) {
        Class clazz = null;

        try {
            clazz = Class.forName("net.minecraft.server." + getVersion() + "." + name);
        } catch (ClassNotFoundException var3) {
            var3.printStackTrace();
        }

        return clazz;
    }

    private static String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().substring(23);
    }
}