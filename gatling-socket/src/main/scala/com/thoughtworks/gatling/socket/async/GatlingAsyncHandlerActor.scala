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

import com.excilys.ebi.gatling.core.session.Session
import java.lang.System._
import akka.actor.{ActorRef, Actor}

class GatlingAsyncHandlerActor(val session : Session, val next : ActorRef) extends Actor {
  def receive = {
    case "disconnected" => {
      System.out.println("Socket Disconnected")
      executeNext(session)
    }
    case m => {
      System.out.println("oh crap I received something: "+ m)
    }
  }

  private def executeNext(newSession: Session) {
    next ! newSession.setAttribute(Session.LAST_ACTION_DURATION_KEY, currentTimeMillis) // - responseEndDate)
    context.stop(self)
  }
}