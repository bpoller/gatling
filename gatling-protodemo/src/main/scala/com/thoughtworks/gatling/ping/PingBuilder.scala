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
package com.thoughtworks.gatling.ping

/**
 * This just exposes a factory method used by the Predef stuff.
 */
object PingBuilder {
  /**
   * Start our main DSL chain from here.
   */
  def ping(requestName: String) = new PingBuilder(requestName)
}

/**
 * This class contains specific kinds of pings you can make. It doesn't
 * make a lot of sense in this example, but in the HTTP library this is
 * where the different types of requests you can make are exposed (for
 * example this is where the get, post, and delete methods live).
 */
class PingBuilder(val requestName: String) {
  /**
   * From here we launch our specific DSL. Again, it's a bit contrived in this
   * example.
   *
   * We create an instance of the PingUrlBuilder here, which is our *actual*
   * DSL class. These two methods are purely illustrative, in the http library
   * they return different builders.
   *
   * Defaults are supplied for parameters, which can later be overridden by using
   * methods on the builder instances.
   */
  def url(url: String) = new PingUrlBuilder(requestName, url, 3000)
  def ipAddress(ip: String) = new PingUrlBuilder(requestName, ip, 3000)
}