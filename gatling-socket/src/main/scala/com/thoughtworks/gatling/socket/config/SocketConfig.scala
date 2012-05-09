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
package com.thoughtworks.gatling.socket.config

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration

object SocketConfig {
  val GATLING_SOCKET_CONFIG_PROVIDER_CLASS = {
    val selectedProvider = configuration.fileConfiguration.getString("gatling.socket.provider", "Netty")
    new StringBuilder("com.ning.http.client.providers.").append(selectedProvider.toLowerCase).append(".").append(selectedProvider).append("AsyncHttpProvider").toString
  }
  val GATLING_SOCKET_CONFIG_CONNECTION_TIMEOUT = configuration.fileConfiguration.getInt("gatling.socket.connectionTimeout", 60000)
  val GATLING_SOCKET_CONFIG_COMPRESSION_ENABLED = configuration.fileConfiguration.getBoolean("gatling.socket.compressionEnabled", true)
  val GATLING_SOCKET_CONFIG_REQUEST_TIMEOUT = configuration.fileConfiguration.getInt("gatling.socket.requestTimeout", 60000)
  val GATLING_SOCKET_CONFIG_MAX_RETRY = configuration.fileConfiguration.getInt("gatling.socket.maxRetry", 5)
  val GATLING_SOCKET_CONFIG_ALLOW_POOLING_CONNECTION = configuration.fileConfiguration.getBoolean("gatling.socket.allowPoolingConnection", true)
}