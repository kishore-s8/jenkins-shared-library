def call(String agentLabel, String imageName, String imageTag, String kubeconfigPath,
         String helmGitUrl, String helmChartPath, String appGitUrl,
         String credentialsId, String branch, String dockerCredentialsId,
         String dockerRegistry, String helmBranch) {

    node(agentLabel) {

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
                    branches: [[name: helmBranch]],
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
                def chartPath = "helm/${helmChartPath}"

                bat """
                    helm upgrade --install ${releaseName} ${chartPath} ^
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
