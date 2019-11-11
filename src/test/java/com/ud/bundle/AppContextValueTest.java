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
  public void registerSameNestedPathTwiceThrowsException() {
    final var ctx = new AppContext();
    ctx.registerValue("a.b", 1);
    assertThrows(IllegalArgumentException.class, () -> ctx.registerValue("a.b", 2));
  }

  @Test
  public void registerSameNestedArrayPathTwiceThrowsException() {
    final var ctx = new AppContext();
    ctx.registerValue("a.0", 1);
    assertThrows(IllegalArgumentException.class, () -> ctx.registerValue("a.0", 2));
  }

  @Test
  public void registerArrayIndexAtRootThrowsException() {
    final var ctx = new AppContext();
    assertThrows(IllegalArgumentException.class, () -> ctx.registerValue("0", 2));
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
  public void registerAndGetStringInObjectValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a.b", "foo");
    assertEquals(ctxValue, ctx.getValue("a.b"));
    assertEquals("foo", ctxValue.asString());
    assertEquals("foo", ctx.getValue("a.b").asString());
  }

  @Test
  public void registerAndGetNumberInObjectValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a.b", 2);
    assertEquals(ctxValue, ctx.getValue("a.b"));
    assertEquals(2, ctxValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a.b").asNumber().intValue());
  }

  @Test
  public void registerAndGetStringInArrayValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a.0", "foo");
    assertEquals(ctxValue, ctx.getValue("a.0"));
    assertEquals("foo", ctxValue.asString());
    assertEquals("foo", ctx.getValue("a.0").asString());
  }

  @Test
  public void registerAndGetNumberInArrayValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a.0", 2);
    assertEquals(ctxValue, ctx.getValue("a.0"));
    assertEquals(2, ctxValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a.0").asNumber().intValue());
  }

  @Test
  public void registerAndGetNumberInNonZeroArrayValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a.1", 2);
    assertEquals(ctxValue, ctx.getValue("a.1"));
    assertEquals(2, ctxValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a.1").asNumber().intValue());
  }

  @Test
  public void registerAndGetNumberInZeroAndNonZeroArrayValue() {
    final var ctx = new AppContext();
    final var numValue = ctx.registerValue("a.0", 2);
    final var numValue1 = ctx.registerValue("a.1", 3);
    assertEquals(numValue, ctx.getValue("a.0"));
    assertEquals(2, numValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a.0").asNumber().intValue());
    assertEquals(numValue1, ctx.getValue("a.1"));
    assertEquals(3, numValue1.asNumber().intValue());
    assertEquals(3, ctx.getValue("a.1").asNumber().intValue());
  }

  @Test
  public void registerAndGetTwoLeafNumbers() {
    final var ctx = new AppContext();
    final var numValue = ctx.registerValue("a", 2);
    final var numValue2 = ctx.registerValue("b", 3);
    assertEquals(numValue, ctx.getValue("a"));
    assertEquals(numValue2, ctx.getValue("b"));
    assertEquals(2, numValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a").asNumber().intValue());
    assertEquals(3, numValue2.asNumber().intValue());
    assertEquals(3, ctx.getValue("b").asNumber().intValue());
  }

  @Test
  public void registerAndGetTwoLeafStrings() {
    final var ctx = new AppContext();
    final var strValue = ctx.registerValue("b", "foo");
    final var strValue2 = ctx.registerValue("a", "bar");
    assertEquals(strValue2, ctx.getValue("a"));
    assertEquals(strValue, ctx.getValue("b"));
    assertEquals("bar", strValue2.asString());
    assertEquals("bar", ctx.getValue("a").asString());
    assertEquals("foo", strValue.asString());
    assertEquals("foo", ctx.getValue("b").asString());
  }

  @Test
  public void registerAndGetLeafNumberAndString() {
    final var ctx = new AppContext();
    final var numValue = ctx.registerValue("a", 2);
    final var strValue = ctx.registerValue("b", "foo");
    assertEquals(numValue, ctx.getValue("a"));
    assertEquals(strValue, ctx.getValue("b"));
    assertEquals(2, numValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a").asNumber().intValue());
    assertEquals("foo", strValue.asString());
    assertEquals("foo", ctx.getValue("b").asString());
  }

  @Test
  public void registerAndGetNumberAndStringInObject() {
    final var ctx = new AppContext();
    final var numValue = ctx.registerValue("a.a", 2);
    final var strValue = ctx.registerValue("a.b", "foo");
    assertEquals(numValue, ctx.getValue("a.a"));
    assertEquals(strValue, ctx.getValue("a.b"));
    assertEquals(2, numValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a.a").asNumber().intValue());
    assertEquals("foo", strValue.asString());
    assertEquals("foo", ctx.getValue("a.b").asString());
  }

  @Test
  public void registerAndGetLeafNumberAndNestedStringInObject() {
    final var ctx = new AppContext();
    final var numValue = ctx.registerValue("a", 2);
    final var strValue = ctx.registerValue("b.a", "foo");
    assertEquals(numValue, ctx.getValue("a"));
    assertEquals(strValue, ctx.getValue("b.a"));
    assertEquals(2, numValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a").asNumber().intValue());
    assertEquals("foo", strValue.asString());
    assertEquals("foo", ctx.getValue("b.a").asString());
  }

  @Test
  public void registerAndGetLeafStringAndNestedNumberInObject() {
    final var ctx = new AppContext();
    final var strValue = ctx.registerValue("a", "foo");
    final var numValue = ctx.registerValue("b.a", 2);
    assertEquals(strValue, ctx.getValue("a"));
    assertEquals(numValue, ctx.getValue("b.a"));
    assertEquals("foo", strValue.asString());
    assertEquals("foo", ctx.getValue("a").asString());
    assertEquals(2, numValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("b.a").asNumber().intValue());
  }

  @Test
  public void registerAndGetLeafNumberAndNestedStringInArray() {
    final var ctx = new AppContext();
    final var numValue = ctx.registerValue("a", 2);
    final var strValue = ctx.registerValue("b.0", "foo");
    assertEquals(numValue, ctx.getValue("a"));
    assertEquals(strValue, ctx.getValue("b.0"));
    assertEquals(2, numValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("a").asNumber().intValue());
    assertEquals("foo", strValue.asString());
    assertEquals("foo", ctx.getValue("b.0").asString());
  }

  @Test
  public void registerAndGetLeafStringAndNestedNumberInArray() {
    final var ctx = new AppContext();
    final var strValue = ctx.registerValue("a", "foo");
    final var numValue = ctx.registerValue("b.0", 2);
    assertEquals(strValue, ctx.getValue("a"));
    assertEquals(numValue, ctx.getValue("b.0"));
    assertEquals("foo", strValue.asString());
    assertEquals("foo", ctx.getValue("a").asString());
    assertEquals(2, numValue.asNumber().intValue());
    assertEquals(2, ctx.getValue("b.0").asNumber().intValue());
  }
}
