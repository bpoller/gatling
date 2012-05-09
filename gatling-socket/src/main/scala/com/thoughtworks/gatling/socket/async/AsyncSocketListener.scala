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
package com.thoughtworks.gatling.socket.async

import akka.actor.ActorRef
import com.ning.http.client.websocket.{WebSocketTextListener, WebSocket}
import AsyncSocketListener._

object AsyncSocketListener {
  object Messages {
    sealed trait WebSocketClientMessage
    case class Open(client: WebSocket) extends WebSocketClientMessage
    case class Close(client: WebSocket) extends WebSocketClientMessage
    case class Error(t: Throwable) extends WebSocketClientMessage
    case class Message(message: String) extends WebSocketClientMessage
    case class Fragment(fragment: String, last : Boolean) extends WebSocketClientMessage
  }
}

class AsyncSocketListener(var actor : ActorRef) extends WebSocketTextListener {
  def onOpen(websocket: WebSocket) {
    actor ! Messages.Open(websocket)
  }

  def onClose(websocket: WebSocket) {
    actor ! Messages.Close(websocket)
  }

  def onError(t: Throwable) {
    actor ! Messages.Error(t)
  }

  def onMessage(message: String) {
    actor ! Messages.Message(message)
  }

  def onFragment(fragment: String, last: Boolean) {
    actor ! Messages.Fragment(fragment, last)
  }
}