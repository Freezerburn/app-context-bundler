package com.ud.bundle;

import org.jetbrains.annotations.NotNull;

public interface ContextValue {

  ContextValue parent();
  ContextValue child(@NotNull final String key);
  ContextValue child(final int key);
  boolean isContainer();
  boolean isObject();
  boolean isArray();
  boolean isLeaf();

  Object update(@NotNull final Object newValue);
  String asString();
  Number asNumber();
}
