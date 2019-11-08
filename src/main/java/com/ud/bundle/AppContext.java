package com.ud.bundle;

import com.ud.bundle.QualifiedBundle.NoQualifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class AppContext {

  private final Map<BundleKey<? extends Enum<?>, ? extends ContextBundle>, ContextBundle> bundles = new HashMap<>();
  private final Map<BundleKey<? extends Enum<?>, ? extends ContextBundle>, List<ContextBundle>> providedBundles = new HashMap<>();
  private final Deque<BundleKey<? extends Enum<?>, ? extends ContextBundle>> registerStack = new ArrayDeque<>();

  public <T extends ContextBundle> void registerBundle(final T bundle) {
    registerBundle(bundle, NoQualifier.INSTANCE);
  }

  public <T extends ContextBundle> void registerBundle(final QualifiedBundle<T> qualifiedBundle) {
    final T bundle = Objects.requireNonNull(qualifiedBundle.getBundle(), "'bundle' in QualifiedBundle must not be null.");
    final Enum<?> qualifier = qualifiedBundle.getQualifier();
    registerBundle(bundle, qualifier);
  }

  public <T extends ContextBundle> void registerBundle(final T bundle, final Enum<?> qualifier) {
    final Class<? extends ContextBundle> clazz = bundle.getClass();
    final var key = new BundleKey<>(qualifier, clazz);
    if (bundles.containsKey(key)) {
      throw new IllegalArgumentException("Bundle for " + clazz + " has already been registered with: " + bundles.get(key));
    }

    final var requiredBundles = bundle.requiredBundles();
    for (final Class<? extends ContextBundle> required : requiredBundles) {
      if (!isBundleRegistered(required)) {
        throw new IllegalStateException(clazz + " requires bundle " + required + " that has not yet been registered. This is a programmer error.");
      }
    }
    final var provided = new ContextBundle[requiredBundles.size()];
    for (int i = 0; i < requiredBundles.size(); i++) {
      provided[i] = getBundle(requiredBundles.get(i));
    }

    bundles.put(key, bundle);
    if (!registerStack.isEmpty()) {
      final var p = providedBundles.computeIfAbsent(registerStack.peek(), c -> new ArrayList<>());
      p.add(bundle);
    }

    registerStack.push(key);
    bundle.apply(this, provided);
    registerStack.pop();
  }

  public <T extends ContextBundle> T getBundle(final Class<T> clazz) {
    return getBundle(clazz, NoQualifier.INSTANCE);
  }

  public <T extends ContextBundle> T getBundle(final QualifiedBundle<T> qualifiedBundle) {
    return getBundle(qualifiedBundle.getClazz(), qualifiedBundle.getQualifier());
  }

  public <T extends ContextBundle> T getBundle(final Class<T> clazz, final Enum<?> qualifier) {
    final var key = new BundleKey<>(qualifier, clazz);
    if (!bundles.containsKey(key)) {
      throw new IllegalArgumentException("Bundle for " + clazz + " has not yet been registered. This is a programmer error.");
    }
    return clazz.cast(bundles.get(key));
  }

  public <T extends ContextBundle> void useBundle(final Class<T> clazz, final Consumer<T> f) {
    useBundle(clazz, NoQualifier.INSTANCE, f);
  }

  public <T extends ContextBundle> void useBundle(final QualifiedBundle<T> qualifiedBundle, final Consumer<T> f) {
    useBundle(qualifiedBundle.getClazz(), qualifiedBundle.getQualifier(), f);
  }

  public <T extends ContextBundle> void useBundle(final Class<T> clazz, final Enum<?> qualifier, final Consumer<T> f) {
    final var key = new BundleKey<>(qualifier, clazz);
    if (!bundles.containsKey(key)) {
      throw new IllegalArgumentException("Bundle for " + clazz + " has not yet been registered. This is a programmer error.");
    }
    f.accept(clazz.cast(bundles.get(key)));
  }

  public boolean isBundleRegistered(final Class<? extends ContextBundle> clazz) {
    return isBundleRegistered(clazz, NoQualifier.INSTANCE);
  }

  public boolean isBundleRegistered(final QualifiedBundle<? extends ContextBundle> qualifiedBundle) {
    return isBundleRegistered(qualifiedBundle.getClazz(), qualifiedBundle.getQualifier());
  }

  public boolean isBundleRegistered(final Class<? extends ContextBundle> clazz, final Enum<?> qualifier) {
    return bundles.containsKey(new BundleKey<>(qualifier, clazz));
  }

  public List<ContextBundle> providedBy(final Class<? extends ContextBundle> clazz) {
    return providedBy(clazz, NoQualifier.INSTANCE);
  }

  public List<ContextBundle> providedBy(final QualifiedBundle<? extends ContextBundle> qualifiedBundle) {
    return providedBy(qualifiedBundle.getClazz(), qualifiedBundle.getQualifier());
  }

  public List<ContextBundle> providedBy(final Class<? extends ContextBundle> clazz, final Enum<?> qualifier) {
    final var key = new BundleKey<>(qualifier, clazz);
    if (!providedBundles.containsKey(key)) {
      throw new IllegalArgumentException("Bundle for " + clazz + " has not yet been registered, thus cannot provide any bundles. This is a programmer error.");
    }
    return providedBundles.get(key);
  }

  private static class BundleKey<T extends Enum<T>, K extends ContextBundle> {

    private final Enum<T> qualifier;
    private final Class<K> clazz;

    private BundleKey(final Enum<T> qualifier, final Class<K> clazz) {
      this.qualifier = qualifier;
      this.clazz = clazz;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BundleKey<?, ?> bundleKey = (BundleKey<?, ?>) o;
      return qualifier.equals(bundleKey.qualifier) &&
          clazz.equals(bundleKey.clazz);
    }

    @Override
    public int hashCode() {
      return Objects.hash(qualifier, clazz);
    }
  }
}
