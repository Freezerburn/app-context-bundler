package com.ud.bundle;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface ContextBundle {

  void apply(@NotNull final AppContext ctx, @NotNull final ContextBundle... requiredBundles);

  @NotNull
  default List<QualifiedBundle<? extends ContextBundle>> requiredBundles() {
    return Collections.emptyList();
  }
}
