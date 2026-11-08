package app.planer.server.task

import app.planer.server.task.data.TaskRepositoryImpl
import app.planer.server.task.domain.TaskRepository
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies

/** The task feature. Only its dependencies for now; its routes come with the task endpoints. */
fun Application.taskModule() {
    dependencies {
        provide<TaskRepository> { TaskRepositoryImpl() }
    }
}
