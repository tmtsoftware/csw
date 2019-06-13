# Releasing

## Prerequisites

### Git
* Make sure git authentication works on jenkins agent by running cmd: `ssh -vT git@github.com`

## Steps to release

### csw
1. Update release notes (`notes/<version>.markdown`)
2. Update top level `CHANGELOG.md`
3. Update top level `README.md`
4. Exclude projects from `build.sbt` which you do not want to release
5. Run `csw-prod` pipeline by providing `VERSION` number. (This automatically triggers `acceptance-release` pipeline)

### csw.g8
1. Merge `dev` branch to master
2. Run `giter8-release` pipeline by providing `VERSION` number

### Release `csw-js` (if needed)
- Release `csw-js` if keycloak version is updated in csw

#### Note - `VERSION` tag is version number with 'v' as prefix. For eg. `v0.0.0`

### More detailed instructions

https://docs.google.com/document/d/1tK9W6NClJOB0wq3bFVEzdrYynCxES6BnPdSLNtyGMXo/edit?usp=sharing