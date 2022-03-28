/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine

case class RedisValueChange[V](oldValue: V, newValue: V)
