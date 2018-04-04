#Brabantian refactoring

This describes the currently ongoing effort of moving to a
dependency-injection style architecture of managing dependencies
between different parts of the system.

## Dependency Injection intro

Dependency Injection, according to Wikipedia: 

(https://en.wikipedia.org/wiki/Dependency_injection)

_In software engineering, dependency injection is a technique whereby
one object supplies the dependencies of another object.  A dependency
is an object that can be used (a service). An injection is the passing
of a dependency to a dependent object (a client) that would use it.
The service is made part of the client's state. Passing the service to
the client, rather than allowing a client to build or find the
service, is the fundamental requirement of the pattern.  This
fundamental requirement means that using values (services) produced
within the class from new or static methods is prohibited.  The class
should accept values passed in from outside. This allows the class to
make acquiring dependencies someone else's problem.  The intent behind
dependency injection is to decouple objects to the extent that no
client code has to be changed simply because an object it depends on
needs to be changed to a different one.  Dependency injection is one
form of the broader technique of inversion of control. Rather than low
level code calling up to high level code, high level code can receive
lower level code that it can call down to. This inverts the typical
control pattern seen in procedural programming._


Using dependency injection, we can move the responsibility to locate,
or create dependencies out of the different parts of our system.  This
results in cleaner code, that makes very clear which dependencies of
other components that exist in a component. It also produces far more
testable code, because it becomes very easy to mock a dependency and
inject them to do unit testing. Before any refactorings occurred,
there were an amount of cyclical dependencies as well, all hidden by
the fact that static methods to Burst.class were used to pick these
up.

To enable this, I am doing

## Ongoing Refactorings

### Moving static methods out of Domain objects, to service classes

A very large amount of static methods, which relied on DB tables were
found in Domain classes, for example in Account.class.  These give
those classes a hard dependency on Burst, to pick up these tables. It
also created a very unclear distinction between the class as a
data-holder, and the class as being responsible for domain logic. For
these kind of methods, that were actually beyond the scope of the
Domain class (dependency on the DB layer, etc), service classes are
made, which can hold the logic in non-static methods.

### Creating Service classes and other dependencies in Burst.class and injecting them where necessary

A service should always be made only once in Burst.class, and passed
its required dependencies, like a DB layer abstraction or potentially
other services (prevent cyclical dependencies between services!).
These services can then be passed in for example the HTTP and Peer
layer, where they can be called from the "usecases" these tend to
form.
 
## Major TODOs

### Testing

- Currently the happy paths of the AbstractTransactionTests are
limited to making sure no nullpointer exceptions, etc occur.  "Errors"
are still returned, however, because of other issues like missing
parameter, etc.  More scaffolding should be made to extend the happy
path of these tests easier.  Though currently the focus of @Brabantian
is on the business logic validations done in the CreateTransaction
HTTP classes.
