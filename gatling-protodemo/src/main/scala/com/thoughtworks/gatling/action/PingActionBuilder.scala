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

import com.excilys.ebi.gatling.core.action.builder.ActionBuilder
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.action._
import akka.actor.{Props, ActorRef}
import com.thoughtworks.gatling.ping.PingUrlBuilder

class PingActionBuilder(requestName: String, requestBuilder: PingUrlBuilder, next: ActorRef) extends ActionBuilder {

  private[com] def withNext(next: ActorRef) = new PingActionBuilder(requestName, requestBuilder, next)

//  private[gatling] lazy val resolvedChecks = checks.find(_.phase == StatusReceived) match {
//    case None => HttpRequestActionBuilder.DEFAULT_HTTP_STATUS_CHECK :: checks
//    case _ => checks
//  }

  private[com] def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {
    system.actorOf(Props(new PingAction(requestName, next, requestBuilder)))
  }
}
