package com.example.teamfinder.repository;

import com.example.teamfinder.model.Event;
import com.example.teamfinder.model.SportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>,
        JpaSpecificationExecutor<Event> {

    /**
     * Build a Specification to filter events.
     * All parameters are optional (null = no filter applied).
     */
    static Specification<Event> withFilters(SportType sport, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (sport != null) {
                predicates.add(cb.equal(root.get("sport"), sport));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDateTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDateTime"), to));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
