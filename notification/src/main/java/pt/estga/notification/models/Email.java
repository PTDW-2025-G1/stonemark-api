package pt.estga.notification.models;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class Email {
    private String to;
    private String subject;
    private String template;
    private Map<String, Object> properties;
}
