package csw.location.api.javadsl;


import csw.location.api.models.NetworkType;

/**
 * Helper class for Java to get the handle of `NetworkType`
 */
public interface JNetworkType {
    /**
     * Used to define an Private Network Type
     */
    NetworkType Private = NetworkType.Private$.MODULE$;

    /**
     * Used to define an Public Network Type
     */
    NetworkType Public = NetworkType.Public$.MODULE$;
}