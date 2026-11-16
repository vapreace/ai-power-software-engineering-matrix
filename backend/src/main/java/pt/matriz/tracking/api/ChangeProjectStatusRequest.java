package pt.matriz.tracking.api;

import pt.matriz.projects.ProjectStatus;

record ChangeProjectStatusRequest(ProjectStatus status, Long version) {
}
