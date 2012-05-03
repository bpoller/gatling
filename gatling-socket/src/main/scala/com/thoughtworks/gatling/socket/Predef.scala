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
package com.thoughtworks.gatling.socket

import request.WebSocketBuilder

object Predef {
  /**
   * This method kicks off our DSL for a ping.
   *
   * By convention this method just delegates to a factory method on the main
   * Builder class. *shrug*
   *
   * @param requestName Your human readable description of this request.
   * @return A PingBuilder instance, which is used to pick a specific kind of ping.
   */
  def socket(requestName: String) = WebSocketBuilder.socket(requestName)
}