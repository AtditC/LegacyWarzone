package com.minehut.warzone.match;

import com.google.common.base.Optional;
import com.google.gson.JsonObject;
import com.minehut.warzone.event.MatchEndEvent;
import com.minehut.warzone.module.ModuleCollection;
import com.minehut.warzone.module.modules.team.TeamModule;
import com.minehut.warzone.Warzone;
import com.minehut.warzone.GameHandler;
import com.minehut.warzone.module.Module;
import com.minehut.warzone.module.ModuleLoadTime;
import com.minehut.warzone.module.modules.startTimer.StartTimer;
import com.minehut.warzone.rotation.LoadedMap;
import com.minehut.warzone.util.json.JsonUtil;
import com.mongodb.DBCollection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.UUID;

public class Match {

    private static int matchNumber = 1;

    private final UUID uuid;
    private final LoadedMap loadedMap;
    private final ModuleCollection<Module> modules;

    private int number;
    private MatchState state;
//    private Document document;

    private JsonObject json;

    private GameType gameType;
    DBCollection gameCollection;

    public Match(UUID id, LoadedMap map) {
        this.uuid = id;
        this.modules = new ModuleCollection<>();
        this.json = JsonUtil.convertFileToJSON(map.getFolder() + "/map.json");
        this.state = MatchState.WAITING;
        this.loadedMap = map;
        this.number = matchNumber;
        matchNumber++;

        if (this.json.has("game")) {
            gameType = GameType.valueOf(this.json.get("game").getAsString().toUpperCase());
        } else {
            gameType = GameType.DTW;
        }

        this.gameCollection = Warzone.getInstance().getDb().getCollection("warzone_" + gameType.toString().toLowerCase() + "_stats");
    }

    public void registerModules() {
        for (ModuleLoadTime time : ModuleLoadTime.getOrdered()) {
            for (Module module : GameHandler.getGameHandler().getModuleFactory().build(this, time)) {
                modules.add(module);
                Warzone.getInstance().getServer().getPluginManager().registerEvents(module, Warzone.getInstance());
            }
        }
        start(30 * 20, true);
    }

    public void unregisterModules() {
        modules.unregisterAll();
    }

    public Match getMatch() {
        return this;
    }

    public boolean isRunning() {
        return getState() == MatchState.PLAYING;
    }

    public MatchState getState() {
        return state;
    }

    public void setState(MatchState state) {
        if (state == null) throw new IllegalArgumentException("MatchState cannot be null!");
        this.state = state;
    }

    public JsonObject getJson() {
        return json;
    }

    public void start(int time) {
        start(time, false);
    }

    public void start(int time, boolean forced) {
        if (state == MatchState.WAITING) {
            StartTimer startTimer = getModules().getModule(StartTimer.class);
            startTimer.setTime(time);
            startTimer.setForced(forced);
            startTimer.setCancelled(false);
            state = MatchState.STARTING;
        }
    }

    public void end(TeamModule team) {
        if (getState() == MatchState.PLAYING) {
            state = MatchState.ENDED;
            Event event = new MatchEndEvent(team == null ? Optional.<TeamModule>absent() : Optional.of(team));
            Bukkit.getServer().getPluginManager().callEvent(event);
        }
    }

    public void end(Player player) {
        if (getState() == MatchState.PLAYING) {
            state = MatchState.ENDED;
            Bukkit.getServer().getPluginManager().callEvent(new MatchEndEvent(player));
        }
    }

    public void end() {
        if (getState() == MatchState.PLAYING) {
            state = MatchState.ENDED;
            Bukkit.getServer().getPluginManager().callEvent(new MatchEndEvent(Optional.<TeamModule>absent()));
        }
    }

    public ModuleCollection<Module> getModules() {
        return modules;
    }

    public int getNumber() {
        return number;
    }

    public LoadedMap getLoadedMap() {
        return loadedMap;
    }

    public UUID getUuid() {
        return uuid;
    }

    public GameType getGameType() {
        return gameType;
    }

    public DBCollection getGameCollection() {
        return gameCollection;
    }
}
