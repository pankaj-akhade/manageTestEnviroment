node('helm3'){
    stage('SCM checkout'){
        checkout scm
    }
}