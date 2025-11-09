package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.entity.Subscribe;

import java.util.List;

public interface SubscribeRepository extends JpaRepository<Subscribe, Long> {
    void deleteByFollowerAndFollowedTo(Long followerUserId, Long followedToUserId);

    //List<Subscribe> findSubscribesByFollower_IdIs(Long followerId);

    @Query("""
            select followedTo
            from Subscribe
            where follower = ?1""")
    List<Long> findFollowedUsersIds(Long followerUserId);
}
