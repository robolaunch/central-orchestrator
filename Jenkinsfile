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
          git branch: 'main', changelog: false, poll: false, url: 'https://github.com/robolaunch/central-orchestrator.git'
          sh "export VER=`grep "<version>" pom.xml | head -n 1 | sed 's/<version>//' | sed 's/<\/version>//'` && echo \$VER > version.txt && echo \$VER"
          script {
            env.VER = readFile('version.txt').trim()
          }
          echo "${env.VER}"
        }
      }
    }
    //stage('Build') {
    //  steps {
    //    container('ubuntu') {
    //      withCredentials([file(credentialsId: 'backend.application.properties', variable: 'cnt')]) {
    //        writeFile file:'./src/main/resources/application.properties', text: readFile(cnt)
    //        sh 'ls -l ./src/main/resources/application.properties && cat ./src/main/resources/application.properties'
    //      }
    //      sh 'mvn clean install'
    //    }
    //  }
    //}
    //stage('Docker Build') {
    //  steps {
    //    container('docker') {
    //      sh "docker build -f src/main/docker/Dockerfile.jvm -t robolaunchio/central-orchestrator:${env.VER} ."
    //      withCredentials([usernamePassword(credentialsId: 'dockerhub-robolaunchio', passwordVariable: 'password', usernameVariable: 'username')]) {
    //        sh 'docker login -u $username -p $password'
    //      }
    //      sh "docker push robolaunchio/central-orchestrator:${env.VER}"
    //    }
    //  }
    //}
    //stage('Kubernetes Deploy') {
    //  steps {
    //    container('ubuntu') {
    //      withCredentials([file(credentialsId: 'hetzner_prod', variable: 'config')]) {
    //        sh 'KUBECONFIG=$config kubectl get ns'
    //        sh 'KUBECONFIG=$config kogito use-project backend'
    //        sh 'KUBECONFIG=$config kogito delete-service central-orchestrator'
    //        sh "KUBECONFIG=$config kogito deploy-service central-orchestrator --image robolaunchio/central-orchestrator:${env.VER} --infra kogito-infinispan-infra --infra kogito-kafka-infra  --replicas 2"
    //      }
    //    }
    //  }
    //}
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
