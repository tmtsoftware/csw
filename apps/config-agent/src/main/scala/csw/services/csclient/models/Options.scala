package csw.services.csclient.models

import java.io.File

/**
  * @param op Gets file with given path from the config service and writes it to the o  utput file.
  * @param repositoryFilePath Path of an existing file in repository. Required for operations such as get, create, update,
  *                             createOrUpdate, history, getDefault, setDefault, resetDefault
  * @param inputFilePath  Input file path from a local disc. Required for operations such as create, update, createOrUpdate etc.
  * @param outputFilePath Output file path from a local disc where a file will be created. Required for operations such as get, getDefault.
  * @param id Optional: version id of the file to get.
  * @param oversize Optional: if the file is an Oversized(large binary file)
  * @param comment Optional: Version history comment to add while creating the file.
  */
case class Options(
                    op:String = "",
                    repositoryFilePath: Option[File] = None,
                    inputFilePath: Option[File] = None,
                    outputFilePath: Option[File] = None,
                    id: Option[String] = None,
                    oversize: Boolean = false,
                    comment: String = ""
                  )