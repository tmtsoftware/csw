#Steps to release csw-prod

1. Update release notes (notes/vX.markdown)
2. Update top level README.md
3. Verify build.sbt (exclude projects which you do not want to release)
4. Tag csw-prod with new release version
5. Modify env variables in jenkins (ex. RELEASE_VERSION)
6. Make sure ssh -vT git@github.com works
7. Run csw-prod-release pipeline (This automatically triggers acceptance-pipeline)
8. Convert test report from test-reports.xml from release page. (junit-merge & junit-viewer)
9. Verify paradox, unidoc links
10. Update csw.g8 project with the new version and tag it