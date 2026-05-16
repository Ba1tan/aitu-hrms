package kz.aitu.hrms.integration.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_settings", schema = "hrms_integration")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanySetting {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;
}
