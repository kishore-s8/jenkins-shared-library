def call(String agentLabel, String imageName, String imageTag, String kubeconfigPath, String k8sPath, String giturl, String credentialsId, string branch) {
    node('') {
        def fullImage = "${imageName}:${imageTag}"

        stage('Checkout Code') {
            checkout([
                $class: 'GitSCM',
                branch: branch,
                userRemoteConfigs: [[
                    credentialsId: credentialsId,
                    url: giturl
                ]]
            ])
        }

        stage('Build Docker Image') {
            bat "docker build -t ${fullImage} ."
        }

        stage('Push Docker Image') {
            withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                bat """
                    docker login -u %DOCKER_USER% -p %DOCKER_PASS%
                    docker push ${fullImage}
                """
            }
        }

        stage('Deploy to Kubernetes') {
            bat "kubectl --kubeconfig=\"${kubeconfigPath}\" apply -f \"${k8sPath}\""
        }

        stage('Verify Deployment') {
            bat "kubectl --kubeconfig=\"${kubeconfigPath}\" get pods"
            bat "kubectl --kubeconfig=\"${kubeconfigPath}\" get svc"
        }
    }
}
