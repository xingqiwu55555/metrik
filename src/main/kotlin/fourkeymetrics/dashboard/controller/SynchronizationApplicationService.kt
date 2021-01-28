package fourkeymetrics.dashboard.controller

import fourkeymetrics.dashboard.model.Pipeline
import fourkeymetrics.dashboard.repository.DashboardRepository
import fourkeymetrics.dashboard.service.PipelineService
import fourkeymetrics.exception.ApplicationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class SynchronizationApplicationService {
    companion object {
        private const val TWO_WEEKS_TIMESTAMP = 14 * 24 * 60 * 60 * 1000L
    }

    @Autowired
    private lateinit var pipelineService: PipelineService

    @Autowired
    private lateinit var dashboardRepository: DashboardRepository


    fun synchronize(dashboardId: String): Long? {
        if (!isDashboardExist(dashboardId)) {
            throw ApplicationException(HttpStatus.NOT_FOUND, "Dashboard ID not exist")
        }

        val lastSyncTimestamp = dashboardRepository.getLastSyncRecord(dashboardId)
        val currentTimeMillis = System.currentTimeMillis()
        val pipelines = getPipelines(dashboardId)

        var synchronizeSuccess = true
        pipelines.parallelStream().forEach {
            try {
                pipelineService.syncBuilds(dashboardId, it.id)
            } catch (e: RuntimeException) {
                synchronizeSuccess = false
            }
        }

        if (!synchronizeSuccess) {
            return lastSyncTimestamp
        }

        return dashboardRepository.updateSynchronizationTime(dashboardId, currentTimeMillis)
    }

    fun getLastSyncTimestamp(dashboardId: String): Long? {
        if (!isDashboardExist(dashboardId)) {
            throw ApplicationException(HttpStatus.NOT_FOUND, "Dashboard ID not exist")
        }

        return dashboardRepository.getLastSyncRecord(dashboardId)
    }

    private fun getPipelines(dashboardId: String): List<Pipeline> {
        return dashboardRepository.getPipelineConfiguration(dashboardId)
    }

    private fun isDashboardExist(dashboardId: String): Boolean {
        dashboardRepository.getDashBoardDetailById(dashboardId) ?: return false
        return true
    }
}