package modoru.proxy.tab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import modoru.proxy.Configuration;
import modoru.proxy.tab.network.FormattedNamesPayload;
import modoru.proxy.util.placeholder.DynamicPlaceholder;
import modoru.proxy.util.placeholder.PlaceholdersUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public final class FormattedNamesHolder {

    private static final DynamicPlaceholder<Player>[] FALLBACK_PLACEHOLDERS;

    static {
        FALLBACK_PLACEHOLDERS = new DynamicPlaceholder[]{
                DynamicPlaceholder.create("player_name", Player::getUsername)
        };
    }

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Configuration configuration;
    private final Map<UUID, CachedName> cachedNames;

    public FormattedNamesHolder(ProxyServer proxyServer, Logger logger, Configuration configuration) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.configuration = configuration;
        this.cachedNames = new HashMap<>();
    }

    public CompletableFuture<String> formattedName(Player player) {
        CachedName cachedName = cachedNames.computeIfAbsent(player.getUniqueId(), CachedName::new);

        if(System.currentTimeMillis() - cachedName.lastReceive > configuration.tab.formattedNames.timeOfRelevanceSeconds * 1000L) {
            byte[] payload = new byte[FormattedNamesPayload.payloadSizeForBackendBound(1)];

            FormattedNamesPayload.fromProxy(Set.of(cachedName.uuid)).encode(Unpooled.wrappedBuffer(payload));
            player.sendPluginMessage(FormattedNamesPayload.IDENTIFIER, payload);
            return cachedName.currentRequest = new CompletableFuture<String>().completeOnTimeout(
                    PlaceholdersUtil.resolveDynamic(
                            configuration.tab.formattedNames.requestTimeoutFallback,
                            player,
                            '%',
                            '%',
                            FALLBACK_PLACEHOLDERS
                    ),
                    configuration.tab.formattedNames.requestTimeoutSeconds * 1000L,
                    TimeUnit.MILLISECONDS
            );
        }

        assert cachedName.lastReceivedName != null;
        return CompletableFuture.<String>completedFuture(cachedName.lastReceivedName);
    }

    public CompletableFuture<Map<UUID, String>> formattedNames(Collection<Player> players) {
        /*
        if player's formatted name was requested recently, plugin skips player and will use old one
        or, if player's formatted name is already requested, plugin will wait to his request to finish and return complete map
         */

        if(players.isEmpty()) return CompletableFuture.completedFuture(Map.of());

        // verify that players are on the same servers
        String serverId = null;
        ServerConnection anyServerConnection = null;
        for (Player player : players) {
            var currentServerOpt = player.getCurrentServer();
            if(currentServerOpt.isEmpty()) return CompletableFuture.failedFuture(new IllegalArgumentException("Players are on the different servers!"));

            ServerConnection currentServer = currentServerOpt.get();
            if(serverId == null) {
                serverId = currentServer.getServerInfo().getName();
                anyServerConnection = currentServer;
                continue;
            }

            if(!serverId.equalsIgnoreCase(currentServer.getServerInfo().getName()))
                return CompletableFuture.failedFuture(new IllegalArgumentException("Players are on the different servers!"));
        }

        Map<UUID, String> skipped = new HashMap<>();
        Set<CachedName> toRequest = new HashSet<>();
        for (Player player : players) {
            CachedName cachedName = cachedNames.computeIfAbsent(player.getUniqueId(), CachedName::new);

            if(cachedName.currentRequest == null || System.currentTimeMillis() - cachedName.lastReceive > configuration.tab.formattedNames.timeOfRelevanceSeconds * 1000L)
                toRequest.add(cachedName);
            else {
                assert cachedName.lastReceivedName != null;
                skipped.put(cachedName.uuid, cachedName.lastReceivedName);
            }
        }

        if(toRequest.isEmpty()) return CompletableFuture.completedFuture(skipped);

        CompletableFuture<Map<UUID, String>> future = new CompletableFuture<>();
        UUID requestUuid = UUID.randomUUID();

        List<CachedName> singleRequesters = new ArrayList<>();
        toRequest.removeIf(cachedName -> {
            if(cachedName.currentRequest != null) {
                singleRequesters.add(cachedName);
                return true;
            }

            cachedName.currentRequest = future.thenApply(map -> map.get(cachedName.uuid));
            cachedName.groupedRequest = future;
            cachedName.groupedRequestUuid = requestUuid;

            return false;
        });

        if(!toRequest.isEmpty()) {
            ByteBuf byteBuf = Unpooled.buffer();
            FormattedNamesPayload.fromProxy(
                    toRequest.stream()
                            .map(cachedName -> cachedName.uuid)
                            .collect(Collectors.toSet())
            ).encode(byteBuf);

            byte[] payload = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(payload);
            anyServerConnection.sendPluginMessage(FormattedNamesPayload.IDENTIFIER, payload);
            logger.warn("Sent {} payload with {} entries", FormattedNamesPayload.IDENTIFIER.toString(), toRequest.size());
        }

        CompletableFuture<Map<UUID, String>> toReturnFuture = future;

        for (CachedName singleRequester : singleRequesters) {
            assert singleRequester.currentRequest != null;

            toReturnFuture = toReturnFuture.thenCombine(
                    singleRequester.currentRequest,
                    (first, second) -> {
                        first.put(singleRequester.uuid, second);
                        return first;
                    }
            );
        }

        return toReturnFuture.thenApply(map -> {
            map.putAll(skipped);
            return map;
        });
    }

    @Subscribe
    private void onPluginMessage(PluginMessageEvent event) {
        if(!FormattedNamesPayload.IDENTIFIER.equals(event.getIdentifier())) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if(!(event.getSource() instanceof ServerConnection backend)) return;

        final long receiveTimestamp = System.currentTimeMillis();

        ByteBuf input = Unpooled.wrappedBuffer(event.getData());
        logger.info("received message with {} length", input.readableBytes());
        FormattedNamesPayload payload;
        try {
            payload = FormattedNamesPayload.decode(input);
        }
        catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Unable to decode payload from %s server",
                    backend.getServerInfo().getName()
            ), exception);
        }

        if(payload.direction() != FormattedNamesPayload.Direction.PROXY_BOUND) return;

        assert payload.resultNames() != null;
        Set<UUID> finishedGroupedRequests = new HashSet<>();
        for (Map.Entry<UUID, String> entry : payload.resultNames().entrySet()) {
            CachedName cachedName = cachedNames.get(entry.getKey());
            if(cachedName == null) continue;

            cachedName.lastReceivedName = entry.getValue();
            cachedName.lastReceive = receiveTimestamp;

            if(cachedName.groupedRequest != null) {
                assert cachedName.groupedRequestUuid != null;
                if(finishedGroupedRequests.add(cachedName.groupedRequestUuid))
                    cachedName.groupedRequest.complete(payload.resultNames());

                cachedName.groupedRequest = null;
                cachedName.currentRequest = null;
                cachedName.groupedRequestUuid = null;
                continue;
            }

            if(cachedName.currentRequest != null && !cachedName.currentRequest.isDone()) {
                cachedName.currentRequest.complete(cachedName.lastReceivedName);
                cachedName.currentRequest = null;
            }
        }
    }

    private static class CachedName {

        final UUID uuid;
        @Nullable String lastReceivedName;
        long lastReceive = 0;

        @Nullable CompletableFuture<String> currentRequest;
        @Nullable CompletableFuture<Map<UUID, String>> groupedRequest;
        @Nullable UUID groupedRequestUuid;

        CachedName(UUID uuid) {
            this.uuid = uuid;
        }

    }

}
