package com.ud.bundle;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class QualifiedBundle<T extends ContextBundle> {

  @Nullable
  private final T bundle;
  @NotNull
  private final Class<T> clazz;
  @NotNull
  private final Enum<?> qualifier;

  @NotNull
  public static <T extends ContextBundle> QualifiedBundle<T> create(@NotNull final T bundle) {
    Objects.requireNonNull(bundle, "'bundle' parameters must not be null.");
    @SuppressWarnings("unchecked") final Class<T> clazz = (Class<T>) bundle.getClass();
    return new QualifiedBundle<>(bundle, clazz, NoQualifier.INSTANCE);
  }

  @NotNull
  public static <T extends ContextBundle> QualifiedBundle<T> create(@NotNull final T bundle, @NotNull final Enum<?> qualifier) {
    Objects.requireNonNull(bundle, "'bundle' parameter must not be null.");
    Objects.requireNonNull(qualifier, "'qualifier' parameter must not be null.");
    @SuppressWarnings("unchecked") final Class<T> clazz = (Class<T>) bundle.getClass();
    return new QualifiedBundle<>(bundle, clazz, qualifier);
  }

  @NotNull
  public static <T extends ContextBundle> QualifiedBundle<T> create(@NotNull final Class<T> clazz) {
    Objects.requireNonNull(clazz, "'clazz' parameter must not be null.");
    return new QualifiedBundle<>(null, clazz, NoQualifier.INSTANCE);
  }

  @NotNull
  public static <T extends ContextBundle> QualifiedBundle<T> create(@NotNull final Class<T> clazz, @NotNull final Enum<?> qualifier) {
    Objects.requireNonNull(clazz, "'clazz' parameter must not be null.");
    Objects.requireNonNull(qualifier, "'qualifier' parameter must not be null.");
    return new QualifiedBundle<>(null, clazz, qualifier);
  }

  private QualifiedBundle(@Nullable final T bundle, @NotNull final Class<T> clazz, @NotNull final Enum<?> qualifier) {
    this.bundle = bundle;
    this.clazz = clazz;
    this.qualifier = qualifier;
  }

  @Nullable
  public T getBundle() {
    return bundle;
  }

  @NotNull
  public Class<T> getClazz() {
    return clazz;
  }

  @NotNull
  public Enum<?> getQualifier() {
    return qualifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualifiedBundle<?> that = (QualifiedBundle<?>) o;
    return Objects.equals(bundle, that.bundle) &&
        clazz.equals(that.clazz) &&
        qualifier.equals(that.qualifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bundle, clazz, qualifier);
  }

  @Override
  public String toString() {
    return "QualifiedBundle{" +
        "bundle=" + bundle +
        ", clazz=" + clazz +
        ", qualifier=" + qualifier +
        '}';
  }

  enum NoQualifier {
    INSTANCE
  }
}
