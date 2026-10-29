// Declarative pipeline for the insurance automation framework.
// Jenkins set-up needed once: Global Tool Configuration -> JDK named "JDK-17" and Maven named "Maven-3.9";
// the agent needs Chrome (or Firefox/Edge) installed for the UI stage. Works on Windows (bat) and Linux/Mac (sh).

// One helper so every stage runs on any operating system.
def run(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}

pipeline {
    agent any

    tools {
        jdk 'JDK-17'
        maven 'Maven-3.9'
    }

    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')          // a hung browser must not block the agent forever
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()                   // the app under test uses a fixed port
    }

    parameters {
        choice(name: 'SUITE', choices: ['smoke.xml', 'api.xml', 'regression.xml'],
               description: 'smoke = every commit, api = every pull request, regression = nightly / release gate')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'edge'], description: 'Browser for UI tests')
        string(name: 'RETRY', defaultValue: '1', description: 'Retries for a failed UI test (0 = none). Retried passes are still reported.')
    }

    triggers {
        // Nightly full regression on weekdays at ~2 AM (H spreads the load across jobs).
        // Smoke on every commit comes from the Git webhook / "Poll SCM" setting of the multibranch job.
        cron('H 2 * * 1-5')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                // Fail fast on compile errors before spending minutes on tests.
                run 'mvn -B -q clean test-compile'
            }
        }

        stage('API + DB tests') {
            // Fast, browser-free gate: if the business rules or the data are broken, there is no point opening a browser.
            steps {
                run 'mvn -B test -DsuiteFile=api.xml'
            }
        }

        stage('UI tests') {
            when { expression { params.SUITE != 'api.xml' } }
            steps {
                // The nightly cron run has no parameters, so it falls back to the first choice; force regression there.
                script {
                    def suite = (currentBuild.getBuildCauses('hudson.triggers.TimerTrigger$TimerTriggerCause')) ? 'regression.xml' : params.SUITE
                    run "mvn -B test -DsuiteFile=${suite} -Dheadless=true -Dbrowser=${params.BROWSER} -Dretry=${params.RETRY}"
                }
            }
        }
    }

    post {
        always {
            // TestNG/Surefire results for the Jenkins trend graph, plus screenshots and the HTML report as artifacts.
            junit testResults: 'target/surefire-reports/TEST-*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'target/screenshots/**, target/surefire-reports/**', allowEmptyArchive: true
        }
        failure {
            // Replace with your team's channel (needs the Email Extension or Slack plugin).
            echo "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${env.BUILD_URL}"
            // emailext to: 'qa-team@example.com', subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}", body: "${env.BUILD_URL}"
        }
        unstable {
            echo 'Unstable: tests passed only on retry or some reports are missing - treat as a flaky-test ticket, not a green build.'
        }
        cleanup {
            cleanWs()
        }
    }
}
