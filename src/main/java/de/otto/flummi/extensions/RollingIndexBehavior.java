package de.otto.flummi.extensions;

import com.google.gson.JsonObject;
import de.otto.flummi.IndicesAdminClient;
import java8.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java8.util.Optional;
import java.util.Set;
import java8.util.function.Function;
import java8.util.function.Predicate;

import static java8.util.stream.Collectors.toSet;

public class RollingIndexBehavior {

    private static final Logger LOG = LoggerFactory.getLogger(RollingIndexBehavior.class);

    private final IndicesAdminClient client;
    private final String aliasName;
    private final String indexPrefixName;
    private final int survivor;
    private final Function<String, String> indexNameFunction;

    RollingIndexBehavior(IndicesAdminClient client, String aliasName, String indexPrefixName, int survivor, Function<String, String> indexNameFunction) {
        this.client = client;
        this.aliasName = aliasName;
        this.indexPrefixName = indexPrefixName;
        this.survivor = survivor;
        this.indexNameFunction = indexNameFunction;
    }

    public String createNewIndex(JsonObject settings, JsonObject mappings) {
        String indexName = newIndexName();
        client.prepareCreate(indexName)
                .setSettings(settings)
                .setMappings(mappings)
                .execute();
        return indexName;
    }

    public String createNewIndex() {
        String indexName = newIndexName();
        client.prepareCreate(indexName).execute();
        LOG.info("Index created {}", indexName);
        return indexName;
    }

    public void abort(String newIndexName) {
        client.prepareDelete(newIndexName).execute();
        LOG.warn("Index deleted {}", newIndexName);

    }

    public Set<String> commit(String indexName) {
        client.pointAliasToCurrentIndex(aliasName, indexName);
        LOG.info("Alias switched to index name {}", indexName);
        return this.deleteOldIndices(aliasName, indexPrefixName, survivor);
    }

    private String newIndexName() {
        return indexNameFunction.apply(indexPrefixName);
    }

    Set<String> deleteOldIndices(String alias, String prefix, int survivor) {
        if(survivor < 1) {
            throw new IllegalArgumentException("must have one survivor");
        }

        Optional<String> aliasToIndex = client.getIndexNameForAlias(alias);
        Set<String> names =
                StreamSupport.stream(client.getAllIndexNames())
                        .filter(startsWith(prefix))
                        .sorted(Comparator.reverseOrder()) // TODO: here we should have Index objects and sort by created date value (Comparator.reverseOrder implies !Comparator.naturalOrder())
                        .skip(survivor)
                        .filter(skipAlias(aliasToIndex)) // never delete current aliased index
                        .collect(toSet());
        if (!names.isEmpty()) {
            client.prepareDelete(StreamSupport.stream(names)).execute();
        }
        LOG.info("Indices deleted {}", names);
        return names;
    }

    private static Predicate<String> startsWith(String prefix) {
        return (s) -> s.startsWith(prefix);
    }

    private static Predicate<String> skipAlias(Optional<String> indexName) {
        return (s) -> !(indexName.isPresent() && s.equals(indexName.get()));
    }


}
