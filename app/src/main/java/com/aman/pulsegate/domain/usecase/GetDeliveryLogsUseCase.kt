package com.aman.pulsegate.domain.usecase

import com.aman.pulsegate.domain.model.DeliveryLog
import com.aman.pulsegate.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

class GetDeliveryLogsUseCase @Inject constructor(
    private val logRepository: LogRepository
) {

    // Used by LogsViewModel — reactive stream of recent logs for the UI
    fun observeRecent(limit: Int = DEFAULT_PAGE_SIZE): Flow<List<DeliveryLog>> {
        return logRepository.observeRecentLogs(limit)
    }

    // Used by LogsViewModel — paginated fetch for load-more
    suspend fun getPaged(page: Int, pageSize: Int = DEFAULT_PAGE_SIZE): Result<List<DeliveryLog>> {
        if (page < 0) return Result.failure(IllegalArgumentException("Page must be >= 0"))
        if (pageSize !in 1..MAX_PAGE_SIZE)
            return Result.failure(IllegalArgumentException("Page size must be between 1 and $MAX_PAGE_SIZE"))

        val offset = page * pageSize
        return runCatching {
            logRepository.getLogsPaged(limit = pageSize, offset = offset)
        }.onFailure { error ->
            Timber.e(error, "GetDeliveryLogsUseCase: failed to fetch page=$page")
        }
    }

    // Used by LogsViewModel — fetch all logs for a specific event detail screen
    suspend fun getForEvent(eventId: Long): Result<List<DeliveryLog>> {
        if (eventId <= 0) return Result.failure(IllegalArgumentException("Invalid eventId=$eventId"))

        return runCatching {
            logRepository.getLogsForEvent(eventId)
        }.onFailure { error ->
            Timber.e(error, "GetDeliveryLogsUseCase: failed to fetch logs for eventId=$eventId")
        }
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 30
        private const val MAX_PAGE_SIZE = 100
    }
}