# Releasing

## Prerequisites
1. Make sure git authentication works on jenkins agent by running cmd: `ssh -vT git@github.com`

## Steps to release

### csw-prod
1. Update release notes (`notes/<version>.markdown`)
2. Update top level `README.md`
3. Exclude projects from `build.sbt` which you do not want to release)
4. Run `csw-prod-release` pipeline by providing `VERSION` number. (This automatically triggers `acceptance-release`)
5. Convert test report from test-reports.xml from release page to HTML format and upload it to release page. (junit-merge & junit-viewer)

### csw.g8
1. Merge `dev` branch to master
2. Run `giter8-release` pipeline by providing `VERSION` number