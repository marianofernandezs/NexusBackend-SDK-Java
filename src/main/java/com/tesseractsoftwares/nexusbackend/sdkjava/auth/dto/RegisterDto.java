package com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto;

public class RegisterDto {
    private String uuid;
    private String email;
    private String password;
    private String birthdate;
    private String player_name;

    public RegisterDto(String uuid, String email, String password, String birthdate, String player_name) {
        this.uuid = uuid;
        this.email = email;
        this.password = password;
        this.birthdate = birthdate;
        this.player_name = player_name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public String getPlayer_name() {
        return player_name;
    }
}
