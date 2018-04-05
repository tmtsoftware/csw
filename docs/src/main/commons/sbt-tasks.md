# sbt Tasks
csw-prod uses multiple plugins in the sbt ecosystem to help accomplish a variety of tasks. 

| plugin         | task            | Description               |
| :------------: |:--------------: | :-------------------------|
| default in sbt | test            | compile and run all tests including scala tests, java tests, multi-jvm/multi-node tests |
| default in sbt | test:test       | compile and run all tests excluding multi-jvm/multi-node tests |
| default in sbt | publishLocal    | install your libraries in local Ivy repository so they can be added as dependencies to other projects |       
| [sbt-multi-jvm](https://doc.akka.io/docs/akka/current/multi-jvm-testing.html?language=scala#multi-jvm-testing) | multi-jvm:test  | provides support for running applications (objects with main methods) and ScalaTest tests in multiple JVMs at the same time. |
| [sbt-site](https://www.scala-sbt.org/sbt-site/) | makeSite | generates project’s webpage in the target/site directory|
| [sbt-ghpages](https://github.com/sbt/sbt-ghpages) | ghpagesPushSite | publish project website to [GitHub Pages](https://pages.github.com/)|
| [sbt-github-release](https://github.com/ohnosequences/sbt-github-release) | githubRelease | creates Github releases with proper release notes and optional artifact uploading. Releases in Github are first-class objects with changelogs and binary assets that present a full project history beyond Git artifacts. They’re accessible from a repository’s homepage.|
| [sbt-bintray](https://github.com/sbt/sbt-bintray) | publish | upload and release artifacts into bintray (command requires proper bintray credentials.  Intended for TMT staff.)|
| [sbt-native-packager](https://sbt-native-packager.readthedocs.io/en/stable/) | stage | locally install your app in target/universal/stage/bin/ so you can run it locally without having the app packaged.|       
| [sbt-native-packager](https://sbt-native-packager.readthedocs.io/en/stable/) | universal:packageBin | Generates a universal zip file |