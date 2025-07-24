def call(String agentLabel, String imageName, String imageTag, String kubeconfigPath, String helmGitUrl, String helmChartPath, String appGitUrl, String credentialsId, String branch) {
    node(agentLabel) {
        def gitCommit = bat(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        def fullImage = "${imageName}:${imageTag}-${gitCommit}"

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

        stage('Build Docker Image') {
            dir('app') {
                bat "docker build -t ${fullImage} ."
            }
        }

        stage('Push Docker Image') {
            withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
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
                    branches: [[name: 'main']], // Adjust if helm repo uses a different branch
                    userRemoteConfigs: [[
                        credentialsId: credentialsId,
                        url: helmGitUrl
                    ]]
                ])
            }
        }

       stage('Deploy to Kubernetes') {
    try {
        def releaseName = imageName.replaceAll('[^a-zA-Z0-9-]', '')
        bat """
            helm upgrade --install ${releaseName} helm/${helmChartPath} ^
                --kubeconfig="${kubeconfigPath}" ^
                --set image.repository=${imageName} ^
                --set image.tag=${imageTag}-${gitCommit}
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
