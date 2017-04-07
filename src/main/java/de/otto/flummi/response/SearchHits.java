package de.otto.flummi.response;

import java.util.Iterator;
import java8.util.Spliterator;
import java8.util.function.Consumer;
import java8.util.stream.Stream;

public interface SearchHits {
    long getTotalHits();

    /**
     * @return The maximum score of this query.
     */
    Float getMaxScore();

    Iterator<SearchHit> iterator();

    void forEach(Consumer<? super SearchHit> action);

    Spliterator<SearchHit> spliterator();

    Stream<SearchHit> stream();
}
