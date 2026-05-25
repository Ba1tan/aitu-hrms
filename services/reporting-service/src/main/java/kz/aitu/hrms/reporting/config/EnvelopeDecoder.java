package kz.aitu.hrms.reporting.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Unwraps the shared {@code ApiResponse<T>} envelope that every HRMS service
 * returns ({@code {success, message, data, errors, timestamp}}) so Feign
 * deserializes the inner {@code data} payload into the client's declared return
 * type — not the wrapper. Without this, {@code PageResponse.content} (and every
 * other field) lands on the top level, isn't found, and comes back null/empty,
 * which is why every report rendered headers but no rows.
 *
 * Mirrors the frontend's axios response interceptor (shared/api.ts). Bodies that
 * are not enveloped are decoded as-is, so it is safe even if an upstream ever
 * returns a raw payload.
 */
@RequiredArgsConstructor
public class EnvelopeDecoder implements Decoder {

    private final ObjectMapper mapper;

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        if (response.body() == null) {
            return null;
        }
        byte[] body = Util.toByteArray(response.body().asInputStream());
        if (body.length == 0) {
            return null;
        }

        JsonNode root = mapper.readTree(body);
        JsonNode payload = isEnvelope(root) ? root.get("data") : root;
        if (payload == null || payload.isNull()) {
            return null;
        }

        JavaType javaType = mapper.getTypeFactory().constructType(type);
        return mapper.convertValue(payload, javaType);
    }

    private boolean isEnvelope(JsonNode root) {
        return root.isObject() && root.has("success") && root.has("data");
    }
}
