package csw.services.location.javadsl;

import csw.services.location.models.Location;
import csw.services.location.models.Registration;
import csw.services.location.models.RegistrationResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ILocationService {
        public CompletableFuture<RegistrationResult> register(Registration registration);

        public CompletableFuture<List<Location>> list();
}
