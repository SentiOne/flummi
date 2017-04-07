package de.otto.flummi.aggregations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.otto.flummi.response.AggregationResult;
import de.otto.flummi.response.Bucket;
import de.otto.flummi.response.BucketAggregationResult;
import java8.util.stream.StreamSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggregationResultParser {
    public static AggregationResult parseBuckets(JsonObject jsonObject) {
        AggregationResult aggregation = null;

        JsonElement bucketsElement = jsonObject.get("buckets");
        if (bucketsElement != null) {
            JsonArray bucketsArray = bucketsElement.getAsJsonArray();
            ArrayList<Bucket> bucketList = new ArrayList<>();
            for (JsonElement elem : bucketsArray) {
                JsonObject elemObject = elem.getAsJsonObject();
                bucketList.add(new Bucket(elemObject.get("key").getAsString(), elemObject.get("doc_count").getAsLong()));
            }
            aggregation = new BucketAggregationResult(bucketList);
        }
        return aggregation;

    }

    public static AggregationResult parseSubAggregations(JsonObject jsonObject, List<AggregationBuilder> subAggregations) {
        Map<String, AggregationResult> aggregations = new HashMap<>();

        if (subAggregations != null) {
            StreamSupport.stream(subAggregations)
                .forEach(t ->
                    aggregations.put(t.getName(), t.parseResponse(jsonObject.get(t.getName()).getAsJsonObject()))
                );
        }

        return new BucketAggregationResult(aggregations);
    }
}
