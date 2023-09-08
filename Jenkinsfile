pipeline {

  environment {
    devRegistry = 'ghcr.io/datakaveri/acl-apd-dev'
    deplRegistry = 'ghcr.io/datakaveri/acl-apd-depl'
    testRegistry = 'ghcr.io/datakaveri/acl-apd-test:latest'
    registryUri = 'https://ghcr.io'
    registryCredential = 'datakaveri-ghcr'
    GIT_HASH = GIT_COMMIT.take(7)
  }

  agent { 
    node {
      label 'slave1' 
    }
  }

  stages {

    stage('Building images') {
      steps{
        script {
          echo 'Pulled - ' + env.GIT_BRANCH
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          deplImage = docker.build( deplRegistry, "-f ./docker/depl.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }

    stage('Unit Tests and CodeCoverage Test'){
      steps{
        script{
          sh 'docker compose -f docker-compose.test.yml up test'
        }
        xunit (
          thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
          tools: [ JUnit(pattern: 'target/surefire-reports/*.xml') ]
        )
        jacoco classPattern: 'target/classes', execPattern: 'target/jacoco.exec', sourcePattern: 'src/main/java', exclusionPattern: '**/*VertxEBProxy.class, **/*VertxProxyHandler.class, **/*Verticle.class, **/*Service.class, iudx/apd/acl/server/deploy/*, **/*Constants.class'
      }
      post{
            always {
                      recordIssues(
                        enabledForFailure: true,
                        blameDisabled: true,
                        forensicsDisabled: true,
                        qualityGates: [[threshold:0, type: 'TOTAL', unstable: false]],
                        tool: checkStyle(pattern: 'target/checkstyle-result.xml')
                      )
                      recordIssues(
                        enabledForFailure: true,
                      	blameDisabled: true,
                        forensicsDisabled: true,
                        qualityGates: [[threshold:0, type: 'TOTAL', unstable: false]],
                        tool: pmdParser(pattern: 'target/pmd.xml')
                      )
                    }
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          }
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'sudo rm -rf target/'
          }
        }
      }
    }
    
    stage('Push Images') {
      when {
        expression {
          return env.GIT_BRANCH == 'origin/main';
        }
      }
      steps {
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            devImage.push("5.0.0-alpha-${env.GIT_HASH}")
            deplImage.push("5.0.0-alpha-${env.GIT_HASH}")
          }
        }
      }
    }
  }
}