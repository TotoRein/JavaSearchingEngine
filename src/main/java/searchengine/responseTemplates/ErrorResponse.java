package searchengine.responseTemplates;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ErrorResponse implements ApiResponse {
    private final boolean result = false;
    private String error = "Задан пустой поисковый запрос";

    public ErrorResponse(String error) {
        this.error = error;
    }
}
