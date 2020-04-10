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

Next commit goal - make things worse, add loading states




