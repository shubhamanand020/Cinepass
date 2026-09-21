package com.cinepass.show.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateScreenRequest {

    @NotBlank
    @Size(max = 100)
    private String name;
    @NotBlank
    @Size(max = 150)
    private String theatreName;
    @NotNull
    @Min(1)
    private Integer rows;
    @NotNull
    @Min(1)
    private Integer seatsPerRow;
    @NotNull
    @Min(0)
    private Integer premiumRows;

    public CreateScreenRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTheatreName() {
        return theatreName;
    }

    public void setTheatreName(String theatreName) {
        this.theatreName = theatreName;
    }

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }

    public Integer getSeatsPerRow() {
        return seatsPerRow;
    }

    public void setSeatsPerRow(Integer seatsPerRow) {
        this.seatsPerRow = seatsPerRow;
    }

    public Integer getPremiumRows() {
        return premiumRows;
    }

    public void setPremiumRows(Integer premiumRows) {
        this.premiumRows = premiumRows;
    }
}
