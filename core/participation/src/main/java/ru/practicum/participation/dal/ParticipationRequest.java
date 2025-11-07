package ru.practicum.participation.dal;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.interaction.dto.participation.RequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "participation_requests")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParticipationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "requester_id", nullable = false)
    Long requesterId;

    @Column(name = "event_id", nullable = false)
    Long eventId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    RequestStatus status;

    @Column(name = "created")
    LocalDateTime created = LocalDateTime.now();
}
