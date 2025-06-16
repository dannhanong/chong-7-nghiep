package com.dan.job_service.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.dtos.enums.ApplicationStatus;
import com.dan.job_service.models.Job;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobRepository extends MongoRepository<Job, String> {
    Integer countByCategoryId(String categoryId);

    List<Job> findByCreatedAtBetweenAndActiveTrue(LocalDateTime start, LocalDateTime end);
    Page<Job> findByActiveTrue(Pageable pageable);
    Page<Job> findByCategoryIdAndActiveTrue(String categoryId, Pageable pageable);
    Page<Job> findByTitleContainingIgnoreCaseAndActiveTrue(String title, Pageable pageable);
    Page<Job> findByCategoryIdAndTitleContainingIgnoreCaseAndActiveTrue(String categoryId, String title, Pageable pageable);
    // Thêm phương thức mới để đếm số job theo userId và active = true
    Integer countByUserIdAndActiveTrue(String userId);
    Page<Job> findJobsByUserIdAndActiveTrue(String userId, Pageable pageable);

    // thêm phương thức để tìm kiếm công việc theo danh mục không truyền phân trang
    List<Job> findByCategoryIdAndActiveTrue(String categoryId);

    // Thêm tổng số job mà người dùng đã làm hoàn thành


}
