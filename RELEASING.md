# Releasing

## Prerequisites
1. Make sure git authentication works on jenkins agent by running cmd: `ssh -vT git@github.com`
2. npm module `junit-merge` is installed (for merging multiple xml test reports into one)
3. npm module `junit-viewer` is installed (for generating html test report from merged xml)

## Steps to release

### csw-prod
1. Update release notes (`notes/<version>.markdown`)
2. Update top level `CHANGELOG.md`
3. Update top level `README.md`
4. Exclude projects from `build.sbt` which you do not want to release
5. Run `csw-prod-release` pipeline by providing `VERSION` number. (This automatically triggers `acceptance-release` pipeline)

### csw.g8
1. Merge `dev` branch to master
2. Run `giter8-release` pipeline by providing `VERSION` number