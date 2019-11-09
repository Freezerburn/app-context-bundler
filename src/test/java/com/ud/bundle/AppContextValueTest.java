package com.ud.bundle;

import org.junit.jupiter.api.Test;

public class AppContextValueTest {

  @Test
  public void registerAndGetLeafStringValue() {
    final var ctx = new AppContext();
    final var ctxValue = ctx.registerValue("a", "foo");
  }
}
