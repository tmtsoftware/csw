package csw.config.api.javadsl;

import csw.config.models.FileType;

/**
 * Helper class for Java to get the handle of file types
 */
public interface JFileType {

    /**
     * Represents a file to be stored in annex store
     */
    FileType Annex = FileType.Annex$.MODULE$;

    /**
     * Represents a file to be stored in the repository normally
     */
    FileType Normal = FileType.Normal$.MODULE$;
}
