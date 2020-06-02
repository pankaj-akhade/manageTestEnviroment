variable "subnetCidr" {
  type = string
}

resource "google_compute_subnetwork" "subnet" {
  name          = "${var.clusterName}-subnet"
  region        = var.region
  network       = google_compute_network.vpc.name
  ip_cidr_range = var.subnetCidr
}