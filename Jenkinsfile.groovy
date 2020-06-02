#!/usr/bin/env groovy
node{
    try {
        stage('SCM Checkout'){
            checkout scm
        }
    } catch(Exception e){
        println(e)
    }
}