pipeline {
  agent {
    kubernetes {
      yaml '''
        apiVersion: v1
        kind: Pod
        metadata: 
          name: kube-pod
        spec:
          containers:
          - name: docker
            image: docker:latest
            imagePullPolicy: Always
            command:
            - cat
            tty: true
            securityContext:
              privileged: true
              runAsUser: 0
            volumeMounts:
            - mountPath: /var/run/docker.sock
              name: docker-sock
          - name: ubuntu
            image: robolaunchio/build-image:1.0
            imagePullPolicy: Always
            command:
            - cat
            tty: true
            env:
            - name: CI
              value: false
          volumes:
          - name: docker-sock
            hostPath:
              path: /var/run/docker.sock
'''
    }
  }
  stages {
    stage('Clone') {
      steps {
        container('ubuntu') {
          git changelog: false, poll: false, url: 'https://github.com/robolaunch/central-orchestrator.git'
          withCredentials([file(credentialsId: 'backend.application.properties', variable: 'cnt')]) {
            writeFile file: './src/main/resources/application.properties', text: '$cnt'
          }
        }
      }
    }
    stage('Build') {
      steps {
        container('ubuntu') {
          sh 'mvn clean install'
        }
      }
    }
    stage('Docker Build') {
      steps {
        container('docker') {
          sh 'docker build -f src/main/docker/Dockerfile.jvm -t robolaunchio/central-orchestrator:pipeline .'
          withCredentials([usernamePassword(credentialsId: 'dockerhub-robolaunchio', passwordVariable: 'password', usernameVariable: 'username')]) {
            sh 'docker login -u $username -p $password'
          }
          sh 'docker push robolaunchio/central-orchestrator:pipeline'
        }
      }
    }
    stage('Kubernetes Deploy') {
      steps {
        container('ubuntu') {
          withCredentials([file(credentialsId: 'hetzner_prod', variable: 'config')]) {
            sh 'kogito use-project backend'
            sh 'kogito deploy-service central-orchestrator --image robolaunchio/central-orchestrator:pipeline --infra kogito-infinispan-infra --infra kogito-kafka-infra'
          }
        }
      }
    }
  }
    post {
      always {
            emailext to: "tamer@robolaunch.cloud",
            subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
            body: "${currentBuild.currentResult}: Job ${env.JOB_NAME} build ${env.BUILD_NUMBER}\n More info at: ${env.BUILD_URL}",
            attachmentsPattern: '*.html'
    }
    }
}
