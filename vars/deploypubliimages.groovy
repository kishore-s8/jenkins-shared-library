def call() {
    // Define the parameter inside the shared library
    properties([
        parameters([
            string(
                name: 'DOCKER_IMAGES',
                defaultValue: 'nginx:latest,redis:alpine,httpd:2.4',
                description: 'Comma-separated list of docker images (name:tag)'
            )
        ])
    ])

    // Fallback if parameter not present (e.g. on first run)
    def imageStr = binding.hasVariable('DOCKER_IMAGES') ? DOCKER_IMAGES : 'nginx:latest,redis:alpine,httpd:2.4'

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
                            echo "Deploying image: ${img.name}:${img.tag}"
                            // Example: pull or deploy
                            bat "docker pull ${img.name}:${img.tag}"
                        }
                    }
                }
            }
        }
    }
}
