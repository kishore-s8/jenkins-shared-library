def call(String agentLabel, String imageName, String imageTag, String kubeconfigPath,
         String helmGitUrl, String helmChartPath, String appGitUrl,
         String credentialsId, String branch, String dockerCredentialsId,
         String dockerRegistry, String helmBranch) {

    node(agentLabel) {

        def fullImage = "${dockerRegistry}/${imageName}:${imageTag}"
        def releaseName = 'calculator-release'
        def chartPath = "helm/${helmChartPath}"
        def manifestOutput = "helm-manifest.yaml"
        def ingressHost = "calculator.example.com"

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
            withCredentials([usernamePassword(credentialsId: dockerCredentialsId, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                bat """
                    docker login -u %DOCKER_USER% -p %DOCKER_PASS% ${dockerRegistry}
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

        stage('Render and Apply Helm Manifests') {
            try {
                bat """
                    helm template ${releaseName} ${chartPath} ^
                        --kubeconfig="${kubeconfigPath}" ^
                        --set image.repository=${dockerRegistry}/${imageName} ^
                        --set image.tag=${imageTag} ^
                        --set service.port=3000 ^
                        --set service.nodePort=30010 ^
                        --set ingress.enabled=true ^
                        --set ingress.className=nginx ^
                        --set ingress.hosts[0].host=${ingressHost} ^
                        --set ingress.hosts[0].paths[0].path=/ ^
                        --set ingress.hosts[0].paths[0].pathType=Prefix ^
                        > ${manifestOutput}
                """

                bat "kubectl apply -f ${manifestOutput} --kubeconfig=\"${kubeconfigPath}\""
            } catch (err) {
                echo "❌ Deployment failed: ${err}"
                error("Stopping pipeline due to manifest error.")
            }
        }

        stage('Verify Deployment') {
            bat "kubectl --kubeconfig=\"${kubeconfigPath}\" get pods"
            bat "kubectl --kubeconfig=\"${kubeconfigPath}\" get svc"
            bat "kubectl --kubeconfig=\"${kubeconfigPath}\" get ingress"
        }
    }
}
