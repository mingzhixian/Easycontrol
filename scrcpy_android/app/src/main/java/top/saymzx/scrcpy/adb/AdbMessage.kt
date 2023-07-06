/*
 * Copyright (c) 2021 mobile.dev inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package top.saymzx.scrcpy.adb

import okio.Buffer
import okio.BufferedSource
import java.nio.charset.StandardCharsets

internal class AdbMessage(
  val command: Int,
  val arg0: Int,
  val arg1: Int,
  val payloadLength: Int,
  val payload: ByteArray
)
