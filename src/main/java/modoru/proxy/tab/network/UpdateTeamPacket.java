package modoru.proxy.tab.network;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import modoru.proxy.util.IdentifiedEnum;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class UpdateTeamPacket implements MinecraftPacket {

    public static final Set<ProtocolVersion> SUPPORTED_VERSIONS = allBetweenIncluding(ProtocolVersion.MINECRAFT_1_21_5, ProtocolVersion.MINECRAFT_26_2);

    private static Set<ProtocolVersion> allBetweenIncluding(ProtocolVersion start, ProtocolVersion end) {
        Set<ProtocolVersion> versions = new HashSet<>();

        if(start.ordinal() > end.ordinal()) throw new IllegalArgumentException();
        for (int i = start.ordinal(), last = end.ordinal(); i <= last; i++) {
            versions.add(ProtocolVersion.values()[i]);
        }

        return versions;
    }

    private String name;
    private Method method;
    private Collection<String> players;
    private @Nullable Parameters parameters;

    private UpdateTeamPacket(String name, Method method, Collection<String> players, @Nullable Parameters parameters) {
        this.name = name;
        this.method = method;
        this.players = players;
        this.parameters = parameters;
    }

    public static UpdateTeamPacket createAddOrModifyPacket(String name, Parameters parameters, Collection<String> players, boolean useAdd) {
        return new UpdateTeamPacket(
                name,
                useAdd ? Method.ADD : Method.CHANGE,
                players,
                parameters
        );
    }

    public static UpdateTeamPacket createRemovePacket(String name) {
        return new UpdateTeamPacket(name, Method.REMOVE, List.of(), null);
    }

    private static void checkProtocolVersion(ProtocolVersion protocolVersion) {
        if(!SUPPORTED_VERSIONS.contains(protocolVersion)) throw new IllegalArgumentException(String.format(
                "Unsupported protocol version: ProtocolVersion.%s",
                protocolVersion.name()
        ));
    }

    @Override
    public void decode(ByteBuf input, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) throws IllegalArgumentException {
        checkProtocolVersion(protocolVersion);

        this.name = ProtocolUtils.readString(input);
        this.method = Method.byId(input.readByte());
        this.parameters = method.hasParameters ? Parameters.decode(input) : null;
        this.players = method.hasPlayers ? ListUtil.read(input, ProtocolUtils::readString) : List.of();
    }

    @Override
    public void encode(ByteBuf output, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) throws IllegalArgumentException {
        checkProtocolVersion(protocolVersion);

        ProtocolUtils.writeString(output, name);
        output.writeByte(method.id());
        if(method.hasParameters) {
            assert parameters != null;
            parameters.encode(output);
        }
        if(method.hasPlayers) ListUtil.write(output, ProtocolUtils::writeString, players);
    }

    static void writeComponent(ByteBuf buf, Component component) {
        final BinaryTag tag = ComponentHolder.serialize(GsonComponentSerializer.gson().serializeToTree(component));
        ProtocolUtils.writeBinaryTag(buf, ProtocolVersion.MINECRAFT_1_20_3, tag);
    }

    static Component readComponent(ByteBuf buf) {
        return GsonComponentSerializer.gson().deserializeFromTree(ComponentHolder.deserialize(
                ProtocolUtils.readBinaryTag(buf, ProtocolVersion.MINECRAFT_1_20_3, null)
        ));
    }

    @Override
    public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
        return false;
    }

    public record Parameters(Component displayName, Component playerPrefix, Component playerSuffix, TeamVisibility nameTagVisibility, TeamCollisionRule collisionRule, @Nullable TeamColor color, byte options) {

        public void encode(ByteBuf output) {
            writeComponent(output, displayName);
            writeComponent(output, playerPrefix);
            writeComponent(output, playerSuffix);
            VarIntUtil.write(output, nameTagVisibility.id());
            VarIntUtil.write(output, collisionRule.id());

            output.writeBoolean(color != null);
            if(color != null) VarIntUtil.write(output, color.id());

            output.writeByte(options);
        }

        public static Parameters decode(ByteBuf input) {
            Component displayName = readComponent(input);
            Component playerPrefix = readComponent(input);
            Component playerSuffix = readComponent(input);

            TeamVisibility teamVisibility = TeamVisibility.byId(VarIntUtil.read(input));
            TeamCollisionRule collisionRule = TeamCollisionRule.byId(VarIntUtil.read(input));

            TeamColor color;
            if(input.readBoolean()) color = TeamColor.byId(VarIntUtil.read(input));
            else color = null;

            byte options = input.readByte();

            return new Parameters(
                    displayName,
                    playerPrefix,
                    playerSuffix,
                    teamVisibility,
                    collisionRule,
                    color,
                    options
            );
        }

    }

    public enum Method implements IdentifiedEnum {
        ADD   (true, true),
        REMOVE(false, false),
        CHANGE(false, true),
        JOIN  (true, false),
        LEAVE (true, false);

        public final boolean hasPlayers, hasParameters;

        Method(boolean hasPlayers, boolean hasParameters) {
            this.hasPlayers = hasPlayers;
            this.hasParameters = hasParameters;
        }

        public static Method byId(int id) {
            return IdentifiedEnum.byId(Method.class, id);
        }

    }

    public enum TeamVisibility implements IdentifiedEnum {
        ALWAYS,
        NEVER,
        HIDE_FOR_OTHER_TEAMS,
        HIDE_FOR_OWN_TEAM;

        public static TeamVisibility byId(int id) {
            return IdentifiedEnum.byId(TeamVisibility.class, id);
        }
    }

    public enum TeamCollisionRule implements IdentifiedEnum {
        ALWAYS,
        NEVER,
        PUSH_OTHER_TEAMS,
        PUSH_OWN_TEAM;

        public static TeamCollisionRule byId(int id) {
            return IdentifiedEnum.byId(TeamCollisionRule.class, id);
        }
    }

    public enum TeamColor implements IdentifiedEnum {
        BLACK,
        DARK_BLUE,
        DARK_GREEN,
        DARK_AQUA,
        DARK_RED,
        DARK_PURPLE,
        GOLD,
        GRAY,
        DARK_GRAY,
        BLUE,
        GREEN,
        AQUA,
        RED,
        LIGHT_PURPLE,
        YELLOW,
        WHITE;

        public static TeamColor byId(int id) {
            return IdentifiedEnum.byId(TeamColor.class, id);
        }
    }

}
