package csw.services.location.javadsl;

import csw.param.models.location.ConnectionType;

/**
 * Java API for connection type. It encapsulates `ConnectionType` which is fundamental to LocationService library.
 */
@SuppressWarnings("unused")
public class JConnectionType {

    /**
     * Used to define an Akka connection
     */
    public static final ConnectionType AkkaType = ConnectionType.AkkaType$.MODULE$;

    /**
     * Used to define a TCP connection
     */
    public static final ConnectionType TcpType = ConnectionType.TcpType$.MODULE$;

    /**
     * Used to define a HTTP connection
     */
    public static final ConnectionType HttpType = ConnectionType.HttpType$.MODULE$;
}
