package pt.estga.notification.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pt.estga.notification.models.Email;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    public String generateBody(Email email) {
        Context context = new Context();
        context.setVariables(email.getProperties());
        return templateEngine.process(email.getTemplate(), context);
    }
}
