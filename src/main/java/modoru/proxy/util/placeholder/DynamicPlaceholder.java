package modoru.proxy.util.placeholder;

import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * DynamicPlaceholder return some string based on passed object
 * @param <E> object for resolving placeholders
 */
public final class DynamicPlaceholder<E> {

    private final String name;
    private final Function<E, @Nullable Object> supplier;

    private DynamicPlaceholder(String name, Function<E, @Nullable Object> supplier) {
        this.name = name;
        this.supplier = supplier;
    }

    public String name() {
        return name;
    }

    public @Nullable Object value(E object) {
        return supplier.apply(object);
    }

    public static <E> DynamicPlaceholder<E> create(String name, Function<E, @Nullable Object> supplier) {
        return new DynamicPlaceholder<>(name, supplier);
    }

}
