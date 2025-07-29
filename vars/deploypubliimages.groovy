def call(String imageStr) {
    def images = imageStr.split(',').collect {
        def parts = it.trim().split(':')
        [name: parts[0], tag: parts[1]]
    }

    pipeline {
        agent any

        stages {
            stage('Deploy Docker Images') {
                steps {
                    script {
                        images.each { img ->
                            echo "Deploying Docker image: ${img.name}:${img.tag}"
                            // Example: docker commands here
                            // sh "docker pull ${img.name}:${img.tag}"
                        }
                    }
                }
            }
        }
    }
}
