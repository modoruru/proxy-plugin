package modoru.proxy;

import net.elytrium.serializer.NameStyle;
import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.nio.file.Path;

public final class Configuration extends YamlSerializable {

    Configuration(Path path) {
        super(
                path,
                new SerializerConfig.Builder()
                        .setFieldNameStyle(NameStyle.CAMEL_CASE)
                        .setNodeNameStyle(NameStyle.SNAKE_CASE)
                        .build()
        );
    }

    public StorageClient storageClient = new StorageClient();

    public static final class StorageClient {
        public String address = "ws://localhost:80";
        public String user = "root";
        public String password = "root";
        public int reconnectAttempts = 3;
        public int reconnectAttemptDelay = 5;
    }

}
