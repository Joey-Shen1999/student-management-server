package com.studentmanagement.studentmanagementserver.domain.university;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(
        name = "universities",
        indexes = {
                @Index(name = "idx_universities_active_name", columnList = "active,name"),
                @Index(name = "idx_universities_province_city", columnList = "province,city")
        }
)
public class University extends BaseEntity {

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 80)
    private String province;

    @Column(length = 120)
    private String city;

    @Column(nullable = false, length = 80)
    private String country = "Canada";

    @Column(length = 255)
    private String website;

    @Column(nullable = false)
    private boolean active = true;

    protected University() {
    }

    public University(String name, String province, String city, String country, String website) {
        this.name = name;
        this.province = province;
        this.city = city;
        this.country = normalizeCountry(country);
        this.website = website;
        this.active = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = normalizeCountry(country);
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private String normalizeCountry(String country) {
        String normalized = country == null ? "" : country.trim();
        return normalized.isEmpty() ? "Canada" : normalized;
    }
}
