package com.viecinema.loyalty.scheduler;

import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.loyalty.service.LoyaltyPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler tặng điểm sinh nhật cho user.
 *
 * <p>Chạy lúc 8:00 sáng mỗi ngày, tìm tất cả user có sinh nhật hôm nay
 * và cộng điểm BONUS qua {@link LoyaltyPointsService#awardBirthdayBonus}.
 *
 * <p>Xử lý theo batch (200 user/lần) để tránh load DB quá lớn.
 * {@code awardBirthdayBonus} đã có idempotency check nên an toàn khi chạy lại.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayBonusScheduler {

    private static final int BATCH_SIZE = 200;

    private final UserRepository userRepository;
    private final LoyaltyPointsService loyaltyPointsService;

    /**
     * Cron: 0 0 8 * * ? — chạy 8:00:00 AM mỗi ngày.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void processBirthdayBonuses() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        log.info("[BirthdayScheduler] Bắt đầu tặng điểm sinh nhật — {}/{}", day, month);

        int page = 0;
        int totalProcessed = 0;

        while (true) {
            List<User> usersWithBirthday = userRepository.findUsersWithBirthdayToday(
                    month, day, PageRequest.of(page, BATCH_SIZE)
            );

            if (usersWithBirthday.isEmpty()) break;

            for (User user : usersWithBirthday) {
                try {
                    loyaltyPointsService.awardBirthdayBonus(user);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("[BirthdayScheduler] Lỗi khi tặng điểm cho user {}: {}",
                            user.getId(), e.getMessage(), e);
                }
            }

            if (usersWithBirthday.size() < BATCH_SIZE) break;
            page++;
        }

        log.info("[BirthdayScheduler] Hoàn thành — đã xử lý {} user.", totalProcessed);
    }
}
