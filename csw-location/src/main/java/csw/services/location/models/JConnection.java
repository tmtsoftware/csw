package csw.services.location.models;

import csw.services.location.models.Connection.*;

public class JConnection {
    /**
     * Represents a connection to a remote akka actor based component
     * @param componentId the id of the target component
     * @return the connection object
     */
    public static AkkaConnection akkaConnection(ComponentId componentId) {
        return new AkkaConnection(componentId);
    }

    /**
     * A connection to a remote http based component
     *
     * @param componentId the id of the target component
     * @return the connection object
     */
    public static HttpConnection httpConnection(ComponentId componentId) {
        return new HttpConnection(componentId);
    }

    /**
     * A connection to a remote tcp based component
     *
     * @param componentId the id of the target component
     * @return the connection object
     */
    public static TcpConnection tcpConnection(ComponentId componentId) {
        return new TcpConnection(componentId);
    }

    /**
     * Gets a Connection based on the component id and connection type
     *
     * @param componentId the component id
     * @param connectionType the connection type
     * @return the connection object
     */
    public static Connection createConnection(ComponentId componentId, ConnectionType connectionType) {
        return Connection$.MODULE$.apply(componentId, connectionType);
    }

    /**
     * Gets a Connection from a string as output by Connection.toString
     *
     * @param name a string in the format output by Connection.toString
     * @return the Connection object
     */
    public static Connection parse(String name) {
        return Connection$.MODULE$.parse(name).get();
    }
}
