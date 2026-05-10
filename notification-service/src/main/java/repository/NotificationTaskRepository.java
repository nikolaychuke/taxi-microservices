package org.example.taxi.notification.repository;

import org.example.taxi.notification.domain.NotificationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {
    List<NotificationTask> findByTripIdOrderByCreatedAtAsc(Long tripId);

    @Query(value = "select * from notification_tasks t where t.status = 'PENDING' and t.attempts < 3 order by t.id asc limit 1 for update skip locked", nativeQuery = true)
    Optional<NotificationTask> findNextPendingTaskForUpdate();
}
