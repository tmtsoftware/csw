package csw.services.location.javadsl.models;

import akka.actor.ActorRef;
import csw.services.location.scaladsl.models.AkkaRegistration;
import csw.services.location.scaladsl.models.Connection;
import csw.services.location.scaladsl.models.HttpRegistration;
import csw.services.location.scaladsl.models.TcpRegistration;

/**
 * Java API for Registration service.
 */
@SuppressWarnings("unused")
public class JRegistration {
    /**
     * Registration type of an TCP based service
     */
    public static TcpRegistration tcpRegistaration(Connection.TcpConnection connection, int port){
        return new TcpRegistration(connection,port);
    }

    public static HttpRegistration httpRegistration(Connection.HttpConnection connection, int port, String path){
        return new HttpRegistration(connection,port,path);
    }

    public static AkkaRegistration akkaRegistration(Connection.AkkaConnection connection, ActorRef component, String prefix){
        return new AkkaRegistration(connection, component, prefix);
    }

}
