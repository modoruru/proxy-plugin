package modoru.proxy.util;

/**
 * Adds id() based on enum's ordinal
 */
public interface IdentifiedEnum {

    default int id() {
        if(this instanceof Enum<?> asEnum) return asEnum.ordinal();
        throw new UnsupportedOperationException("Not a enum");
    }

    static <E extends Enum<E> & IdentifiedEnum> E byId(Class<E> clazz, int id) {
        if(!clazz.isAssignableFrom(Enum.class)) throw new UnsupportedOperationException("Not a enum");
        return clazz.getEnumConstants()[id];
    }

}
