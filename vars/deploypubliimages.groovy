// vars/deployMultipleImages.groovy
def call(List<Map> images, String registry = '') {
    images.each { image ->
        def name = image.name
        def tag = image.tag ?: 'latest'

        // Create image reference safely (skip leading slash if registry is blank)
        def imageRef = registry ? "${registry}/${name}:${tag}" : "${name}:${tag}"

        echo "=== Deploying image: ${name}:${tag} ==="

        bat """
            docker pull ${name}:${tag}
            docker tag ${name}:${tag} ${imageRef}
            docker push ${imageRef}
        """

        echo "=== Verifying Docker container list after pushing ${name}:${tag} ==="
        bat "docker ps -a"
    }
}



// def call(String agentLabel, String imageName, String imageTag) {
//     node('') {
//         def fullImage = "${imageName}:${imageTag}"

//         stage('Pull Public Image') {
//             bat "docker pull ${fullImage}"
//         }

//         stage('Run Container') {
//             bat """
//                 docker stop ${imageName} || echo "No such container"
//                 docker rm ${imageName} || echo "No container to remove"
//                 docker run -d --restart unless-stopped --name ${imageName} -p 8081:80 ${fullImage}
//             """
//         }

//         stage('Verify Container') {
//             bat "docker ps -a"
//         }
//     }
// }
