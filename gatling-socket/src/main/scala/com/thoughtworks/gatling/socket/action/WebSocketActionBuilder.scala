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
package com.thoughtworks.gatling.socket.action

import com.excilys.ebi.gatling.core.action.builder.ActionBuilder
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.action._
import akka.actor.{Props, ActorRef}
import com.thoughtworks.gatling.socket.request.UrlWebSocketBuilder

/**
 * This creates a WebSocketAction from our UrlWebSocketBuilder. It does something with the Actor
 * framework stuff, but I'm not familiar with all that jazz so I can't really shine
 * much light on that yet.
 */
class WebSocketActionBuilder(requestName: String, requestBuilder: UrlWebSocketBuilder, next: ActorRef) extends ActionBuilder {
  /**
   * Something todo with the action chain. Creates a new instance of our builder with a new
   * next action point.
   */
  private[com] def withNext(next: ActorRef) = new WebSocketActionBuilder(requestName, requestBuilder, next)

  /**
   * This actually creates our WebSocketAction, and wires it up with the Actor stuff.
   */
  private[com] def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {
    system.actorOf(Props(new WebSocketAction(requestName, next, requestBuilder)))
  }
}
