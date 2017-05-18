package csw.services.config.api.javadsl;

import csw.services.config.api.models.FileType;

/**
 * Java API for FileType.
 */
public class JFileType {
    /**
     * Represents a file to be stored in annex store
     */
    public static final FileType Annex = FileType.Annex$.MODULE$;

    /**
     * Represents a file to be stored in the repository normally
     */
    public static final FileType Normal = FileType.Normal$.MODULE$;
}
