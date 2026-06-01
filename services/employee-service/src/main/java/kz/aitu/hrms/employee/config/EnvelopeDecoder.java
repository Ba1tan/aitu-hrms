package kz.aitu.hrms.employee.config;

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
 * Unwraps the shared {@code ApiResponse<T>} envelope ({@code {success,
 * message, data, ...}}) so Feign deserializes the inner {@code data}
 * payload into the client's declared return type. Mirrors the same
 * decoder reporting-service uses.
 */
@RequiredArgsConstructor
public class EnvelopeDecoder implements Decoder {

    private final ObjectMapper mapper;

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        if (response.body() == null) return null;
        byte[] body = Util.toByteArray(response.body().asInputStream());
        if (body.length == 0) return null;

        JsonNode root = mapper.readTree(body);
        JsonNode payload = isEnvelope(root) ? root.get("data") : root;
        if (payload == null || payload.isNull()) return null;

        JavaType javaType = mapper.getTypeFactory().constructType(type);
        return mapper.convertValue(payload, javaType);
    }

    private boolean isEnvelope(JsonNode root) {
        return root.isObject() && root.has("success") && root.has("data");
    }
}