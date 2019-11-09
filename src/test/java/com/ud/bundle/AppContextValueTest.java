package com.ud.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class AppContextValueTest {

  @Test
  public void registerSameLeafPathTwiceThrowsException() {
    final var ctx = new AppContext();
    ctx.registerValue("a", 1);
    assertThrows(IllegalArgumentException.class, () -> ctx.registerValue("a", 2));
  }

  @Test
  public void registerAndGetLeafStringValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a", "foo");
    assertEquals(ctxValue, ctx.getValue("a"));
    assertEquals("foo", ctxValue.asString());
    assertEquals("foo", ctx.getValue("a").asString());
  }

  @Test
  public void registerAndGetLeafNumberValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a", 2);
    assertEquals(ctxValue, ctx.getValue("a"));
    assertEquals(2, ctxValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a").asNumber());
  }

  @Test
  public void registerAndGetLeafStringInObjectValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a.b", "foo");
    assertEquals(ctxValue, ctx.getValue("a.b"));
    assertEquals("foo", ctxValue.asString());
    assertEquals("foo", ctx.getValue("a.b").asString());
  }

  @Test
  public void registerAndGetLeafNumberInObjectValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a.b", 2);
    assertEquals(ctxValue, ctx.getValue("a.b"));
    assertEquals(2, ctxValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a.b").asNumber().intValue());
  }

  @Test
  public void registerAndGetLeafStringInArrayValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a.0", "foo");
    assertEquals(ctxValue, ctx.getValue("a.0"));
    assertEquals("foo", ctxValue.asString());
    assertEquals("foo", ctx.getValue("a.0").asString());
  }

  @Test
  public void registerAndGetLeafNumberInArrayValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a.0", 2);
    assertEquals(ctxValue, ctx.getValue("a.0"));
    assertEquals(2, ctxValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a.0").asNumber().intValue());
  }
}
