package modoru.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPreShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import modoru.proxy.tab.Tab;
import modoru.proxy.tab.TabListener;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SuppressWarnings("unused")
public final class ProxyPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private final ScheduledExecutorService executorService;
    private final Configuration configuration;

    private final Tab tab;

    @Inject
    public ProxyPlugin(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        this.executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        this.configuration = new Configuration(dataDirectory.resolve("config.yml"));

        this.tab = new Tab(proxyServer, executorService, configuration);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        configuration.reload();
        proxyServer.getEventManager().register(this, new TabListener(tab));
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onProxyPreShutdown(ProxyPreShutdownEvent event) {
    }

}
