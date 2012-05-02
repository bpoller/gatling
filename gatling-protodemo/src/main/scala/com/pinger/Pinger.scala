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
package com.pinger

import java.net.InetAddress

/**
 * Don't worry too much about me. I represent a class in a 3rd party library.
 * This is equivalent to the ning.Request instance used by the Http protocol.
 * @param url
 * @param timeout
 */
class Pinger(val url : String, val timeout : Int) {
  def ping = InetAddress.getByName(url).isReachable(timeout)
}