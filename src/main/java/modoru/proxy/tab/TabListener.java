package modoru.proxy.tab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;

public final class TabListener {

    private final Tab tab;

    public TabListener(Tab tab) {
        this.tab = tab;
    }

    @Subscribe
    private void onServerPostConnect(ServerPostConnectEvent event) {
        tab.removePlayer(event.getPlayer());
    }

}
