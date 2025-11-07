package ru.practicum.participation.dal;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "participation_requests")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParticipationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

/*    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "requester_id", nullable = false)
    User requester;*/

    @Column(name = "requester_id", nullable = false)
    Long requesterId;

/*    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "event_id", nullable = false)
    Event event;*/

    @Column(name = "event_id", nullable = false)
    Long eventId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    RequestStatus status;

    @Column(name = "created")
    @CreationTimestamp
    LocalDateTime created;
}
