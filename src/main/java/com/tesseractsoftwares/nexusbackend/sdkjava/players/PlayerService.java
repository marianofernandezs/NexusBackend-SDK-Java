package com.tesseractsoftwares.nexusbackend.sdkjava.players;

import com.google.gson.Gson;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.NexusHttpClient;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.exceptions.NexusHttpException;
import com.tesseractsoftwares.nexusbackend.sdkjava.players.dto.PlayerDto;

public class PlayerService {

    private final NexusHttpClient http;
    private final Gson gson = new Gson();

    public PlayerService(NexusHttpClient http) {
        this.http = http;
    }

    public PlayerDto getByUuid(String uuid) throws NexusHttpException {
        String json = http.get("players/" + uuid);
        return gson.fromJson(json, PlayerDto.class);
    }

    public PlayerDto update(String uuid, PlayerDto dto) throws NexusHttpException {
        String payload = gson.toJson(dto);
        String json = http.put("players/" + uuid, payload);
        return gson.fromJson(json, PlayerDto.class);
    }

    public boolean delete(String uuid) throws NexusHttpException {
        http.delete("players/" + uuid);
        return true;
    }
}
