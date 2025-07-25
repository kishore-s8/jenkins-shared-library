
def call(String agentLabel, String imageName, String imageTag) {

    node(agentLabel) {

        def fullImage = "${imageName}:${imageTag}"  // No registry prefix — assumes public image

        stage('Pull Public Image') {
            bat "docker pull ${fullImage}"
        }

        stage('Run Container') {
            bat """
                docker stop ${imageName} || echo "No existing container"
                docker rm ${imageName} || echo "No container to remove"
                docker run -d --name ${imageName} -p 8080:8080 ${fullImage}
            """
        }

        stage('Verify Container') {
            bat "docker ps -a"
        }
    }
}
