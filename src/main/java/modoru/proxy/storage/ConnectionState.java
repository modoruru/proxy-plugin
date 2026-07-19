package modoru.proxy.storage;

enum ConnectionState {

    NEVER_OPENED,
    OPENED,
    AUTHORIZED,
    RECONNECTING,
    CLOSED

}