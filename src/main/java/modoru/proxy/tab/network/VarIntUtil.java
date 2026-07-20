package modoru.proxy.tab.network;

import io.netty.buffer.ByteBuf;

public final class VarIntUtil {

    private VarIntUtil() {}

    public static int read(ByteBuf input) {
        int out = 0;
        int bytes = 0;

        byte in;
        do {
            in = input.readByte();
            out |= (in & 127) << bytes++ * 7;
            if (bytes > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while(hasContinuationBit(in));

        return out;
    }

    public static ByteBuf write(ByteBuf output, int value) {
        if ((value & -128) == 0) {
            output.writeByte(value);
        } else if ((value & -16384) == 0) {
            int s = (value & 127 | 128) << 8 | value >>> 7;
            output.writeShort(s);
        } else {
            writeSlow(output, value);
        }

        return output;
    }

    public static boolean hasContinuationBit(byte in) {
        return (in & 128) == 128;
    }

    public static ByteBuf writeSlow(ByteBuf output, int value) {
        while((value & -128) != 0) {
            output.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        output.writeByte(value);
        return output;
    }

}
