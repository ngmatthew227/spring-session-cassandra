package io.github.ngmatthew227.utils;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.util.Base64Utils;

import io.github.ngmatthew227.CassandraHttpSession;

public class SessionAttributeSerializationUtils {

  private static final Logger log = LoggerFactory.getLogger(SessionAttributeSerializationUtils.class);
  private final static SerializingConverter serializeConverter = new SerializingConverter();
  private final static DeserializingConverter deSerializeConverter = new DeserializingConverter();

  public static Map<String, String> serialize(CassandraHttpSession session) {
    Map<String, String> result = new HashMap<>();
    for (String attributeName : session.getAttributeNames()) {
      Object value = session.getAttribute(attributeName);
      String serializedValue = Base64Utils.encodeToString(serializeConverter.convert(value));
      result.put(attributeName, serializedValue);
    }
    return result;
  }

  public static CassandraHttpSession deserialize(Map<String, String> serializedAttributes,
      CassandraHttpSession targetSession) {
    if (serializedAttributes == null) {
      return (CassandraHttpSession) targetSession;
    }
    serializedAttributes.forEach((key, serialized) -> {
      if (key != null && serialized != null) {
        byte[] data = Base64Utils.decodeFromString(serialized);
        Object deserializedValue = deSerializeConverter.convert(data);
        targetSession.setAttribute(key, deserializedValue);
      }
    });
    return (CassandraHttpSession) targetSession;

  }
}
