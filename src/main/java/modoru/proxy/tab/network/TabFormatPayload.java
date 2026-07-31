package modoru.proxy.tab.network;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import modoru.proxy.tab.network.util.VarIntUtil;

public final class TabFormatPayload {

    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.create("modoru", "tab_format");

    private final String header, footer;
    private final int updateIntervalSeconds;

    private TabFormatPayload(String header, String footer, int updateIntervalSeconds) {
        this.header = header;
        this.footer = footer;
        this.updateIntervalSeconds = updateIntervalSeconds;
    }

    public static TabFormatPayload create(String header, String footer, int updateIntervalSeconds) {
        return new TabFormatPayload(header, footer, updateIntervalSeconds);
    }

    public void encode(ByteBuf output) {
        ProtocolUtils.writeString(output, header);
        ProtocolUtils.writeString(output, footer);
        VarIntUtil.write(output, updateIntervalSeconds);
    }

}
