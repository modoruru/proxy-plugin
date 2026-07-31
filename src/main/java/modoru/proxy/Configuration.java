package modoru.proxy;

import net.elytrium.serializer.NameStyle;
import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.nio.file.Path;
import java.util.List;

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

    public Tab tab = new Tab();

    public static final class Tab {
        public int updateIntervalSeconds = 1;

        public List<String> header = List.of(
                "",
                "    example tab    ",
                ""
        );
        public List<String> footer = List.of(
                "",
                "    online %global_online% | ping %ping%     ",
                "    %tps% ticks/s | %mspt% ms/tick    ",
                ""
        );
    }

}
