package com.xshards.scheduler;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

import java.util.function.Consumer;

/**
 * Scheduler adapter for Folia and Bukkit compatibility
 */
public class SchedulerAdapter {

    private final Plugin plugin;
    private final GracefulScheduling scheduling;
    private final boolean isFolia;

    public SchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        MorePaperLib morePaperLib = new MorePaperLib(plugin);
        this.scheduling = morePaperLib.scheduling();
        this.isFolia = scheduling.isUsingFolia();
    }

    /**
     * Check if running on Folia
     */
    public boolean isFolia() {
        return isFolia;
    }

    /**
     * Run task asynchronously
     */
    public void runAsync(Runnable task) {
        scheduling.asyncScheduler().run(task);
    }

    /**
     * Run task on global region (main thread on Bukkit)
     */
    public void runGlobal(Runnable task) {
        scheduling.globalRegionalScheduler().run(task);
    }

    /**
     * Run task with delay on global region
     */
    public void runGlobalDelayed(Runnable task, long delayTicks) {
        scheduling.globalRegionalScheduler().runDelayed(task, delayTicks);
    }

    /**
     * Run repeating task on global region
     */
    public void runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        scheduling.globalRegionalScheduler().runAtFixedRate(task, delayTicks, periodTicks);
    }

    /**
     * Run task at a specific location (region-specific on Folia)
     */
    public void runAtLocation(Location location, Runnable task) {
        scheduling.regionSpecificScheduler(location).run(task);
    }

    /**
     * Run task at a specific location with delay
     */
    public void runAtLocationDelayed(Location location, Runnable task, long delayTicks) {
        scheduling.regionSpecificScheduler(location).runDelayed(task, delayTicks);
    }

    /**
     * Run repeating task at a specific location
     */
    public void runAtLocationTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        scheduling.regionSpecificScheduler(location).runAtFixedRate(task, delayTicks, periodTicks);
    }

    /**
     * Check if current thread is the global region thread
     */
    public boolean isGlobalThread() {
        return scheduling.isOnGlobalRegionThread();
    }

    /**
     * Cancel all tasks
     */
    public void cancelAllTasks() {
        scheduling.cancelGlobalTasks();
    }
}