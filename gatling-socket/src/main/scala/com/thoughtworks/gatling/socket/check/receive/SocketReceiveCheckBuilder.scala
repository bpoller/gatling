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
package com.thoughtworks.gatling.socket.check.receive

import com.excilys.ebi.gatling.core.check.ExtractorFactory
import com.excilys.ebi.gatling.core.check.MatcherCheckBuilder
import com.thoughtworks.gatling.socket.check.{SocketCheck, SocketExtractorCheckBuilder}
import com.excilys.ebi.gatling.core.util.StringHelper._

object SocketReceiveCheckBuilder {
  def receive = new SocketReceiveCheckBuilder

  private def findExtractorFactory: ExtractorFactory[String, String] = (response: String) => (expression: String) => Some(response)
}

/**
 * This class builds a response status check
 */
class SocketReceiveCheckBuilder extends SocketExtractorCheckBuilder[String](Session => EMPTY) {
  def find = new MatcherCheckBuilder[SocketCheck, String, String](socketCheckBuilderFactory, SocketReceiveCheckBuilder.findExtractorFactory)
}