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

}
