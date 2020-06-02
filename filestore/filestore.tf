variable "project" {
  type = string
}

variable "clusterName" {
  type = string
}

variable "region" {
  type = string
  default = "us-east1"
}

variable "filestore" {
  type = string
  default = "filestore"
}

provider "google" {
  credentials = file("../account.json")
  project     = var.project
  region      = var.region
}

data google_compute_network "vpc"{
  name = "gke-jenkins-vpc"
}

resource "google_filestore_instance" "instance" {
  name = "${var.clusterName}-fs"
  zone = "${var.region}-b"
  tier = "STANDARD"

  file_shares {
    capacity_gb = 1024
    name        = var.filestore
  }

  networks {
    network = data.google_compute_network.vpc.name
    modes   = ["MODE_IPV4"]
  }
}