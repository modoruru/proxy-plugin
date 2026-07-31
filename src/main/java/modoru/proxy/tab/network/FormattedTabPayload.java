package modoru.proxy.tab.network;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public final class FormattedTabPayload {

    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.create("modoru", "formatted_tab");

    private final String header, footer;

    private FormattedTabPayload(String header, String footer) {
        this.header = header;
        this.footer = footer;
    }

    public String header() {
        return header;
    }

    public String footer() {
        return footer;
    }

    public static FormattedTabPayload decode(ByteBuf input) {
        return new FormattedTabPayload(ProtocolUtils.readString(input), ProtocolUtils.readString(input));
    }

}
