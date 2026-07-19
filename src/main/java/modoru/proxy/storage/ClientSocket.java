package modoru.proxy.storage;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

final class ClientSocket extends WebSocketClient {

    private final StorageClient storageClient;

    public ClientSocket(URI serverUri, StorageClient storageClient) {
        super(serverUri);
        this.storageClient = storageClient;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    @Override
    public void onMessage(String message) {
        JSONObject messageBody;

        try {
            messageBody = new JSONObject(message);

            storageClient.message(messageBody);
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if(remote)
            storageClient.connectionClosed(code, reason, true);
    }

    @Override
    public void onError(Exception ex) {
    }

}