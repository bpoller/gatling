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
import java.lang.System._
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.result.message.RequestStatus
import com.thoughtworks.gatling.socket.request.UrlWebSocketBuilder
import com.excilys.ebi.gatling.core.action._
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.Executors
import akka.actor.{Props, ActorRef}
import com.thoughtworks.gatling.socket.async.{GatlingAsyncHandlerActor, GatlingAsyncHandler}
import java.net.{URI, InetSocketAddress}
import java.util.HashMap
import org.jboss.netty.handler.codec.http.{HttpResponseDecoder, HttpRequestEncoder}
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.websocketx._
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import java.nio.charset.Charset
import client.WebSocketClient
import client.WebSocketClient.Messages._

//object WebSocketAction extends Logging {
//  def socketClient(host : String) = {
//
//  }
//}

/**
 * Finally, the big guy. This is our WebSocketAction, an Action being the thing which
 * actually *does something*. In our case, it creates a web socket request.
 */
class WebSocketAction(requestName: String, next: ActorRef, requestBuilder: UrlWebSocketBuilder)
  extends Action with Logging {

  def execute(session: Session) {
    val c = WebSocketClient(new URI("ws://echo.websocket.org:80")) {
      case Connecting => System.out.println("Connecting")
      case ConnectionFailed(client,reason) => System.out.println("Connection failed " + reason)
      case Connected(client) => {
        System.out.println("Connection has been established to: " + client.url.toASCIIString)
        client.send("Hello?")
      }
      case TextMessage(client, message) => {
        System.out.println("RECV: " + message)
        client.disconnect
      }
      case WriteFailed(client, message, reason) => System.out.println("Write failed, "+message+", "+reason)
      case Disconnecting => System.out.println("Disconnecting")
      case Disconnected(client, _) => System.out.println("The websocket to " + client.url.toASCIIString + " disconnected.")
      case Error(client, th) => System.out.println("ERROR: "+th)
    }
    c.connect
  }
}
