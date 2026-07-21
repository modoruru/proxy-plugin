package modoru.proxy.tab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;

public final class TabListener {

    private final Tab tab;

    public TabListener(Tab tab) {
        this.tab = tab;
    }

    @Subscribe
    private void onServerPostConnect(ServerPostConnectEvent event) {
        tab.addPlayer(event.getPlayer());
    }

    @Subscribe
    private void onDisconnect(DisconnectEvent event) {
        tab.removePlayer(event.getPlayer());
    }

}
