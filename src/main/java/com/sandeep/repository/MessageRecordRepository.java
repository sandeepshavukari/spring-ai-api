package com.sandeep.repository;

import com.sandeep.model.MessageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRecordRepository extends JpaRepository<MessageRecord, Long> {

    List<MessageRecord> findBySessionIdOrderByTimestampAsc(Long sessionId);

    long countBySessionId(Long sessionId);

    @Transactional
    void deleteBySessionId(Long sessionId);
}
