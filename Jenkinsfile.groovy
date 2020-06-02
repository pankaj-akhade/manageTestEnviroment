#!/usr/bin/env groovy
node{
    try {
        stage('Hello World'){
            println("Hello World")
        }
    } catch(Exception e){
        println(e)
    }
}