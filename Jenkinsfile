/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
pipeline {
    agent { label 'ubuntu' }

    stages {
        stage('Get submodules') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    sh 'git submodule update --init --recursive'
                }
            }
        }
        stage('Set up gradle') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    sh """if [ ! -f "gradle-5.0-bin.zip" ]; then
                            wget https://services.gradle.org/distributions/gradle-6.3-bin.zip
                            unzip gradle-6.3-bin.zip
                            gradle-6.3/bin/gradle setup
                          fi
                        """
                }
            }
        }
        stage('Build') {
            steps {
                timeout(time: 60, unit: 'MINUTES') {
                    sh "./gradlew allDependencies checkLicenses spotlessCheck assemble test integrationTest"
                }
            }
        }
        stage('Publish') {
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    sh "./gradlew publish"
                }
            }
        }
    }
    post {
        always {
           junit '**/build/test-results/test/*.xml'
        }
    }
}