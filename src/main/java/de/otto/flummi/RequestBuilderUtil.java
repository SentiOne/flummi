package de.otto.flummi;

import com.ning.http.client.Response;
import de.otto.flummi.response.HttpServerErrorException;
import de.otto.flummi.util.StringUtils;
import de.otto.flummi.util.UncheckedIOException;

import java.io.IOException;

public class RequestBuilderUtil {

    public static final String[] EMPTY_ARRAY = new String[]{};

    public static String buildUrl(String[] indexNames, String[] types, String operationOrId) {
        StringBuilder urlBuilder = new StringBuilder();
        if (indexNames != null && indexNames.length > 0) {
            urlBuilder.append("/").append(StringUtils.join(indexNames, ","));
        }
        if (types != null && types.length > 0) {
            urlBuilder.append("/").append(StringUtils.join(types, ","));
        }
        if (operationOrId != null) {
            // final String[] splitBySlash = operationOrId.split("/");
            urlBuilder.append("/").append(operationOrId);
        }
        return urlBuilder.toString();
    }

    public static HttpServerErrorException toHttpServerErrorException(Response response) {
        try {
            return new HttpServerErrorException(response.getStatusCode(), response.getStatusText() , new String(response.getResponseBodyAsBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String buildUrl(String indexName, String type, String operationOrId) {
        String[] types = type != null ? new String[]{type} : EMPTY_ARRAY;
        String[] indexNames = indexName != null ? new String[]{indexName} : EMPTY_ARRAY;
        return buildUrl(indexNames, types, operationOrId);
    }

    public static String buildUrl(String indexName, String documentType) {
        return buildUrl(indexName, documentType, null);
    }
}
