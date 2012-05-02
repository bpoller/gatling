package com.thoughtworks.gatling.ping

import com.thoughtworks.gatling.action.PingActionBuilder
import com.pinger.Pinger

/**
 * This is where all the customising methods live for the builder. They all
 * work by creating a new instance of the builder class, passing through
 * all the existing values and overriding ones with the parameters from
 * the customise methods.
 *
 * Following convention again, in addition to whatever public DSL methods
 * are needed this class exposes a build method which builds up the actual
 * instance that will perform the request (in our case the Pinger instance,
 * http it would be the 3rd party Request object).
 *
 * Finally, and the reason for this eludes me, there's an implicit conversion
 * between this builder and the PingActionBuilder. In the case of the http protocol
 * there's a conversion between a GetHttpRequestBuilder and an HttpRequestActionBuilder.
 */
class PingUrlBuilder(val requestName : String, val url : String, val timeout : Int) {
  /**
   * This is an example of a DSL-only method. Create ones like this for altering
   * the parameters of your request.
   *
   * The approach of creating a new instance seems a little strange to me, and I don't
   * think it'd scale very well, but that's just like my opinion man.
   */
  def timeout(timeout : Int) = new PingUrlBuilder(requestName, url, timeout)

  /**
   * This guy handles building the instance using all the parameters we've passed in
   * (Pinger in our case, but more likely something from a 3rd party library)
   */
  private[gatling] def build = new Pinger(url, timeout)

  /**
   * This converts from this builder to a PingActionBuilder. Not sure why this is
   * necessary, but it simply creates a new PingActionBuilder with the current
   * builder as an argument.
   */
  private[gatling] def toActionBuilder = new PingActionBuilder(requestName, this, null)
}

/**
 * Just setup the implicit conversion here. This is used by the exec method of Gatling
 * to convert our PingUrlBuilder into a PingActionBuilder.
 */
object PingUrlBuilder {
  implicit def toActionBuilder(requestBuilder: PingUrlBuilder) = requestBuilder.toActionBuilder
}