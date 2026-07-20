package modoru.proxy.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;

public final class NetworkUtil {

    private NetworkUtil() {}

    public static void sendPacket(Player player, MinecraftPacket packet) {
        ((ConnectedPlayer) player).getConnection().write(packet);
    }

}
