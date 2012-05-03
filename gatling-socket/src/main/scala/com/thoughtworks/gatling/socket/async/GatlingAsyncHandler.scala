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
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.HttpResponse
import grizzled.slf4j.Logging
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http.websocketx._
import java.nio.charset.Charset

class GatlingAsyncHandler(val handshaker : WebSocketClientHandshaker, val actor : ActorRef)
  extends SimpleChannelUpstreamHandler with Logging {

  def futureListener(handleWith: ChannelFuture => Unit) = new ChannelFutureListener {
    def operationComplete(future: ChannelFuture) {
      handleWith(future)
    }
  }

  override def channelConnected(ctx : ChannelHandlerContext, e : ChannelStateEvent) {
    System.out.println("connected")
    val message = "Oh hello there"
    e.getChannel.write(new TextWebSocketFrame(ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8))).addListener(futureListener { fut =>
      if (!fut.isSuccess) {
        System.out.println("write failed")
        //handler(WriteFailed(this, message, Option(fut.getCause)))
      }
    })
    //actor ! "connected"
  }

  override def channelDisconnected(ctx : ChannelHandlerContext, e : ChannelStateEvent) {
    System.out.println("disconnected")
    //actor ! "disconnected"
  }

  override def writeComplete(ctx : ChannelHandlerContext, e : WriteCompletionEvent) {
    System.out.println("write complete")
    //actor ! "write-complete"
  }

  override def messageReceived(ctx : ChannelHandlerContext, e : MessageEvent) {
    System.out.println("message received")

    e.getMessage match {
      case resp: HttpResponse if handshaker.isHandshakeComplete =>
        throw new Exception("Unexpected HttpResponse (status=" + resp.getStatus + ", content="
          + resp.getContent.toString(CharsetUtil.UTF_8) + ")")
      case resp: HttpResponse =>
        handshaker.finishHandshake(ctx.getChannel, e.getMessage.asInstanceOf[HttpResponse])
        //client.handler(Connected(client))

      case f: TextWebSocketFrame => System.out.println("Msg: " + f.getText)//client.handler(TextMessage(client, f.getText))
      case _: PongWebSocketFrame => System.out.println("Pong")
      case _: CloseWebSocketFrame => {
        System.out.println("Close")
        ctx.getChannel.close()
      }
    }
//
//    val ch = ctx.getChannel;
//    if (!handshaker.isHandshakeComplete) {
//      handshaker.finishHandshake(ch, e.getMessage.asInstanceOf[HttpResponse])
//      logger.debug("WebSocket Client connected!")
//      return;
//    }
//
//    if (e.getMessage.isInstanceOf[HttpResponse]) {
//      val response = e.getMessage.asInstanceOf[HttpResponse]
//      throw new Exception("Unexpected HttpResponse (status=" + response.getStatus + ", content="
//        + response.getContent.toString(CharsetUtil.UTF_8) + ")")
//    }
//
//    val frame = e.getMessage.asInstanceOf[WebSocketFrame]
//    if (frame.isInstanceOf[TextWebSocketFrame]) {
//      val textFrame = frame.asInstanceOf[TextWebSocketFrame];
//      logger.info("WebSocket Client received message: " + textFrame.getText);
//    } else if (frame.isInstanceOf[PongWebSocketFrame]) {
//      logger.info("WebSocket Client received pong");
//    } else if (frame.isInstanceOf[CloseWebSocketFrame]) {
//      logger.info("WebSocket Client received closing");
//      ch.close();
//    }
  }
}