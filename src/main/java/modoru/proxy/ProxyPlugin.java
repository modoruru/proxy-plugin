package modoru.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@SuppressWarnings("unused")
public final class ProxyPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;

    @Inject
    public ProxyPlugin(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

}
