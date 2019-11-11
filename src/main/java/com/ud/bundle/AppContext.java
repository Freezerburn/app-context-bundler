package com.ud.bundle;

import com.ud.bundle.QualifiedBundle.NoQualifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AppContext {

  private static final Pattern PATH_SPLITTER = Pattern.compile("\\.");

  public static final String VALUE_PATH_SEPARATOR = ".";

  public static String joinPath(final String... parts) {
    return String.join(VALUE_PATH_SEPARATOR, parts);
  }

  private final Map<BundleKey<? extends Enum<?>, ? extends ContextBundle>, ContextBundle> bundles = new HashMap<>();
  private final Map<BundleKey<? extends Enum<?>, ? extends ContextBundle>, List<ContextBundle>> providedBundles = new HashMap<>();
  private final Deque<BundleKey<? extends Enum<?>, ? extends ContextBundle>> registerStack = new ArrayDeque<>();

  private final ContextValue root = new ObjectContainerValue(null);
  private final Set<String> registeredPaths = new HashSet<>();
  private final Map<ValueKey<? extends Enum<?>>, ContextValue> values = new HashMap<>();

  // Values are one of 4 things:
  // - Container, which is one of:
  //   1 Object (string key)
  //   2 Array (int key)
  // 3 String
  // 4 Number
  // (in Java-land a Number can technically be an int, long, float, double, or byte, but those are details to only worry about when it
  // comes to optimization. focus on the common, simple case first)
  //
  // Types are based off of how JSON works. A path is just a series of keys used to traverse a piece of JSON. All values should be
  // possible to be created from JSON, or turned back into JSON.
  //
  // - Should values be allowed to be mutated into new types? (e.g.: update a number to be an array or string)
  //
  // Paths indicate things about the structure of values:
  // - "a": leaf node, string or number
  // - "a.0": "a" is an array, with the first element being a string or number
  // - "a.b": "a" is an Object, "b" is the leaf node with a string or number
  // - "a.0.b": "a" is an array, the first element of "a" is an object, "b" is the leaf node being a string or number
  // - "a.b.c": "a" is an object, "b" is the key for an object, "c" is the leaf node with a string or number
  // - ... etc. for more nested cases ...
  // Which we can summarize with:
  // - The root of all values can be considered to be an object with string keys
  // - The last part of a path is always the leaf node and is a string or number
  // - A string followed by a number means we have an array
  // - A number as the last part of a path is still a leaf node
  // - A string followed by a string means the part is an object
  //
  // Storage of a value is a tree: in order to store or retrieve at any given path, must traverse the tree to get to the leaf.
  //
  // Registering simple values is only going to be designed to store leaf nodes. Everything about the path dictates whether or not an object
  // or array will be traversed at each step to get to the leaf and values will be created to make that path happen.
  // In order to store more complex sets of values, a separate method will take in a ContextValue that can be created using static factory
  // methods to nest a more complex structure and that will be ingested into something internally with simple leaf nodes. Which will make
  // it easy for the developer to create things like an array of values without having to do a bunch of String concatenation/formatting
  // themselves to make that happen.
  // The third way to handle creating value will be to ingest JSON and have everything done for you automatically.

  public ContextValue registerValue(@NotNull final String path, @NotNull final Object value) {
    Objects.requireNonNull(path, "'path' parameter must not be null.");
    Objects.requireNonNull(value, "'value' parameter must not be null.");

    if (path.isBlank()) {
      throw new IllegalArgumentException("'path' parameter must contain non-whitespace characters.");
    }
    if (registeredPaths.contains(path)) {
      throw new IllegalArgumentException("Path " + path + " has already been registered with a value. This is a programmer error.");
    }
    // Only supports registering the leaf node values. The path is what defines how the internal structure is traversed.
    if (!(value instanceof String || value instanceof Number)) {
      throw new IllegalArgumentException("Value must be a String or Number. Was: " + value.getClass());
    }
    final var parts = PATH_SPLITTER.split(path.toLowerCase());
    for (int i = 0; i < parts.length; i++) {
      if (parts[i].isBlank()) {
        throw new IllegalArgumentException("No parts of the path can consist of only whitespace. (part " + i + " violated this rule)");
      }
    }

    var parent = root;
    final var key = new ValueKey<>(path, NoQualifier.INSTANCE);
    for (int i = 0; i < parts.length; i++) {
      final var part = parts[i];

      var isNumeric = false;
      var numericIdx = -1;
      try {
        numericIdx = Integer.parseInt(part);
        isNumeric = true;
      } catch (final NumberFormatException ignored) {
        // isNumeric already false, nothing to do
      }
      if (isNumeric && parent == root) {
        throw new IllegalArgumentException("Cannot use a numeric array index at the root level.");
      }

      if (i + 1 == parts.length) {
        // reached leaf
        final var holder = new ValueHolder(parent, value);
        if (parent.isArray()) {
          ((ArrayContainerValue) parent).addChildWithStringIndex(part, holder);
        } else {
          ((ObjectContainerValue) parent).addChild(part, holder);
        }
        values.put(key, holder);
        registeredPaths.add(path);
        return holder;
      }

      ContextValue child = null;
      if (isNumeric) {
        // index into array received.
        if (!parent.isArray()) {
          // TODO: Print out path and type. Basically include useful error information for developer since this can be user facing.
          throw new IllegalArgumentException("Got array index in path, but value was not an array.");
        }
        child = parent.child(numericIdx);
      } else {
        // string key into object received.
        if (!parent.isObject()) {
          // TODO: Print out path and type. Basically include useful error information for developer since this can be user facing.
          throw new IllegalArgumentException("Got string key in path, but value was not an object.");
        }
        child = parent.child(part);
      }

      if (child == null) {
        parent = addChildPath(parts, i, parent);
      } else {
        parent = child;
      }
    }

    throw new IllegalStateException("Reached end of registerValue without either thrown another exception or returning the value. This is a library error.");
  }

  private ContextValue addChildPath(final String[] parts, final int i, final ContextValue parent) {
    final var nextPart = parts[i + 1];
    var nextIsNumeric = false;
    var nextIdx = -1;
    try {
      nextIdx = Integer.parseInt(nextPart);
      nextIsNumeric = true;
    } catch (final NumberFormatException ignore) {
      // Numeric flag already false, nothing to do.
    }

    final ContextValue ret;
    if (nextIsNumeric) {
      ret = new ArrayContainerValue(parent);
    } else {
      ret = new ObjectContainerValue(parent);
    }
    if (parent.isArray()) {
      ((ArrayContainerValue) parent).addChild(nextIdx, ret);
    } else {
      ((ObjectContainerValue) parent).addChild(nextPart, ret);
    }
    return ret;
  }

  public ContextValue getValue(final String path) {
    if (!registeredPaths.contains(path)) {
      throw new IllegalArgumentException("Path " + path + " has not yet been registered.");
    }
    return values.get(new ValueKey<>(path, NoQualifier.INSTANCE));
  }

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
    for (final QualifiedBundle<? extends ContextBundle> required : requiredBundles) {
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

  private static class ValueKey<K extends Enum<K>> {

    @NotNull
    private final String pathPart;
    @NotNull
    private final Enum<K> qualifier;

    private ValueKey(@NotNull final String pathPart, @NotNull final Enum<K> qualifier) {
      this.pathPart = pathPart;
      this.qualifier = qualifier;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ValueKey<?> valueKey = (ValueKey<?>) o;
      return pathPart.equals(valueKey.pathPart) &&
          qualifier.equals(valueKey.qualifier);
    }

    @Override
    public int hashCode() {
      return Objects.hash(pathPart, qualifier);
    }
  }

  private static class ArrayContainerValue implements ContextValue {

    @NotNull
    private final ContextValue parent;
    @NotNull
    private final List<ContextValue> children = new ArrayList<>();

    private ArrayContainerValue(@NotNull final ContextValue parent) {
      this.parent = parent;
    }

    private void addChildWithStringIndex(final String idx, final ContextValue child) {
      try {
        addChild(Integer.parseInt(idx), child);
      } catch (final NumberFormatException e) {
        throw new IllegalStateException("Attempted to add a child with string representing a numeric index but was not an integer. This is a library error.", e);
      }
    }

    private void addChild(final int idx, final ContextValue child) {
      while (children.size() < idx - 1) {
        children.add(null);
      }
      children.add(child);
    }

    @Override
    public ContextValue parent() {
      return parent;
    }

    @Override
    public ContextValue child(@NotNull final String key) {
      throw new UnsupportedOperationException("Cannot get a child by String key from an array. This is a library error.");
    }

    @Override
    public ContextValue child(final int key) {
      return children.get(key);
    }

    @Override
    public boolean isContainer() {
      return true;
    }

    @Override
    public boolean isObject() {
      return false;
    }

    @Override
    public boolean isArray() {
      return true;
    }

    @Override
    public boolean isLeaf() {
      return false;
    }

    @Override
    public Object update(@NotNull Object newValue) {
      throw new UnsupportedOperationException("Cannot update an array with a new array. This is a library error.");
    }

    @Override
    public String asString() {
      throw new UnsupportedOperationException("Cannot represent an object as a string.");
    }

    @Override
    public Number asNumber() {
      throw new UnsupportedOperationException("Cannot represent an object as a number.");
    }
  }

  private static class ObjectContainerValue implements ContextValue {

    @Nullable
    private final ContextValue parent;
    private final Map<String, ContextValue> children = new HashMap<>();

    private ObjectContainerValue(final ContextValue parent) {
      this.parent = parent;
    }

    private void addChild(final String key, final ContextValue value) {
      children.put(key, value);
    }

    @Override
    public ContextValue parent() {
      return parent;
    }

    @Override
    public ContextValue child(@NotNull final String key) {
      return children.get(key);
    }

    @Override
    public ContextValue child(final int key) {
      throw new UnsupportedOperationException("Cannot get an array index from an object container. This is a library error.");
    }

    @Override
    public boolean isContainer() {
      return true;
    }

    @Override
    public boolean isObject() {
      return true;
    }

    @Override
    public boolean isArray() {
      return false;
    }

    @Override
    public boolean isLeaf() {
      return false;
    }

    @Override
    public Object update(@NotNull Object newValue) {
      throw new UnsupportedOperationException("Cannot update an array container. This is a library error.");
    }

    @Override
    public String asString() {
      throw new UnsupportedOperationException("Cannot represent an array as a string.");
    }

    @Override
    public Number asNumber() {
      throw new UnsupportedOperationException("Cannot represent an array as a number.");
    }
  }

  private static class ValueHolder implements ContextValue {

    @NotNull
    private ContextValue parent;
    @NotNull
    private Object value;

    ValueHolder(@NotNull final ContextValue parent, @NotNull final Object value) {
      this.parent = parent;
      this.value = value;
    }

    @Override
    public ContextValue parent() {
      return parent;
    }

    @Override
    public ContextValue child(@NotNull final String key) {
      throw new UnsupportedOperationException("Cannot get a child of a leaf value. This is a library error.");
    }

    @Override
    public ContextValue child(final int key) {
      throw new UnsupportedOperationException("Cannot get a child of a leaf value. This is a library error.");
    }

    @Override
    public boolean isContainer() {
      return false;
    }

    @Override
    public boolean isObject() {
      return false;
    }

    @Override
    public boolean isArray() {
      return false;
    }

    @Override
    public boolean isLeaf() {
      return true;
    }

    @Override
    public Object update(@NotNull final Object newValue) {
      final var old = value;
      value = newValue;
      return old;
    }

    @Override
    public String asString() {
      return value.toString();
    }

    @Override
    public Number asNumber() {
      if (value instanceof Number) {
        return (Number) value;
      } else {
        final String strValue = (String) value;
        try {
          return Long.parseLong(strValue);
        } catch (final NumberFormatException ignored) {
        }
        try {
          return Double.parseDouble(strValue);
        } catch (final NumberFormatException ignored) {
        }
      }

      throw new UnsupportedOperationException("Cannot represent the value " + value + " as a number.");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ValueHolder that = (ValueHolder) o;
      return parent.equals(that.parent) &&
          value.equals(that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(parent, value);
    }

    @Override
    public String toString() {
      return "ValueHolder{" +
          "parent=" + parent +
          ", value=" + value +
          '}';
    }
  }
}
