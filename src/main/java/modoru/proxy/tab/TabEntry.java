package modoru.proxy.tab;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import modoru.proxy.tab.network.packet.UpdateTeamPacket;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public final class TabEntry implements Comparable<TabEntry> {

    final Tab tab;
    final Player player;
    final SequencedMap<Key, Comparator<TabEntry>> sorters;
    final Predicate<Player> listedPredicate;
    final Set<TabEntry> unlisted;
    final Map<String, UUID> fakeTeams;

    @Nullable String formattedName;
    Component displayName;
    long lastNameUpdate;
    boolean freshDisplayName = true;

    @Nullable String teamName;
    boolean freshTeamName = true;
    @Nullable UpdateTeamPacket teamAddPacket;
    @Nullable UpsertPlayerInfoPacket updateDisplayNamePacket;

    TabEntry(Tab tab, Player player, SequencedMap<Key, Comparator<TabEntry>> sorters, Predicate<Player> listedPredicate) {
        this.tab = tab;
        this.player = player;
        this.sorters = sorters;
        this.listedPredicate = listedPredicate;
        this.unlisted = new HashSet<>();
        this.fakeTeams = new HashMap<>();

        this.displayName = Component.text(player.getUsername());
    }

    /**
     * @return should update & current state
     */
    ListedResult isListed(TabEntry other) {
        if(other == this) return new ListedResult(false, true);
        boolean listed = !this.unlisted.contains(other);

        if(listedPredicate.test(player) == listed) return new ListedResult(false, listed);

        if(listed) unlisted.remove(other);
        else unlisted.add(other);

        return new ListedResult(true, listed);
    }

    @Override
    public int compareTo(TabEntry that) {
        for (Comparator<TabEntry> comparator : sorters.sequencedValues()) {
            int result = comparator.compare(this, that);
            if(result != 0) return result;
        }
        return 0;
    }

    record ListedResult(boolean shouldUpdate, boolean listed) {

    }

}
