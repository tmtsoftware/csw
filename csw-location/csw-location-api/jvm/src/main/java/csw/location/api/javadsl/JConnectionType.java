/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.javadsl;


import csw.location.api.models.ConnectionType;

/**
 * Helper class for Java to get the handle of `ConnectionType` which is fundamental to LocationService library
 */
public interface JConnectionType {
    /**
     * Used to define an Akka connection
     */
    ConnectionType AkkaType = ConnectionType.AkkaType$.MODULE$;

    /**
     * Used to define a TCP connection
     */
    ConnectionType TcpType = ConnectionType.TcpType$.MODULE$;

    /**
     * Used to define a HTTP connection
     */
    ConnectionType HttpType = ConnectionType.HttpType$.MODULE$;
}