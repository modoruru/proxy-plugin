package modoru.proxy.tab;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import modoru.proxy.Configuration;
import net.kyori.adventure.key.Key;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

public final class Tab {

    private final ProxyServer proxyServer;
    private final ScheduledExecutorService executorService;
    private final Configuration configuration;

    private final Map<Player, TabEntry> tabEntries;
    private final SequencedMap<Key, Comparator<TabEntry>> sorters;

    public Tab(ProxyServer proxyServer, ScheduledExecutorService executorService, Configuration configuration) {
        this.proxyServer = proxyServer;
        this.executorService = executorService;
        this.configuration = configuration;

        this.tabEntries = new HashMap<>();
        this.sorters = new LinkedHashMap<>();
    }

    void addPlayer(Player player) {
        if(tabEntries.containsKey(player)) return;
        // todo: unlisted players
        tabEntries.put(player, new TabEntry(player, sorters, _ -> true));
    }

    void removePlayer(Player player) {
        TabEntry removed = tabEntries.remove(player);
        if(removed == null) return;

        for (TabEntry entry : tabEntries.values()) {
            entry.unlisted.remove(removed);
            entry.player.getTabList().removeEntry(player.getUniqueId());
        }
    }

}
