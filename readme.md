# Why
One of the 4 final interviews at Facebook will be an architecture interview.
I thought what is better then actually implement a simple app that will showcase
some of the standard problems that arise in app development and tackle them.

# Problems that I want to illustrate
* UI consistency
* How to design a good source of truth
* How to not do full blown Model View Update and have some component level mutable state
* How to test business logic
* How to keep track of view hierarchy and manipulate it

# Problems I don't want to illustrate
* Styling, integrations with 3d party apis

# The app
The sample app is a messenger.

Feature request
* Ability to see all chats with most recent message
* Chats should be sorted in order of most recent message
* Ability to use individual chat - send new messages

Nice to have
* Testability of states

Some ideas for the future
* Forward message to other chats
* Enrich message content
* Send message later if unable to deliver

# Random ideas

* Why when we supply the default parameter to a function we still are forced to
annotate its type? The inference should not be too complicated since we are already
giving it an object of a specific type. Also from the programmer's perspective
this should offload cognitive stress. What is better
  fun display(message: Message = Message.sample())
  or
  fun display(message = Message.sample())
  ? Arguably a good idea

# Challenges of Kotlin Native
Keeping IO and concurrent code shared sounds pretty complicated. Both iOS and Android have
different ways of handling IO and concurrency, some good common ground interface is needed.
Ideally that will allow both platforms to use their best capabilities.
Examples:
Kotlin JVM has good support for coroutines, but Swift doesn't. What about GCD?

# Log

Initial commit
* Played around with Jetpack Compose.
* Made a simple model for a single chat that holds all messages
and currently edited message.
* Created this readme to set some milestones and track progress

Created MessengerState class that holds a list of Chats.
I am already thinking how will I implement the navigation.
Looks like with jepack compose we need this data to be also part of the
composable functions. But that should be separate from the business logic/data.

Ok, so now I added NavigationState to Messenger component.
Well its now a problem since we need to navigate back when done. How are we
going to do that? Evaluating options: a) pass closure, b) pass navigation object to chat
(a) is safe and hides implementation of navigation state
(b) could be done if made compliant to some interface like BackNavigatable { fun back() }
  the only problem is - its the same crap as passing a lambda! Only now we are standardizing
  the objects that handle back navigation, which isn't a bad idea. The info of what the
  anonymous lambda does can depend on the name the developer chooses, if using the interface approach
  no matter what the identifier is called it will still have the type that sufficiently
  standardizes the capabilities.

Added navigation using a lambda for now to not make things more complicated.

Problem that didn't happen, but might be a good idea for it to happen.
* When switching to displaying a chat with a given ID the ChatState class
  didn't get copied, instead just a reference to it in the original list got passed.
  So when new messages were appended to the chat they persisted.
* Problem with this. The changes to the parent state are implicit - no transaction.

The bigger problem is that right now this is not very testable. The mutations to the
chat only happen when the user clicks send on the TextField. What if we get a message from the server?
A good next step would be to do either of these:
    - Write business logic tests
    - Write a mock server that will be emitting new messages to random chats

Problem I faced - Compose uses an experimental Kotlin compiler
and it doesn't support coroutines well :(
Example Flow<T>.collect function doesn't work (fails at compile time)

# Commit
* Added daemon server mock that
    - Creates new chats
    - Adds a new message to one of the existing chats
* Moved state creation to the Activity level

Next commit goal - make things worse, add sending to mock api logic

Here is a code snippet from when the user clicks send
```
    state.messages = state.messages.plus(state.current)
    sendMessage(state.current)
    state.current = ""
```
I think this just got way too ugly and out of the scope of View acceptable logic.
Achievement complete - make things worse!
Now if we think about adding failure handling of sending the message..
And that would of course be asynchronous. And then we of course if it fails
want to add it to the queue of messages that need to be resent when network
gets connected .. after N unsuccessful sends... that sounds about right.
And also add this to the persistence layer so when the user reopens the app
they see the correct messages. And we also want to add analytics to this event.
We are totally screwed at this point.

Right now we got ourselves a good hackathon base project for managing 2 screens
of a messenger. But they are horrible screens. They barely do anything, are not
testable and so on.

Also fun bug that happened. When running a callback to produce a Deferred to send a message
an exception was thrown - I was accessing a var field of the Chat model (var id: UUID)
and the getter failed. Looks like variable capturing in lambda's doesn't work as safely 
as I expected. To address it I made an immutable copy of the id outside of the 
lambda scope and then used that new value in a coroutine without a problem.

An interesting trick is to try over-engineer the data model on purpose, and then 
trim it. This will allow to think 

-----

A lot of problems arose during this phase. Mostly these two:
* Coroutines default scopes didnt want to work when running in test environment.
  To resolve added the coroutines-test library and used runBlockingTest coroutine builder
  from it that created test coroutine scope reference to which I later passed to
  the State constructor and later used internally when running dispatch.
  The builder also blocked the current thread until the job associated
  with the coroutine scope was complete
* When testing it made sense to pass the reducer to the Store constructor 
  as a lambda - only used locally and to spare type annotations and names.
  The problem was with returning from that lambda. It yelled that a non local
  return was used, when no return was used at all! I suspect some sort of 
  compiler bug. I read some docs about inlining and crossinlining. 
  Tried to put crossinline attribute to the Store constructor, but 
  apparently this is only allowed in function arguments.
  Regular named functions were used as reducers and passed as ::funcName

Commit 
* Added Store, Action, Command concepts, pretty much stolen from Fabulous/Elm
* Tested Store, Action, Command
* Removed old state management code

----

Actual almost committed, but while writing commit message realized I forgot to test
whether the redelivery count can drop below zero. And I caught a bug! 
I had a variable local message that I kept changing and an outer while loop
that was checking whether the delivery attempts counter dropped to zero or not.
The bug: in the while condition I was checking the initial immutable local message
delivery count, that of course, remained constant. So when I set MockApiClient(attemptsBeforeSuccess = 10)
the attempts count on the local message were -7. Maybe a good idea is to actually 
do some validation in the init {} block for data types and throw errors if they were constructed 
with weird values (like in this case Message.Local)

Commit 
* Added Experimental annotations to test classes since using unstable coroutine-test functions
* Refactored AppStoreUnitTests.kt: removed boilerplate code that sets up store
* Added tests for redelivery - passing

----

Next big challenges:
How do we handle navigation. I want to start by using local state in our
Composable functions. Later showcase how bad and not testable it will look like 
when adding say deep links. By no means I want to argue the point that 
navigation state always need be handled in the common state of the app from the start.
All I am saying is that I can see problems when there are too many navigation
transition variations. 

But that sounds like a secondary matter. Here is the real problem:
I have this nice Store that keeps track of the current app state 
as well as updates to that state in response to some actions/events/messages 
whatever you call them. And then I have compose that has a couple of ways
to re-render the view. Now the simplest way is to create a class
with @Model annotation and a var state that will be a reflection of the
same named var in our store. Hell, why not just make our Store
a @Model?

I think this is what I will do: @Model State(var state...)

I will put the state into the activity and inject it into the 
top level @Composable App.
This way every time the store updates its state we will 
re-render the App.

But that will most definitely be inefficient. First 
I need to inspect how bad it really is. For that I will 
probably need to make sure my daemon mock server is sending 
updates to my client (and dispatching those updates).

In any case, I am certain it will not work well. What we need
for this to work well can be the following:

* create even streams for different store changes (ie: redux selectors?)
  and subscribe our views to them. The event values will also need to be
  formatted in a simple way that the view can directly use
  - not clear who will be responsible for this? Probably same code that navigates.
    Yet another problem with local navigation state - will be harder to test
  - a better way would probably be to pass entire state to each @Composable
    and let it create the selector for itself and subscribe - 
    very simple would be if we use something like Recompose.
    This will give us very low level control over this
    
* create another set of @Model annotated classes that will be updated on every 
  state change
  - the problem of this approach is that first of all it will be very similar to 
    the one desribed above, because if not, we will probably end up 
    doing a lot of waisted work in computing this @Model instances say for every 
    chat, when only one chat is active at a time. In other words, this @Model 
    tree is a direct output of a given state, so it doesn't really get 'stored'
    nicely. We will just be adding another level of incremental state management
    to our system :(




