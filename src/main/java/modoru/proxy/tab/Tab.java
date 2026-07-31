package modoru.proxy.tab;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import modoru.proxy.Configuration;
import modoru.proxy.tab.network.packet.PacketRegistration;
import modoru.proxy.tab.network.packet.UpdateTeamPacket;
import modoru.proxy.util.NetworkUtil;
import modoru.proxy.util.placeholder.DynamicPlaceholder;
import modoru.proxy.util.placeholder.PlaceholdersUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class Tab {

    private static final DynamicPlaceholder<TabEntry>[] PLACEHOLDERS = create(
            DynamicPlaceholder.create("player_name", entry -> entry.player.getUsername()),
            DynamicPlaceholder.create("formatted_name", entry -> entry.formattedName),
            DynamicPlaceholder.create("ping", entry -> entry.player.getPing()),
            DynamicPlaceholder.create(
                    "global_online",
                    entry -> entry.tab.proxyServer.getAllPlayers()
                            .stream()
                            .map(Player::getCurrentServer)
                            .filter(Optional::isPresent)
                            .count()
            )
    );


    @SafeVarargs
    private static DynamicPlaceholder<TabEntry>[] create(DynamicPlaceholder<TabEntry>... placeholders) {
        return placeholders;
    }

    private final ProxyServer proxyServer;
    private final ScheduledExecutorService executorService;
    private final Configuration configuration;
    private final BackendCommunication backendCommunication;

    private final MiniMessage miniMessage;
    private final Map<Player, TabEntry> tabEntries;
    private final SequencedMap<Key, Comparator<TabEntry>> sorters;

    private @Nullable ScheduledFuture<?> updateTask;

    public Tab(ProxyServer proxyServer, ScheduledExecutorService executorService, Configuration configuration, BackendCommunication backendCommunication) {
        this.proxyServer = proxyServer;
        this.executorService = executorService;
        this.configuration = configuration;
        this.backendCommunication = backendCommunication;

        this.miniMessage = MiniMessage.miniMessage();
        this.tabEntries = new HashMap<>();
        this.sorters = new LinkedHashMap<>();

        sorters.putLast(
                Key.key("modoru", "alphabetical"),
                (first, second) -> first.player.getUsername().compareToIgnoreCase(second.player.getUsername())
        );

        PacketRegistration.bootstrap();
    }

    public void start() {
        if(updateTask != null) return;

        var config = configuration.tab;
        final long oneTickMillis = 50L;
        updateTask = executorService.scheduleAtFixedRate(
                this::update,
                oneTickMillis * 2,
                config.updateIntervalSeconds * 1000L,
                TimeUnit.MILLISECONDS
        );
    }

    public void stop() {
        if(updateTask == null) return;

        for (TabEntry value : tabEntries.values()) {
            clearFakeTeams(value);
        }
        tabEntries.clear();

        updateTask.cancel(true);
        updateTask = null;
    }

    private void clearFakeTeams(TabEntry entry) {
        if(entry.fakeTeams.isEmpty()) return;

        for (String oldTeam : entry.fakeTeams) {
            NetworkUtil.sendPacket(entry.player, UpdateTeamPacket.createRemovePacket(oldTeam));
        }
        entry.fakeTeams.clear();
    }

    private void update() {
        List<TabEntry> list = new ArrayList<>(tabEntries.values());
        list.sort(TabEntry::compareTo);

        int listSize = list.size();
        int maxIndexLength = String.valueOf(listSize).length();

        long updateTimestamp = System.currentTimeMillis();
        for (int i = 0; i < listSize; i++) {
            TabEntry entry = list.get(i);
            clearFakeTeams(entry);

            String playerName = entry.player.getUsername();
            String teamName = String.format("%0" + maxIndexLength + "d", i);
            if(!teamName.equalsIgnoreCase(entry.teamName)) {
                entry.freshTeamName = true;
                entry.teamName = teamName;

                entry.teamAddPacket = UpdateTeamPacket.createAddOrModifyPacket(
                        teamName,
                        new UpdateTeamPacket.Parameters(
                                Component.empty(),
                                Component.empty(),
                                Component.empty(),
                                UpdateTeamPacket.TeamVisibility.ALWAYS,
                                UpdateTeamPacket.TeamCollisionRule.ALWAYS,
                                null,
                                (byte) 3 // first bit is set to 1 if team allows friendly fire, second if team members can see each other when invisible; both are true by default, and 1 | 2 = 3
                        ),
                        Set.of(playerName),
                        true
                );
            }

            var formattedUsername = backendCommunication.formattedUsername(entry.player);
            if(formattedUsername.receiveTimestamp() > entry.lastNameUpdate) {
                entry.lastNameUpdate = updateTimestamp;
                entry.freshDisplayName = true;

                entry.formattedName = formattedUsername.formattedUsername();
                entry.displayName = miniMessage.deserialize(entry.formattedName);

                var updateDisplayNameEntry = new UpsertPlayerInfoPacket.Entry(entry.player.getUniqueId());
                updateDisplayNameEntry.setDisplayName(new ComponentHolder(entry.player.getProtocolVersion(), entry.displayName));

                entry.updateDisplayNamePacket = new UpsertPlayerInfoPacket(
                        EnumSet.of(UpsertPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME),
                        List.of(updateDisplayNameEntry)
                );
            }

            for (int j = 0; j < listSize; j++) {
                TabEntry viewerEntry = list.get(j);
                Player viewer = viewerEntry.player;
                if(entry != viewerEntry) {
                    TabEntry.ListedResult result = viewerEntry.isListed(entry);
                    if(result.shouldUpdate()) {
                        var packetEntry = new UpsertPlayerInfoPacket.Entry(viewer.getUniqueId());
                        packetEntry.setListed(result.listed());

                        NetworkUtil.sendPacket(viewer, new UpsertPlayerInfoPacket(
                                EnumSet.of(UpsertPlayerInfoPacket.Action.UPDATE_LISTED),
                                List.of(packetEntry)
                        ));
                    }

                    if(!result.listed()) continue;
                }

                entry.fakeTeams.add(teamName);

                if(entry.freshDisplayName) {
                    assert entry.updateDisplayNamePacket != null;
                    NetworkUtil.sendPacket(viewer, entry.updateDisplayNamePacket);
                }
                if(entry.freshTeamName) {
                    assert entry.teamAddPacket != null;
                    NetworkUtil.sendPacket(viewer, entry.teamAddPacket);
                }
            }

            entry.freshDisplayName = false;
            entry.freshTeamName = false;

            var headerAndFooter = backendCommunication.headerAndFooter(entry.player);
            entry.player.sendPlayerListHeaderAndFooter(
                    buildHeaderOrFooter(entry, headerAndFooter.header()),
                    buildHeaderOrFooter(entry, headerAndFooter.footer())
            );
        }
    }

    private Component buildHeaderOrFooter(TabEntry entry, List<String> lines) {
        return miniMessage.deserialize(PlaceholdersUtil.resolveDynamic(
                String.join("\n", lines),
                entry,
                '%', '%',
                PLACEHOLDERS
        ));
    }

    void addPlayer(Player player) {
        if(tabEntries.containsKey(player)) return;
        tabEntries.put(player, new TabEntry(
                this,
                player,
                sorters,
                _ -> true // todo: unlisted players
        ));
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
