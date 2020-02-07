pipeline {
    agent { label 'ubuntu' }

    stages {
        stage('Get submodules') {
            steps {
                sh 'git submodule update --init --recursive'
            }
        }
        stage('Set up gradle') {
            steps {
                sh """if [ ! -f "gradle-5.0-bin.zip" ]; then
                        wget https://services.gradle.org/distributions/gradle-5.0-bin.zip
                        unzip gradle-5.0-bin.zip
                        gradle-5.0/bin/gradle setup
                      fi
                    """
            }
        }
        stage('Build') {
            steps {
                sh "./gradlew allDependencies checkLicenses spotlessCheck test assemble"
            }
        }
        stage('Publish') {
            when {
                branch "master"
            }
            steps {
                sh "./gradlew publish"
            }
        }
    }
    post {
        always {
           junit '**/build/test-results/test/*.xml'
        }
    }
}