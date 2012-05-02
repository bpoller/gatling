package com.thoughtworks.gatling.action

import com.thoughtworks.gatling.ping.PingUrlBuilder
import com.excilys.ebi.gatling.core.session.Session
import akka.actor.ActorRef
import com.excilys.ebi.gatling.core.action.Action
import grizzled.slf4j.Logging
import java.lang.System._
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.result.message.RequestStatus
import java.net.InetAddress

/**
 * Finally, the big guy. This is our PingAction, an Action being the thing which
 * actually *does something*. In our case, it executes the ping.
 */
class PingAction(requestName: String, next: ActorRef, requestBuilder: PingUrlBuilder)
  extends Action with Logging {

  /**
   * Execute our ping request, or whatever request you're actually doing. This
   * uses the PingUrlBuilder to build the 3rd party Pinger instance; then
   * it executes the ping and records the result.
   */
  def execute(session: Session) {
    val pinger = requestBuilder.build           // build our 3rd party instance
    val requestStartDate = currentTimeMillis()

    System.out.println("I'm pinging "+pinger.url+" with timeout "+pinger.timeout)

    val result = pinger.ping                    // execute the ping

    val responseEndDate = currentTimeMillis()
    val endOfRequestSendingDate = currentTimeMillis()
    val requestResult = if (result) {
      RequestStatus.OK
    } else {
      RequestStatus.KO
    }
    val requestMessage = if (result) {
      "Success"
    } else {
      "Fail"
    }

    // This is an important line. This actually records the request and it's result. Without this call
    // you won't see any data. Customise the parameters to your heart's content.
    DataWriter.logRequest(session.scenarioName, session.userId, "Request " + requestName, requestStartDate, responseEndDate, endOfRequestSendingDate, endOfRequestSendingDate, requestResult, requestMessage)

    // This is also an important line. This passes the focus onto the next action in the chain.
    // Without this line your pipeline will just hang indefinitely.
    next ! session
  }
}
