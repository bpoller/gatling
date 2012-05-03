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
package com.thoughtworks.socket

import akka.actor.ActorRef
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http.websocketx._
import grizzled.slf4j.Logging
import org.jboss.netty.handler.codec.http.HttpHeaders.{Values, Names}
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.base64.Base64
import org.jboss.netty.bootstrap.ClientBootstrap
import java.util.concurrent.Executors
import java.net.{InetSocketAddress, URI}
import java.security.MessageDigest
import java.nio.charset.Charset
import org.jboss.netty.channel._
import socket.nio.NioClientSocketChannelFactory
import java.util.HashMap
import scala.collection.JavaConversions._

trait WebSocketClient {
  def url: URI
  def reader: WebSocketClient.FrameReader
  def handler: WebSocketClient.Handler
  def connect
  def disconnect
  def send(message: String, charset: Charset = CharsetUtil.UTF_8)
}

object WebSocketClient {

  object Messages {
    sealed trait WebSocketClientMessage
    case object Connecting extends WebSocketClientMessage
    case class ConnectionFailed(client: WebSocketClient, reason: Option[Throwable] = None) extends WebSocketClientMessage
    case class Connected(client: WebSocketClient) extends WebSocketClientMessage
    case class TextMessage(client: WebSocketClient, text: String) extends WebSocketClientMessage
    case class WriteFailed(client: WebSocketClient, message: String, reason: Option[Throwable]) extends WebSocketClientMessage
    case object Disconnecting extends WebSocketClientMessage
    case class Disconnected(client: WebSocketClient, reason: Option[Throwable] = None) extends WebSocketClientMessage
    case class Error(client: WebSocketClient, th: Throwable) extends WebSocketClientMessage
  }

  type Handler = PartialFunction[Messages.WebSocketClientMessage, Unit]
  type FrameReader = WebSocketFrame => String

  val defaultFrameReader = (_: WebSocketFrame) match {
    case f: TextWebSocketFrame => f.getText
    case _ => throw new UnsupportedOperationException("Only single text frames are supported for now")
  }

  def apply(url: URI, version: WebSocketVersion = WebSocketVersion.V13, reader: FrameReader = defaultFrameReader)(handle: Handler): WebSocketClient = {
    require(url.getScheme.startsWith("ws"), "The scheme of the url should be 'ws' or 'wss'")
    new DefaultWebSocketClient(url, version, handle, reader)
  }

  def apply(url: URI, handle: ActorRef): WebSocketClient = {
    require(url.getScheme.startsWith("ws"), "The scheme of the url should be 'ws' or 'wss'")
    WebSocketClient(url) {
      case x => handle ! x
    }
  }

  private class WebSocketClientHandler(handshaker: WebSocketClientHandshaker, client: WebSocketClient) extends SimpleChannelUpstreamHandler {
    import Messages._

    override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      client.handler(Disconnected(client))
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      e.getMessage match {
        case resp: HttpResponse if handshaker.isHandshakeComplete =>
          throw new WebSocketException("Unexpected HttpResponse (status=" + resp.getStatus + ", content="
            + resp.getContent.toString(CharsetUtil.UTF_8) + ")")
        case resp: HttpResponse =>
          handshaker.finishHandshake(ctx.getChannel, e.getMessage.asInstanceOf[HttpResponse])
          client.handler(Connected(client))

        case f: TextWebSocketFrame => client.handler(TextMessage(client, f.getText))
        case _: PongWebSocketFrame =>
        case _: CloseWebSocketFrame => ctx.getChannel.close()
      }
    }


    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      client.handler(Error(client, e.getCause))
      e.getChannel.close()
    }

  }

  // The Handshaker in the current version of Netty incorrectly checks the handshake
  // messages case sensitively, when according to the spec they should be insensitive.
  // echo.websockets.org responds with a WebSocket upgrade response which causes Netty
  // to baulk. This handshaker fixes that in a rather hacky way, until a new version
  // of Netty is made available.
  private class BugFixedHandshaker(webSocketURL: URI, version: WebSocketVersion, subprotocol: String, allowExtensions: Boolean, customHeaders: java.util.Map[String, String])
    extends WebSocketClientHandshaker13(webSocketURL, version, subprotocol, allowExtensions, customHeaders)
    with Logging {

    private val MAGIC_GUID: String = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    private var expectedChallengeResponseString: String = _

    override def handshake(channel: Channel) = {
      // Get path
      val wsURL = getWebSocketUrl;
      var path = wsURL.getPath;
      if (wsURL.getQuery != null && wsURL.getQuery.length() > 0) {
        path = wsURL.getPath + "?" + wsURL.getQuery;
      }

      // Get 16 bit nonce and base 64 encode it
      val nonce = WebSocketUtil.randomBytes(16);
      val key = WebSocketUtil.base64(nonce);

      val acceptSeed = key + MAGIC_GUID;
      val sha1 = WebSocketUtil.sha1(acceptSeed.getBytes(CharsetUtil.US_ASCII.name()));
      expectedChallengeResponseString = WebSocketUtil.base64(sha1);

      if (logger.isDebugEnabled) {
        logger.debug(String.format("WS Version 13 Client Handshake key: %s. Expected response: %s.", key,
          expectedChallengeResponseString));
      }

      // Format request
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
      request.addHeader(Names.UPGRADE, Values.WEBSOCKET.toLowerCase);
      request.addHeader(Names.CONNECTION, Values.UPGRADE);
      request.addHeader(Names.SEC_WEBSOCKET_KEY, key);
      request.addHeader(Names.HOST, wsURL.getHost);

      val wsPort = wsURL.getPort;
      var originValue = "http://" + wsURL.getHost;
      if (wsPort != 80 && wsPort != 443) {
        // if the port is not standard (80/443) its needed to add the port to the header.
        // See http://tools.ietf.org/html/rfc6454#section-6.2
        originValue = originValue + ":" + wsPort;
      }
      request.addHeader(Names.ORIGIN, originValue);

      //      if (protocol != null && !protocol.equals("")) {
      //        request.addHeader(Names.SEC_WEBSOCKET_PROTOCOL, protocol);
      //      }
      request.addHeader(Names.SEC_WEBSOCKET_VERSION, "13");

      if (customHeaders != null) {
        for (val header <- customHeaders.keySet) {
          request.addHeader(header, customHeaders.get(header));
        }
      }

      val future = channel.write(request);

      channel.getPipeline.replace(classOf[HttpRequestEncoder], "ws-encoder", new WebSocket13FrameEncoder(true));

      future
    }

    override def finishHandshake(channel: Channel, response: HttpResponse) = {
      val status = HttpResponseStatus.SWITCHING_PROTOCOLS

      if (!response.getStatus.equals(status)) {
        throw new WebSocketHandshakeException("Invalid handshake response status: " + response.getStatus)
      }

      val upgrade = response.getHeader(Names.UPGRADE)
      // Upgrade header should be matched case-insensitive.
      // See https://github.com/netty/netty/issues/278
      if (upgrade == null || !upgrade.toLowerCase.equals(Values.WEBSOCKET.toLowerCase)) {
        throw new WebSocketHandshakeException("Invalid handshake response upgrade: "+response.getHeader(Names.UPGRADE))
      }

      // Connection header should be matched case-insensitive.
      // See https://github.com/netty/netty/issues/278
      val connection = response.getHeader(Names.CONNECTION)
      if (connection == null || !connection.toLowerCase.equals(Values.UPGRADE.toLowerCase)) {
        throw new WebSocketHandshakeException("Invalid handshake response connection: "+response.getHeader(Names.CONNECTION))
      }

      val accept = response.getHeader(Names.SEC_WEBSOCKET_ACCEPT)
      if (accept == null || !accept.equals(expectedChallengeResponseString)) {
        throw new WebSocketHandshakeException(String.format("Invalid challenge. Actual: %s. Expected: %s", accept, expectedChallengeResponseString))
      }

      channel.getPipeline.replace(classOf[HttpResponseDecoder], "ws-decoder", new WebSocket13FrameDecoder(false, allowExtensions))

      setHandshakeComplete();
    }
  }

  // See above
  private object WebSocketUtil {

    /**
     * Performs an MD5 hash
     *
     * @param bytes
     * Data to hash
     * @return Hashed data
     */
    def md5(bytes: Array[Byte]) = {
      val md = MessageDigest.getInstance("MD5");
      md.digest(bytes);
    }

    /**
     * Performs an SHA-1 hash
     *
     * @param bytes
     * Data to hash
     * @return Hashed data
     */
    def sha1(bytes: Array[Byte]) = {
      val md = MessageDigest.getInstance("SHA1")
      md.digest(bytes)
    }

    /**
     * Base 64 encoding
     *
     * @param bytes
     * Bytes to encode
     * @return encoded string
     */
    def base64(bytes: Array[Byte]) = {
      val hashed = ChannelBuffers.wrappedBuffer(bytes)
      Base64.encode(hashed).toString(CharsetUtil.UTF_8)
    }

    /**
     * Creates some random bytes
     *
     * @param size
     * Number of random bytes to create
     * @return random bytes
     */
    def randomBytes(size: Int) = {
      val bytes = new Array[Byte](size)

      for (val i <- 0 until size) {
        bytes(i) = randomNumber(0, 255).asInstanceOf[Byte]
      }

      bytes
    }

    /**
     * Generates a random number
     *
     * @param min
     * Minimum value
     * @param max
     * Maximum value
     * @return Random number
     */
    def randomNumber(min: Int, max: Int): Int = (scala.math.random * max + min).asInstanceOf[Int]
  }

  private class DefaultWebSocketClient(
                                        val url: URI,
                                        version: WebSocketVersion,
                                        private[this] val _handler: Handler,
                                        val reader: FrameReader = defaultFrameReader) extends WebSocketClient {
    val normalized = url.normalize()
    val tgt = if (normalized.getPath == null || normalized.getPath.trim().isEmpty) {
      new URI(normalized.getScheme, normalized.getAuthority, "/", normalized.getQuery, normalized.getFragment)
    } else normalized

    val bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool, Executors.newCachedThreadPool))
    val handshaker = new BugFixedHandshaker(tgt, version, null, false, new HashMap[String, String])
    val self = this
    var channel: Channel = _

    import Messages._

    val handler = _handler orElse defaultHandler

    private def defaultHandler: Handler = {
      case Error(_, ex) => ex.printStackTrace()
      case f: WebSocketClientMessage => System.out.println("OUT: " + f)
    }


    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      def getPipeline = {
        val pipeline = Channels.pipeline()
        if (version == WebSocketVersion.V00)
          pipeline.addLast("decoder", new WebSocketHttpResponseDecoder)
        else
          pipeline.addLast("decoder", new HttpResponseDecoder)

        pipeline.addLast("encoder", new HttpRequestEncoder)
        pipeline.addLast("ws-handler", new WebSocketClientHandler(handshaker, self))
        pipeline
      }
    })

    import WebSocketClient.Messages._

    def connect = {
      if (channel == null || !channel.isConnected) {
        val listener = futureListener {
          future =>
            if (future.isSuccess) {
              synchronized {
                channel = future.getChannel
              }
              handshaker.handshake(channel)
            } else {
              handler(ConnectionFailed(this, Option(future.getCause)))
            }
        }
        handler(Connecting)
        val fut = bootstrap.connect(new InetSocketAddress(url.getHost, url.getPort))
        fut.addListener(listener)
        fut.await(5000L)
      }
    }

    def disconnect = {
      if (channel != null && channel.isConnected) {
        handler(Disconnecting)
        channel.write(new CloseWebSocketFrame())
      }
    }

    def send(message: String, charset: Charset = CharsetUtil.UTF_8) = {
      channel.write(new TextWebSocketFrame(ChannelBuffers.copiedBuffer(message, charset))).addListener(futureListener {
        fut =>
          if (!fut.isSuccess) {
            handler(WriteFailed(this, message, Option(fut.getCause)))
          }
      })
    }

    def futureListener(handleWith: ChannelFuture => Unit) = new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        handleWith(future)
      }
    }
  }

  /**
   * Fix bug in standard HttpResponseDecoder for web socket clients. When status 101 is received for Hybi00, there are 16
   * bytes of contents expected
   */
  class WebSocketHttpResponseDecoder extends HttpResponseDecoder {

    val codes = List(101, 200, 204, 205, 304)

    protected override def isContentAlwaysEmpty(msg: HttpMessage) = {
      msg match {
        case res: HttpResponse => codes contains res.getStatus.getCode
        case _ => false
      }
    }
  }

  /**
   * A WebSocket related exception
   *
   * Copied from https://github.com/cgbystrom/netty-tools
   */
  class WebSocketException(s: String, th: Throwable) extends java.io.IOException(s, th) {
    def this(s: String) = this(s, null)
  }

}