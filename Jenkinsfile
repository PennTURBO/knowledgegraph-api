/**
 * A Jenkins Pipeline for Carnival CI
 * Builds and tests carnival and publishes groovyDocs
 * 
 */
pipeline {
    agent any
    parameters {
        //string(name: 'STATUS_EMAIL', defaultValue: 'hwilli@pennmedicine.upenn.edu', description: 'Comma sep list of email addresses that should recieve test status notifications.')
        string(name: 'STATUS_EMAIL', defaultValue: 'hwilli@pennmedicine.upenn.edu, hfree@pennmedicine.upenn.edu', description: 'Comma sep list of email addresses that should recieve test status notifications.')
    }
    options {
        timeout(time: 10, unit: 'MINUTES') 
    }
    stages {
        stage('Setup Workspace') { 
            steps {

                /*git(
                    //credentialsId: 'github-jenkins-spaghetti',
                    branch: "master", 
                    url: 'git@github.com:PennTURBO/Turbo-API.git'
                )*/
                checkout scm

                // setup local workspace
                fileExists("turboAPI.properties.template")
                sh 'cp turboAPI.properties.template turboAPI.properties'

                script {
                    withCredentials([usernamePassword(credentialsId: 'Hayden_prd_graphDB_credentials', usernameVariable: 'graphDbUserName', passwordVariable: 'graphDbPassword')]) {
                        sh "sed -i 's/username = your_username/username = $graphDbUserName/g' turboAPI.properties"
                        sh "sed -i 's/password = your_password/password = $graphDbPassword/g' turboAPI.properties"
                    }
                }
            }
        }
        stage('Compile') { 
            steps {
                sh 'sbt compile'
            }
        }
        stage('Integration Tests') { 
            steps {
                sh 'sbt test'
            }
            post {
                always {
                    junit '**/target/test-reports/*.xml'                    
                }
            }
        }
        stage('Deploy to Dev Server') {
            when {
                branch 'master'
            }
            steps {
                build 'Turbo-API deploy turbo-dev-app01'
            }
        }
        stage('Deploy to Prd Server') {
            when {
                branch 'production'
            }
            steps {
                echo 'coming soon'
            }
        }
    }
    post {
        failure {
            echo 'Pipeline Failure'
            emailext attachLog: false, 
                compressLog: false,
                subject: 'Job \'${JOB_NAME}\' (${BUILD_NUMBER}) failure',
                body: '''${SCRIPT, template="groovy-html.template"}''', 
                mimeType: 'text/html',
                to: "${params.STATUS_EMAIL}",
                //recipientProviders: [culprits()],
                replyTo: "${params.STATUS_EMAIL}"
        }
        unstable {
            echo 'Pipeline Unstable'
            emailext attachLog: false, 
                compressLog: false,
                subject: 'Job \'${JOB_NAME}\' (${BUILD_NUMBER}) unstable',
                body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html', 
                to: "${params.STATUS_EMAIL}",
                //recipientProviders: [culprits()],
                replyTo: "${params.STATUS_EMAIL}"
        }
        aborted {
            echo 'Pipeline Aborted or Timeout'
            emailext attachLog: false, 
                compressLog: false,
                subject: 'Job \'${JOB_NAME}\' (${BUILD_NUMBER}) unstable',
                body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html', 
                to: "${params.STATUS_EMAIL}",
                //recipientProviders: [culprits()],
                replyTo: "${params.STATUS_EMAIL}"
        }
        fixed {
            echo 'Pipeline is back to normal'
            emailext attachLog: false, 
                compressLog: false,
                subject: 'Job \'${JOB_NAME}\' (${BUILD_NUMBER}) is back to normal',
                body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html', 
                to: "${params.STATUS_EMAIL}",
                //recipientProviders: [culprits()], 
                replyTo: "${params.STATUS_EMAIL}"
        }
    }    
}
