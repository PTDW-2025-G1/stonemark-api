package pt.estga.shared.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AuditActor {

    private Long id;

    private String identifier;

}
