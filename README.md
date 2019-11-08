App Context Bundler
===================

Provides a simple library for managing the various objects/context that you need when writing an application such as a web server.
Created as a way to provide simple "dependency injection" when writing web servers that are not Spring-based, but in a way that
should be much quicker during startup than Spring and in a way that makes more sense to the author. Instead of writing a bunch of
XML or annotations and doing classpath scanning, instead just provide the different "bundles" of context that your application
needs stored inside of a single object, and pass that object to the various places that you need access to the global context.
Similar to having an object that has direct properties on it with all the different globally-required objects.

Possible Future Enhancements
============================

* Utilities for loading configuration from files into a bundle. A likely scenario in a lot of applications is going to be that you want
to load a config file into a bundle that can be accessed across the application, so an easy way to do that is highly desirable.
* Ability to provide values. These would be something that can be changed and would cause updates to be sent out to anything that
uses those values. Potentially useful for something where you might have a properties file that gets changed on the file system, and
you need to propagate those changes throughout the application. Or you get those values from a database. Or any other scenario where
a value that has changes tracked and propagated would be useful. (this might also be how the configuration loading is implemented?)
* Ability for a bundle to provide values. Could be used as a mechanism for allowing a bundle internally updating itself to communicate
those changes without having to manually wire that change tracking up yourself. Can potentially be used for some kind of communication
mechanism between bundles provided by other bundles?

API
===

## interface ContextBundle

The base interface that needs to be implemented in order to be able to use objects as bundles with an `AppContext`. Can specify a list
of other bundles that must be registered before a specific bundle. Any required bundles will be provided at the time of registration to
the bundle that requires them. Every bundle gets a reference to the current `AppContext` so it can interact with the current context for
anything, such as registering bundles itself. Any bundles registered in this way are tracked as "provided" by a bundle.

* `List<QualifiedBundle<? extends ContextBundle>> requiredBundles()`: By default returns an empty `List`. Can be overridden to return a classes
that the bundle requires to exist in the `AppContext` before this `ContextBundle` gets registered.
* `apply(AppContext, ContextBundle...)`: Called when the class implementing `ContextBundle` has been registered into an `AppContext`.
Any required `ContextBundle`s are passed into the varargs in the same order that they are returned from `requiredBundles`.

## AppContext

The object that is designed to hold all the necessary context for running an application. Provides methods for registering bundles, getting bundles,
etc.

* `registerBundle(ContextBundle)`: Store an unqualified bundle into the context. Uses the result of `getClass()` as the type to retrieve it by later.
If you attempt to register the same unqualified bundle type twice, will throw an `IllegalArgumentExeption`.
* `registerBundle(ContextBundle, Enum)`: Store a bundle into the context, but uses an `Enum` to be able to qualify which specific instance
of the specific bundle type. The same `Enum` can be used later to retrieve it. Used when you need to have multiple of the same bundle type
in the context. Note that when specifying a custom Enum, all instances of a custom enum are regarded as different from an unqualified
bundle and the unqualified bundle must be retrieved by an unqualified get. If you attempt to register the same qualified bundle type twice,
will throw an `IllegalArgumentException`.
* `registerBundle(QualifiedBundle)`: The same as calling the other two `registerBundle` overloads but with a single object descriptor
instead of two parameters.
* `getBundle(Class<ContextBundle>)`: Get an unqualified bundle by class type. If you attempt to get an unqualified bundle type that has not
yet been registered, will throw an `IllegalArgumentException`.
* `getBundle(Class<ContextBundle>, Enum)`: Get a qualified bundle by class type and enum. If you attempt to get a qualified bundle type that
has not yet been registered, will throw an `IllegalArgumentException`.
* `getBundle(QualifiedBundle)`: Same as the one or two argument versions, but with a single object descriptor. If you attempt to get an unqualified
or qualified bundle type that has not yet been registered, will throw an `IllegalArgumentException`.
* `getBundle(Class<ContextBundle>, Consumer<ContextBundle>)`: Get an unqualified bundle by class type, passed as an argument to a `Consumer` instead
of returned. If you attempt to get an unqualified bundle type that has not
yet been registered, will throw an `IllegalArgumentException`.
* `getBundle(Class<ContextBundle>, Consumer<ContextBundle>, Enum)`: Get a qualified bundle by class type, passed as an argument to a `Consumer` instead
of returned. If you attempt to get a qualified bundle type that has not
yet been registered, will throw an `IllegalArgumentException`.
* `getBundle(QualifiedBundle, Enum)`: Get an unqualified or qualified bundle by class type as a single argument instead of multiple,
passed as an argument to a `Consumer` instead of returned. If you attempt to get a qualified bundle type that has not
yet been registered, will throw an `IllegalArgumentException`.
* `List<ContextBundle> provicedBy(Class<ContextBundle>)`: Get the list of unqualified bundles that were registered by another bundle during their `apply` method being called.
* `List<ContextBundle> provicedBy(Class<ContextBundle>, Enum)`: Get the list of qualified bundles that were registered by another bundle during their `apply` method being called.
* `List<ContextBundle> provicedBy(QualifiedBunele)`: Get the list of unqualified or qualified bundles that were registered by another bundle during their `apply` method being called.
Using a single argument instead of multiple.

## QualifiedBundle

Used as a way to wrap over the tuple of bundle + qualifier in a single object. The basic purpose of this is to be the container for this tuple
for the required list from a `ContextBundle`. But can also be used in the `AppContext` API in place of passing in multiple parameters to qualify
any given bundle.

* `QualifiedBundle.create(Class<ContextBundle>)`: Create an "unqualified" qualified bundle. Unqualified has its own internal qualification that is
used automatically from this, and other, APIs in order to keep the internal API consistent.
* `QualifiedBundle.create(Class<ContextBundle>, Enum)`: Create a qualified bundle tuple with user-provided qualification.
* `QualifiedBundle.create(ContextBundle)`: Create an "unqualified" qualified bundle, using an actual bundle. Will retrieve the `Class<ContextBundle>` from the
`getClass()` method.
* `QualifiedBundle.create(ContextBundle, Enum)`: Create a qualified bundle tuple with user-provided qualification, using an actual bundle.
Will retrieve the `Class<ContextBundle>` from the `getClass()` method.
