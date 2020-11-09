package csw.location.api.javadsl;


import csw.location.api.models.NetworkType;

/**
 * Helper class for Java to get the handle of `NetworkType`
 */
public interface JNetworkType {
    /**
     * Used to define an Inside Network Type
     */
    NetworkType Inside = NetworkType.Inside$.MODULE$;

    /**
     * Used to define an Outside Network Type
     */
    NetworkType Outside = NetworkType.Outside$.MODULE$;
}