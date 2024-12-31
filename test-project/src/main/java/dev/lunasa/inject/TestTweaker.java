package dev.lunasa.inject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.lunasa.patcher.example.ClassPatcher;
import dev.lunasa.patcher.inject.GDiffTransformer;
import net.minecraft.client.main.Main;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestTweaker implements ITweaker {
    private List<String> launchArgs;

    private LaunchClassLoader classLoader;

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.launchArgs = new ArrayList<>(args);

        if (!this.launchArgs.contains("--version"))
        {
            launchArgs.add("--version");
            launchArgs.add(profile != null ? profile : "UnknownFMLProfile");
        }

        if (!this.launchArgs.contains("--gameDir") && gameDir != null)
        {
            launchArgs.add("--gameDir");
            launchArgs.add(gameDir.getAbsolutePath());
        }

        if (!this.launchArgs.contains("--assetsDir") && assetsDir != null)
        {
            launchArgs.add("--assetsDir");
            launchArgs.add(assetsDir.getAbsolutePath());
        }

        ClassPatcher.INSTANCE.discoverPatches();
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader launchClassLoader) {
        PatcherMain.classLoader = launchClassLoader;

        launchClassLoader.registerTransformer("net.minecraft.launchwrapper.injector.VanillaTweakInjector");
        launchClassLoader.registerTransformer(GDiffTransformer.class.getName());
    }

    @Override
    public String getLaunchTarget() {
        return PatcherMain.class.getName();
    }

    @Override
    public String[] getLaunchArguments() {
        return launchArgs.toArray(new String[0]);
    }
}
