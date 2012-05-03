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

import com.excilys.ebi.gatling.core.session.Session
import grizzled.slf4j.Logging
import com.thoughtworks.gatling.socket.request.UrlWebSocketBuilder
import com.excilys.ebi.gatling.core.action._
import java.net.URI
import akka.actor.{Props, ActorRef}
import com.thoughtworks.gatling.socket.async.GatlingAsyncHandlerActor
import com.thoughtworks.socket.WebSocketClient

object WebSocketAction extends Logging {
  def socketClient(host : String, handler : ActorRef) = {
    WebSocketClient(new URI("ws://"+host+":80")) {
      case msg => handler ! msg
    }
  }
}

/**
 * Finally, the big guy. This is our WebSocketAction, an Action being the thing which
 * actually *does something*. In our case, it creates a web socket request.
 */
class WebSocketAction(requestName: String, next: ActorRef, requestBuilder: UrlWebSocketBuilder)
  extends Action with Logging {

  def execute(session: Session) {
    val actor = context.actorOf(Props(new GatlingAsyncHandlerActor(requestName, session, next, requestBuilder.messages, requestBuilder.checks)))
    val client = WebSocketAction.socketClient(requestBuilder.url, actor)
    client.connect
  }
}
