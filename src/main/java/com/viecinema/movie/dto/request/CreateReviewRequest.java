package com.viecinema.movie.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateReviewRequest {

    @NotNull(message = "movieId là bắt buộc")
    private Integer movieId;

    @NotNull(message = "rating là bắt buộc")
    @Min(value = 1, message = "rating tối thiểu là 1")
    @Max(value = 5, message = "rating tối đa là 5")
    private Integer rating;

    @Min(1) @Max(5)
    private Integer ratingVideo;

    @Min(1) @Max(5)
    private Integer ratingAudio;

    @Min(1) @Max(5)
    private Integer ratingSubtitle;

    @Size(max = 2000, message = "Bình luận tối đa 2000 ký tự")
    private String comment;
}
