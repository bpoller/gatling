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
import com.ning.http.client.websocket.{WebSocketUpgradeHandler, WebSocket}
import com.thoughtworks.gatling.socket.async.{AsyncSocketListener, AsyncSocketHandlerActor}
import akka.util.duration._
import com.thoughtworks.gatling.socket.action.WebSocketAction.WebSocketBundle

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

  class WebSocketBundle(val websocket : WebSocket, val listener : AsyncSocketListener) {
    def assignNewActor(actor : ActorRef) {
      listener.actor = actor

      if (websocket.isOpen) {
        // not really sure about this - we've got a connection that's already open, but
        // the actor sends its messages on connection open. Right now, tell the listener
        // the connection has just opened despite it having technically not.
        listener.onOpen(websocket)
      }
    }
  }
}

/**
 * Finally, the big guy. This is our WebSocketAction, an Action being the thing which
 * actually *does something*. In our case, it creates a web socket request.
 */
class WebSocketAction(requestName: String, next: ActorRef, requestBuilder: UrlWebSocketBuilder)
  extends Action with Logging {

  def getOrOpenConnectionForUrl(url: String, session: Session) : WebSocketBundle = {
    try {
      val connection = session.getAttribute("websocket-connection:"+url).asInstanceOf[WebSocketBundle]
      System.out.println("REUSING CONNECTION")
      connection
    } catch {
      case ex : IllegalArgumentException => {
        System.out.println("NEW CONNECTION")
        val client = WebSocketAction.SOCKET_CLIENT
        val listener = new AsyncSocketListener(null)
        val bundle = new WebSocketBundle(
          client
            .prepareGet(requestBuilder.url)
            .execute(new WebSocketUpgradeHandler.Builder().build())
            .get(),
          listener)

        System.out.println("I got past here")
        System.out.println("I got past here 2")
        bundle
      }
    }
  }

  def execute(session: Session) {
    info("Sending Request '" + requestName + "': Scenario '" + session.scenarioName + "', UserId #" + session.userId)

    // context:
    // * we're trying to keep a connection open to a socket for the whole lifecycle of a user.
    // * a connection has a listener, and that listener delegates actions to a socket handler actor
    // * the actor has references to the messages it should send, messages it should receive, and the checks it should perform
    // * when we hit a new exec step, we want to make sure only the messages from that exec are sent, not ones previously used
    // So... We're trying to replace the actor reference with a new one in the listener.
    val bundle = getOrOpenConnectionForUrl(requestBuilder.url, session)
    val actor = system.actorOf(Props(new AsyncSocketHandlerActor(requestName, session, next, requestBuilder, bundle, 10 seconds)))
    bundle.assignNewActor(actor)
  }
}
