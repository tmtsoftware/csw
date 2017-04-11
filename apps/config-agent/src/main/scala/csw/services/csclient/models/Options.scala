package csw.services.csclient.models

import java.io.File

/**
  * @param op Gets file with given path from the config service and writes it to the output file.
  * @param path Path in the repository.
  * @param inputFilePath  Input file path. Required for operations such as create, update, createOrUpdate etc.
  * @param outputFilePath Output file path. Required for operations such as get, getDefault.
  * @param id Optional: version id of the file to get.
  * @param oversize Optional: if the file is an Oversized(large binary file)
  * @param comment Optional: Version history comment to add while creating the file.
  */
case class Options(
                    op:String = "",
                    path: String = "",
                    inputFilePath: String = "",
                    outputFilePath: String = "",
                    id: Option[String] = None,
                    oversize: Boolean = false,
                    comment: String = ""
                  )