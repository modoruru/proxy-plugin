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
        public String playerNameFormat = "%formatted_name%";

        public FormattedNames formattedNames = new FormattedNames();

        public List<String> header = List.of("", "example tab", "");
        public List<String> footer = List.of("", "online %online% | ping %ping%", "");

        public static final class FormattedNames {
            @Comment(value = {@CommentValue("Plugin requests a formatted name from the backend servers. This parameter determines how long it takes for formatted name to become irrelevant, requiring the plugin to request it again.")})
            public long timeOfRelevance = 3000L;

            public long requestTimeout = 5000L;
            public String requestTimeoutFallback = "%player_name%";
        }
    }

}
