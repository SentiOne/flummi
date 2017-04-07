package de.otto.flummi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java8.util.function.BiConsumer;
import java8.util.function.BinaryOperator;
import java8.util.function.Supplier;
import java8.util.stream.Collector;
import java8.util.stream.Collectors;

import static java8.util.function.Functions.identity;

public class GsonCollectors {
    public static Collector<JsonElement, JsonArray, JsonArray> toJsonArray() {
        Supplier<JsonArray> supplier = JsonArray::new;
        BiConsumer<JsonArray, JsonElement> accumulator = JsonArray::add;
        BinaryOperator<JsonArray> combiner = (array1, array2) -> {
            array1.addAll(array2);
            return array1;
        };
        return Collectors.of(supplier, accumulator, combiner, identity());
    }
}
