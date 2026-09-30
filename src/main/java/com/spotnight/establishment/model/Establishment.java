package com.spotnight.establishment.model;

import com.spotnight.category.model.Category;
import com.spotnight.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "establishments")
public class Establishment {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "phone", columnDefinition = "varchar")
    private String phone;

    @Column(name = "website_url", columnDefinition = "varchar")
    private String websiteUrl;

    @Column(name = "instagram_handle", columnDefinition = "varchar")
    private String instagramHandle;

    @Column(name = "street", nullable = false, columnDefinition = "varchar")
    private String street;

    @Column(name = "number", columnDefinition = "varchar")
    private String number;

    @Column(name = "complement", columnDefinition = "varchar")
    private String complement;

    @Column(name = "neighborhood", columnDefinition = "varchar")
    private String neighborhood;

    @Column(name = "city", nullable = false, columnDefinition = "varchar")
    private String city;

    @Column(name = "state", nullable = false, columnDefinition = "varchar")
    private String state;

    @Column(name = "postal_code", columnDefinition = "varchar")
    private String postalCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "time_zone", nullable = false, columnDefinition = "varchar")
    private String timeZone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar")
    private EstablishmentStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Establishment() {}

    public Establishment(
            User owner, Category category, String name, String description,
            String phone, String websiteUrl, String instagramHandle,
            String street, String number, String complement, String neighborhood,
            String city, String state, String postalCode, String countryCode,
            double latitude, double longitude, String timeZone, Instant now) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.category = category;
        this.name = name;
        this.description = description;
        this.phone = phone;
        this.websiteUrl = websiteUrl;
        this.instagramHandle = instagramHandle;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeZone = timeZone;
        this.status = EstablishmentStatus.PENDING_REVIEW;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public User getOwner() { return owner; }
    public Category getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPhone() { return phone; }
    public String getWebsiteUrl() { return websiteUrl; }
    public String getInstagramHandle() { return instagramHandle; }
    public String getStreet() { return street; }
    public String getNumber() { return number; }
    public String getComplement() { return complement; }
    public String getNeighborhood() { return neighborhood; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getCountryCode() { return countryCode; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getTimeZone() { return timeZone; }
    public EstablishmentStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
