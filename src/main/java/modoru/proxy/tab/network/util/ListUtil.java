package modoru.proxy.tab.network.util;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ListUtil {

    private ListUtil() {}

    public static <E> List<E> read(ByteBuf input, Function<ByteBuf, E> elementDecoder) {
        final int count = VarIntUtil.read(input);
        List<E> result = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            result.add(elementDecoder.apply(input));
        }

        return result;
    }

    public static <E> void write(ByteBuf output, BiConsumer<ByteBuf, E> elementEncoder, Collection<E> collection) {
        VarIntUtil.write(output, collection.size());

        for (E element : collection) {
            elementEncoder.accept(output, element);
        }
    }

}
