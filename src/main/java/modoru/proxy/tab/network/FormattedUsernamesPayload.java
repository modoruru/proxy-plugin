package modoru.proxy.tab.network;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import modoru.proxy.tab.network.util.VarIntUtil;

import java.util.*;

public final class FormattedUsernamesPayload {

    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.create("modoru", "formatted_names");

    private final Map<UUID, String> resultNames;

    private FormattedUsernamesPayload(Map<UUID, String> resultNames) {
        this.resultNames = resultNames;
    }

    public Map<UUID, String> resultNames() {
        return resultNames;
    }

    public static FormattedUsernamesPayload decode(ByteBuf input) {
        int size = VarIntUtil.read(input);

        Map<UUID, String> resultNames = new HashMap<>(size);

        for (int i = 0; i < size; i++) {
            resultNames.put(
                    UUID.fromString(ProtocolUtils.readString(input)),
                    ProtocolUtils.readString(input)
            );
        }

        return new FormattedUsernamesPayload(resultNames);
    }

}
