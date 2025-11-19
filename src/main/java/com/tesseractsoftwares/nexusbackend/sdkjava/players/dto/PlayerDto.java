package com.tesseractsoftwares.nexusbackend.sdkjava.players.dto;

public class PlayerDto {

    private String uuid;
    private String email;
    private String birthdate;
    private String player_name;
    private boolean online;
    private String server_connected;

    public PlayerDto(String uuid, String email, String birthdate, String player_name, boolean online, String server_connected) {
        this.uuid = uuid;
        this.email = email;
        this.birthdate = birthdate;
        this.player_name = player_name;
        this.online = online;
        this.server_connected = server_connected;
    }

    // Getters
    public String getUuid() { return uuid; }
    public String getEmail() { return email; }
    public String getBirthdate() { return birthdate; }
    public String getPlayer_name() { return player_name; }
    public boolean isOnline() { return online; }
    public String getServer_connected() { return server_connected; }
}
