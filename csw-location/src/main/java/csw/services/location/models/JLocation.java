package csw.services.location.models;

import akka.actor.ActorRef;
import csw.services.location.models.Connection.*;
import java.net.URI;
import java.util.Optional;

public class JLocation {

    public final static ResolvedAkkaLocation resolvedAkkaLocation(Connection.AkkaConnection akkaConnection, URI uri, String prefix, Optional<ActorRef> actorRef) {
        return new ResolvedAkkaLocation(akkaConnection,uri,prefix,actorRef);
    }

    public final static ResolvedHttpLocation resolvedHttpLocation(HttpConnection httpConnection, URI uri, String path) {
        return new ResolvedHttpLocation(httpConnection,uri,path);
    }

    public final static ResolvedTcpLocation resolvedTcpLocation(TcpConnection tcpConnection, URI uri) {
        return new ResolvedTcpLocation(tcpConnection,uri);
    }
}
