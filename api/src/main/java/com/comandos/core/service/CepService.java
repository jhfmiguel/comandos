package com.comandos.core.service;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CepService {
    private final HttpClient client;
    private final String baseUrl;

    public CepService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(), "https://viacep.com.br/ws/");
    }

    CepService(HttpClient client, String baseUrl) {
        this.client = client;
        this.baseUrl = baseUrl;
    }

    public Map<String, String> lookup(String cep) {
        if (!cep.matches("[0-9]{5}-?[0-9]{3}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CEP inválido. Informe oito dígitos.");
        try {
            var request = HttpRequest.newBuilder(URI.create(baseUrl + cep.replace("-", "") + "/json/"))
                .timeout(Duration.ofSeconds(5)).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("CEP provider unavailable");
            var data = JsonMapper.builder().build().readTree(response.body());
            if (data.path("erro").asBoolean(false))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CEP não encontrado. Confira o CEP ou preencha o endereço manualmente.");
            if (data.path("localidade").asText("").isBlank() || data.path("uf").asText("").isBlank())
                throw new IllegalStateException("Invalid CEP response");
            return Map.of("street", data.path("logradouro").asText(""), "district", data.path("bairro").asText(""),
                "city", data.path("localidade").asText(""), "state", data.path("uf").asText(""),
                "complement", data.path("complemento").asText(""));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Consulta de CEP indisponível. Preencha manualmente ou tente novamente.");
        }
    }
}
