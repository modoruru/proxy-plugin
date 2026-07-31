package modoru.proxy.tab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import modoru.proxy.Configuration;
import modoru.proxy.tab.network.FormattedTabPayload;
import modoru.proxy.tab.network.FormattedUsernamesPayload;
import modoru.proxy.tab.network.TabFormatPayload;

import java.util.*;

// works by accepting recurring packets from the backend
public final class BackendCommunication {

    private final Configuration configuration;
    private final Map<UUID, TabViewer> tabViewers;

    private final Set<String> serversAwareOfTabFormat;

    private final HeaderAndFooter fallbackHeaderAndFooter;

    public BackendCommunication(Configuration configuration) {
        this.configuration = configuration;
        this.tabViewers = new HashMap<>();

        this.serversAwareOfTabFormat = new HashSet<>();

        this.fallbackHeaderAndFooter = new HeaderAndFooter(configuration.tab.header, configuration.tab.footer, System.currentTimeMillis());
    }

    private TabViewer tabViewer(Player player) {
        return tabViewers.computeIfAbsent(
                player.getUniqueId(),
                uuid -> new TabViewer(uuid, fallbackHeaderAndFooter, new FormattedUsername(player.getUsername(), System.currentTimeMillis()))
        );
    }

    public FormattedUsername formattedUsername(Player player) {
        return tabViewer(player).formattedUsername;
    }

    private void confirmServerAwareOfTabFormat(ServerConnection connection) {
        if(!serversAwareOfTabFormat.add(connection.getServerInfo().getName())) return;

        Configuration.Tab config = configuration.tab;

        ByteBuf output = Unpooled.buffer();
        TabFormatPayload.create(
                String.join("\n", config.header),
                String.join("\n", config.footer),
                config.updateIntervalSeconds
        ).encode(output);

        byte[] data = new byte[output.readableBytes()];
        output.readBytes(data);

        connection.sendPluginMessage(TabFormatPayload.IDENTIFIER, data);
    }

    public HeaderAndFooter headerAndFooter(Player player) {
        TabViewer tabViewer = tabViewer(player);

        Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();
        if(optionalServerConnection.isEmpty()) return fallbackHeaderAndFooter;

        ServerConnection connection = optionalServerConnection.get();
        confirmServerAwareOfTabFormat(connection);

        return tabViewer.headerAndFooter;
    }

    @Subscribe
    private void onPluginMessage(PluginMessageEvent event) {
        ChannelIdentifier identifier = event.getIdentifier();
        if(!identifier.equals(FormattedUsernamesPayload.IDENTIFIER) && !identifier.equals(FormattedTabPayload.IDENTIFIER)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if(!(event.getSource() instanceof ServerConnection backend)) return;

        ByteBuf input = Unpooled.wrappedBuffer(event.getData());
        if(identifier.equals(FormattedUsernamesPayload.IDENTIFIER)) handleUsernamesPayload(backend, input);
        else handleTabPayload(backend, input);
    }

    @Subscribe
    private void onDisconnect(DisconnectEvent event) {
        tabViewers.remove(event.getPlayer().getUniqueId());
    }

    private void handleUsernamesPayload(ServerConnection backend, ByteBuf input) {
        long receiveTimestamp = System.currentTimeMillis();
        FormattedUsernamesPayload payload;
        try {
            payload = FormattedUsernamesPayload.decode(input);
        }
        catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Unable to decode payload from %s server",
                    backend.getServerInfo().getName()
            ), exception);
        }

        for (Map.Entry<UUID, String> entry : payload.resultNames().entrySet()) {
            TabViewer tabViewer = tabViewers.get(entry.getKey());
            if(tabViewer != null) {
                FormattedUsername formattedUsername = tabViewer.formattedUsername;
                formattedUsername.formattedUsername = entry.getValue();
                formattedUsername.receiveTimestamp = receiveTimestamp;
            }
        }
    }

    private void handleTabPayload(ServerConnection backend, ByteBuf input) {
        FormattedTabPayload payload;
        try {
            payload = FormattedTabPayload.decode(input);
        }
        catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Unable to decode payload from %s server",
                    backend.getServerInfo().getName()
            ), exception);
        }

        Player player = backend.getPlayer();
        TabViewer tabViewer = tabViewers.get(player.getUniqueId());
        if(tabViewer == null) return;

        HeaderAndFooter headerAndFooter = tabViewer.headerAndFooter;
        headerAndFooter.header = List.of(payload.header().split("\n", -1));
        headerAndFooter.footer = List.of(payload.footer().split("\n", -1));
        headerAndFooter.receiveTimestamp = System.currentTimeMillis();
    }

    public static class FormattedUsername {

        private String formattedUsername;
        private long receiveTimestamp;

        public FormattedUsername(String formattedUsername, long receiveTimestamp) {
            this.formattedUsername = formattedUsername;
            this.receiveTimestamp = receiveTimestamp;
        }

        public String formattedUsername() {
            return formattedUsername;
        }

        public long receiveTimestamp() {
            return receiveTimestamp;
        }

    }

    public static class HeaderAndFooter {

        private List<String> header;
        private List<String> footer;
        private long receiveTimestamp;

        private HeaderAndFooter(List<String> header, List<String> footer, long receiveTimestamp) {
            this.header = header;
            this.footer = footer;
            this.receiveTimestamp = receiveTimestamp;
        }

        public List<String> header() {
            return header;
        }

        public List<String> footer() {
            return footer;
        }

        public long receiveTimestamp() {
            return receiveTimestamp;
        }

    }

    private static class TabViewer {

        final UUID uuid;
        final HeaderAndFooter headerAndFooter;
        final FormattedUsername formattedUsername;

        TabViewer(UUID uuid, HeaderAndFooter headerAndFooter, FormattedUsername formattedUsername) {
            this.uuid = uuid;
            this.headerAndFooter = new HeaderAndFooter(headerAndFooter.header, headerAndFooter.footer, headerAndFooter.receiveTimestamp);
            this.formattedUsername = formattedUsername;
        }

    }

}
