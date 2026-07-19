package modoru.proxy.storage;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import modoru.proxy.Configuration;
import net.kyori.adventure.text.Component;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.net.URI;
import java.util.concurrent.*;

public final class StorageClient {

    private final ProxyServer proxyServer;
    private final ScheduledExecutorService executorService;
    private final Configuration configuration;
    private final Logger logger;
    private final ClientSocket clientSocket;

    private CompletableFuture<Void> openFuture;

    private ConnectionState state;
    private int connectionAttempts;

    private ScheduledFuture<Boolean> delayedConnectionAttempt;
    private Thread delayedConnectionThread;

    public StorageClient(ProxyServer proxyServer, ScheduledExecutorService executorService, Configuration configuration, Logger logger) {
        this.proxyServer = proxyServer;
        this.executorService = executorService;
        this.configuration = configuration;
        this.logger = logger;
        this.clientSocket = new ClientSocket(URI.create(configuration.storageClient.address), this);

        this.openFuture = new CompletableFuture<>();
        this.state = ConnectionState.NEVER_OPENED;
    }

    void message(JSONObject messageBody) {
        switch (messageBody.optString("type", "").toLowerCase()) {
            case "storage_connect" -> {
                if(state != ConnectionState.OPENED) break;

                boolean success = messageBody.optBoolean("success", false);
                if(success) {
                    logger.info("RemoteStorage initialized!");

                    openFuture.complete(null);

                    state = ConnectionState.AUTHORIZED;
                }
                else {
                    printLockMessage(-1, String.format(
                            "Unable to authenticate to RemoteStorage server: %s",
                            messageBody.optString("error", "no error present")
                    ), connectionAttempts + 1);

                    openFuture.completeExceptionally(new IllegalStateException("Unable to authenticate to RemoteStorage server."));
                    state = ConnectionState.CLOSED;
                    internalClose(true);
                }
            }
            default -> {}
        }
    }

    public void open() {
        if(state != ConnectionState.NEVER_OPENED) return;

        executorService.execute(() -> connect(false));
    }

    boolean connect(boolean reconnection) {
        try {
            if((reconnection ? !clientSocket.reconnectBlocking() : !clientSocket.connectBlocking())) {
                connectionClosed(-1, "Unable to connect", false);
                return false;
            }
            logger.info("Connected to the endpoint");

            state = ConnectionState.OPENED;

            var cfg = configuration.storageClient;
            clientSocket.send(
                    new JSONObject()
                            .put("type", "storage_connect")
                            .put("user", cfg.user)
                            .put("password", cfg.password)
                            .toString()
            );
            logger.info("Sent connection packet");

            connectionAttempts = 0;

            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    void connectionClosed(int code, String reason, boolean immediate) {
        var config = configuration.storageClient;
        int allowedAttempts = config.reconnectAttempts;
        if(allowedAttempts <= 0 || connectionAttempts >= allowedAttempts) {
            state = ConnectionState.CLOSED;
            // printLockMessage(code, reason, connectionAttempts + 1);
            internalClose(true);
            return;
        }

        state = ConnectionState.RECONNECTING;

        if (!openFuture.isDone())
            openFuture.completeExceptionally(new TimeoutException());

        openFuture = new CompletableFuture<>();

        delayedConnectionThread = null;

        if(!immediate && ++connectionAttempts > 1) {
            delayedConnectionAttempt = executorService.schedule(() -> {
                delayedConnectionThread = Thread.currentThread();
                return connect(true);
            }, config.reconnectAttemptDelay, TimeUnit.SECONDS);
            return;
        }

        executorService.execute(() -> connect(true));
    }

    private void printLockMessage(int code, String reason, int attempts) {
        final String separator = "========================================";
        logger.error(separator);
        logger.error("The connection to the storage could not be established. Server is now locked - non of the players can join.");
        logger.error("Restart the module or, what better, restart the server.");
        logger.error("Below is the most detailed information the module can provide about why the connection was lost.");
        logger.error("WebSocket close code - {}, reason - \"{}\", connection attempts: {}", code, reason, attempts);
        logger.error(separator);
    }

    private void internalClose(boolean kickPlayers) {
        try {
            if(delayedConnectionAttempt != null && delayedConnectionThread != Thread.currentThread())
                delayedConnectionAttempt.cancel(true);

            if(kickPlayers) {
                for (Player player : proxyServer.getAllPlayers()) {
                    player.disconnect(Component.text("Internal problem"));
                }
            }

            if(!clientSocket.isClosed() && !clientSocket.isClosing())
                clientSocket.closeBlocking();
        }
        catch (Throwable exception) {
            logger.error("Error closing StorageClient", exception);
        }
        finally {
            delayedConnectionAttempt = null;
        }
    }

    public void close() {
        if(state == ConnectionState.NEVER_OPENED || state == ConnectionState.CLOSED) return;
        state = ConnectionState.CLOSED;
        internalClose(false);
    }

}
