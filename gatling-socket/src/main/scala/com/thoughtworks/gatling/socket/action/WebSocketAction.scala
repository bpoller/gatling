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
import akka.actor.{Props, ActorRef}
import com.thoughtworks.gatling.socket.config.SocketConfig._
import com.ning.http.client.{AsyncHttpClient, AsyncHttpClientConfig}
import com.ning.http.client.websocket.{WebSocketTextListener, WebSocketListener, WebSocketUpgradeHandler, WebSocket}
import com.thoughtworks.gatling.socket.async.{OurWebSocketListener, AsyncSocketHandlerActor}

object WebSocketAction extends Logging {
  /**
   * The HTTP client used to send the requests
   */
  val SOCKET_CLIENT = {
    // set up Netty LoggerFactory for slf4j instead of default JDK
    try {
      val nettyInternalLoggerFactoryClass = Class.forName("org.jboss.netty.logging.InternalLoggerFactory")
      val nettySlf4JLoggerFactoryInstance = Class.forName("org.jboss.netty.logging.Slf4JLoggerFactory").newInstance
      val setDefaultFactoryMethod = nettyInternalLoggerFactoryClass.getMethod("setDefaultFactory", nettyInternalLoggerFactoryClass)
      setDefaultFactoryMethod.invoke(null, nettySlf4JLoggerFactoryInstance.asInstanceOf[AnyRef])

    } catch {
      case e => logger.info("Netty logger wasn't set up")
    }

    val ahcConfigBuilder = new AsyncHttpClientConfig.Builder()
      .setCompressionEnabled(GATLING_SOCKET_CONFIG_COMPRESSION_ENABLED)
      .setConnectionTimeoutInMs(GATLING_SOCKET_CONFIG_CONNECTION_TIMEOUT)
      .setRequestTimeoutInMs(GATLING_SOCKET_CONFIG_REQUEST_TIMEOUT)
      .setMaxRequestRetry(GATLING_SOCKET_CONFIG_MAX_RETRY)
      .setAllowPoolingConnection(GATLING_SOCKET_CONFIG_ALLOW_POOLING_CONNECTION)
      .build

    val client = new AsyncHttpClient(GATLING_SOCKET_CONFIG_PROVIDER_CLASS, ahcConfigBuilder)

    system.registerOnTermination(client.close)

    client
  }
}

/**
 * Finally, the big guy. This is our WebSocketAction, an Action being the thing which
 * actually *does something*. In our case, it creates a web socket request.
 */
class WebSocketAction(requestName: String, next: ActorRef, requestBuilder: UrlWebSocketBuilder)
  extends Action with Logging {

  def execute(session: Session) {
    info("Sending Request '" + requestName + "': Scenario '" + session.scenarioName + "', UserId #" + session.userId)

    val client = WebSocketAction.SOCKET_CLIENT
    val actor = context.actorOf(Props(new AsyncSocketHandlerActor(requestName, session, next, requestBuilder.messages, requestBuilder.checks)))
    val listener = new OurWebSocketListener(actor)

    client.prepareGet(requestBuilder.url)
      .execute(
        new WebSocketUpgradeHandler.Builder()
          .addWebSocketListener(listener)
          .build()
      )
      .get()
  }
}
