package com.ud.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

public class AppContextTest {

  @Test
  void registerAndGetBundle() {
    final var ctx = new AppContext();
    ctx.registerBundle(new TestBundle());
    final var retrieved = ctx.getBundle(TestBundle.class);
    assertNotNull(retrieved);
  }

  @Test
  void registerQualifiedBundlesAndGetThem() {
    final var ctx = new AppContext();
    ctx.registerBundle(new TestBundle(), TestDiscriminator.ONE);
    ctx.registerBundle(QualifiedBundle.create(new TestBundle(), TestDiscriminator.TWO));
    final var retrievedOne = ctx.getBundle(TestBundle.class, TestDiscriminator.ONE);
    final var retrievedTwo = ctx.getBundle(QualifiedBundle.create(TestBundle.class, TestDiscriminator.TWO));
    assertNotNull(retrievedOne);
    assertNotNull(retrievedTwo);
    assertFalse(ctx.isBundleRegistered(QualifiedBundle.create(TestBundle.class, TestDiscriminator.THREE)));
  }

  @Test
  void registerQualifiedAndUnqualifiedBundlesAndGetThem() {
    final var ctx = new AppContext();
    ctx.registerBundle(new TestBundle());
    ctx.registerBundle(new TestBundle(), TestDiscriminator.ONE);
    ctx.registerBundle(new TestBundle2(), TestDiscriminator.ONE);

    final var retrievedOne = ctx.getBundle(TestBundle.class);
    final var retrievedTwo = ctx.getBundle(TestBundle.class, TestDiscriminator.ONE);
    final var retrievedThree = ctx.getBundle(TestBundle2.class, TestDiscriminator.ONE);

    assertNotNull(retrievedOne);
    assertNotNull(retrievedTwo);
    assertNotNull(retrievedThree);
  }

  @Test
  void registerAndUseBundle() {
    final var ctx = new AppContext();
    ctx.registerBundle(new TestBundle());
    final var b = new AtomicBoolean(false);
    ctx.useBundle(TestBundle.class, bundle -> b.set(true));
    assertTrue(b.get());
  }

  @Test
  void registerAndUseQualifiedBundle() {
    final var ctx = new AppContext();
    ctx.registerBundle(new TestBundle(), TestDiscriminator.ONE);
    final var b = new AtomicBoolean(false);
    ctx.useBundle(QualifiedBundle.create(TestBundle.class, TestDiscriminator.ONE), bundle -> b.set(true));
    assertTrue(b.get());
  }

  @Test
  void throwOnGetWithNoRegister() {
    final var ctx = new AppContext();
    assertThrows(IllegalArgumentException.class, () -> ctx.getBundle(TestBundle.class));
  }

  @Test
  void throwOnUseWithNoRegister() {
    final var ctx = new AppContext();
    assertThrows(IllegalArgumentException.class, () -> ctx.useBundle(TestBundle.class, bundle -> {
    }));
  }

  @Test
  void isRegisteredBundleReturnsTrueForRegisteredBundle() {
    final var ctx = new AppContext();
    ctx.registerBundle(new TestBundle());
    assertTrue(ctx.isBundleRegistered(TestBundle.class));
  }

  @Test
  void isRegisteredBundleReturnsFalseForNotRegisteredBundle() {
    final var ctx = new AppContext();
    assertFalse(ctx.isBundleRegistered(TestBundle.class));
  }

  @Test
  void dependentBundleGetsRequired() {
    final var ctx = new AppContext();
    ctx.registerBundle(new TestBundle());
    final var dependent = new TestBundleWithRequires();
    ctx.registerBundle(dependent);
    assertTrue(dependent.gotRequiredBundles);
  }

  @Test
  void throwOnDependentRegisterWithoutRequiredRegisteredFirst() {
    final var ctx = new AppContext();
    final var dependent = new TestBundleWithRequires();
    assertThrows(IllegalStateException.class, () -> ctx.registerBundle(dependent));
  }

  @Test
  void providedBundlesAreRegistered() {
    final var ctx = new AppContext();
    ctx.registerBundle(new TestBundleThatProvides());
    assertNotNull(ctx.getBundle(TestBundleThatProvides.class));
    assertNotNull(ctx.getBundle(TestBundleThatProvides2.class));
    assertNotNull(ctx.getBundle(TestBundle.class));

    final var provided = ctx.providedBy(TestBundleThatProvides.class);
    assertEquals(1, provided.size());
    assertEquals(TestBundleThatProvides2.class, provided.get(0).getClass());

    final var nestedProvided = ctx.providedBy(QualifiedBundle.create(TestBundleThatProvides2.class));
    assertEquals(1, nestedProvided.size());
    assertEquals(TestBundle.class, nestedProvided.get(0).getClass());
  }

  @Test
  void bundlesRegisteredAfterProviderNotAddedToOriginal() {
    final var ctx = new AppContext();
    ctx.registerBundle(new TestBundleThatProvides2());
    ctx.registerBundle(new TestBundle2());

    final var provided = ctx.providedBy(TestBundleThatProvides2.class);
    assertEquals(1, provided.size());
    assertEquals(TestBundle.class, provided.get(0).getClass());
  }

  private static final class TestBundle implements ContextBundle {

    @Override
    public void apply(@NotNull final AppContext ctx, @NotNull final ContextBundle... requiredBundles) {
    }
  }

  private static final class TestBundle2 implements ContextBundle {

    @Override
    public void apply(@NotNull final AppContext ctx, @NotNull final ContextBundle... requiredBundles) {
    }
  }

  private static final class TestBundleWithRequires implements ContextBundle {

    boolean gotRequiredBundles = false;

    @Override
    public void apply(@NotNull final AppContext ctx, @NotNull final ContextBundle... requiredBundles) {
      if (requiredBundles.length == 0) {
        return;
      }
      gotRequiredBundles = requiredBundles[0] instanceof TestBundle;
    }

    @Override
    @NotNull
    public List<QualifiedBundle<? extends ContextBundle>> requiredBundles() {
      return Collections.singletonList(QualifiedBundle.create(TestBundle.class));
    }
  }

  private static final class TestBundleThatProvides implements ContextBundle {

    @Override
    public void apply(@NotNull final AppContext ctx, @NotNull final ContextBundle... requiredBundles) {
      ctx.registerBundle(new TestBundleThatProvides2());
    }
  }

  private static final class TestBundleThatProvides2 implements ContextBundle {

    @Override
    public void apply(@NotNull final AppContext ctx, @NotNull final ContextBundle... requiredBundles) {
      ctx.registerBundle(new TestBundle());
    }
  }

  private enum TestDiscriminator {
    ONE,
    TWO,
    THREE
  }
}
