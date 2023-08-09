# sbt Tasks
csw uses multiple plugins in the sbt ecosystem to help accomplish a variety of tasks. 

| plugin         | task            | Description               |
| :------------: |:--------------: | :-------------------------|
| default in sbt | test            | compile and run all tests including scala tests, java tests, multi-jvm/multi-node tests |
| default in sbt | test:test       | compile and run all tests excluding multi-jvm/multi-node tests |
| default in sbt | publishLocal    | install your libraries in the local Ivy repository so they can be added as dependencies to other projects |       
| [sbt-multi-jvm](https://doc.pekko.io/docs/pekko/current/multi-jvm-testing.html?language=scala#multi-jvm-testing) | multi-jvm:test  | provides support for running applications (objects with main methods) and ScalaTest tests in multiple JVMs at the same time. |
| [sbt-site](https://www.scala-sbt.org/sbt-site/) | makeSite | generates project’s webpage in the target/site directory|
| [sbt-ghpages](https://github.com/sbt/sbt-ghpages) | ghpagesPushSite | publish project website to [GitHub Pages](https://pages.github.com/)|
| [sbt-github-release](https://github.com/ohnosequences/sbt-github-release) | githubRelease | creates Github releases with proper release notes and optional artifact uploading. Releases in Github are first-class objects with changelogs and binary assets that present a full project history beyond Git artifacts. They’re accessible from a repository’s homepage.|
