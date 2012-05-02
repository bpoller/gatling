/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

class PingAction(requestName: String, next: ActorRef, requestBuilder: PingUrlBuilder)
  extends Action with Logging {

  def execute(session: Session) {
    System.out.println("I'm pinging...")
    val requestStartDate = currentTimeMillis()
    val pinger = requestBuilder.build

    pinger.ping

    val responseEndDate = currentTimeMillis()
    val endOfRequestSendingDate = currentTimeMillis()
    val requestResult = RequestStatus.OK
    val requestMessage = "it's all good"

    DataWriter.logRequest(session.scenarioName, session.userId, "Request " + requestName, requestStartDate, responseEndDate, endOfRequestSendingDate, endOfRequestSendingDate, requestResult, requestMessage)
    next ! session
  }
}
