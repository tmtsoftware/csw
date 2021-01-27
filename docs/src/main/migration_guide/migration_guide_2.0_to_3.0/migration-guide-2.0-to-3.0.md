# Migration Guide from 2.0 to 3.0

This guide shows how to migrate from CSW Version 2 to CSW Version 3. 

## Parameter Sets
- In version 2.0, the `Key` class used to create a `Parameter` had a `->` method that could take either a `varargs` of values or
an array of values, as such:

```scala
val k2: Key[Short]   = KeyType.ShortKey.make("RandomKeyName")
val paramWithShorts1: Parameter[Short] = k2 -> 1
val paramWithShorts2: Parameter[Short] = k2 -> (1, 2, 3, 4)
val paramWithShorts3: Parameter[Short] = k2 -> Array[Short](1, 2, 3, 4)
```

In 3.0, the `->` operator now only takes a single value.  To use multiple values, the `set` and `setAll` methods should be
used for `varargs` or arrays, respectively:

```scala
val k2: Key[Short]   = KeyType.ShortKey.make("RandomKeyName")
val paramWithShorts1: Parameter[Short] = k2 -> 1
val paramWithShorts2: Parameter[Short] = k2.set(1, 2, 3, 4)
val paramWithShorts3: Parameter[Short] = k2.setAll(Array[Short](1, 2, 3, 4))
```
- Additionally, Parameter `Key` names can no longer contain the characters `[`, `]` or `/`.

## Location Service
- The `RegistrationFactory` class has been removed from `location-server` module.  In most cases, `AkkaRegistrationFactory`
 would have been used to create an `AkkaRegistration`, so this change should not affect most users.  
 However, there has been a small API change to the usage of this class.  
    - For Scala users, `AkkaRegistrationFactory` now takes an `actorRef` instead of URI of the `actorRef` being registered.
    - For Java users, a new `JAkkaRegistrationFactory` has been created which should be used.

In 2.0, Scala usage might have looked like this:
```scala
def behavior(): Behavior[String] = Behaviors.setup { ctx =>
  Behaviors.receiveMessage { msg =>
    Behaviors.same
  }
}
val typedActorRef: ActorRef[String] = context.system.spawn(behavior(), "typed-actor-ref")
val assemblyConnection = AkkaConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "assembly1"), ComponentType.Assembly))
val assemblyRegistration: AkkaRegistration = AkkaRegistrationFactory.make(assemblyConnection, typedActorRef.toURI)
```

and in Java:
```java
Behavior<String> behavior = Behaviors.setup(ctx -> Behaviors.same());
akka.actor.typed.ActorRef<String> typedActorRef = Adapter.spawn(context(), behavior, "typed-actor-ref");
AkkaConnection assemblyConnection = new AkkaConnection(new ComponentId(new Prefix(JSubsystem.NFIRAOS, "assembly1"), JComponentType.Assembly));
AkkaRegistration assemblyRegistration = new RegistrationFactory().akkaTyped(assemblyConnection, typedActorRef);
```    

Now, in 3.0, the Scala code is similar, but when creating the registration, use the `actorRef` instead of the URI:

```scala
val assemblyRegistration: AkkaRegistration = AkkaRegistrationFactory.make(assemblyConnection, typedActorRef)
```
and in Java, use `JAkkaRegistrationFactory`:

```java
AkkaRegistration assemblyRegistration = JAkkaRegistrationFactory.make(assemblyConnection, typedActorRef);
```

- There was also a change to the Location Service models to allow some metadata to be stored in the `location` and `registration` entities.
The metadata is primarily used for internal OSW tasks, such as allowing the ESW Agent to manage remotely started processes.  
Since the affected models are typically created by factory methods, it shouldn't impact component developers.

## Other
- The component framework code has been modified in a way that should simplify component development.  The return type of
handler methods `initialize` and `onShutdown` was changed from `Future[Unit]` to `Unit`.  Since both of these methods are
now blocking, developers should minimize the time to complete these tasks.  However, we feel that it is appropriate to be 
blocking in these methods since nothing much can be done while these methods are in progress.

