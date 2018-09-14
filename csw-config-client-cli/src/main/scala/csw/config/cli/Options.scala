package csw.config.cli

import java.nio.file.Path
import java.time.Instant

/**
 * @param op Gets file with given path from the config service and writes it to the o  utput file.
 * @param relativeRepoPath Path of an existing file in repository. Required for operations such as get, create, update,
 *                         createOrUpdate, history, setActiveVersion, resetActiveVersion, getActiveVersion, getActiveByTime, getActive
 * @param inputFilePath  Input file path from a local disc. Required for operations such as create, update, createOrUpdate etc.
 * @param outputFilePath Output file path from a local disc where a file will be created. Required for operations such as get, getActive.
 * @param id Optional: version id of the file to get.
 * @param annex Optional: if the file is an Annexd(large binary file)
 * @param comment Version history comment to add while creating the file.
 * @param latest Optional: Get the latest file.
 * @param pattern Optional: List all files whose path matches the given pattern.
 * @param normal Optional: If the file is normal.
 */
case class Options(
    op: String = "",
    relativeRepoPath: Option[Path] = None,
    inputFilePath: Option[Path] = None,
    outputFilePath: Option[Path] = None,
    id: Option[String] = None,
    date: Option[Instant] = None,
    fromDate: Instant = Instant.MIN,
    toDate: Instant = Instant.MAX,
    maxFileVersions: Int = Int.MaxValue,
    annex: Boolean = false,
    comment: Option[String] = None,
    latest: Boolean = false,
    pattern: Option[String] = None,
    normal: Boolean = false
)
