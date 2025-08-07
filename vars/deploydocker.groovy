def call(Map config = [:]) {
    def agentLabel         = config.get('agentLabel', '')
    def imageName          = config.get('imageName', 'web-calculate')
    def imageTag           = config.get('imageTag', 'v1.0')
    def appGitUrl          = config.get('appGitUrl', 'https://github.com/kishore-s8/calculator.git')
    def credentialsId      = config.get('credentialsId', 'gitpvt')
    def branch             = config.get('branch', 'main')
    def dockerCredentialsId= config.get('dockerCredentialsId', 'dockerhub-creds')
    def dockerRegistry     = config.get('dockerRegistry', 'docker.io/8kishore8')

    // 📁 Volume definitions (internal only)
    def namedVolume  = "calcdata:/data"                           // Docker volume → /data in container
    def bindMount    = "C:/jenkins_volume/calc_config:/config"   // Host bind mount → /config in container

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
                    echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin ${dockerRegistry}
                    docker push ${fullImage}
                """
            }
        }

        stage('Ensure Docker Volume Exists') {
            def volumeName = namedVolume.split(':')[0]
            bat """
                docker volume inspect ${volumeName} >nul 2>&1 || docker volume create ${volumeName}
            """
        }

        stage('Prepare Host Directory for Bind Mount') {
            def hostPath = bindMount.split(':')[0]
            echo "Creating host directory for bind mount: ${hostPath}"
            bat """
                if not exist "${hostPath}" mkdir "${hostPath}"
            """
        }

        stage('Run Container') {
            def volumeOptions = "-v ${namedVolume} -v ${bindMount}"

            bat """
                docker stop ${imageName} || echo "No existing container"
                docker rm ${imageName} || echo "No container to remove"
                docker run -d --name ${imageName} ${volumeOptions} -p 8082:3000 ${fullImage}
            """
        }

        stage('Verify Container') {
            bat "docker ps -a"
        }

        stage('Inspect Volume & Bind Mount') {
            def volumeName = namedVolume.split(':')[0]
            def volumeMount = namedVolume.split(':')[1]
            def bindHostPath = bindMount.split(':')[0]

            echo "🔍 Listing contents of Docker volume '${volumeName}' mounted at ${volumeMount}:"
            bat """
                docker run --rm -v ${namedVolume} alpine sh -c "ls -l ${volumeMount}"
            """

            echo "🔍 Listing contents of bind mount directory '${bindHostPath}':"
            bat """
                dir ${bindHostPath}
            """
        }
    }
}
