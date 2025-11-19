package ru.practicum.subscription.dal;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "follower_user_id", nullable = false)
    Long follower;

    @Column(name = "followed_to_user_id", nullable = false)
    Long followedTo;

    public Subscription(Long follower, Long followedTo) {
        this.follower = follower;
        this.followedTo = followedTo;
    }
}
