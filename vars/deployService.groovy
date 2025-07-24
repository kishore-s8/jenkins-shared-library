def call(String agentLabel, String dockerRegistry, String imageName, String imageTag, String kubeconfigPath,
         String helmGitUrl, String helmChartPath, String appGitUrl,
         String credentialsId, String branch, String dockerCredentialsId) {

    node(agentLabel) {  // Use passed agent label

        stage('Checkout Application Code') {
            dir('app') {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: branch]],
                    userRemoteConfigs: [[
                        credentialsId: credentialsId,
                        url: appGitUrl
                    ]]
                ])
            }
        }

        def fullImage = "${dockerRegistry}/${imageName}:${imageTag}"

        stage('Build Docker Image') {
            dir('app') {
                bat "docker build -t ${fullImage} ."
            }
        }

        stage('Push Docker Image') {
            withCredentials([usernamePassword(credentialsId: dockerCredentialsId, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                bat """
                    docker login -u %DOCKER_USER% -p %DOCKER_PASS%
                    docker push ${fullImage}
                """
            }
        }

        stage('Checkout Helm Repo') {
            dir('helm') {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: 'main']],
                    userRemoteConfigs: [[
                        credentialsId: credentialsId,
                        url: helmGitUrl
                    ]]
                ])
            }
        }

        stage('Deploy to Kubernetes') {
            try {
                def releaseName = 'calculator-release'

                bat """
                    helm upgrade --install ${releaseName} helm/${helmChartPath} ^
                        --kubeconfig="${kubeconfigPath}" ^
                        --set image.repository=${dockerRegistry}/${imageName} ^
                        --set image.tag=${imageTag}
                """
            } catch (err) {
                echo "Deployment failed: ${err}"
                error("Stopping pipeline due to deployment error.")
            }
        }

        stage('Verify Deployment') {
            bat "kubectl --kubeconfig=\"${kubeconfigPath}\" get pods"
            bat "kubectl --kubeconfig=\"${kubeconfigPath}\" get svc"
        }
    }
}
