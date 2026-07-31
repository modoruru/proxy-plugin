package modoru.proxy.tab.network.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class PacketRegistration {

    private static final Method StateRegistry_PacketRegistry$register;
    private static final Field StateRegistry_clientbound;
    private static final Method StateRegistry_map;

    static {
        try {
            StateRegistry_PacketRegistry$register = StateRegistry.PacketRegistry.class.getDeclaredMethod("register", Class.class, Supplier.class, StateRegistry.PacketMapping[].class);
            StateRegistry_PacketRegistry$register.setAccessible(true);

            StateRegistry_clientbound = StateRegistry.class.getDeclaredField("clientbound");
            StateRegistry_clientbound.setAccessible(true);

            StateRegistry_map = StateRegistry.class.getDeclaredMethod("map", int.class, ProtocolVersion.class, boolean.class);
            StateRegistry_map.setAccessible(true);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to inject into Velocity's packet registration", e);
        }
    }

    private PacketRegistration() {}

    public static void bootstrap() {
        register(
                StateRegistry.PLAY,
                UpdateTeamPacket.class,
                UpdateTeamPacket::new,
                mapping(0x60, ProtocolVersion.MINECRAFT_1_20_5),
                mapping(0x67, ProtocolVersion.MINECRAFT_1_21_2),
                mapping(0x66, ProtocolVersion.MINECRAFT_1_21_5),
                mapping(0x6B, ProtocolVersion.MINECRAFT_1_21_9),
                mapping(0x6D, ProtocolVersion.MINECRAFT_26_1),
                mapping(0x6D, ProtocolVersion.MINECRAFT_26_2)
        );
    }

    private static StateRegistry.PacketMapping mapping(int id, ProtocolVersion version) {
        try {
            return (StateRegistry.PacketMapping) StateRegistry_map.invoke(
                    null,
                    id,
                    version,
                    false
            );
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <P extends MinecraftPacket> void register(StateRegistry stateRegistry, Class<P> clazz, Supplier<P> packetSupplier, StateRegistry.PacketMapping... mappings) {
        try {
            StateRegistry_PacketRegistry$register.invoke(
                    StateRegistry_clientbound.get(stateRegistry),
                    clazz,
                    packetSupplier,
                    mappings
            );
        }
        catch (IllegalAccessException _) {}
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
