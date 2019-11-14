package com.ud.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ud.bundle.reader.JsonContextValueReader;
import org.junit.jupiter.api.Test;

public class JsonContextValueReaderTest {

  @Test
  public void readSingleRootStringPropertyFromString() {
    final var json = "{\"a\": \"foo\"}";
    final var ctx = new AppContext();
    final var reader = new JsonContextValueReader(json);
    reader.readInto(ctx);
    assertEquals("foo", ctx.getValue("a").asString());
  }

  @Test
  public void readSingleRootNumberPropertyFromString() {
    final var json = "{\"a\": 5}";
    final var ctx = new AppContext();
    final var reader = new JsonContextValueReader(json);
    reader.readInto(ctx);
    assertEquals(5, ctx.getValue("a").asNumber().intValue());
  }

  @Test
  public void readRootStringAndNumberPropertiesFromString() {
    final var json = "{\"a\": \"foo\", \"b\": 5}";
    final var ctx = new AppContext();
    final var reader = new JsonContextValueReader(json);
    reader.readInto(ctx);
    assertEquals("foo", ctx.getValue("a").asString());
    assertEquals(5, ctx.getValue("b").asNumber().intValue());
  }

  @Test
  public void readNestedObjectStringAndNumberPropertyFromString() {
    final var json = "{\"a\": {\"a\": \"foo\", \"b\": 5}}";
    final var ctx = new AppContext();
    final var reader = new JsonContextValueReader(json);
    reader.readInto(ctx);
    assertTrue(ctx.getValue("a").isObject());
    assertEquals("foo", ctx.getValue("a.a").asString());
    assertEquals(5, ctx.getValue("a.b").asNumber().intValue());
  }

  @Test
  public void readNestedArrayStringAndNumberPropertyFromString() {
    final var json = "{\"a\": [\"foo\", 5]}";
    final var ctx = new AppContext();
    final var reader = new JsonContextValueReader(json);
    reader.readInto(ctx);
    assertTrue(ctx.getValue("a").isArray());
    assertEquals("foo", ctx.getValue("a.0").asString());
    assertEquals(5, ctx.getValue("a.1").asNumber().intValue());
  }

  @Test
  public void readNestedObjectInObjectStringAndNumberPropertyFromString() {
    final var json = "{\"a\": {\"a\": {\"a\": \"foo\", \"b\": 5}}}";
    final var ctx = new AppContext();
    final var reader = new JsonContextValueReader(json);
    reader.readInto(ctx);
    assertTrue(ctx.getValue("a").isObject());
    assertTrue(ctx.getValue("a.a").isObject());
    assertEquals("foo", ctx.getValue("a.a.a").asString());
    assertEquals(5, ctx.getValue("a.a.b").asNumber().intValue());
  }
}
