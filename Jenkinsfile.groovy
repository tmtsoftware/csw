#!groovy
node {
    def failBuild = false
    try{
        stage('Checkout') {
            git 'https://github.com/tmtsoftware/csw-prod.git'
        }

        stage('Build') {
            sh "sbt -Dcheck.cycles=true clean scalastyle compile"
        }

        stage('Test') {
            try {
                sh "sbt csw-location/test"
            }
            catch (Exception e) {
                currentBuild.result = 'FAILED'
                failBuild = true
            }
            try {
                sh "sbt trackLocation/test"
            }
            catch (Exception e) {
                currentBuild.result = 'FAILED'
                failBuild = true
            }
            try{
                sh "sbt coverageReport"
            }
            catch (Exception ex){
                failBuild = true
            }
            sh "sbt coverageAggregate"
            if(failBuild == true)
                sh "exit 1"
        }

        stage('Package') {
            sh "./universal_package.sh"
        }

        stage('Integration') {
            sh "./integration/scripts/runner.sh '-v /var/lib/jenkins/workspace/csw-prod/:/source -v /var/lib/jenkins/.ivy2/:/root/.ivy2'"
        }

        stage('Infra Test') {
            parallel(
                    "Multiple NIC's": {
                        stage("NIC") {
                            try {
                                sh "./integration/scripts/multiple_nic_test.sh '-v /var/lib/jenkins/workspace/csw-prod/:/source -v /var/lib/jenkins/.ivy2/:/root/.ivy2'"
                            }
                            catch (Exception ex) {
                                currentBuild.result = 'FAILED'
                                throw ex
                            }
                        }
                    },
                    "Multiple Subnet's": {
                        stage("Subnet") {
                            try {
                                sh "./integration/scripts/multiple_subnets_test.sh '-v /var/lib/jenkins/workspace/csw-prod/:/source -v /var/lib/jenkins/.ivy2/:/root/.ivy2'"
                            }
                            catch (Exception ex) {
                                currentBuild.result = 'FAILED'
                                throw ex
                            }
                        }
                    }
            )
        }
    }
    catch (Exception e) {
        currentBuild.result = "FAILED"
        throw e
    } finally {
//        notifyBuild(currentBuild.result)
    }
}
/*

def notifyBuild(String buildStatus = 'STARTED') {
    // build status of null means successful
    buildStatus =  buildStatus ?: 'SUCCESSFUL'

    // Default values
    def colorName = 'RED'
    def colorCode = '#FF0000'
    def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    def summary = "${subject} (${env.BUILD_URL})"
    def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

    // Override default values based on build status
    if (buildStatus == 'STARTED') {
        color = 'YELLOW'
        colorCode = '#FFFF00'
    } else if (buildStatus == 'SUCCESSFUL') {
        color = 'GREEN'
        colorCode = '#00FF00'
    } else {
        color = 'RED'
        colorCode = '#FF0000'
    }

    // Send notifications
//    slackSend (color: colorCode, message: summary)

    emailext(
            subject: subject,
            body: details,
            recipientProviders: [[$class: 'DevelopersRecipientProvider']]
    )
}*/
