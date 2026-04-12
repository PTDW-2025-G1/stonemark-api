package pt.estga.review.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
public class AcceptNewMarkRequest {

    @NotBlank(message = "Mark title is required when creating a new mark")
    @Size(min = 2, max = 100, message = "Mark title must be between 2 and 100 characters")
    private String markTitle;

    private String comment;
}
