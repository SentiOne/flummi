package de.otto.flummi;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ning.http.client.Response;
import de.otto.flummi.request.*;
import de.otto.flummi.util.HttpClientWrapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java8.util.Optional;
import java.util.concurrent.ExecutionException;

import java8.util.stream.RefStreams;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import static de.otto.flummi.request.GsonHelper.object;
import static java8.util.stream.Collectors.toList;

public class IndicesAdminClient {

    private HttpClientWrapper httpClient;
    private Gson gson = new Gson();

    public IndicesAdminClient(HttpClientWrapper httpClient) {
        this.httpClient = httpClient;
    }

    public CreateIndexRequestBuilder prepareCreate(String indexName) {
        return new CreateIndexRequestBuilder(httpClient, indexName);
    }

    public IndicesExistsRequestBuilder prepareExists(String indexName) {
        return new IndicesExistsRequestBuilder(httpClient, indexName);
    }

    public DeleteIndexRequestBuilder prepareDelete(Stream<String> indexNameSupplier) {
        return new DeleteIndexRequestBuilder(httpClient, indexNameSupplier);
    }

    public DeleteIndexRequestBuilder prepareDelete(String... indexNames) {
        return new DeleteIndexRequestBuilder(httpClient, RefStreams.of(indexNames));
    }

    public RefreshRequestBuilder prepareRefresh(String indexName) {
        return new RefreshRequestBuilder(httpClient, indexName);
    }

    public ForceMergeRequestBuilder forceMerge(String indexName) {
        return new ForceMergeRequestBuilder(httpClient, indexName);
    }

    public JsonObject getIndexSettings() {
        try {
            Response response = httpClient.prepareGet("/_all/_settings").execute().get();
            if (response.getStatusCode() != 200) {
                throw RequestBuilderUtil.toHttpServerErrorException(response);
            }
            String jsonString = null;
            try {
                jsonString = response.getResponseBody();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return gson.fromJson(jsonString, JsonObject.class);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getAllIndexNames() {
        try {
            Response response = httpClient.prepareGet("/_all").execute().get();
            if (response.getStatusCode() != 200) {
                throw RequestBuilderUtil.toHttpServerErrorException(response);
            }
            String jsonString = response.getResponseBody();
            JsonObject responseObject = gson.fromJson(jsonString, JsonObject.class);

            return StreamSupport.stream(responseObject.entrySet())
                    .map(Map.Entry::getKey)
                    .collect(toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<String> getIndexNameForAlias(String aliasName) {
        try {
            Response response = httpClient.prepareGet("/_aliases").execute().get();
            if (response.getStatusCode() != 200) {
                throw RequestBuilderUtil.toHttpServerErrorException(response);
            }
            String jsonString = response.getResponseBody();

            return StreamSupport.stream(gson.fromJson(jsonString, JsonObject.class).entrySet())
                    .filter(e -> (e.getValue() != null
                            && e.getValue().isJsonObject()
                            && e.getValue().getAsJsonObject().get("aliases") != null
                            && e.getValue().getAsJsonObject().get("aliases").isJsonObject()
                            && e.getValue().getAsJsonObject().get("aliases").getAsJsonObject().has(aliasName)))
                    .map(e -> e.getKey())
                    .findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void pointAliasToCurrentIndex(String aliasName, String indexName) {
        try {
            JsonArray actions = new JsonArray();
            actions.add(object("remove", object("index", "*", "alias", aliasName)));
            actions.add(object("add", object("index", indexName, "alias", aliasName)));
            JsonObject jsonObject = object("actions", actions);

            Response response = httpClient
                    .preparePost("/_aliases")
                    .setBody(gson.toJson(jsonObject))
                    .execute().get();
            if (response.getStatusCode() >= 300) {
                throw RequestBuilderUtil.toHttpServerErrorException(response);
            }
            JsonObject responseObject = gson.fromJson(response.getResponseBody(), JsonObject.class);
            if (!responseObject.has("acknowledged")) {
                throw new InvalidElasticsearchResponseException("Response does not contain field 'acknowledged': " + responseObject);
            } else {
                if (!responseObject.get("acknowledged").getAsBoolean()) {
                    throw new RuntimeException("Pointing product alias to current index not acknowledged");
                }
            }
            return;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean aliasExists(String aliasName) {
        try {
            Response response = httpClient.prepareGet("/_aliases").execute().get();
            if (response.getStatusCode() != 200) {
                throw RequestBuilderUtil.toHttpServerErrorException(response);
            }
            String jsonString = response.getResponseBody();
            JsonObject responseObject = gson.fromJson(jsonString, JsonObject.class);

            return StreamSupport.stream(responseObject.entrySet()).filter(e ->
                    (e.getValue() != null
                            && e.getValue().isJsonObject()
                            && e.getValue().getAsJsonObject().get("aliases") != null
                            && e.getValue().getAsJsonObject().get("aliases").isJsonObject()
                            && e.getValue().getAsJsonObject().get("aliases").getAsJsonObject().has(aliasName)))
                    .count() > 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
