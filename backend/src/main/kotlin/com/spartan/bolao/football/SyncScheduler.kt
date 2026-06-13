package com.spartan.bolao.football

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Dispara a sincronização periodicamente. Fica num componente à parte para que a
 * chamada a `sync()` passe pelo proxy do Spring (senão o @Transactional não vale).
 * Intervalo padrão de 60s respeita folgadamente o limite de 10 req/min do plano grátis.
 */
@Component
class SyncScheduler(
    private val syncService: WorldCupSyncService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        initialDelayString = "\${app.football.initial-delay-ms:8000}",
        fixedDelayString = "\${app.football.poll-ms:60000}",
    )
    fun tick() {
        try {
            syncService.sync()
        } catch (e: Exception) {
            log.warn("Ciclo de sync falhou: {}", e.message)
        }
    }
}
