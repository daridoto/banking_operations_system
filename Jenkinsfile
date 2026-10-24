pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 20, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
    }

    triggers {
        // Comprueba GitHub cada cinco minutos; construye solo si hay cambios.
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('Pruebas y empaquetado') {
            steps {
                sh './mvnw -B -ntp clean verify'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/TEST-*.xml'
                }
            }
        }
        stage('Guardar aplicación') {
            steps {
                archiveArtifacts artifacts: 'target/banking-operations-system-*.jar', fingerprint: true
            }
        }
    }
}
