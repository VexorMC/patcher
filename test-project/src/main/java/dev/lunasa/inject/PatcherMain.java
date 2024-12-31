package dev.lunasa.inject;

import net.minecraft.client.main.Main;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.lang.reflect.InvocationTargetException;

public class PatcherMain {
    public static LaunchClassLoader classLoader;

    public static void main(String[] args) {
        try {
            Class<Main> mainClass = (Class<Main>) classLoader.loadClass("net.minecraft.client.main.Main");

            mainClass.getMethod("main", String[].class).invoke(null, args);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
