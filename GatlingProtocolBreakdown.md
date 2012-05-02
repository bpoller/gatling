# Doing stuff

There's actually very little to the guts of a Gatling protocol, most of the code is there to support the DSL.

The only exception is the  `ProtocolConfigurationRegistry`, which I haven't explored yet.

## com.pinger.Pinger
Ignore this. This represents your 3rd party library for doing whatever it is you're doing. Substitute your library of choice here.

## action.PingAction
This is the meat of a Gatling pipeline.

An `Action` handles actually *doing stuff*, namely executing your request, then recording the stats of that request.

In the `execute` method you call our to your 3rd party library, then ensure you call the following lines:

    DataWriter.logRequest(session.scenarioName, session.userId, "Request " + requestName, requestStartDate, responseEndDate, endOfRequestSendingDate, endOfRequestSendingDate, requestResult, requestMessage)

This bit actually records the request; without this call you won't get any data in the output. Customise the parameters to your heart's content.

    next ! session

This executes the next step in the action chain; without this call your pipeline will just hang indefinitely. Some of the other actions do some funky stuff with this to simulate conditionals etc...

That's it, that is a really basic action. Everything else from here on is magic for the DSL.

# DSL specific stuff

There's a lot of it here. Some of it seems contrived and a bit over-engineered, but who am I to judge? Bare with me here...

## ping.Predef
The convention in Gatling seems to be to expose all the DSL starting/launching points from a class called `Predef`; the methods from these classes are statically imported to give us the top-level DSL methods (`http` primarily, and our `ping`).

Each method seems to just take a description as their only parameter, and then delegate to a factory method on a Builder class. In our case, we have a `ping` method which returns a `PingBuilder` instance.

## ping.PingBuilder
> This class feels unnecessary in this example, because we only have one kind of builder, but I've stuck to the convention anyway.

This builder represents the highest level of our DSL. It's easier to think of it in terms of the http library. This class contains the equivalent of the `get`, `post`, and `delete` methods which launch their own sub/mini DSL.

The methods in this class return a more specific builder, in our case the `PingUrlBuilder`, and in the http protocol the `GetHttpRequestBuilder` or the `PostHttpRequestBuilder` etc...

## ping.PingUrlBuilder
This is where all the customising methods live for the builder. Following convention again, in addition to whatever public DSL methods are needed this class exposes a `build` method which builds up the actual instance that will perform the request (in our case the `Pinger` instance, http it would be the 3rd party `Request` object).

This is where it starts to get a bit hairy in the DSL. The Gatling `exec` method takes a `ActionBuilder`, while we're currently using a `PingUrlBuilder`; for some reason to remedy this there's an implicit conversion done between the builder and an `ActionBuilder`. In our case we convert from `PingUrlBuilder` to a `PingActionBuilder`.

## ping.action.PingActionBuilder
Finally, this is what creates the `PingAction` (the bit that does the actual work). It takes our `PingUrlBuilder` and creates a `PingAction` with it using the Actor stuff from Scala, I'm not familiar with that stuff so I'll leave this as an exercise for the reader.

This class also exposes a `withNext` method, which is something about the action chaining. It just creates a new instance of the builder with the same values but a different `next` step reference.