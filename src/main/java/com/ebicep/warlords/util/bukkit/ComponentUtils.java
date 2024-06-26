package com.ebicep.warlords.util.bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ComponentUtils {

    public static final Component CLICK_TO_VIEW = Component.text("Click to view!", NamedTextColor.YELLOW);

    public static String getFlattenedText(Component component) {
        OnlyTextFlattener flattener = new OnlyTextFlattener();
        ComponentFlattener.textOnly().flatten(component, flattener);
        return flattener.toString();
    }

    private static class OnlyTextFlattener implements FlattenerListener {

        private final StringBuilder stringBuilder = new StringBuilder();

        @Override
        public void component(@NotNull String text) {
            stringBuilder.append(text);
        }

        @Override
        public String toString() {
            return stringBuilder.toString();
        }
    }

    @Nonnull
    public static TextComponent componentBase() {
        return Component.empty()
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> flattenComponents(List<Component> component) {
        List<Component> components = new ArrayList<>();
        for (Component c : component) {
            List<Component> collection = flattenComponent(c);
            components.addAll(collection);
        }
        return components;
    }

    public static List<Component> flattenComponent(Component component) {
        List<Component> output = new ArrayList<>();
        Component toAdd = null;
        List<Component> components = new ArrayList<>(component.children());
        components.add(0, component.children(new ArrayList<>()));

        for (Component child : components) {
            if (child.equals(Component.newline())) {
                if (toAdd != null) {
                    output.add(toAdd);
                }
                output.add(Component.empty());
                toAdd = null;

            } else {
                if (toAdd == null) {
                    toAdd = child;
                } else {
                    toAdd = toAdd.append(child);
                }
            }
        }
        if (toAdd != null) {
            output.add(toAdd);
        }
        return output;
    }

    public static Component flattenComponentWithNewLine(List<Component> components) {
        return components.stream().collect(Component.toComponent(Component.newline()));
    }

}
