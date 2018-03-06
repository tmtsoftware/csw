# Getting Started

## Intro

- Overview on how to build and use CSW

## Tools

- JDK (1.8+) - do we need specific instructions if they want to use Java9?
- sbt
- IntelliJ?
- Supported OS (CentOS, MacOS)
- Redis, to run Event Service (not fully supported)
- Testing frameworks/tools (although inform that they don't need to download and install, sbt will do it)

## Working with SBT Projects

- SBT basics, structure of project
    - How to set up for multi-projects (i.e. multiple components)
    - Managing dependencies, particularly how they add CSW to their project
    - Example build.sbt and Dependencies.scala
- Constructing a project for your component(s)
    - project templates? (look into defining giter8 project?).  If we do this, we can have them use it
    and it can provide example code for them to follow.
        - template specific for Java-only builds?
        - single project vs. multi-project?
    - Setting project in IntelliJ
- Conventions:
    - package naming (org.tmt.<subsystem>)
    
## Downloading and installing Apps

- Not too much detail here, will go through each one as needed.


