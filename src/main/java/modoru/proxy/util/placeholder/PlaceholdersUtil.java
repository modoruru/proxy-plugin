package modoru.proxy.util.placeholder;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public final class PlaceholdersUtil {

    private PlaceholdersUtil() {}

    @SafeVarargs
    public static <E> String resolveDynamic(String input, E object, @Nullable Character placeholderStartChar, @Nullable Character placeholderEndChar, DynamicPlaceholder<E>... placeholders) {
        for (DynamicPlaceholder<E> placeholder : placeholders) {
            StringBuilder builder = new StringBuilder();
            if(placeholderStartChar != null) builder.append(placeholderStartChar);
            builder.append(placeholder.name());
            if(placeholderEndChar != null) builder.append(placeholderEndChar);

            input = replace(input, builder.toString(), () -> placeholder.value(object));
        }

        return input;
    }

    private static String replace(String original, String substring, Supplier<@Nullable Object> supplier) {
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        int substringLength = substring.length();

        int index;
        while ((index = original.indexOf(substring, lastIndex)) != -1) {
            result.append(original, lastIndex, index);
            result.append(supplier.get());
            lastIndex = index + substringLength;
        }

        result.append(original.substring(lastIndex));

        return result.toString();
    }

}
