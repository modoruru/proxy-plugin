package modoru.proxy.tab.network;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class FormattedNamesPayload {

    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.create("modoru", "formatted_names");

    private final Direction direction;
    private final @Nullable Set<UUID> requestedNames;
    private final @Nullable Map<UUID, String> resultNames;

    private FormattedNamesPayload(Direction direction, @Nullable Set<UUID> requestedNames, @Nullable Map<UUID, String> resultNames) {
        this.direction = direction;
        this.requestedNames = requestedNames;
        this.resultNames = resultNames;
    }

    public Direction direction() {
        return direction;
    }

    public @Nullable Set<UUID> requestedNames() {
        return requestedNames;
    }

    public @Nullable Map<UUID, String> resultNames() {
        return resultNames;
    }

    public static FormattedNamesPayload fromProxy(Set<UUID> requestedNames) {
        return new FormattedNamesPayload(Direction.BACKEND_BOUND, Set.copyOf(requestedNames), null);
    }

    public static int payloadSizeForBackendBound(int size) {
        return VarIntUtil.bytesToWrite(size) + 37 * size; // 37 is how many bytes it takes to encode one uuid as string
    }

    public void encode(ByteBuf output) {
        output.writeByte(direction.ordinal());

        switch (direction) {
            case PROXY_BOUND -> {
                assert resultNames != null;
                int size = resultNames.size();

                VarIntUtil.write(output, size);
                for (Map.Entry<UUID, String> entry : resultNames.entrySet()) {
                    ProtocolUtils.writeString(output, entry.getKey().toString());
                    ProtocolUtils.writeString(output, entry.getValue());
                }
            }
            case BACKEND_BOUND -> {
                assert requestedNames != null;
                int size = requestedNames.size();

                VarIntUtil.write(output, size);
                for (UUID requestedName : requestedNames) {
                    ProtocolUtils.writeString(output, requestedName.toString());
                }
            }
        }
    }

    public static FormattedNamesPayload decode(ByteBuf input) {
        Direction direction = Direction.values()[input.readByte()];

        int size = VarIntUtil.read(input);

        Set<UUID> requestedNames = null;
        Map<UUID, String> resultNames = null;
        switch (direction) {
            case PROXY_BOUND -> {
                resultNames = new HashMap<>(size);

                for (int i = 0; i < size; i++) {
                    resultNames.put(
                            UUID.fromString(ProtocolUtils.readString(input)),
                            ProtocolUtils.readString(input)
                    );
                }
            }
            case BACKEND_BOUND -> {
                requestedNames = new HashSet<>(size);

                for (int i = 0; i < size; i++) {
                    requestedNames.add(UUID.fromString(ProtocolUtils.readString(input)));
                }
            }
        }

        return new FormattedNamesPayload(direction, requestedNames, resultNames);
    }

    public enum Direction {
        PROXY_BOUND,
        BACKEND_BOUND
    }

}
