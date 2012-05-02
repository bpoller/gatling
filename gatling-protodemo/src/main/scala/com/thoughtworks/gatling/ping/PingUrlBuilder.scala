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

import com.thoughtworks.gatling.action.PingActionBuilder
import com.pinger.Pinger

object PingUrlBuilder {
  implicit def toActionBuilder(requestBuilder: PingUrlBuilder) = requestBuilder.toActionBuilder
}

class PingUrlBuilder(val requestName : String, val url : String, val timeout : Int) {
  private[gatling] def build = new Pinger(url, timeout)

  def timeout(timeout : Int) = new PingUrlBuilder(requestName, url, timeout)

  private[gatling] def toActionBuilder = new PingActionBuilder(requestName, this, null)
}
