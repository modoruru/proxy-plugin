package modoru.proxy.tab;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

import java.text.NumberFormat;
import java.util.*;
import java.util.function.Predicate;

public final class TabEntry implements Comparable<TabEntry> {

    final Player player;
    final SequencedMap<Key, Comparator<TabEntry>> sorters;
    final Predicate<Player> listedPredicate;
    final Set<TabEntry> unlisted;
    final Set<String> fakeTeams;

    boolean objectiveInitialized;
    @Nullable NumberFormat objectiveValue;

    TabEntry(Player player, SequencedMap<Key, Comparator<TabEntry>> sorters, Predicate<Player> listedPredicate) {
        this.player = player;
        this.sorters = sorters;
        this.listedPredicate = listedPredicate;
        this.unlisted = new HashSet<>();
        this.fakeTeams = new HashSet<>();
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
