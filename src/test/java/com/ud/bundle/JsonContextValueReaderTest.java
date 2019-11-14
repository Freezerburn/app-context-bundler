package com.ud.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
