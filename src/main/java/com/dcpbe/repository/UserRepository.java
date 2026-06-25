package com.dcpbe.repository;

import com.dcpbe.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findAllByOrderByIdDesc();
//    @Query("""
//    SELECT u FROM User u
//    WHERE (:search IS NULL OR
//           LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
//           LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
//    AND (:position IS NULL OR u.position = :position)
//""")
    @Query("""
        SELECT u FROM User u
            WHERE
                (:search IS NULL
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                 )
            AND (:position IS NULL OR u.position = :position)
            AND (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
            AND (:fullName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :fullName, '%')))
            AND (:email IS NULL OR LOWER(u.email)LIKE LOWER(CONCAT('%', :email, '%')))
""")
    Page<User> searchUsers(String search, String position, String username, String fullName, String email, Pageable pageable);
}
