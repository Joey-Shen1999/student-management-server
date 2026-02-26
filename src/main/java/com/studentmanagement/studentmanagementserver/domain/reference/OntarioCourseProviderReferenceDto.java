package com.studentmanagement.studentmanagementserver.domain.reference;

public class OntarioCourseProviderReferenceDto {

    private final String id;
    private final String name;
    private final String boardName;
    private final String schoolSpecialConditions;
    private final String streetAddress;
    private final String city;
    private final String state;
    private final String country;
    private final String postal;

    public OntarioCourseProviderReferenceDto(String id,
                                             String name,
                                             String boardName,
                                             String schoolSpecialConditions,
                                             String streetAddress,
                                             String city,
                                             String state,
                                             String country,
                                             String postal) {
        this.id = id;
        this.name = name;
        this.boardName = boardName;
        this.schoolSpecialConditions = schoolSpecialConditions;
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postal = postal;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBoardName() {
        return boardName;
    }

    public String getSchoolSpecialConditions() {
        return schoolSpecialConditions;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public String getPostal() {
        return postal;
    }
}
