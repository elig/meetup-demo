package org.demo

class BuildMarker implements Serializable {
    List builds
    List repositories
    String build_name
    String build_number
    String build_url
    String artifact_build_name
    String artifact_build_number
    String artifact_repository
    BuildMarkerStatus test_status

    BuildMarker(String build_name, String build_number, BuildMarkerStatus test_status, String build_url, String artifact_build_name, String artifact_build_number, String artifact_repository) {
        this.build_name = build_name
        this.build_number = build_number
        this.build_url = build_url
        this.artifact_build_name = artifact_build_name
        this.artifact_build_number = artifact_build_number
        this.artifact_repository = artifact_repository
        this.test_status = test_status
        this.builds = artifact_build_name.split(" ")
        this.repositories = artifact_repository.split(" ")

    }


    @Override
    public String toString() {
        return "BuildMarker{" +
                "builds=" + builds +
                ", repositories=" + repositories +
                ", build_name='" + build_name + '\'' +
                ", build_number='" + build_number + '\'' +
                ", build_url='" + build_url + '\'' +
                ", artifact_build_name='" + artifact_build_name + '\'' +
                ", artifact_build_number='" + artifact_build_number + '\'' +
                ", artifact_repository='" + artifact_repository + '\'' +
                ", test_status='" + test_status + '\'' +
                '}';
    }
}